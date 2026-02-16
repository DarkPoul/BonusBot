package esvar.ua.bonusbot.model.entity;

import esvar.ua.bonusbot.model.enums.SessionState;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sessions")
public class SessionEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionState state;

    @Lob
    private String payloadJson;

    @Column(nullable = false)
    private Instant updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public SessionState getState() { return state; }
    public void setState(SessionState state) { this.state = state; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
