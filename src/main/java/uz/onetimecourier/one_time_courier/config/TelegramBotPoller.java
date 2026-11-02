package uz.onetimecourier.one_time_courier.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService;
import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService.BotReply;
import uz.onetimecourier.one_time_courier.telegram.TelegramBotClient;
import uz.onetimecourier.one_time_courier.telegram.TelegramUpdateHandler;

import java.util.List;
import java.util.Map;

@Component
public class TelegramBotPoller {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotPoller.class);

    private final TelegramLoginSessionStore store;
    private final TelegramPhoneVerificationService phoneVerification;
    private final TelegramBotClient bot;
    private final TelegramUpdateHandler handler;

    private volatile boolean running;
    private Thread thread;

    public TelegramBotPoller(TelegramLoginSessionStore store,
                             TelegramPhoneVerificationService phoneVerification,
                             TelegramBotClient bot,
                             TelegramUpdateHandler handler) {
        this.store = store;
        this.phoneVerification = phoneVerification;
        this.bot = bot;
        this.handler = handler;
    }

    @PostConstruct
    void start() {
        if (!bot.enabled()) {
            log.warn("Telegram bot tokeni berilmagan: Telegram funksiyalari o'chiq");
            return;
        }
        running = true;
        thread = new Thread(this::loop, "telegram-poller");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    private void loop() {
        try {
            bot.call("deleteWebhook", Map.of("drop_pending_updates", false));
        } catch (Exception e) {
            log.warn("Telegram webhook o'chirilmadi: {}", bot.safe(e));
        }

        long offset = 0;
        long backoff = 1000;
        while (running) {
            try {
                Map<String, Object> resp = bot.call("getUpdates", Map.of(
                        "offset", offset,
                        "timeout", 25,
                        "allowed_updates", List.of("message", "callback_query")));
                for (Map<String, Object> update : list(resp.get("result"))) {
                    offset = longOf(update.get("update_id")) + 1;
                    try {
                        handle(update);
                    } catch (RuntimeException e) {
                        log.warn("Telegram yangilanishi qayta ishlanmadi: {}", bot.safe(e));
                    }
                }
                backoff = 1000;
            } catch (Exception e) {
                if (!running) return;
                log.warn("Telegram polling xatosi: {}", bot.safe(e));
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    return;
                }
                backoff = Math.min(backoff * 2, 30_000);
            }
        }
    }

    private void handle(Map<String, Object> update) {
        Map<String, Object> callback = map(update.get("callback_query"));
        if (!callback.isEmpty()) {
            handler.onCallback(
                    str(callback.get("id")),
                    longOf(map(callback.get("from")).get("id")),
                    str(callback.get("data")));
            return;
        }

        Map<String, Object> message = map(update.get("message"));
        Map<String, Object> chat = map(message.get("chat"));
        Map<String, Object> from = map(message.get("from"));
        if (!"private".equals(chat.get("type")) || chat.get("id") == null || from.get("id") == null) {
            return;
        }
        long chatId = longOf(chat.get("id"));
        long fromId = longOf(from.get("id"));

        // Telefon kontakti: foydalanuvchi "Raqamni yuborish" tugmasini bosganda
        Map<String, Object> contact = map(message.get("contact"));
        if (!contact.isEmpty()) {
            Long contactUserId = contact.get("user_id") == null ? null : longOf(contact.get("user_id"));
            BotReply reply = phoneVerification.onContact(
                    chatId, fromId, contactUserId, str(contact.get("phone_number")));
            sendReply(chatId, reply);
            return;
        }

        Object text = message.get("text");
        if (!(text instanceof String t)) {
            return;
        }
        String trimmed = t.trim();

        if (trimmed.startsWith("/start")) {
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length >= 2) {
                String token = parts[1];
                // Avval telefon tasdiqlash oqimi, u tanimasa login oqimi
                BotReply reply = phoneVerification.onStart(token, chatId);
                if (reply != null) {
                    sendReply(chatId, reply);
                    return;
                }
                login(chatId, token, from);
                return;
            }
        }
        handler.onMessage(chatId, trimmed);
    }

    /** Telefon oqimi javobi. askContact bo'lsa, "Raqamni yuborish" tugmasi chiqadi, aks holda klaviatura yopiladi. */
    private void sendReply(long chatId, BotReply reply) {
        if (reply == null) return;
        Map<String, Object> markup;
        if (reply.askContact()) {
            Map<String, Object> button = Map.of("text", "Raqamni yuborish", "request_contact", true);
            markup = Map.of(
                    "keyboard", List.of(List.of(button)),
                    "resize_keyboard", true,
                    "one_time_keyboard", true);
        } else {
            markup = Map.of("remove_keyboard", true);
        }
        bot.sendMessage(chatId, reply.text(), markup);
    }

    private void login(long chatId, String loginToken, Map<String, Object> from) {
        TelegramLoginSessionStore.TelegramProfile profile = new TelegramLoginSessionStore.TelegramProfile(
                longOf(from.get("id")),
                str(from.get("first_name")),
                str(from.get("last_name")),
                str(from.get("username")));
        boolean ok = store.complete(loginToken, profile);
        bot.sendMessage(chatId,
                ok ? "Tasdiqlandi. Saytga qaytishingiz mumkin."
                   : "Havola eskirgan. Saytdan qaytadan urinib ko'ring.",
                null);
    }

    private static long longOf(Object o) {
        return ((Number) o).longValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : List.of();
    }

    private static String str(Object o) {
        return o instanceof String s ? s : null;
    }
}