package uz.onetimecourier.one_time_courier.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService;


import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBotPoller {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotPoller.class);

    private final TelegramLoginSessionStore store;
    private final TelegramPhoneVerificationService phoneVerification;
    private final String token;
    private final RestClient client;

    private volatile boolean running;
    private Thread thread;

    private static final Map<String, Object> CONTACT_KEYBOARD = contactKeyboard();
    private static final Map<String, Object> REMOVE_KEYBOARD = Map.of("remove_keyboard", true);

    public TelegramBotPoller(TelegramLoginSessionStore store,
                             TelegramPhoneVerificationService phoneVerification,
                             @Value("${telegram.bot.token:}") String token) {
        this.store = store;
        this.phoneVerification = phoneVerification;
        this.token = token;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(40));
        this.client = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .requestFactory(factory)
                .build();
    }

    private static Map<String, Object> contactKeyboard() {
        Map<String, Object> button = Map.of("text", "Raqamni yuborish", "request_contact", true);
        List<Object> row = List.of(button);
        List<Object> rows = List.of(row);
        return Map.of("keyboard", rows, "one_time_keyboard", true, "resize_keyboard", true);
    }

    @PostConstruct
    void start() {
        if (token == null || token.isBlank()) {
            log.warn("Telegram bot tokeni berilmagan: Telegram orqali kirish o'chiq");
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
            call("deleteWebhook", Map.of("drop_pending_updates", false));
        } catch (Exception e) {
            log.warn("Telegram webhook o'chirilmadi: {}", safe(e));
        }
        long offset = 0;
        long backoff = 1000;
        while (running) {
            try {
                Map<String, Object> resp = call("getUpdates", Map.of(
                        "offset", offset, "timeout", 25, "allowed_updates", List.of("message")));
                for (Map<String, Object> update : list(resp.get("result"))) {
                    offset = ((Number) update.get("update_id")).longValue() + 1;
                    handle(update);
                }
                backoff = 1000;
            } catch (Exception e) {
                if (!running) return;
                log.warn("Telegram polling xatosi: {}", safe(e));
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    return;
                }
                backoff = Math.min(backoff * 2, 30_000);
            }
        }
    }

    private void send(long chatId, String text, Map<String, Object> markup) {
        try {
            call("sendMessage", Map.of("chat_id", chatId, "text", text, "reply_markup", markup));
        } catch (Exception e) {
            log.warn("Telegram xabar yuborilmadi: {}", safe(e));
        }
    }
    private void handle(Map<String, Object> update) {
        Map<String, Object> message = map(update.get("message"));
        Map<String, Object> chat = map(message.get("chat"));
        Map<String, Object> from = map(message.get("from"));
        if (!"private".equals(chat.get("type")) || chat.get("id") == null || from.get("id") == null) {
            return;
        }
        long chatId = ((Number) chat.get("id")).longValue();
        long fromId = ((Number) from.get("id")).longValue();

        // Kontakt (telefon raqam) yuborilgan bo'lsa: ro'yxatdan o'tish oqimi
        Map<String, Object> contact = map(message.get("contact"));
        if (!contact.isEmpty()) {
            TelegramPhoneVerificationService.BotReply r = phoneVerification.onContact(
                    chatId, fromId, num(contact.get("user_id")), str(contact.get("phone_number")));
            send(chatId, r.text(), r.askContact() ? CONTACT_KEYBOARD : REMOVE_KEYBOARD);
            return;
        }

        Object text = message.get("text");
        if (!(text instanceof String t) || !t.startsWith("/start")) {
            return;
        }
        String[] parts = t.trim().split("\\s+", 2);
        if (parts.length < 2) {
            send(chatId, "Salom! YukBor saytida \"Telegram orqali kirish\" tugmasini bosing.", REMOVE_KEYBOARD);
            return;
        }
        String param = parts[1];

        // Ro'yxatdan o'tish tokeni bo'lsa
        TelegramPhoneVerificationService.BotReply phoneReply = phoneVerification.onStart(param, chatId);
        if (phoneReply != null) {
            send(chatId, phoneReply.text(), phoneReply.askContact() ? CONTACT_KEYBOARD : REMOVE_KEYBOARD);
            return;
        }

        // Aks holda: Telegram orqali kirish (oldingi oqim)
        TelegramLoginSessionStore.TelegramProfile profile = new TelegramLoginSessionStore.TelegramProfile(
                fromId, str(from.get("first_name")), str(from.get("last_name")), str(from.get("username")));
        boolean ok = store.complete(param, profile);
        send(chatId, ok ? "Tasdiqlandi. Saytga qaytishingiz mumkin."
                : "Havola eskirgan. Saytdan qaytadan urinib ko'ring.", REMOVE_KEYBOARD);
    }

    private Map<String, Object> call(String method, Map<String, Object> body) {
        Map<String, Object> response = client.post()
                .uri("/bot{token}/{method}", token, method)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            throw new IllegalStateException("Telegram API: ok=false (" + method + ")");
        }
        return response;
    }

    private static Long num(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }

    /** Xato matnida token (URL ichida) bo'lishi mumkin, logga yozishdan oldin yashiramiz. */
    private String safe(Exception e) {
        return String.valueOf(e.getMessage()).replace(token, "***");
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