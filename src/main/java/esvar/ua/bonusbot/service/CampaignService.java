package esvar.ua.bonusbot.service;

import esvar.ua.bonusbot.exception.BotException;
import esvar.ua.bonusbot.model.entity.CampaignEntity;
import esvar.ua.bonusbot.model.enums.CampaignStatus;
import esvar.ua.bonusbot.repository.CampaignRepository;
import esvar.ua.bonusbot.repository.TicketRepository;
import esvar.ua.bonusbot.service.dto.TopParticipantRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class CampaignService {
    private final CampaignRepository campaignRepository;
    private final TicketRepository ticketRepository;

    public CampaignService(CampaignRepository campaignRepository, TicketRepository ticketRepository) {
        this.campaignRepository = campaignRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public CampaignEntity getCurrent() {
        return campaignRepository.findTopByOrderByCreatedAtDesc().orElse(null);
    }

    @Transactional(readOnly = true)
    public CampaignEntity getRunning() {
        return campaignRepository.findByStatus(CampaignStatus.RUNNING).orElse(null);
    }

    @Transactional
    public CampaignEntity createDraft(Map<String, Object> fields) {
        CampaignEntity campaign = new CampaignEntity();
        campaign.setStatus(CampaignStatus.DRAFT);
        applyFields(campaign, fields);
        campaign.setCreatedAt(Instant.now());
        return campaignRepository.save(campaign);
    }

    @Transactional
    public CampaignEntity updateFields(CampaignEntity campaign, Map<String, Object> fields) {
        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            throw new BotException("Не можна редагувати активну акцію.");
        }
        CampaignEntity managed = campaignRepository.findById(campaign.getId()).orElseThrow(() -> new BotException("Акцію не знайдено."));
        applyFields(managed, fields);
        return campaignRepository.save(managed);
    }

    @Transactional
    public CampaignEntity start(CampaignEntity campaign) {
        if (getRunning() != null && campaign.getStatus() != CampaignStatus.RUNNING) {
            throw new BotException("Вже є запущена акція.");
        }
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.STOPPED) {
            throw new BotException("Запуск дозволено тільки для DRAFT або STOPPED.");
        }
        CampaignEntity managed = campaignRepository.findById(campaign.getId()).orElseThrow(() -> new BotException("Акцію не знайдено."));
        managed.setStatus(CampaignStatus.RUNNING);
        managed.setStartedAt(Instant.now());
        return campaignRepository.save(managed);
    }

    @Transactional
    public CampaignEntity stop(CampaignEntity campaign) {
        if (campaign.getStatus() != CampaignStatus.RUNNING) {
            throw new BotException("Немає активної акції для зупинки.");
        }
        CampaignEntity managed = campaignRepository.findById(campaign.getId()).orElseThrow(() -> new BotException("Акцію не знайдено."));
        managed.setStatus(CampaignStatus.STOPPED);
        managed.setFinishedAt(Instant.now());
        return campaignRepository.save(managed);
    }

    @Transactional
    public CampaignEntity finish(CampaignEntity campaign) {
        CampaignEntity managed = campaignRepository.findById(campaign.getId()).orElseThrow(() -> new BotException("Акцію не знайдено."));
        managed.setStatus(CampaignStatus.FINISHED);
        if (managed.getFinishedAt() == null) {
            managed.setFinishedAt(Instant.now());
        }
        return campaignRepository.save(managed);
    }

    @Transactional(readOnly = true)
    public long registeredCount(Long campaignId) {
        return ticketRepository.countByCampaign_IdAndUserIsNotNull(campaignId);
    }

    @Transactional(readOnly = true)
    public long remainingCount(CampaignEntity campaign) {
        return campaign.getMaxCodes() - registeredCount(campaign.getId());
    }

    @Transactional(readOnly = true)
    public long uniqueParticipants(Long campaignId) {
        return ticketRepository.countUniqueParticipants(campaignId);
    }

    @Transactional(readOnly = true)
    public List<TopParticipantRow> topParticipants(Long campaignId, int max) {
        return ticketRepository.findTopParticipants(campaignId).stream().limit(max).toList();
    }

    private void applyFields(CampaignEntity campaign, Map<String, Object> fields) {
        campaign.setName(asString(fields.get("name")));
        campaign.setPrizeProduct(asString(fields.get("prizeProduct")));
        campaign.setPromoProductText(asString(fields.get("promoProductText")));
        campaign.setDescription(asString(fields.get("description")));
        campaign.setRules(asString(fields.get("rules")));
        campaign.setMaxCodes(Integer.parseInt(asString(fields.get("maxCodes"))));
    }

    private String asString(Object value) {
        if (value == null) {
            throw new BotException("Відсутні обов'язкові поля акції.");
        }
        return String.valueOf(value).trim();
    }
}
