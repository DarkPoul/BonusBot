package esvar.ua.bonusbot.model.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "winners")
public class WinnerEntity {
    @Id
    @Column(name = "campaign_id")
    private Long campaignId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)
    private CampaignEntity campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private TicketEntity ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private Instant pickedAt;

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public CampaignEntity getCampaign() { return campaign; }
    public TicketEntity getTicket() { return ticket; }
    public void setTicket(TicketEntity ticket) { this.ticket = ticket; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public Instant getPickedAt() { return pickedAt; }
    public void setPickedAt(Instant pickedAt) { this.pickedAt = pickedAt; }
}
