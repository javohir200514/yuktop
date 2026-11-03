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
import uz.onetimecourier.one_time_courier.service.AuthService;
import uz.onetimecourier.one_time_courier.service.DeliveryService;
import uz.onetimecourier.one_time_courier.service.RatingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Bot buyruqlari, inline tugmalar va hisobni Telegram'ga ulash.
 * Klass @Transactional emas: DeliveryService o'z tranzaksiyasiga ega, xatoni shu yerda yutish
 * tashqi tranzaksiyani rollback-only qilib qo'ymasligi uchun.
 */
@Service
public class TelegramUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    static final int MAX_SUBSCRIPTIONS = 10;
    private static final String ANY = "*";
    private static final int MAX_ANSWER_LENGTH = 190;
    private static final Map<String, Object> REMOVE_KEYBOARD = Map.of("remove_keyboard", true);

    private final TelegramBotClient bot;
    private final UserRepository userRepository;
    private final CourierRouteSubscriptionRepository subscriptions;
    private final DeliveryService deliveryService;
    private final RatingService ratingService;

    public TelegramUpdateHandler(TelegramBotClient bot,
                                 UserRepository userRepository,
                                 CourierRouteSubscriptionRepository subscriptions,
                                 DeliveryService deliveryService,
                                 RatingService ratingService) {
        this.bot = bot;
        this.userRepository = userRepository;
        this.subscriptions = subscriptions;
        this.deliveryService = deliveryService;
        this.ratingService = ratingService;
    }

    // ================================================================ buyruqlar

    public void onMessage(long chatId, String text) {
        String command = text.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        switch (command) {
            case "/start" -> onStart(chatId);
            case "/bildirishnoma" -> showRouteMenu(chatId);
            default -> bot.sendMessage(chatId,
                    "Buyruqlar:\n/start - hisobni ulash\n/bildirishnoma - yo'nalishlar", null);
        }
    }

    private void onStart(long chatId) {
        Optional<UserEntity> user = userByChat(chatId);
        if (user.isPresent()) {
            bot.sendMessage(chatId, "Xush kelibsiz! Hisobingiz Telegram'ga ulangan.", REMOVE_KEYBOARD);
            if (user.get().getRole() == Role.COURIER) {
                showRouteMenu(chatId);
            }
            return;
        }
        bot.sendMessage(chatId,
                "Hisobingizni ulash uchun pastdagi \"Raqamni yuborish\" tugmasini bosing. "
                        + "Raqam saytda ro'yxatdan o'tgan raqam bilan bir xil bo'lishi kerak.",
                contactKeyboard());
    }

    // ================================================================ hisobni ulash

    /**
     * Telefon kontakti: agar bu saytda ro'yxatdan o'tgan hisobning raqami bo'lsa, telegramId saqlanadi.
     * true qaytsa, xabar shu yerda qayta ishlangan. false bo'lsa, telefon tasdiqlash oqimi davom etadi.
     */
    public boolean tryLinkContact(long chatId, long fromId, Long contactUserId, String rawPhone) {
        if (contactUserId == null || contactUserId != fromId) {
            return false; // boshqa odamning kontakti
        }
        String phone = normalize(rawPhone);
        if (phone == null) {
            return false;
        }
        Optional<UserEntity> found = userRepository.findByPhone(phone);
        if (found.isEmpty()) {
            return false; // yangi ro'yxatdan o'tayotgan foydalanuvchi
        }

        UserEntity user = found.get();
        if (user.getTelegramId() != null) {
            String message = user.getTelegramId() == fromId
                    ? "Hisobingiz allaqachon ulangan."
                    : "Bu hisob boshqa Telegram akkauntga ulangan.";
            bot.sendMessage(chatId, message, REMOVE_KEYBOARD);
            return true;
        }

        user.setTelegramId(fromId);
        userRepository.save(user);
        bot.sendMessage(chatId, "✅ Hisobingiz Telegram'ga ulandi.", REMOVE_KEYBOARD);
        if (user.getRole() == Role.COURIER) {
            showRouteMenu(chatId);
        }
        return true;
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AuthService.normalizePhone(raw);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ================================================================ tugmalar

    /**
     * Callback ma'lumot formatlari:
     *   rt:<yuk>:<baho>           - yetkazilgan yukni baholash (kuryer ham, jo'natuvchi ham bosadi)
     *   sub:new / sub:list        - yo'nalish qo'shish / ro'yxat
     *   f:<kod>                   - jo'nash hududi
     *   t:<from>:<to>             - yetkazish hududi, yo'nalish saqlanadi
     *   del:<id>                  - yo'nalishni o'chirish
     *   acc:<id>                  - yukni qabul qilish
     *   pick:<id>                 - yuk olib ketildi ("Ha")
     *   dlv:<id>                  - yuk yetkazildi ("Ha")
     *   no:<id>                   - "Yo'q", keyinroq eslatiladi
     */
    public void onCallback(String callbackId, long fromId, String data) {
        try {
            // Baholash kuryer va jo'natuvchi uchun umumiy, shuning uchun kuryer tekshiruvidan oldin
            if (data != null && data.startsWith("rt:")) {
                rate(callbackId, fromId, data);
                return;
            }

            Optional<UserEntity> user = userByChat(fromId);
            if (user.isEmpty() || user.get().getRole() != Role.COURIER) {
                answer(callbackId, "Bu amal faqat kuryer hisobi uchun");
                return;
            }
            if (data == null) {
                answer(callbackId, null);
                return;
            }
            UserEntity courier = user.get();

            if (data.equals("sub:new")) {
                answer(callbackId, null);
                fromMenu(fromId);
            } else if (data.equals("sub:list")) {
                answer(callbackId, null);
                listSubscriptions(fromId, courier);
            } else if (data.startsWith("f:")) {
                answer(callbackId, null);
                toMenu(fromId, data.substring(2));
            } else if (data.startsWith("t:")) {
                String[] p = data.substring(2).split(":", 2);
                subscribe(callbackId, fromId, courier, regionOf(p[0]), regionOf(p[1]));
            } else if (data.startsWith("del:")) {
                delete(callbackId, courier, Long.parseLong(data.substring(4)));
            } else if (data.startsWith("acc:")) {
                accept(callbackId, fromId, courier, Long.parseLong(data.substring(4)));
            } else if (data.startsWith("pick:")) {
                pickUp(callbackId, fromId, courier, Long.parseLong(data.substring(5)));
            } else if (data.startsWith("dlv:")) {
                deliver(callbackId, fromId, courier, Long.parseLong(data.substring(4)));
            } else if (data.startsWith("no:")) {
                answer(callbackId, "Yaxshi, keyinroq eslataman");
            } else {
                answer(callbackId, null);
            }
        } catch (RuntimeException e) {
            log.warn("Telegram tugma xatosi: {}", e.getMessage());
            answer(callbackId, "Xato yuz berdi. Qayta urinib ko'ring.");
        }
    }

    // ================================================================ baholash

    /** rt:<yuk_id>:<baho>. Baholovchi Telegram ID bo'yicha topiladi. */
    private void rate(String callbackId, long fromId, String data) {
        String[] parts = data.split(":");
        if (parts.length != 3) {
            answer(callbackId, "Noto'g'ri so'rov");
            return;
        }

        final long deliveryId;
        final int stars;
        try {
            deliveryId = Long.parseLong(parts[1]);
            stars = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            answer(callbackId, "Noto'g'ri so'rov");
            return;
        }

        Optional<UserEntity> user = userByChat(fromId);
        if (user.isEmpty()) {
            answer(callbackId, "Hisobingiz Telegram'ga ulanmagan");
            return;
        }

        try {
            ratingService.rate(user.get().getId(), deliveryId, stars, null);
            answer(callbackId, "✅ Rahmat! Bahoyingiz saqlandi");
        } catch (BusinessException e) {
            answer(callbackId, e.getMessage());
        }
    }

    // ================================================================ yo'nalishlar

    private void showRouteMenu(long chatId) {
        bot.sendMessage(chatId,
                "Bildirishnomalar. Yangi yuk siz tanlagan yo'nalishda chiqsa, sizga xabar yuboriladi.",
                TelegramBotClient.keyboard(List.of(
                        List.of(TelegramBotClient.button("➕ Yo'nalish qo'shish", "sub:new")),
                        List.of(TelegramBotClient.button("📋 Yo'nalishlarim", "sub:list")))));
    }

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

    // ================================================================ yuk holati

    private void accept(String callbackId, long chatId, UserEntity courier, long deliveryId) {
        try {
            deliveryService.accept(courier.getId(), deliveryId);
        } catch (BusinessException e) {
            answer(callbackId, e.getMessage());
            return;
        }
        answer(callbackId, "Yuk sizga biriktirildi");
        bot.sendMessage(chatId,
                "✅ Yuk #" + deliveryId + " sizga biriktirildi. Olib ketganingizda pastdagi tugmani bosing.",
                TelegramBotClient.keyboard(List.of(List.of(
                        TelegramBotClient.button("🚚 Olib ketdim", "pick:" + deliveryId)))));
    }

    private void pickUp(String callbackId, long chatId, UserEntity courier, long deliveryId) {
        try {
            deliveryService.pickUp(courier.getId(), deliveryId);
        } catch (BusinessException e) {
            answer(callbackId, e.getMessage());
            return;
        }
        answer(callbackId, "Yuk olib ketildi");
        bot.sendMessage(chatId,
                "🚚 Yuk #" + deliveryId + " olib ketildi. Yetkazganingizda pastdagi tugmani bosing.",
                TelegramBotClient.keyboard(List.of(List.of(
                        TelegramBotClient.button("🏁 Yetkazdim", "dlv:" + deliveryId)))));
    }

    private void deliver(String callbackId, long chatId, UserEntity courier, long deliveryId) {
        try {
            deliveryService.deliver(courier.getId(), deliveryId);
        } catch (BusinessException e) {
            answer(callbackId, e.getMessage());
            return;
        }
        answer(callbackId, "Yetkazildi");
        bot.sendMessage(chatId, "🎉 Yuk #" + deliveryId + " yetkazildi. Rahmat!", null);
    }

    // ================================================================ yordamchilar

    private Optional<UserEntity> userByChat(long chatId) {
        return userRepository.findByTelegramId(chatId);
    }

    private void answer(String callbackId, String text) {
        if (callbackId == null) return;
        String trimmed = text == null ? null
                : (text.length() > MAX_ANSWER_LENGTH ? text.substring(0, MAX_ANSWER_LENGTH) : text);
        bot.answerCallback(callbackId, trimmed);
    }

    /** Telefon raqamini so'rovchi reply klaviatura. */
    private static Map<String, Object> contactKeyboard() {
        Map<String, Object> button = Map.of("text", "Raqamni yuborish", "request_contact", true);
        return Map.of(
                "keyboard", List.of(List.of(button)),
                "resize_keyboard", true,
                "one_time_keyboard", true);
    }

    private static Region regionOf(String code) {
        return ANY.equals(code) ? null : Region.values()[Integer.parseInt(code)];
    }

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