package esvar.ua.bonusbot.repository;

import esvar.ua.bonusbot.model.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
}
