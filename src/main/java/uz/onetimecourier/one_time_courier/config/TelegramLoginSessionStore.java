package uz.onetimecourier.one_time_courier.config;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Telegram orqali kirish uchun bir martalik, 5 daqiqalik tokenlar (xotirada). */
@Component
public class TelegramLoginSessionStore {

    public record TelegramProfile(long id, String firstName, String lastName, String username) {}

    public enum State { PENDING, EXPIRED, DONE }

    public record PollResult(State state, TelegramProfile profile) {}

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_PENDING = 5000;

    private static final class Entry {
        final Instant expiresAt = Instant.now().plus(TTL);
        volatile TelegramProfile profile;
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

    /** Bot chaqiradi: token yaroqli va hali ishlatilmagan bo'lsa true. */
    public boolean complete(String token, TelegramProfile profile) {
        Entry e = entries.get(token);
        if (e == null || e.expired() || e.profile != null) {
            return false;
        }
        e.profile = profile;
        return true;
    }

    /** Sahifa chaqiradi. DONE qaytsa token o'chiriladi (bir marta ishlatiladi). */
    public PollResult poll(String token) {
        Entry e = token == null ? null : entries.get(token);
        if (e == null || e.expired()) {
            if (token != null) entries.remove(token);
            return new PollResult(State.EXPIRED, null);
        }
        if (e.profile == null) {
            return new PollResult(State.PENDING, null);
        }
        entries.remove(token);
        return new PollResult(State.DONE, e.profile);
    }
}