package uz.onetimecourier.one_time_courier.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Ro'yxatdan o'tishda telefon raqamni Telegram orqali tasdiqlash (xotirada). */
@Service
public class TelegramPhoneVerificationService {

    public record BotReply(String text, boolean askContact) {}

    public enum Status { WAITING_CONTACT, WAITING_CODE, EXPIRED }

    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_SESSIONS = 5000;

    private static final class Session {
        final String phone;
        final Instant expiresAt = Instant.now().plus(SESSION_TTL);
        Long chatId;
        String code;
        Instant codeExpiresAt;
        int attempts;
        boolean verified;

        Session(String phone) {
            this.phone = phone;
        }

        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** Sayt: "Ro'yxatdan o'tish" bosilganda. Qaytgan token Telegram havolasiga qo'yiladi. */
    public String createSession(String phone) {
        sessions.values().removeIf(Session::expired);
        if (sessions.size() >= MAX_SESSIONS) {
            throw new IllegalStateException("Juda ko'p so'rov. Keyinroq urinib ko'ring");
        }
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(phone));
        return token;
    }

    /** Bot: /start TOKEN. Token bu oqimga tegishli bo'lmasa null qaytadi (login oqimi davom etadi). */
    public BotReply onStart(String token, long chatId) {
        Session s = sessions.get(token);
        if (s == null) {
            return null;
        }
        if (s.expired()) {
            sessions.remove(token);
            return new BotReply("Havola eskirgan. Saytda qaytadan urinib ko'ring.", false);
        }
        synchronized (s) {
            s.chatId = chatId;
        }
        return new BotReply("Ro'yxatdan o'tish uchun pastdagi \"Raqamni yuborish\" tugmasini bosing.", true);
    }

    /** Bot: foydalanuvchi kontakt yuborganda. Raqam mos bo'lsa, tasdiqlash kodi yuboriladi. */
    public BotReply onContact(long chatId, long fromId, Long contactUserId, String contactPhone) {
        Session s = findByChat(chatId);
        if (s == null) {
            return new BotReply("Avval saytda ro'yxatdan o'tishni boshlang.", false);
        }
        if (contactUserId == null || contactUserId != fromId) {
            return new BotReply("Iltimos, faqat o'zingizning raqamingizni yuboring.", true);
        }
        String tgPhone = contactPhone == null ? null : AuthService.normalizePhone(contactPhone);
        if (!s.phone.equals(tgPhone)) {
            return new BotReply("Bu raqam saytda kiritilgan raqam bilan mos emas. "
                    + "Saytda tekshirib, qaytadan urinib ko'ring.", false);
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        synchronized (s) {
            s.code = code;
            s.codeExpiresAt = Instant.now().plus(CODE_TTL);
            s.attempts = 0;
        }
        return new BotReply("Tasdiqlash kodingiz: " + code
                + "\nKodni saytdagi oynaga kiriting. Kod 5 daqiqa amal qiladi.", false);
    }

    /** Sayt: holatni so'raydi (har bir poll). */
    public Status poll(String token) {
        Session s = token == null ? null : sessions.get(token);
        if (s == null || s.expired()) {
            if (token != null) {
                sessions.remove(token);
            }
            return Status.EXPIRED;
        }
        return s.code == null ? Status.WAITING_CONTACT : Status.WAITING_CODE;
    }

    /** Sayt: foydalanuvchi kiritgan kodni tekshiradi. To'g'ri bo'lsa sessiya tasdiqlanadi. */
    public boolean checkCode(String token, String code) {
        Session s = token == null ? null : sessions.get(token);
        if (s == null || s.expired()) {
            return false;
        }
        synchronized (s) {
            if (s.code == null || Instant.now().isAfter(s.codeExpiresAt) || s.attempts >= MAX_ATTEMPTS) {
                return false;
            }
            s.attempts++;
            if (!s.code.equals(code == null ? "" : code.trim())) {
                return false;
            }
            s.verified = true;
            return true;
        }
    }

    public void discard(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private Session findByChat(long chatId) {
        for (Session s : sessions.values()) {
            if (!s.expired() && !s.verified && s.chatId != null && s.chatId == chatId) {
                return s;
            }
        }
        return null;
    }
}