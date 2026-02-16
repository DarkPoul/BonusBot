package esvar.ua.bonusbot.repository;

import esvar.ua.bonusbot.model.entity.WinnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WinnerRepository extends JpaRepository<WinnerEntity, Long> {
}
