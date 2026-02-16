package esvar.ua.bonusbot;

import esvar.ua.bonusbot.config.AdminProperties;
import esvar.ua.bonusbot.config.BotProperties;
import esvar.ua.bonusbot.config.LimitsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BotProperties.class, AdminProperties.class, LimitsProperties.class})
public class BonusBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BonusBotApplication.class, args);
    }
}
