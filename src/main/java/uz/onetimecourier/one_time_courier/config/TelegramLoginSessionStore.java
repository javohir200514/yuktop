package uz.onetimecourier.one_time_courier.config;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telegram orqali kirish uchun 5 daqiqalik tokenlar (xotirada).
 *
 * Oqim: sayt token yaratadi -> foydalanuvchi botda Start bosadi -> bot 6 xonali kod yuboradi ->
 * foydalanuvchi kodni SAYTGA kiritadi. Kod kiritilmaguncha sessiya ochilmaydi, shuning uchun
 * birovning havolasini bosgan odam hujumchining brauzerini tizimga kirgiza olmaydi.
 */
@Component
public class TelegramLoginSessionStore {

    public record TelegramProfile(long id, String firstName, String lastName, String username) {}

    /** PENDING: bot kutilmoqda. WAITING_CODE: kod saytga kiritilishi kerak. DONE: tayyor. EXPIRED: muddat/urinish tugadi. */
    public enum State { PENDING, WAITING_CODE, EXPIRED, DONE }

    public record PollResult(State state, TelegramProfile profile) {}

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_PENDING = 5000;
    private static final int MAX_ATTEMPTS = 5;

    private static final class Entry {
        final Instant expiresAt = Instant.now().plus(TTL);
        volatile TelegramProfile profile;
        volatile String code;
        int attempts;
        boolean expired() { return Instant.now().isAfter(expiresAt); }
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public String create() {
        entries.values().removeIf(Entry::expired);
        if (entries.size() >= MAX_PENDING) {
            throw new IllegalStateException("Juda ko'p kutayotgan so'rov");
        }
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        // Telegram start parametri: A-Z a-z 0-9 _ - (64 belgigacha)
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entries.put(token, new Entry());
        return token;
    }

    /**
     * Bot chaqiradi. Token yaroqli va hali ishlatilmagan bo'lsa, foydalanuvchiga yuboriladigan 6 xonali kodni
     * qaytaradi, aks holda null.
     */
    public String complete(String token, TelegramProfile profile) {
        Entry e = token == null ? null : entries.get(token);
        if (e == null) {
            return null;
        }
        synchronized (e) {
            if (e.expired() || e.profile != null) {
                return null;
            }
            e.profile = profile;
            e.code = String.format("%06d", random.nextInt(1_000_000));
            return e.code;
        }
    }

    /** Sahifa chaqiradi (har poll). Kod tekshirilmaguncha profil hech qachon qaytmaydi. */
    public PollResult poll(String token) {
        Entry e = token == null ? null : entries.get(token);
        if (e == null || e.expired()) {
            if (token != null) entries.remove(token);
            return new PollResult(State.EXPIRED, null);
        }
        if (e.profile == null) {
            return new PollResult(State.PENDING, null);
        }
        return new PollResult(State.WAITING_CODE, null);
    }

    /**
     * Sahifa: foydalanuvchi kiritgan kodni tekshiradi. To'g'ri bo'lsa token o'chiriladi (bir marta ishlatiladi)
     * va DONE qaytadi. Noto'g'ri bo'lsa WAITING_CODE, urinishlar tugasa EXPIRED.
     */
    public PollResult verify(String token, String input) {
        Entry e = token == null ? null : entries.get(token);
        if (e == null || e.expired()) {
            if (token != null) entries.remove(token);
            return new PollResult(State.EXPIRED, null);
        }
        synchronized (e) {
            if (e.profile == null || e.code == null) {
                return new PollResult(State.PENDING, null);
            }
            e.attempts++;
            boolean ok = input != null && MessageDigest.isEqual(
                    e.code.getBytes(StandardCharsets.UTF_8),
                    input.trim().getBytes(StandardCharsets.UTF_8));
            if (ok) {
                entries.remove(token);
                return new PollResult(State.DONE, e.profile);
            }
            if (e.attempts >= MAX_ATTEMPTS) {
                entries.remove(token);
                return new PollResult(State.EXPIRED, null);
            }
            return new PollResult(State.WAITING_CODE, null);
        }
    }
}
