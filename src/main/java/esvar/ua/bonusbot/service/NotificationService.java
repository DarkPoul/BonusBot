package esvar.ua.bonusbot.service;

import esvar.ua.bonusbot.bot.BonusTelegramBot;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class NotificationService {
    private final BonusTelegramBot bot;

    public NotificationService(@Lazy BonusTelegramBot bot) {
        this.bot = bot;
    }

    public void safeSend(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException ignored) {
        }
    }
}
