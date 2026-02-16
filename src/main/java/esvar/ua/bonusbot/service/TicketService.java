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
import java.util.List;

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

        long registered = ticketRepository.countByCampaign_Id(campaign.getId());
        if (registered >= campaign.getMaxCodes()) {
            campaignService.finish(campaign);
            throw new BotException("Ліміт кодів вже вичерпано, акцію завершено.");
        }

        TicketEntity ticket = new TicketEntity();
        ticket.setCampaign(campaign);
        ticket.setUser(user);
        ticket.setCode(code);
        ticket.setCreatedAt(Instant.now());
        try {
            ticket = ticketRepository.saveAndFlush(ticket);
        } catch (DataIntegrityViolationException e) {
            throw new BotException("Цей код уже зареєстровано в поточній акції.");
        }

        long after = ticketRepository.countByCampaign_Id(campaign.getId());
        if (after >= campaign.getMaxCodes() && campaign.getStatus() == CampaignStatus.RUNNING) {
            campaignService.finish(campaign);
        }
        return ticket;
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
        return ticketRepository.countByCampaign_Id(campaign.getId());
    }

    @Transactional(readOnly = true)
    public long countRemaining(CampaignEntity campaign) {
        return campaign.getMaxCodes() - countRegistered(campaign);
    }
}
