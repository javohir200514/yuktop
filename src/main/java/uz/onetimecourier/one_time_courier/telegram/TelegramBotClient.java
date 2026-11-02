package uz.onetimecourier.one_time_courier.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Telegram Bot API bilan yagona bog'lanish nuqtasi. Token faqat shu yerda ishlatiladi. */
@Component
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final String token;
    private final RestClient client;

    public TelegramBotClient(@Value("${telegram.bot.token:}") String token) {
        this.token = token == null ? "" : token;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(40)); // long polling (25s) dan uzunroq
        this.client = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .requestFactory(factory)
                .build();
    }

    public boolean enabled() {
        return !token.isBlank();
    }

    public Map<String, Object> call(String method, Map<String, Object> body) {
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

    /** Xato matnida token bo'lishi mumkin, logga yozishdan oldin yashiramiz. */
    public String safe(Exception e) {
        String message = String.valueOf(e.getMessage());
        return token.isEmpty() ? message : message.replace(token, "***");
    }

    public void sendMessage(long chatId, String text, Map<String, Object> replyMarkup) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("disable_web_page_preview", true);
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        try {
            call("sendMessage", body);
        } catch (Exception e) {
            log.warn("Telegram xabar yuborilmadi: {}", safe(e));
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