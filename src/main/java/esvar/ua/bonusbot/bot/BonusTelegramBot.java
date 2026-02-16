package esvar.ua.bonusbot.bot;

import esvar.ua.bonusbot.config.AdminProperties;
import esvar.ua.bonusbot.config.BotProperties;
import esvar.ua.bonusbot.exception.BanException;
import esvar.ua.bonusbot.exception.BotException;
import esvar.ua.bonusbot.model.entity.CampaignEntity;
import esvar.ua.bonusbot.model.entity.SessionEntity;
import esvar.ua.bonusbot.model.entity.TicketEntity;
import esvar.ua.bonusbot.model.entity.UserEntity;
import esvar.ua.bonusbot.model.entity.WinnerEntity;
import esvar.ua.bonusbot.model.enums.CampaignStatus;
import esvar.ua.bonusbot.model.enums.SessionState;
import esvar.ua.bonusbot.model.enums.UserRole;
import esvar.ua.bonusbot.service.*;
import esvar.ua.bonusbot.service.dto.TopParticipantRow;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class BonusTelegramBot extends TelegramLongPollingBot {
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{4}$");
    private static final List<String> WIZARD_FIELDS = List.of("name", "prizeProduct", "promoProductText", "description", "rules", "maxCodes");
    private static final List<String> WIZARD_PROMPTS = List.of(
            "Введіть назву акції:",
            "Введіть приз (товар):",
            "Введіть текст промо-продукту:",
            "Введіть опис акції:",
            "Введіть правила акції:",
            "Введіть ліміт кодів (ціле число):"
    );

    private final BotProperties botProperties;
    private final AdminProperties adminProperties;
    private final UserService userService;
    private final SessionService sessionService;
    private final CampaignService campaignService;
    private final TicketService ticketService;
    private final WinnerService winnerService;
    private final AntiAbuseService antiAbuseService;

    public BonusTelegramBot(BotProperties botProperties,
                            AdminProperties adminProperties,
                            UserService userService,
                            SessionService sessionService,
                            CampaignService campaignService,
                            TicketService ticketService,
                            WinnerService winnerService,
                            AntiAbuseService antiAbuseService) {
        super(botProperties.token());
        this.botProperties = botProperties;
        this.adminProperties = adminProperties;
        this.userService = userService;
        this.sessionService = sessionService;
        this.campaignService = campaignService;
        this.ticketService = ticketService;
        this.winnerService = winnerService;
        this.antiAbuseService = antiAbuseService;
    }

    @Override
    public String getBotUsername() {
        return botProperties.username();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        Long telegramUserId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        String name = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : message.getFrom().getFirstName();

        UserEntity user = userService.getOrCreateByTelegram(telegramUserId, chatId, name);
        SessionEntity session = sessionService.get(user.getId());

        try {
            if ("/start".equalsIgnoreCase(text)) {
                handleStart(user);
                return;
            }
            if ("/admin".equalsIgnoreCase(text)) {
                handleAdminCommand(user);
                return;
            }

            if (session != null && session.getState() == SessionState.AWAIT_ADMIN_PIN) {
                handleAdminPin(user, text, session);
                return;
            }
            if (session != null && session.getState() == SessionState.AWAIT_CODE) {
                handleCodeInput(user, text);
                return;
            }
            if (session != null && session.getState() == SessionState.AWAIT_CAMPAIGN_WIZARD) {
                handleCampaignWizard(user, text, session);
                return;
            }

            routeByMenu(user, text);
        } catch (BotException e) {
            sendText(chatId, e.getMessage(), menuFor(user));
        }
    }

    private void handleStart(UserEntity user) {
        sessionService.clear(user.getId());
        sendText(user.getChatId(), "Вітаємо! Це бонус-бот. Тут ви отримуєте повідомлення про розіграші та реєструєте бонус-коди.", menuFor(user));
    }

    private void handleAdminCommand(UserEntity user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attempts", 0);
        sessionService.set(user.getId(), SessionState.AWAIT_ADMIN_PIN, payload);
        sendText(user.getChatId(), "Введіть PIN адміністратора:", null);
    }

    private void handleAdminPin(UserEntity user, String pin, SessionEntity session) {
        Map<String, Object> payload = new HashMap<>(sessionService.getPayload(session));
        int attempts = ((Number) payload.getOrDefault("attempts", 0)).intValue();
        if (adminProperties.pin().equals(pin)) {
            userService.setRole(user, UserRole.ADMIN);
            sessionService.clear(user.getId());
            sendText(user.getChatId(), "Доступ ADMIN надано.", menuFor(user));
            return;
        }

        attempts++;
        if (attempts >= 3) {
            sessionService.clear(user.getId());
            sendText(user.getChatId(), "Невірний PIN. Спроби вичерпано.", menuFor(user));
        } else {
            payload.put("attempts", attempts);
            sessionService.set(user.getId(), SessionState.AWAIT_ADMIN_PIN, payload);
            sendText(user.getChatId(), "Невірний PIN. Залишилось спроб: " + (3 - attempts), null);
        }
    }

    private void routeByMenu(UserEntity user, String text) {
        switch (text) {
            case "Зареєструвати код" -> {
                sessionService.set(user.getId(), SessionState.AWAIT_CODE, Collections.emptyMap());
                sendText(user.getChatId(), "Введіть 4-значний код:", null);
            }
            case "Активна акція" -> sendActiveCampaign(user);
            case "Мої квитки" -> sendMyTickets(user);
            case "➕ Створити акцію" -> adminCreateCampaign(user);
            case "✏️ Редагувати акцію" -> adminEditCampaign(user);
            case "📊 Статистика" -> adminStats(user);
            case "▶️ Запустити" -> adminStart(user);
            case "⏸ Зупинити" -> adminStop(user);
            case "🏆 Обрати переможця" -> adminPickWinner(user);
            default -> sendText(user.getChatId(), "Оберіть дію з меню.", menuFor(user));
        }
    }

    private void handleCodeInput(UserEntity user, String text) {
        if (!CODE_PATTERN.matcher(text).matches()) {
            sendText(user.getChatId(), "Код має бути у форматі 4 цифри. Спробуйте ще раз:", null);
            return;
        }

        try {
            antiAbuseService.enforceCooldownAndBan(user);
            ticketService.registerTicket(user, text);
            antiAbuseService.markSuccessfulRegistration(user);
            sessionService.clear(user.getId());
            sendText(user.getChatId(), "Код успішно зареєстровано ✅", menuFor(user));
        } catch (BanException banException) {
            long sec = Math.max(1, banException.getRemaining().getSeconds());
            sendText(user.getChatId(), "Тимчасове блокування на реєстрацію коду. Залишилось: " + sec + " с.", null);
        }
    }

    private void sendActiveCampaign(UserEntity user) {
        CampaignEntity campaign = campaignService.getCurrent();
        if (campaign == null) {
            sendText(user.getChatId(), "Акцій поки немає.", menuFor(user));
            return;
        }

        long registered = campaignService.registeredCount(campaign.getId());
        long remaining = campaignService.remainingCount(campaign);
        String text = "📣 *" + campaign.getName() + "*\n" +
                "Статус: " + campaign.getStatus() + "\n" +
                "Приз: " + campaign.getPrizeProduct() + "\n" +
                "Промо-продукт: " + campaign.getPromoProductText() + "\n" +
                "Опис: " + campaign.getDescription() + "\n" +
                "Правила: " + campaign.getRules() + "\n" +
                "Ліміт кодів: " + campaign.getMaxCodes() + "\n" +
                "Зареєстровано: " + registered + "\n" +
                "Залишилось: " + remaining;
        sendText(user.getChatId(), text, menuFor(user));
    }

    private void sendMyTickets(UserEntity user) {
        CampaignEntity campaign = campaignService.getCurrent();
        if (campaign == null) {
            sendText(user.getChatId(), "Немає кампанії для перегляду квитків.", menuFor(user));
            return;
        }

        List<TicketEntity> tickets = ticketService.listUserTicketsForCurrentCampaign(user);
        if (tickets.isEmpty()) {
            sendText(user.getChatId(), "У поточній акції у вас ще немає квитків.", menuFor(user));
            return;
        }

        String codes = tickets.stream().map(TicketEntity::getCode).reduce((a, b) -> a + "\n" + b).orElse("");
        sendText(user.getChatId(), "Ваших квитків: " + tickets.size() + "\nКоди:\n" + codes, menuFor(user));
    }

    private void adminCreateCampaign(UserEntity user) {
        requireAdmin(user);
        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "CREATE");
        payload.put("step", 0);
        payload.put("fields", new HashMap<String, Object>());
        sessionService.set(user.getId(), SessionState.AWAIT_CAMPAIGN_WIZARD, payload);
        sendText(user.getChatId(), "Створення акції. " + WIZARD_PROMPTS.get(0), null);
    }

    @SuppressWarnings("unchecked")
    private void handleCampaignWizard(UserEntity user, String text, SessionEntity session) {
        requireAdmin(user);
        Map<String, Object> payload = new HashMap<>(sessionService.getPayload(session));
        int step = ((Number) payload.getOrDefault("step", 0)).intValue();
        Map<String, Object> fields = (Map<String, Object>) payload.getOrDefault("fields", new HashMap<>());

        if (WIZARD_FIELDS.get(step).equals("maxCodes")) {
            try {
                int maxCodes = Integer.parseInt(text);
                if (maxCodes <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                sendText(user.getChatId(), "maxCodes має бути додатним цілим числом. Спробуйте ще раз:", null);
                return;
            }
        }

        fields.put(WIZARD_FIELDS.get(step), text);
        step++;

        if (step >= WIZARD_FIELDS.size()) {
            String mode = String.valueOf(payload.get("mode"));
            if ("CREATE".equals(mode)) {
                campaignService.createDraft(fields);
                sendText(user.getChatId(), "Акцію створено у статусі DRAFT.", menuFor(user));
            } else {
                CampaignEntity current = campaignService.getCurrent();
                if (current == null) {
                    sendText(user.getChatId(), "Акції для редагування не знайдено.", menuFor(user));
                } else {
                    campaignService.updateFields(current, fields);
                    sendText(user.getChatId(), "Акцію оновлено.", menuFor(user));
                }
            }
            sessionService.clear(user.getId());
            return;
        }

        payload.put("step", step);
        payload.put("fields", fields);
        sessionService.set(user.getId(), SessionState.AWAIT_CAMPAIGN_WIZARD, payload);
        sendText(user.getChatId(), WIZARD_PROMPTS.get(step), null);
    }

    private void adminEditCampaign(UserEntity user) {
        requireAdmin(user);
        CampaignEntity current = campaignService.getCurrent();
        if (current == null) {
            sendText(user.getChatId(), "Немає акції для редагування.", menuFor(user));
            return;
        }
        if (current.getStatus() == CampaignStatus.RUNNING) {
            sendText(user.getChatId(), "Активну акцію не можна редагувати.", menuFor(user));
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "EDIT");
        payload.put("step", 0);
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", current.getName());
        fields.put("prizeProduct", current.getPrizeProduct());
        fields.put("promoProductText", current.getPromoProductText());
        fields.put("description", current.getDescription());
        fields.put("rules", current.getRules());
        fields.put("maxCodes", String.valueOf(current.getMaxCodes()));
        payload.put("fields", fields);
        sessionService.set(user.getId(), SessionState.AWAIT_CAMPAIGN_WIZARD, payload);
        sendText(user.getChatId(), "Редагування акції. " + WIZARD_PROMPTS.get(0), null);
    }

    private void adminStart(UserEntity user) {
        requireAdmin(user);
        CampaignEntity current = campaignService.getCurrent();
        if (current == null) {
            throw new BotException("Немає акції для запуску.");
        }
        campaignService.start(current);
        sendText(user.getChatId(), "Акцію запущено ▶️", menuFor(user));
    }

    private void adminStop(UserEntity user) {
        requireAdmin(user);
        CampaignEntity running = campaignService.getRunning();
        if (running == null) {
            throw new BotException("Немає активної акції.");
        }
        campaignService.stop(running);
        sendText(user.getChatId(), "Акцію зупинено ⏸", menuFor(user));
    }

    private void adminStats(UserEntity user) {
        requireAdmin(user);
        CampaignEntity current = campaignService.getCurrent();
        if (current == null) {
            sendText(user.getChatId(), "Немає акції для статистики.", menuFor(user));
            return;
        }

        long registered = campaignService.registeredCount(current.getId());
        long remaining = campaignService.remainingCount(current);
        long participants = campaignService.uniqueParticipants(current.getId());
        List<TopParticipantRow> top = campaignService.topParticipants(current.getId(), 5);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Статистика акції \"").append(current.getName()).append("\"\n")
                .append("Зареєстровано: ").append(registered).append("\n")
                .append("Залишилось: ").append(remaining).append("\n")
                .append("Учасників: ").append(participants);

        if (!top.isEmpty()) {
            sb.append("\nТОП-5:\n");
            int i = 1;
            for (TopParticipantRow row : top) {
                sb.append(i++).append(") ").append(row.displayName() == null ? "Користувач" : row.displayName())
                        .append(" - ").append(row.ticketCount()).append("\n");
            }
        }

        sendText(user.getChatId(), sb.toString(), menuFor(user));
    }

    private void adminPickWinner(UserEntity user) {
        requireAdmin(user);
        CampaignEntity current = campaignService.getCurrent();
        if (current == null) {
            throw new BotException("Немає акції для вибору переможця.");
        }

        WinnerEntity winner = winnerService.pickWinner(current);
        String winnerName = winner.getUser().getDisplayName() == null ? "Користувач" : winner.getUser().getDisplayName();
        sendText(user.getChatId(), "🏆 Переможця обрано: " + winnerName + " (код " + winner.getTicket().getCode() + ")", menuFor(user));
    }

    private void requireAdmin(UserEntity user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new BotException("Ця функція доступна тільки для ADMIN.");
        }
    }

    private ReplyKeyboardMarkup menuFor(UserEntity user) {
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Зареєструвати код");
        row1.add("Активна акція");
        row1.add("Мої квитки");
        rows.add(row1);

        if (user.getRole() == UserRole.ADMIN) {
            rows.add(one("➕ Створити акцію", "✏️ Редагувати акцію"));
            rows.add(one("📊 Статистика", "▶️ Запустити"));
            rows.add(one("⏸ Зупинити", "🏆 Обрати переможця"));
        }

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private KeyboardRow one(String first, String second) {
        KeyboardRow row = new KeyboardRow();
        row.add(first);
        row.add(second);
        return row;
    }

    private void sendText(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        try {
            execute(message);
        } catch (TelegramApiException ignored) {
        }
    }
}
