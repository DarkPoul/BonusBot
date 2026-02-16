package esvar.ua.bonusbot.repository;

import esvar.ua.bonusbot.model.entity.TicketEntity;
import esvar.ua.bonusbot.service.dto.TopParticipantRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
    long countByCampaign_Id(Long campaignId);
    long countByCampaign_IdAndUser_Id(Long campaignId, Long userId);
    List<TicketEntity> findByCampaign_IdAndUser_IdOrderByCreatedAtAsc(Long campaignId, Long userId);
    List<TicketEntity> findByCampaign_Id(Long campaignId);

    @Query("select count(distinct t.user.id) from TicketEntity t where t.campaign.id = :campaignId")
    long countUniqueParticipants(Long campaignId);

    @Query("select new esvar.ua.bonusbot.service.dto.TopParticipantRow(t.user.id, t.user.displayName, t.user.chatId, count(t.id)) " +
            "from TicketEntity t where t.campaign.id = :campaignId group by t.user.id, t.user.displayName, t.user.chatId order by count(t.id) desc")
    List<TopParticipantRow> findTopParticipants(Long campaignId);
}
