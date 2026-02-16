package esvar.ua.bonusbot.service;

import esvar.ua.bonusbot.config.LimitsProperties;
import esvar.ua.bonusbot.exception.BanException;
import esvar.ua.bonusbot.model.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AntiAbuseService {
    private final LimitsProperties limitsProperties;
    private final UserService userService;

    public AntiAbuseService(LimitsProperties limitsProperties, UserService userService) {
        this.limitsProperties = limitsProperties;
        this.userService = userService;
    }

    @Transactional
    public void enforceCooldownAndBan(UserEntity user) {
        Instant now = Instant.now();
        if (user.getBanUntil() != null && now.isBefore(user.getBanUntil())) {
            throw new BanException(Duration.between(now, user.getBanUntil()));
        }

        if (user.getLastCodeAt() != null) {
            long seconds = Duration.between(user.getLastCodeAt(), now).getSeconds();
            if (seconds < limitsProperties.cooldownSeconds()) {
                Instant banUntil = now.plusSeconds(limitsProperties.banSeconds());
                user.setBanUntil(banUntil);
                userService.save(user);
                throw new BanException(Duration.between(now, banUntil));
            }
        }
    }

    @Transactional
    public void markSuccessfulRegistration(UserEntity user) {
        user.setLastCodeAt(Instant.now());
        user.setBanUntil(null);
        userService.save(user);
    }
}
