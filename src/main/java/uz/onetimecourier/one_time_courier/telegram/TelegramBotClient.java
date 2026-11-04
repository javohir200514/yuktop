package uz.onetimecourier.one_time_courier.telegram;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Telegram Bot API bilan yagona bog'lanish nuqtasi. Token faqat shu yerda ishlatiladi. */
@Component
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    // Har bir Telegram chat uchun oxirgi yuborilgan xabar ID si
    private final ConcurrentMap<Long, Integer> lastMessageIds =
            new ConcurrentHashMap<>();

    // Bir chatga parallel xabar yuborilishining oldini oladi
    private final ConcurrentMap<Long, Object> chatLocks =
            new ConcurrentHashMap<>();
    private final String token;
    private final RestClient client;
    private final ObjectMapper json = new ObjectMapper();

    public TelegramBotClient(@Value("${telegram.bot.token:}") String token) {
        this.token = token == null ? "" : token;

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(40)); // long polling (25s) dan uzunroq

        this.client = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .requestFactory(factory)
                .build();
    }

    public boolean enabled() {
        return !token.isBlank();
    }

    /**
     * Telegram metodini chaqiradi. Javob avval matn sifatida olinadi, shunda
     * noto'g'ri content-type yoki HTML javob bo'lsa, sababi log'da ko'rinadi.
     */
    public Map<String, Object> call(String method, Map<String, Object> body) {
        ResponseEntity<String> resp = client.post()
                .uri("/bot{token}/{method}", token, method)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body == null ? Map.of() : body)
                .retrieve()
                .toEntity(String.class);

        String raw = resp.getBody();
        String contentType = resp.getHeaders().getFirst("Content-Type");

        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Telegram bo'sh javob qaytardi (" + method
                    + ", status=" + resp.getStatusCode() + ")");
        }

        Map<String, Object> response;
        try {
            response = json.readValue(raw, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Telegram JSON emas (" + method
                    + ", status=" + resp.getStatusCode()
                    + ", content-type=" + contentType
                    + ", javob=" + head(raw) + ")", e);
        }

        if (!Boolean.TRUE.equals(response.get("ok"))) {
            throw new IllegalStateException("Telegram API: ok=false (" + method
                    + ", description=" + response.get("description") + ")");
        }
        return response;
    }

    private static String head(String s) {
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    /** Xato matnida token bo'lishi mumkin, logga yozishdan oldin yashiramiz. */
    public String safe(Exception e) {
        String message = String.valueOf(e.getMessage());
        return token.isEmpty() ? message : message.replace(token, "***");
    }

    public void sendMessage(
            long chatId,
            String text,
            Map<String, Object> replyMarkup) {

        Object lock = chatLocks.computeIfAbsent(chatId, id -> new Object());

        synchronized (lock) {

            // 1. Avvalgi xabarni o'chirish
            Integer oldMessageId = lastMessageIds.get(chatId);

            if (oldMessageId != null) {
                try {
                    Map<String, Object> deleteBody = new LinkedHashMap<>();
                    deleteBody.put("chat_id", chatId);
                    deleteBody.put("message_id", oldMessageId);

                    call("deleteMessage", deleteBody);

                    log.info("Eski Telegram xabari o'chirildi: chat={}, message={}",
                            chatId, oldMessageId);

                } catch (Exception e) {
                    log.warn("Eski xabarni o'chirib bo'lmadi: {}",
                            safe(e));
                }
            }

            // 2. Yangi xabar yuborish
            try {
                Map<String, Object> body = new LinkedHashMap<>();

                body.put("chat_id", chatId);
                body.put("text", text);
                body.put("disable_web_page_preview", true);

                if (replyMarkup != null) {
                    body.put("reply_markup", replyMarkup);
                }

                Map<String, Object> response = call("sendMessage", body);

                // 3. Yangi xabarning ID sini olish
                Object result = response.get("result");

                if (result instanceof Map<?, ?> message) {
                    Object messageId = message.get("message_id");

                    if (messageId instanceof Number number) {
                        lastMessageIds.put(chatId, number.intValue());
                    }
                }

            } catch (Exception e) {
                log.warn("Telegram xabar yuborilmadi: {}", safe(e));
            }
        }
    }

    public void answerCallback(String callbackId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("callback_query_id", callbackId);
        if (text != null) {
            body.put("text", text);
        }
        try {
            call("answerCallbackQuery", body);
        } catch (Exception e) {
            log.warn("Telegram callback javobi yuborilmadi: {}", safe(e));
        }
    }

    /** Inline tugma. callbackData 64 baytdan oshmasligi kerak. */
    public static Map<String, Object> button(String text, String callbackData) {
        return Map.of("text", text, "callback_data", callbackData);
    }

    public static Map<String, Object> keyboard(List<List<Map<String, Object>>> rows) {
        return Map.of("inline_keyboard", rows);
    }
}