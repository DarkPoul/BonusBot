package esvar.ua.bonusbot.model.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "tickets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_campaign_code", columnNames = {"campaign_id", "code"})
})
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private CampaignEntity campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false, length = 4)
    private String code;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public CampaignEntity getCampaign() { return campaign; }
    public void setCampaign(CampaignEntity campaign) { this.campaign = campaign; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
