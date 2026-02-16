package esvar.ua.bonusbot.service;

import esvar.ua.bonusbot.exception.BotException;
import esvar.ua.bonusbot.model.entity.CampaignEntity;
import esvar.ua.bonusbot.model.entity.TicketEntity;
import esvar.ua.bonusbot.model.entity.WinnerEntity;
import esvar.ua.bonusbot.model.enums.CampaignStatus;
import esvar.ua.bonusbot.repository.TicketRepository;
import esvar.ua.bonusbot.repository.WinnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class WinnerService {
    private final WinnerRepository winnerRepository;
    private final TicketRepository ticketRepository;
    private final CampaignService campaignService;
    private final NotificationService notificationService;

    public WinnerService(WinnerRepository winnerRepository,
                         TicketRepository ticketRepository,
                         CampaignService campaignService,
                         NotificationService notificationService) {
        this.winnerRepository = winnerRepository;
        this.ticketRepository = ticketRepository;
        this.campaignService = campaignService;
        this.notificationService = notificationService;
    }

    @Transactional
    public WinnerEntity pickWinner(CampaignEntity campaign) {
        if (campaign.getStatus() != CampaignStatus.STOPPED && campaign.getStatus() != CampaignStatus.FINISHED) {
            throw new BotException("Переможця можна обрати лише після зупинки або завершення акції.");
        }

        WinnerEntity existing = winnerRepository.findById(campaign.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        List<TicketEntity> tickets = ticketRepository.findByCampaign_Id(campaign.getId());
        if (tickets.isEmpty()) {
            throw new BotException("Немає зареєстрованих квитків для вибору переможця.");
        }

        TicketEntity winnerTicket = tickets.get(new Random().nextInt(tickets.size()));
        WinnerEntity winner = new WinnerEntity();
        winner.setCampaignId(campaign.getId());
        winner.setTicket(winnerTicket);
        winner.setUser(winnerTicket.getUser());
        winner.setPickedAt(Instant.now());
        winnerRepository.save(winner);

        campaignService.finish(campaign);
        notifyWinnerAndParticipants(campaign, winner, tickets);
        return winner;
    }

    public void notifyWinnerAndParticipants(CampaignEntity campaign, WinnerEntity winner, List<TicketEntity> tickets) {
        long winnerTickets = tickets.stream().filter(t -> Objects.equals(t.getUser().getId(), winner.getUser().getId())).count();
        String winnerName = winner.getUser().getDisplayName() == null ? "Користувач" : winner.getUser().getDisplayName();

        notificationService.safeSend(winner.getUser().getChatId(),
                "🎉 Вітаємо! Ви перемогли в акції \"" + campaign.getName() + "\". Приз: " + campaign.getPrizeProduct() +
                        ". Зв'яжіться з організатором для отримання призу.");

        Set<Long> participantUserIds = new HashSet<>();
        for (TicketEntity ticket : tickets) {
            if (participantUserIds.add(ticket.getUser().getId())) {
                notificationService.safeSend(ticket.getUser().getChatId(),
                        "🏁 Акцію \"" + campaign.getName() + "\" завершено. Переможець: " + winnerName +
                                ". У переможця квитків: " + winnerTickets + ".");
            }
        }
    }
}
