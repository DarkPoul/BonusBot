package esvar.ua.bonusbot.exception;

import java.time.Duration;

public class BanException extends RuntimeException {
    private final Duration remaining;

    public BanException(Duration remaining) {
        super("User is banned");
        this.remaining = remaining;
    }

    public Duration getRemaining() {
        return remaining;
    }
}
