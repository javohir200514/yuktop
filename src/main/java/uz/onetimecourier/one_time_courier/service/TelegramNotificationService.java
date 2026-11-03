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

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final DeliveryRepository deliveries;
    private final CourierRouteSubscriptionRepository subscriptions;
    private final TelegramBotClient bot;

    public TelegramNotificationService(
            DeliveryRepository deliveries,
            CourierRouteSubscriptionRepository subscriptions,
            TelegramBotClient bot) {
        this.deliveries = deliveries;
        this.subscriptions = subscriptions;
        this.bot = bot;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        if (!bot.enabled()) {
            return;
        }
        try {
            deliveries.findById(event.deliveryId())
                    .ifPresent(d -> dispatch(d, event.status()));
        } catch (RuntimeException e) {
            log.error("Telegram bildirishnomasi tayyorlanmadi (yuk #{}, holat {})",
                    event.deliveryId(), event.status(), e);
        }
    }

    private void dispatch(DeliveryEntity d, DeliveryStatus status) {
        switch (status) {
            case CREATED -> notifyCouriers(d);

            case MATCHED -> notifySenderMatched(d);

            case PICKED_UP -> notifySender(d,
                    "🚚 YUK YO'LGA CHIQDI!\n\n"
                            + "Sizning #" + d.getId() + "-raqamli yukingiz kuryer tomonidan olib ketildi.\n"
                            + "Hozirda yuk manzil tomon yetkazilmoqda.\n\n"
                            + "📍 Yukingiz xavfsiz yetkazilishini tilaymiz!");

            case DELIVERED -> notifySender(d,
                    "🎉 YUK MUVAFFAQIYATLI YETKAZILDI!\n\n"
                            + "Sizning #" + d.getId() + "-raqamli yukingiz belgilangan manzilga yetkazildi.\n\n"
                            + "🤝 Xizmatimizdan foydalanganingiz uchun rahmat!\n"
                            + "Yana xizmatimizdan foydalanishingizdan mamnun bo'lamiz.");

            case CANCELLED -> notifyCourierCancelled(d);

            default -> { }
        }
    }

    // ---------------------------------------------------------------- kuryerlarga

    /** Yangi yuk haqida mos yo'nalishdagi kuryerlarga xabar. Har bir kuryer alohida yuboriladi. */
    private void notifyCouriers(DeliveryEntity d) {
        List<UserEntity> couriers = subscriptions.findCouriersForRoute(
                d.getFromRegion(), d.getToRegion(), Role.COURIER);

        if (couriers.isEmpty()) {
            log.debug("Yuk #{}: mos yo'nalishda obuna bo'lgan kuryer yo'q", d.getId());
            return;
        }

        Long senderId = d.getSender() == null ? null : d.getSender().getId();

        String text =
                "🆕 YANGI YUK BUYURTMASI!\n\n"
                        + "Sizning yo'nalishingiz bo'yicha yangi yuk mavjud.\n"
                        + "Agar ushbu buyurtmani bajarish imkoniyatingiz bo'lsa, uni qabul qilishingiz mumkin.\n\n"
                        + "━━━━━━━━━━━━━━━━━━\n"
                        + "📦 Buyurtma raqami: #" + d.getId() + "\n\n"
                        + "🛣 Yo'nalish: "
                        + TelegramUpdateHandler.routeLabel(d.getFromRegion(), d.getToRegion()) + "\n\n"
                        + "📍 Olib ketish manzili:\n"
                        + Objects.toString(d.getPickupAddress(), "Ko'rsatilmagan") + "\n\n"
                        + "🏁 Yetkazish manzili:\n"
                        + Objects.toString(d.getDropoffAddress(), "Ko'rsatilmagan") + "\n\n"
                        + "📐 Yuk hajmi: "
                        + Objects.toString(d.getPackageSize(), "Ko'rsatilmagan") + "\n"
                        + "⚖️ Og'irligi: "
                        + Objects.toString(d.getWeightKg(), "Ko'rsatilmagan") + " kg\n\n"
                        + "💰 Kuryer haqi: "
                        + Objects.toString(d.getReward(), "Ko'rsatilmagan") + "\n"
                        + "━━━━━━━━━━━━━━━━━━\n\n"
                        + "Buyurtmani qabul qilish uchun quyidagi tugmani bosing.";

        Map<String, Object> markup = TelegramBotClient.keyboard(List.of(List.of(
                TelegramBotClient.button("✅ Buyurtmani qabul qilish", "acc:" + d.getId()))));

        int sent = 0;
        for (UserEntity courier : couriers) {
            if (Objects.equals(courier.getId(), senderId)) {
                continue; // jo'natuvchi o'zi yuborgan yukni o'ziga ko'rsatmasin
            }
            if (send(courier, text, markup, "yangi yuk #" + d.getId())) {
                sent++;
            }
        }
        log.info("Yuk #{}: {} ta kuryerga yangi yuk haqida xabar yuborildi", d.getId(), sent);
    }

    /** Kuryer olgan yuk bekor qilinsa, kuryerga xabar. */
    private void notifyCourierCancelled(DeliveryEntity d) {
        UserEntity courier = d.getCourier();
        if (courier == null) {
            return; // kuryer hali biriktirilmagan, xabar kerak emas
        }
        send(courier,
                "❌ YUK BEKOR QILINDI\n\n"
                        + "Siz qabul qilgan #" + d.getId() + "-raqamli yuk jo'natuvchi tomonidan bekor qilindi.",
                null,
                "bekor qilish #" + d.getId());
    }

    // ---------------------------------------------------------------- jo'natuvchiga

    /** Kuryer buyurtmani qabul qilganda jo'natuvchiga kuryer haqida xabar. */
    private void notifySenderMatched(DeliveryEntity d) {
        UserEntity courier = d.getCourier();

        String who = courier == null
                ? "Ma'lumot mavjud emas"
                : Objects.toString(courier.getFullName(), "Ma'lumot mavjud emas");

        String vehicle = courier == null ? "Ma'lumot mavjud emas" : vehicle(courier);

        notifySender(d,
                "✅ KURYER TOPILDI!\n\n"
                        + "Yaxshi yangilik! Sizning #" + d.getId()
                        + "-raqamli yuk buyurtmangizni kuryer qabul qildi.\n\n"
                        + "👤 Kuryer haqida ma'lumot:\n"
                        + "Ismi: " + who + "\n"
                        + "🚗 Transporti: " + vehicle + "\n\n"
                        + "📦 Tez orada kuryer yukingizni olib ketish uchun yo'lga chiqadi.\n\n"
                        + "Xizmatimizdan foydalanganingiz uchun rahmat!");
    }

    private void notifySender(DeliveryEntity d, String text) {
        UserEntity sender = d.getSender();
        if (sender == null) {
            log.warn("Yuk #{}: jo'natuvchi topilmadi, xabar yuborilmadi", d.getId());
            return;
        }
        send(sender, text, null, "jo'natuvchi, yuk #" + d.getId());
    }

    // ---------------------------------------------------------------- yordamchilar

    /**
     * Bitta foydalanuvchiga xabar yuboradi. Telegram ID yo'q bo'lsa yoki yuborish xato bersa,
     * boshqa xabarlarga ta'sir qilmaydi va sababini log'ga yozadi.
     */
    private boolean send(UserEntity user, String text, Map<String, Object> markup, String context) {
        Long chatId = user.getTelegramId();
        if (chatId == null) {
            log.info("Telegram bildirishnomasi o'tkazib yuborildi: foydalanuvchi #{} ({}) Telegram'ga bog'lanmagan, {}",
                    user.getId(), user.getRole(), context);
            return false;
        }
        try {
            bot.sendMessage(chatId, text, markup);
            return true;
        } catch (RuntimeException e) {
            log.warn("Telegram xabari yuborilmadi: foydalanuvchi #{}, {}: {}",
                    user.getId(), context, e.getMessage());
            return false;
        }
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

        return sb.length() == 0 ? "Ko'rsatilmagan" : sb.toString();
    }
}