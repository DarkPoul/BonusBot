package esvar.ua.bonusbot.model.entity;

import esvar.ua.bonusbot.model.enums.CampaignStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "campaigns")
public class CampaignEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String prizeProduct;

    @Column(nullable = false, length = 2000)
    private String promoProductText;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 4000)
    private String rules;

    @Column(nullable = false)
    private Integer maxCodes;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;
    private Instant finishedAt;

    public Long getId() { return id; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrizeProduct() { return prizeProduct; }
    public void setPrizeProduct(String prizeProduct) { this.prizeProduct = prizeProduct; }
    public String getPromoProductText() { return promoProductText; }
    public void setPromoProductText(String promoProductText) { this.promoProductText = promoProductText; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }
    public Integer getMaxCodes() { return maxCodes; }
    public void setMaxCodes(Integer maxCodes) { this.maxCodes = maxCodes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
