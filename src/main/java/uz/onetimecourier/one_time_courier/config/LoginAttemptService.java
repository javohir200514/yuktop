package uz.onetimecourier.one_time_courier.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parol bilan kirishda brute-force himoyasi (xotirada, bitta server uchun).
 * Bir kalit (telefon yoki IP) bo'yicha oynada MAX_FAILURES ta xato bo'lsa, LOCK muddatiga bloklanadi.
 * Telefon bo'yicha limit bitta akkauntga parol terishni, IP bo'yicha limit esa ko'p akkauntga urinishni to'xtatadi.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_FAILURES_PER_PHONE = 5;
    private static final int MAX_FAILURES_PER_IP = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);

    private static final class Counter {
        int failures;
        Instant windowStart = Instant.now();
        Instant lockedUntil;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /** true bo'lsa, urinishga ruxsat yo'q. */
    public boolean isBlocked(String phone, String ip) {
        return blocked("p:" + phone) || blocked("i:" + ip);
    }

    /** Bloklanguncha qolgan daqiqalar (xabar uchun), kamida 1. */
    public long minutesLeft(String phone, String ip) {
        Instant now = Instant.now();
        long seconds = 0;
        for (String key : new String[]{"p:" + phone, "i:" + ip}) {
            Counter c = counters.get(key);
            if (c != null && c.lockedUntil != null && c.lockedUntil.isAfter(now)) {
                seconds = Math.max(seconds, Duration.between(now, c.lockedUntil).getSeconds());
            }
        }
        return Math.max(1, (seconds + 59) / 60);
    }

    public void recordFailure(String phone, String ip) {
        fail("p:" + phone, MAX_FAILURES_PER_PHONE);
        fail("i:" + ip, MAX_FAILURES_PER_IP);
    }

    /** Muvaffaqiyatli kirishda faqat telefon hisoblagichi tozalanadi (IP limiti saqlanadi). */
    public void recordSuccess(String phone) {
        counters.remove("p:" + phone);
    }

    private boolean blocked(String key) {
        Counter c = counters.get(key);
        if (c == null) return false;
        synchronized (c) {
            return c.lockedUntil != null && c.lockedUntil.isAfter(Instant.now());
        }
    }

    private void fail(String key, int max) {
        Counter c = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (c) {
            Instant now = Instant.now();
            if (now.isAfter(c.windowStart.plus(WINDOW))) {
                c.failures = 0;
                c.windowStart = now;
                c.lockedUntil = null;
            }
            c.failures++;
            if (c.failures >= max) {
                c.lockedUntil = now.plus(LOCK);
            }
        }
    }

    /** Eskirgan yozuvlarni tozalaydi (xotira to'lib ketmasligi uchun). */
    @Scheduled(fixedDelay = 600_000)
    void cleanup() {
        Instant now = Instant.now();
        counters.entrySet().removeIf(e -> {
            Counter c = e.getValue();
            synchronized (c) {
                boolean lockOver = c.lockedUntil == null || c.lockedUntil.isBefore(now);
                return lockOver && now.isAfter(c.windowStart.plus(WINDOW));
            }
        });
    }
}
