package esvar.ua.bonusbot.repository;

import esvar.ua.bonusbot.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByTelegramUserId(Long telegramUserId);
}
