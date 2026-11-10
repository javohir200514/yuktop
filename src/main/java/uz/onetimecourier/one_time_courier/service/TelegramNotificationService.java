package uz.onetimecourier.one_time_courier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uz.onetimecourier.one_time_courier.config.AsyncConfig;
import uz.onetimecourier.one_time_courier.dto.DeliveryStatusChangedEvent;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.repository.CourierRouteSubscriptionRepository;
import uz.onetimecourier.one_time_courier.repository.DeliveryRepository;
import uz.onetimecourier.one_time_courier.telegram.TelegramBotClient;
import uz.onetimecourier.one_time_courier.telegram.TelegramUpdateHandler;

import java.util.ArrayList;
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

    @Async(AsyncConfig.TELEGRAM_EXECUTOR)
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
            case MATCHED -> notifyMatched(d);
            case PICKED_UP -> notifyPickedUp(d);
            case DELIVERED -> notifyDelivered(d);
            case CANCELLED -> notifyCourierCancelled(d);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- holat xabarlari

    /**
     * MATCHED hodisasi ikki holatda chiqadi:
     * 1) kuryer yukni qabul qildi — jo'natuvchiga "kuryer topildi";
     * 2) kuryer "olib ketdim" dedi — jo'natuvchiga botda "Ha / Yo'q" tugmalari bilan so'rov.
     */
    private void notifyMatched(DeliveryEntity d) {
        if (d.getCourierPickupAt() == null) {
            notifySenderMatched(d);
        } else if (d.getSenderPickupConfirmedAt() == null) {
            send(d.getSender(),
                    "📦 KURYER YUKNI OLGANINI BILDIRDI\n\n"
                            + title(d) + " yo'nalishidagi yukni kuryer olib ketganini bildirdi.\n"
                            + "Olib ketilganini tasdiqlaysizmi?",
                    confirmKeyboard("spy:" + d.getId(), "spn:" + d.getId(), "✅ Ha, olib ketdi", "❌ Yo'q"),
                    "olib ketish tasdig'i (jo'natuvchi), yuk " + d.getId());
        }
    }

    /**
     * PICKED_UP hodisasi ikki holatda chiqadi:
     * 1) jo'natuvchi olib ketishni tasdiqladi — jo'natuvchiga xabar, kuryerga "Yetkazdim" tugmasi;
     * 2) kuryer "yetkazdim" dedi — jo'natuvchiga botda "Ha / Yo'q" tugmalari bilan so'rov.
     */
    private void notifyPickedUp(DeliveryEntity d) {
        if (d.getCourierDeliveryAt() == null) {
            send(d.getSender(),
                    "🚚 YUK YO'LGA CHIQDI!\n\n"
                            + title(d) + " yo'nalishidagi yukingiz kuryer tomonidan olib ketildi.\n"
                            + "📍 Yukingiz xavfsiz yetkazilishini tilaymiz!",
                    null,
                    "yo'lga chiqdi (jo'natuvchi), yuk " + d.getId());

            send(d.getCourier(),
                    "✅ Jo'natuvchi yukni olib ketganingizni tasdiqladi.\n\n"
                            + title(d) + " yo'nalishida yukni manzilga yetkazing.\n"
                            + "Yetkazganingizda pastdagi tugmani bosing.",
                    TelegramBotClient.keyboard(List.of(List.of(
                            TelegramBotClient.button("🏁 Yetkazdim", "dlv:" + d.getId())))),
                    "yetkazdim tugmasi (kuryer), yuk " + d.getId());
        } else if (d.getSenderDeliveryConfirmedAt() == null) {
            send(d.getSender(),
                    "📬 YUK YETKAZILDIMI?\n\n"
                            + "Kuryer " + title(d) + " yo'nalishidagi yukni yetkazganini bildirdi.\n"
                            + "Yetkazilganini tasdiqlaysizmi?",
                    confirmKeyboard("dyy:" + d.getId(), "dyn:" + d.getId(), "✅ Ha, yetkazildi", "❌ Yo'q"),
                    "yetkazish tasdig'i (jo'natuvchi), yuk " + d.getId());
        }
    }

    /** Yuk yakunlanganda: jo'natuvchiga baholash, kuryerga rahmat. */
    private void notifyDelivered(DeliveryEntity d) {
        send(d.getSender(),
                "🎉 YUK MUVAFFAQIYATLI YETKAZILDI!\n\n"
                        + title(d) + " yo'nalishidagi yukingiz tasdiqlandi.\n\n"
                        + "🤝 Xizmatimizdan foydalanganingiz uchun rahmat!",
                null,
                "yakunlandi (jo'natuvchi), yuk " + d.getId());

        send(d.getCourier(),
                "🎉 YUK YAKUNLANDI!\n\n"
                        + title(d) + " yo'nalishidagi yuk jo'natuvchi tomonidan tasdiqlandi. Rahmat!",
                null,
                "yakunlandi (kuryer), yuk " + d.getId());

        askRating(d);
    }

    // ---------------------------------------------------------------- baholash

    /** Yetkazilgandan keyin faqat jo'natuvchiga baholash tugmalari. */
    private void askRating(DeliveryEntity d) {
        send(d.getSender(),
                "⭐ KURYERNI BAHOLANG\n\n"
                        + title(d) + " yo'nalishidagi yuk bo'yicha kuryer xizmatini baholang.\n"
                        + "1 — yomon, 5 — a'lo.",
                ratingKeyboard(d.getId()),
                "baholash (jo'natuvchi), yuk " + d.getId());
    }

    /** 1 dan 5 gacha yulduzlar. Tugmalar qisqa va ikki qatorga bo'lingan, shunda hammasi ko'rinadi. */
    private static Map<String, Object> ratingKeyboard(Long deliveryId) {
        List<Map<String, Object>> first = new ArrayList<>();
        List<Map<String, Object>> second = new ArrayList<>();
        for (int stars = 1; stars <= 5; stars++) {
            Map<String, Object> button = TelegramBotClient.button(
                    stars + " ⭐", "rt:" + deliveryId + ":" + stars);
            (stars <= 3 ? first : second).add(button);
        }
        return TelegramBotClient.keyboard(List.of(first, second));
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
                        + "💰 Kuryer haqi: " + formatReward(d) + "\n"
                        + "━━━━━━━━━━━━━━━━━━\n\n"
                        + "Buyurtmani qabul qilish uchun quyidagi tugmani bosing.";

        Map<String, Object> markup = TelegramBotClient.keyboard(List.of(List.of(
                TelegramBotClient.button("✅ Buyurtmani qabul qilish", "acc:" + d.getId()))));

        int sent = 0;
        for (UserEntity courier : couriers) {
            if (Objects.equals(courier.getId(), senderId)) {
                continue; // jo'natuvchi o'zi yuborgan yukni o'ziga ko'rsatmasin
            }
            if (send(courier, text, markup, "yangi yuk " + d.getId())) {
                sent++;
            }
        }
        log.info("Yuk #{}: {} ta kuryerga yangi yuk haqida xabar yuborildi", d.getId(), sent);
    }

    /** Kuryer olgan yuk bekor qilinsa, kuryerga xabar. */
    private void notifyCourierCancelled(DeliveryEntity d) {
        UserEntity courier = d.getCourier();
        if (courier == null) {
            return; // kuryer hali biriktirilmagan
        }
        send(courier,
                "❌ YUK BEKOR QILINDI\n\n"
                        + title(d) + " yo'nalishidagi yuk jo'natuvchi tomonidan bekor qilindi.",
                null,
                "bekor qilish, yuk " + d.getId());
    }

    // ---------------------------------------------------------------- jo'natuvchiga

    /** Kuryer buyurtmani qabul qilganda jo'natuvchiga kuryer haqida xabar. */
    private void notifySenderMatched(DeliveryEntity d) {
        UserEntity courier = d.getCourier();

        String who = courier == null
                ? "Ma'lumot mavjud emas"
                : Objects.toString(courier.getFullName(), "Ma'lumot mavjud emas");

        String vehicle = courier == null ? "Ma'lumot mavjud emas" : vehicle(courier);

        send(d.getSender(),
                "✅ KURYER TOPILDI!\n\n"
                        + "Yaxshi yangilik! " + title(d) + " yo'nalishidagi yukingizni kuryer qabul qildi.\n\n"
                        + "👤 Kuryer haqida ma'lumot:\n"
                        + "Ismi: " + who + "\n"
                        + "🚗 Transporti: " + vehicle + "\n\n"
                        + "📦 Tez orada kuryer yukingizni olib ketish uchun yo'lga chiqadi.\n\n"
                        + "Xizmatimizdan foydalanganingiz uchun rahmat!",
                null,
                "kuryer topildi (jo'natuvchi), yuk " + d.getId());
    }

    // ---------------------------------------------------------------- yordamchilar

    /** Ha/Yo'q tugmalari bitta qatorda. */
    private static Map<String, Object> confirmKeyboard(String yesData, String noData,
                                                       String yesText, String noText) {
        return TelegramBotClient.keyboard(List.of(List.of(
                TelegramBotClient.button(yesText, yesData),
                TelegramBotClient.button(noText, noData))));
    }

    /** Foydalanuvchiga ko'rsatiladigan yuk nomi: yo'nalish. ID ishlatilmaydi. */
    private static String title(DeliveryEntity d) {
        return "«" + TelegramUpdateHandler.routeLabel(d.getFromRegion(), d.getToRegion()) + "»";
    }

    private static String formatReward(DeliveryEntity d) {
        if (d.getReward() == null) {
            return "Ko'rsatilmagan";
        }
        return d.getReward().stripTrailingZeros().toPlainString() + " so'm";
    }

    /**
     * Bitta foydalanuvchiga xabar yuboradi. Telegram ID yo'q bo'lsa yoki yuborish xato bersa,
     * boshqa xabarlarga ta'sir qilmaydi va sababini log'ga yozadi.
     */
    private boolean send(UserEntity user, String text, Map<String, Object> markup, String context) {
        if (user == null) {
            log.warn("Telegram xabari o'tkazib yuborildi: foydalanuvchi topilmadi, {}", context);
            return false;
        }
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