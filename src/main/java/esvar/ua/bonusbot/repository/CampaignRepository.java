package esvar.ua.bonusbot.repository;

import esvar.ua.bonusbot.model.entity.CampaignEntity;
import esvar.ua.bonusbot.model.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> {
    Optional<CampaignEntity> findTopByOrderByCreatedAtDesc();
    Optional<CampaignEntity> findByStatus(CampaignStatus status);
}
