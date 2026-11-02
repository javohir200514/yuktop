package uz.onetimecourier.one_time_courier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uz.onetimecourier.one_time_courier.dto.DeliveryStatusChangedEvent;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.repository.CourierRouteSubscriptionRepository;
import uz.onetimecourier.one_time_courier.repository.DeliveryRepository;
import uz.onetimecourier.one_time_courier.telegram.TelegramBotClient;
import uz.onetimecourier.one_time_courier.telegram.TelegramUpdateHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Holat o'zgarganda Telegram xabarlari.
 * AFTER_COMMIT: xabar faqat ma'lumot bazaga yozilgandan keyin ketadi.
 */
@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final DeliveryRepository deliveries;
    private final CourierRouteSubscriptionRepository subscriptions;
    private final TelegramBotClient bot;

    public TelegramNotificationService(DeliveryRepository deliveries,
                                       CourierRouteSubscriptionRepository subscriptions,
                                       TelegramBotClient bot) {
        this.deliveries = deliveries;
        this.subscriptions = subscriptions;
        this.bot = bot;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        if (!bot.enabled()) return;
        try {
            deliveries.findById(event.deliveryId())
                    .ifPresent(d -> dispatch(d, event.status()));
        } catch (RuntimeException e) {
            log.warn("Telegram bildirishnoma yuborilmadi (yuk {}): {}", event.deliveryId(), e.getMessage());
        }
    }

    private void dispatch(DeliveryEntity d, DeliveryStatus status) {
        switch (status) {
            case CREATED -> notifyCouriers(d);
            case MATCHED -> notifySenderMatched(d);
            case PICKED_UP -> notifySender(d, "🚚 Yuk #" + d.getId() + " olib ketildi va yo'lda.");
            case DELIVERED -> notifySender(d, "📦 Yuk #" + d.getId() + " yetkazildi. Rahmat!");
            default -> { }
        }
    }

    /** Yangi yuk: mos yo'nalishdagi kuryerlarga, "Qabul qilish" tugmasi bilan. */
    private void notifyCouriers(DeliveryEntity d) {
        List<UserEntity> couriers = subscriptions.findCouriersForRoute(
                d.getFromRegion(), d.getToRegion(), Role.COURIER);
        if (couriers.isEmpty()) return;

        Long senderId = d.getSender() == null ? null : d.getSender().getId();
        String text = "🆕 Yangi yuk #" + d.getId() + "\n"
                + "Yo'nalish: " + TelegramUpdateHandler.routeLabel(d.getFromRegion(), d.getToRegion()) + "\n"
                + "Olib ketish: " + Objects.toString(d.getPickupAddress(), "-") + "\n"
                + "Yetkazish: " + Objects.toString(d.getDropoffAddress(), "-") + "\n"
                + "Hajm: " + Objects.toString(d.getPackageSize(), "-")
                + ", og'irligi: " + Objects.toString(d.getWeightKg(), "-") + " kg\n"
                + "Haq: " + Objects.toString(d.getReward(), "-");

        Map<String, Object> markup = TelegramBotClient.keyboard(List.of(List.of(
                TelegramBotClient.button("✅ Qabul qilish", "acc:" + d.getId()))));

        for (UserEntity courier : couriers) {
            if (courier.getTelegramId() == null) continue;
            if (courier.getId().equals(senderId)) continue; // o'z yukini o'zi olmasin
            bot.sendMessage(courier.getTelegramId(), text, markup);
        }
    }

    /** Kuryer qabul qildi: jo'natuvchiga kuryer va transport ma'lumoti. */
    private void notifySenderMatched(DeliveryEntity d) {
        UserEntity courier = d.getCourier();
        String who = courier == null ? "-" : Objects.toString(courier.getFullName(), "-");
        String vehicle = courier == null ? "-" : vehicle(courier);
        notifySender(d, "✅ Yuk #" + d.getId() + " uchun kuryer topildi.\n"
                + "Kuryer: " + who + "\n"
                + "Transport: " + vehicle);
    }

    private void notifySender(DeliveryEntity d, String text) {
        UserEntity sender = d.getSender();
        if (sender == null || sender.getTelegramId() == null) return;
        bot.sendMessage(sender.getTelegramId(), text, null);
    }

    private static String vehicle(UserEntity c) {
        StringBuilder sb = new StringBuilder();
        for (Object part : new Object[]{c.getVehicleType(), c.getVehicleBrand(), c.getVehicleModel()}) {
            if (part != null) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(part);
            }
        }
        if (c.getVehicleColor() != null) {
            if (sb.length() > 0) sb.append(", rang: ");
            sb.append(c.getVehicleColor());
        }
        return sb.length() == 0 ? "ko'rsatilmagan" : sb.toString();
    }
}