package uz.onetimecourier.one_time_courier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.repository.TelegramDeliveryRepository;
import uz.onetimecourier.one_time_courier.telegram.TelegramBotClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kuryerga "Yuk olindimi?" va "Yetkazildimi?" deb so'raydi.
 * Holat o'zgarsa (saytdan yoki Telegram'dan), eslatma avtomatik to'xtaydi.
 */
@Service
public class TelegramReminderService {

    private static final Logger log = LoggerFactory.getLogger(TelegramReminderService.class);

    private static final Duration PICKUP_EVERY = Duration.ofMinutes(20);
    private static final Duration DELIVERY_EVERY = Duration.ofMinutes(40);

    private final TelegramDeliveryRepository deliveries;
    private final TelegramBotClient bot;

    /** Kalit: "yukId:holat". Qiymat: oxirgi so'ralgan vaqt (birinchi ko'rilgan vaqt ham shu yerda). */
    private final Map<String, Instant> lastAsked = new ConcurrentHashMap<>();

    public TelegramReminderService(TelegramDeliveryRepository deliveries, TelegramBotClient bot) {
        this.deliveries = deliveries;
        this.bot = bot;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional(readOnly = true)
    public void askCouriers() {
        if (!bot.enabled()) return;

        Instant now = Instant.now();
        Set<String> activeKeys = new HashSet<>();
        List<DeliveryEntity> active = deliveries.findActiveWithCourier(
                List.of(DeliveryStatus.MATCHED, DeliveryStatus.PICKED_UP));

        for (DeliveryEntity d : active) {
            try {
                UserEntity courier = d.getCourier();
                if (courier == null || courier.getTelegramId() == null) continue;

                String key = d.getId() + ":" + d.getStatus();
                activeKeys.add(key);

                boolean pickup = d.getStatus() == DeliveryStatus.MATCHED;
                Duration every = pickup ? PICKUP_EVERY : DELIVERY_EVERY;

                // Birinchi marta ko'rilganda hisob boshlanadi, darhol so'ramaydi
                Instant last = lastAsked.computeIfAbsent(key, k -> now);
                if (now.isBefore(last.plus(every))) continue;

                lastAsked.put(key, now);
                ask(courier.getTelegramId(), d.getId(), pickup);
            } catch (RuntimeException e) {
                log.warn("Eslatma yuborilmadi (yuk {}): {}", d.getId(), e.getMessage());
            }
        }

        // Holati o'zgargan yoki yakunlangan yuklar kalitini tozalaymiz
        lastAsked.keySet().removeIf(k -> !activeKeys.contains(k));
    }

    private void ask(long chatId, long deliveryId, boolean pickup) {
        String text = pickup
                ? "📦 Yuk #" + deliveryId + " ni olib ketdingizmi?"
                : "🏁 Yuk #" + deliveryId + " yetkazildimi?";
        String yesData = pickup ? "pick:" + deliveryId : "dlv:" + deliveryId;

        Map<String, Object> markup = TelegramBotClient.keyboard(List.of(List.of(
                TelegramBotClient.button("✅ Ha", yesData),
                TelegramBotClient.button("❌ Yo'q", "no:" + deliveryId))));
        bot.sendMessage(chatId, text, markup);
    }
}