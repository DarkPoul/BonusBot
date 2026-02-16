package esvar.ua.bonusbot.model.entity;

import esvar.ua.bonusbot.model.enums.UserRole;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long telegramUserId;

    @Column(nullable = false)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastCodeAt;
    private Instant banUntil;
    private String displayName;

    public Long getId() { return id; }
    public Long getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(Long telegramUserId) { this.telegramUserId = telegramUserId; }
    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastCodeAt() { return lastCodeAt; }
    public void setLastCodeAt(Instant lastCodeAt) { this.lastCodeAt = lastCodeAt; }
    public Instant getBanUntil() { return banUntil; }
    public void setBanUntil(Instant banUntil) { this.banUntil = banUntil; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
