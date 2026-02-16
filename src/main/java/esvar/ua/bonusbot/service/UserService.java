package esvar.ua.bonusbot.service;

import esvar.ua.bonusbot.model.entity.UserEntity;
import esvar.ua.bonusbot.model.enums.UserRole;
import esvar.ua.bonusbot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity getOrCreateByTelegram(Long telegramUserId, Long chatId, String name) {
        UserEntity user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        if (user == null) {
            user = new UserEntity();
            user.setTelegramUserId(telegramUserId);
            user.setChatId(chatId);
            user.setDisplayName(name);
            user.setRole(UserRole.CLIENT);
            user.setCreatedAt(Instant.now());
            return userRepository.save(user);
        }
        user.setChatId(chatId);
        user.setDisplayName(name);
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity setRole(UserEntity user, UserRole role) {
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }
}
