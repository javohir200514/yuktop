package uz.onetimecourier.one_time_courier.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.onetimecourier.one_time_courier.entity.CourierRouteSubscription;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.repository.CourierRouteSubscriptionRepository;
import uz.onetimecourier.one_time_courier.repository.UserRepository;
import uz.onetimecourier.one_time_courier.service.DeliveryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Bot buyruqlari va inline tugmalar.
 * Klass @Transactional emas: DeliveryService o'z tranzaksiyasiga ega, xatoni shu yerda yutish
 * tashqi tranzaksiyani rollback-only qilib qo'ymasligi uchun.
 */
@Service
public class TelegramUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    static final int MAX_SUBSCRIPTIONS = 10;
    private static final String ANY = "*";
    private static final int MAX_ANSWER_LENGTH = 190;

    private final TelegramBotClient bot;
    private final UserRepository userRepository;
    private final CourierRouteSubscriptionRepository subscriptions;
    private final DeliveryService deliveryService;

    public TelegramUpdateHandler(TelegramBotClient bot,
                                 UserRepository userRepository,
                                 CourierRouteSubscriptionRepository subscriptions,
                                 DeliveryService deliveryService) {
        this.bot = bot;
        this.userRepository = userRepository;
        this.subscriptions = subscriptions;
        this.deliveryService = deliveryService;
    }

    // ================================================================ buyruqlar

    public void onMessage(long chatId, String text) {
        String command = text.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        switch (command) {
            case "/start" -> bot.sendMessage(chatId,
                    "Salom! YukBor botiga xush kelibsiz.\n\n"
                            + "Saytga kirish uchun saytdagi \"Telegram orqali kirish\" tugmasini bosing.\n"
                            + "Kuryer bo'lsangiz: /bildirishnoma",
                    null);
            case "/bildirishnoma" -> showMenu(chatId);
            default -> bot.sendMessage(chatId, "Buyruqlar: /bildirishnoma", null);
        }
    }

    private void showMenu(long chatId) {
        if (courierByChat(chatId).isEmpty()) {
            bot.sendMessage(chatId,
                    "Bu buyruq faqat kuryer hisobi uchun. Saytda kuryer sifatida ro'yxatdan o'ting.",
                    null);
            return;
        }
        bot.sendMessage(chatId,
                "Bildirishnomalar. Yangi yuk siz tanlagan yo'nalishda chiqsa, sizga xabar yuboriladi.",
                TelegramBotClient.keyboard(List.of(
                        List.of(TelegramBotClient.button("➕ Yo'nalish qo'shish", "sub:new")),
                        List.of(TelegramBotClient.button("📋 Yo'nalishlarim", "sub:list")))));
    }

    // ================================================================ tugmalar

    /**
     * Callback ma'lumot formatlari:
     *   sub:new            - yo'nalish qo'shish boshlanishi
     *   sub:list           - yo'nalishlar ro'yxati
     *   f:<kod>            - jo'nash hududi (kod: ordinal yoki *)
     *   t:<from>:<to>      - yetkazish hududi, yo'nalish saqlanadi
     *   del:<id>           - yo'nalishni o'chirish
     *   acc:<deliveryId>   - yukni qabul qilish
     */
    public void onCallback(String callbackId, long fromId, String data) {
        try {
            Optional<UserEntity> courier = courierByChat(fromId);
            if (courier.isEmpty()) {
                answer(callbackId, "Bu amal faqat kuryer hisobi uchun");
                return;
            }
            if (data == null) {
                answer(callbackId, null);
                return;
            }
            UserEntity c = courier.get();

            if (data.equals("sub:new")) {
                answer(callbackId, null);
                fromMenu(fromId);
            } else if (data.equals("sub:list")) {
                answer(callbackId, null);
                listSubscriptions(fromId, c);
            } else if (data.startsWith("f:")) {
                answer(callbackId, null);
                toMenu(fromId, data.substring(2));
            } else if (data.startsWith("t:")) {
                String[] p = data.substring(2).split(":", 2);
                subscribe(callbackId, fromId, c, regionOf(p[0]), regionOf(p[1]));
            } else if (data.startsWith("del:")) {
                delete(callbackId, c, Long.parseLong(data.substring(4)));
            } else if (data.startsWith("acc:")) {
                accept(callbackId, fromId, c, Long.parseLong(data.substring(4)));
            } else {
                answer(callbackId, null);
            }
        } catch (RuntimeException e) {
            log.warn("Telegram tugma xatosi: {}", e.getMessage());
            answer(callbackId, "Xato yuz berdi. Qayta urinib ko'ring.");
        }
    }

    // ================================================================ yo'nalishlar

    private void fromMenu(long chatId) {
        List<Map<String, Object>> buttons = new ArrayList<>();
        Region[] all = Region.values();
        for (int i = 0; i < all.length; i++) {
            buttons.add(TelegramBotClient.button(regionLabel(all[i]), "f:" + i));
        }
        buttons.add(TelegramBotClient.button("Istalgan", "f:" + ANY));
        bot.sendMessage(chatId, "Qayerdan yuk olasiz? (jo'nash hududi)",
                TelegramBotClient.keyboard(grid(buttons)));
    }

    private void toMenu(long chatId, String fromCode) {
        List<Map<String, Object>> buttons = new ArrayList<>();
        Region[] all = Region.values();
        for (int i = 0; i < all.length; i++) {
            buttons.add(TelegramBotClient.button(regionLabel(all[i]), "t:" + fromCode + ":" + i));
        }
        buttons.add(TelegramBotClient.button("Istalgan", "t:" + fromCode + ":" + ANY));
        bot.sendMessage(chatId, "Qayerga? (yetkazish hududi)",
                TelegramBotClient.keyboard(grid(buttons)));
    }

    private void subscribe(String callbackId, long chatId, UserEntity courier, Region from, Region to) {
        if (subscriptions.countByCourierId(courier.getId()) >= MAX_SUBSCRIPTIONS) {
            answer(callbackId, "Ko'pi bilan " + MAX_SUBSCRIPTIONS + " ta yo'nalish qo'shish mumkin");
            return;
        }
        if (subscriptions.findFirstByCourierIdAndFromRegionAndToRegion(courier.getId(), from, to).isPresent()) {
            answer(callbackId, "Bu yo'nalish allaqachon qo'shilgan");
            return;
        }
        subscriptions.save(new CourierRouteSubscription(courier, from, to));
        answer(callbackId, "Qo'shildi");
        bot.sendMessage(chatId,
                "✅ Yo'nalish qo'shildi: " + routeLabel(from, to)
                        + "\nShu yo'nalishda yangi yuk chiqsa, xabar yuboraman.",
                null);
    }

    private void listSubscriptions(long chatId, UserEntity courier) {
        List<CourierRouteSubscription> list = subscriptions.findByCourierIdOrderByIdAsc(courier.getId());
        if (list.isEmpty()) {
            bot.sendMessage(chatId, "Hozircha yo'nalish yo'q.",
                    TelegramBotClient.keyboard(List.of(
                            List.of(TelegramBotClient.button("➕ Yo'nalish qo'shish", "sub:new")))));
            return;
        }
        List<List<Map<String, Object>>> rows = new ArrayList<>();
        for (CourierRouteSubscription s : list) {
            rows.add(List.of(TelegramBotClient.button(
                    "❌ " + routeLabel(s.getFromRegion(), s.getToRegion()),
                    "del:" + s.getId())));
        }
        bot.sendMessage(chatId, "Yo'nalishlaringiz. O'chirish uchun bosing:",
                TelegramBotClient.keyboard(rows));
    }

    private void delete(String callbackId, UserEntity courier, long id) {
        subscriptions.findByIdAndCourierId(id, courier.getId()).ifPresentOrElse(
                s -> {
                    subscriptions.delete(s);
                    answer(callbackId, "O'chirildi");
                },
                () -> answer(callbackId, "Yo'nalish topilmadi"));
    }

    // ================================================================ qabul qilish

    private void accept(String callbackId, long chatId, UserEntity courier, long deliveryId) {
        try {
            // DeliveryService o'zi holat, limit va kuryer huquqini tekshiradi
            deliveryService.accept(courier.getId(), deliveryId);
        } catch (BusinessException e) {
            answer(callbackId, e.getMessage());
            return;
        }
        answer(callbackId, "Yuk sizga biriktirildi");
        bot.sendMessage(chatId,
                "✅ Yuk #" + deliveryId + " sizga biriktirildi. Yetkazish tafsilotlarini YukBor saytida ko'ring.",
                null);
    }

    // ================================================================ yordamchilar

    private Optional<UserEntity> courierByChat(long chatId) {
        return userRepository.findByTelegramId(chatId)
                .filter(u -> u.getRole() == Role.COURIER);
    }

    private void answer(String callbackId, String text) {
        if (callbackId == null) return;
        String trimmed = text == null ? null
                : (text.length() > MAX_ANSWER_LENGTH ? text.substring(0, MAX_ANSWER_LENGTH) : text);
        bot.answerCallback(callbackId, trimmed);
    }

    private static Region regionOf(String code) {
        return ANY.equals(code) ? null : Region.values()[Integer.parseInt(code)];
    }

    /** Tugmalarni ikki ustunli qatorlarga bo'ladi. */
    private static List<List<Map<String, Object>>> grid(List<Map<String, Object>> buttons) {
        List<List<Map<String, Object>>> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 2) {
            rows.add(buttons.subList(i, Math.min(i + 2, buttons.size())));
        }
        return rows;
    }

    /** Ko'rinadigan nom. Agar Region enumida getLabel() bo'lsa, shu yerni o'zgartiring. */
    public static String regionLabel(Region region) {
        return region == null ? "Istalgan" : region.name().replace('_', ' ');
    }

    public static String routeLabel(Region from, Region to) {
        return regionLabel(from) + " → " + regionLabel(to);
    }
}