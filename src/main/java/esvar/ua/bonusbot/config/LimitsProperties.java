package esvar.ua.bonusbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "limits.code")
public record LimitsProperties(long cooldownSeconds, long banSeconds) {
}
