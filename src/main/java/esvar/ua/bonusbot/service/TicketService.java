package esvar.ua.bonusbot.service;

import esvar.ua.bonusbot.exception.BotException;
import esvar.ua.bonusbot.model.entity.CampaignEntity;
import esvar.ua.bonusbot.model.entity.TicketEntity;
import esvar.ua.bonusbot.model.entity.UserEntity;
import esvar.ua.bonusbot.model.enums.CampaignStatus;
import esvar.ua.bonusbot.repository.TicketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TicketService {
    private final CampaignService campaignService;
    private final TicketRepository ticketRepository;

    public TicketService(CampaignService campaignService, TicketRepository ticketRepository) {
        this.campaignService = campaignService;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketEntity registerTicket(UserEntity user, String code) {
        CampaignEntity campaign = campaignService.getRunning();
        if (campaign == null) {
            throw new BotException("Наразі немає активної акції.");
        }

        Optional<TicketEntity> candidate = ticketRepository.findByCampaign_IdAndCode(campaign.getId(), code);
        if (candidate.isEmpty()) {
            throw new BotException("Такого коду немає в поточній акції.");
        }

        TicketEntity ticket = candidate.get();
        if (ticket.getUser() != null) {
            throw new BotException("Цей код уже активовано.");
        }

        ticket.setUser(user);
        ticket.setCreatedAt(Instant.now());
        try {
            ticket = ticketRepository.saveAndFlush(ticket);
        } catch (DataIntegrityViolationException e) {
            throw new BotException("Не вдалося активувати код. Спробуйте ще раз.");
        }

        long after = ticketRepository.countByCampaign_IdAndUserIsNotNull(campaign.getId());
        if (after >= campaign.getMaxCodes() && campaign.getStatus() == CampaignStatus.RUNNING) {
            campaignService.finish(campaign);
        }
        return ticket;
    }

    @Transactional
    public void generateCodesForCampaign(CampaignEntity campaign) {
        int maxCodes = campaign.getMaxCodes();
        validateMaxCodes(maxCodes);

        List<String> codes = generateUniqueFourDigitCodes(maxCodes);
        List<TicketEntity> tickets = new ArrayList<>(maxCodes);
        Instant now = Instant.now();
        for (String code : codes) {
            TicketEntity ticket = new TicketEntity();
            ticket.setCampaign(campaign);
            ticket.setCode(code);
            ticket.setCreatedAt(now);
            tickets.add(ticket);
        }
        ticketRepository.saveAll(tickets);
    }

    @Transactional(readOnly = true)
    public List<TicketEntity> listCampaignCodes(CampaignEntity campaign) {
        return ticketRepository.findByCampaign_IdOrderByCodeAsc(campaign.getId());
    }

    @Transactional
    public void syncGeneratedCodes(CampaignEntity campaign) {
        int requiredCodes = campaign.getMaxCodes();
        validateMaxCodes(requiredCodes);

        List<TicketEntity> existing = ticketRepository.findByCampaign_IdOrderByCodeAsc(campaign.getId());
        int activated = (int) existing.stream().filter(t -> t.getUser() != null).count();
        if (requiredCodes < activated) {
            throw new BotException("Не можна встановити ліміт менше ніж кількість вже активованих кодів.");
        }

        if (existing.size() == requiredCodes) {
            return;
        }

        if (existing.size() < requiredCodes) {
            Set<String> usedCodes = new HashSet<>();
            existing.forEach(t -> usedCodes.add(t.getCode()));
            List<String> allCodes = generateUniqueFourDigitCodes(10_000);
            Instant now = Instant.now();
            List<TicketEntity> toAdd = new ArrayList<>();
            for (String code : allCodes) {
                if (usedCodes.contains(code)) {
                    continue;
                }
                TicketEntity ticket = new TicketEntity();
                ticket.setCampaign(campaign);
                ticket.setCode(code);
                ticket.setCreatedAt(now);
                toAdd.add(ticket);
                if (existing.size() + toAdd.size() >= requiredCodes) {
                    break;
                }
            }
            ticketRepository.saveAll(toAdd);
            return;
        }

        int toRemove = existing.size() - requiredCodes;
        List<TicketEntity> removable = existing.stream().filter(t -> t.getUser() == null).limit(toRemove).toList();
        if (removable.size() < toRemove) {
            throw new BotException("Недостатньо неактивованих кодів для зменшення ліміту.");
        }
        ticketRepository.deleteAll(removable);
    }

    @Transactional(readOnly = true)
    public List<TicketEntity> listUserTicketsForCurrentCampaign(UserEntity user) {
        CampaignEntity current = campaignService.getCurrent();
        if (current == null) {
            return List.of();
        }
        return ticketRepository.findByCampaign_IdAndUser_IdOrderByCreatedAtAsc(current.getId(), user.getId());
    }

    @Transactional(readOnly = true)
    public long countRegistered(CampaignEntity campaign) {
        return ticketRepository.countByCampaign_IdAndUserIsNotNull(campaign.getId());
    }

    @Transactional(readOnly = true)
    public long countRemaining(CampaignEntity campaign) {
        return campaign.getMaxCodes() - countRegistered(campaign);
    }

    private List<String> generateUniqueFourDigitCodes(int count) {
        List<String> pool = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            pool.add(String.format("%04d", i));
        }
        Collections.shuffle(pool);
        return pool.subList(0, count);
    }

    private void validateMaxCodes(int maxCodes) {
        if (maxCodes <= 0 || maxCodes > 10_000) {
            throw new BotException("Ліміт кодів має бути в межах 1..10000.");
        }
    }
}
