package uz.onetimecourier.one_time_courier.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import uz.onetimecourier.one_time_courier.dto.*;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.mapper.DeliveryMapper;
import uz.onetimecourier.one_time_courier.repository.DeliveryRepository;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class DeliveryService {

    public static final int MAX_ACTIVE_DELIVERIES = 20;
    public static final int MAX_ACTIVE_COURIER_DELIVERIES = 10;
    public static final int MAX_PAGE_SIZE = 50;
    public static final int MAX_CANCEL_REASON = 300;
    static final String SENDER_CONFIRM_FAILED =
            "Tasdiqlab bo'lmadi. Kuryer hali yuk olganini bildirmagan bo'lishi mumkin.";
    static final String SENDER_DELIVERY_CONFIRM_FAILED =
            "Tasdiqlab bo'lmadi. Kuryer hali yuk yetkazilganini bildirmagan bo'lishi mumkin.";
    /** O'zbekiston hududini qamrab oluvchi taxminiy to'rtburchak. */
    public static final double MIN_LAT = 37.1;
    public static final double MAX_LAT = 45.6;
    public static final double MIN_LNG = 55.9;
    public static final double MAX_LNG = 73.2;

    static final String NOT_FOUND = "Yuk topilmadi";
    static final String CANCEL_FAILED =
            "Yukni bekor qilib bo'lmadi. U topilmadi yoki holati o'zgargan bo'lishi mumkin.";
    static final String EDIT_NOT_ALLOWED =
            "Faqat «Kuryer qidirilmoqda» holatidagi yukni tahrirlash mumkin.";
    static final String ACCEPT_FAILED =
            "Bu yukni qabul qilib bo'lmadi. Uni boshqa kuryer olgan yoki bekor qilingan bo'lishi mumkin.";
    static final String PICK_UP_FAILED =
            "Yukni «Yo'lda» holatiga o'tkazib bo'lmadi. Yuk sizga biriktirilmagan yoki holati o'zgargan.";
    static final String PICK_UP_ALREADY =
            "Siz allaqachon olganingizni bildirgansiz. Jo'natuvchi tasdig'ini kuting.";
    static final String DELIVER_FAILED =
            "Yukni yetkazildi deb belgilab bo'lmadi. Yuk sizga biriktirilmagan yoki holati o'zgargan.";
    static final String DELIVER_ALREADY =
            "Siz allaqachon yetkazganingizni bildirgansiz. Jo'natuvchi tasdig'ini kuting.";
    static final String CONTACT_REQUIRED =
            "Aloqa uchun telefon raqami yoki Telegram username kiriting";

    private static final List<DeliveryStatus> ACTIVE_STATUSES =
            List.of(DeliveryStatus.CREATED, DeliveryStatus.MATCHED, DeliveryStatus.PICKED_UP);

    private static final List<DeliveryStatus> COURIER_ACTIVE_STATUSES =
            List.of(DeliveryStatus.MATCHED, DeliveryStatus.PICKED_UP);

    private static final List<DeliveryStatus> CANCELLABLE_STATUSES =
            List.of(DeliveryStatus.CREATED, DeliveryStatus.MATCHED);

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+998\\d{9}$");
    private static final Pattern TELEGRAM_PATTERN = Pattern.compile("^[A-Za-z0-9_]{5,32}$");

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    // ================================================================ yaratish

    public DeliveryDto create(@NotNull Long senderId, @NotNull @Valid CreateDeliveryRequestDto request) {
        UserEntity sender = requireSender(senderId);

        // Bir vaqtda kelgan so'rovlar limitni aylanib o'tmasligi uchun jo'natuvchi qatorini qulflaymiz
        entityManager.lock(sender, LockModeType.PESSIMISTIC_WRITE);

        long active = deliveryRepository.countBySenderIdAndStatusIn(senderId, ACTIVE_STATUSES);
        if (active >= MAX_ACTIVE_DELIVERIES) {
            throw new BusinessException("Bir vaqtda ko'pi bilan " + MAX_ACTIVE_DELIVERIES
                    + " ta faol yukingiz bo'lishi mumkin. Avvalgi yuklar yakunlanishini kuting yoki ularni bekor qiling.");
        }

        DeliveryEntity entity = new DeliveryEntity();
        entity.setSender(sender);
        applyRequest(entity, request);
        applyContact(entity, sender, request);
        entity.setStatus(DeliveryStatus.CREATED);

        DeliveryEntity saved = deliveryRepository.save(entity);
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(senderId, saved.getId(), saved.getStatus()));
        return DeliveryMapper.toDto(saved);
    }

    // ================================================================ tahrirlash

    /** Faqat egasi va faqat CREATED holatida. Qator qulflanadi, shu sababli bekor qilish bilan to'qnashmaydi. */
    public DeliveryDto update(@NotNull Long senderId,
                              @NotNull Long id,
                              @NotNull @Valid CreateDeliveryRequestDto request) {
        UserEntity sender = requireSender(senderId);

        DeliveryEntity entity = deliveryRepository.findForUpdateByIdAndSenderId(id, senderId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));

        if (entity.getStatus() != DeliveryStatus.CREATED) {
            throw new BusinessException(EDIT_NOT_ALLOWED);
        }

        applyRequest(entity, request);
        applyContact(entity, sender, request);
        DeliveryEntity saved = deliveryRepository.save(entity);
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(senderId, saved.getId(), saved.getStatus()));
        return DeliveryMapper.toDto(saved);
    }

    // ================================================================ jo'natuvchi: o'qish

    @Transactional(readOnly = true)
    public DeliveryDto getMine(@NotNull Long senderId, @NotNull Long id) {
        requireSender(senderId);
        // Egasi bo'lmasa ham, mavjud bo'lmasa ham bir xil xabar: boshqa ID'larni topib bo'lmasin
        return deliveryRepository.findByIdAndSenderId(id, senderId)
                .map(DeliveryMapper::toDto)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    /**
     * Jo'natuvchining FAQAT o'z yuklari, yangisi birinchi.
     * statuses null yoki bo'sh bo'lsa, hamma holatdagi yuklar qaytadi.
     */
    @Transactional(readOnly = true)
    public Page<DeliverySummaryDto> mine(@NotNull Long senderId,
                                         Set<DeliveryStatus> statuses,
                                         @NotNull Pageable pageable) {
        requireSender(senderId);
        Pageable safe = safePageable(pageable);

        Page<DeliveryEntity> result = (statuses == null || statuses.isEmpty())
                ? deliveryRepository.findBySenderId(senderId, safe)
                : deliveryRepository.findBySenderIdAndStatusIn(senderId, statuses, safe);

        return result.map(DeliveryMapper::toSummaryDto);
    }

    /**
     * Jo'natuvchi uchun: yukka biriktirilgan kuryer ma'lumoti.
     * Faqat egasi uchun va faqat kuryer biriktirilgan, bekor qilinmagan yuk uchun.
     */
    @Transactional(readOnly = true)
    public Optional<CourierInfoDto> courierInfo(@NotNull Long senderId, @NotNull Long id) {
        requireSender(senderId);
        DeliveryEntity d = deliveryRepository.findByIdAndSenderId(id, senderId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));

        UserEntity c = d.getCourier();
        if (c == null
                || d.getStatus() == DeliveryStatus.CREATED
                || d.getStatus() == DeliveryStatus.CANCELLED) {
            return Optional.empty();
        }
        return Optional.of(new CourierInfoDto(
                c.getFullName(),
                c.getPhone(),
                c.getTelegramUsername(),
                c.getVehicleType(),
                c.getVehicleBrand(),
                c.getVehicleModel(),
                c.getVehicleColor(),
                c.getPlateNumber(),
                d.getCourierPickupAt() != null,
                d.getSenderPickupConfirmedAt() != null));
    }

    /**
     * Jo'natuvchi yetkazilganini tasdiqlashi kerakmi: kuryer "Yetkazdim" degan, lekin jo'natuvchi hali tasdiqlamagan.
     */
    @Transactional(readOnly = true)
    public boolean awaitingDeliveryConfirmation(@NotNull Long senderId, @NotNull Long id) {
        return deliveryRepository.findByIdAndSenderId(id, senderId)
                .map(d -> d.getStatus() == DeliveryStatus.PICKED_UP
                        && d.getCourierDeliveryAt() != null
                        && d.getSenderDeliveryConfirmedAt() == null)
                .orElse(false);
    }

    /** Forma uchun: kontakt so'ralishi kerakmi va oxirgi kiritilgan kontakt. */
    @Transactional(readOnly = true)
    public SenderContactInfoDto contactInfo(@NotNull Long senderId) {
        UserEntity sender = requireSender(senderId);
        if (tryNormalizePhone(sender.getPhone()) != null) {
            return new SenderContactInfoDto(false, null, null);
        }
        List<DeliveryEntity> last = deliveryRepository.findWithContact(senderId, PageRequest.of(0, 1));
        if (last.isEmpty()) {
            return new SenderContactInfoDto(true, null, null);
        }
        DeliveryEntity d = last.get(0);
        return new SenderContactInfoDto(true, d.getSenderContactPhone(), d.getSenderContactTelegram());
    }

    // ================================================================ ochiq ro'yxat

    /** Hammaga ochiq: faqat CREATED yuklar, faqat ochiq maydonlar. */
    @Transactional(readOnly = true)
    public Page<DeliveryPublicDto> open(Region from, Region to, @NotNull Pageable pageable) {
        return findOpen(from, to, safePageable(pageable)).map(DeliveryMapper::toPublicDto);
    }

    /** Kirgan kuryer uchun: ochiq ma'lumot + jo'natuvchi ismi va niqoblangan kontakt. */
    @Transactional(readOnly = true)
    public Page<CourierOpenDeliveryDto> openForCourier(@NotNull Long courierId,
                                                       Region from,
                                                       Region to,
                                                       @NotNull Pageable pageable) {
        requireCourier(courierId);
        return findOpen(from, to, safePageable(pageable)).map(DeliveryMapper::toCourierOpenDto);
    }

    /** Kuryerning FAQAT o'ziga biriktirilgan yuklari, to'liq ma'lumot bilan. */
    @Transactional(readOnly = true)
    public Page<CourierDeliveryDto> courierDeliveries(@NotNull Long courierId,
                                                      Set<DeliveryStatus> statuses,
                                                      @NotNull Pageable pageable) {
        requireCourier(courierId);
        Pageable safe = safePageable(pageable);

        Page<DeliveryEntity> result = (statuses == null || statuses.isEmpty())
                ? deliveryRepository.findByCourierId(courierId, safe)
                : deliveryRepository.findByCourierIdAndStatusIn(courierId, statuses, safe);

        return result.map(DeliveryMapper::toCourierDto);
    }

    private Page<DeliveryEntity> findOpen(Region from, Region to, Pageable pageable) {
        DeliveryStatus status = DeliveryStatus.CREATED;
        if (from != null && to != null) {
            return deliveryRepository.findByStatusAndFromRegionAndToRegion(status, from, to, pageable);
        }
        if (from != null) {
            return deliveryRepository.findByStatusAndFromRegion(status, from, pageable);
        }
        if (to != null) {
            return deliveryRepository.findByStatusAndToRegion(status, to, pageable);
        }
        return deliveryRepository.findByStatus(status, pageable);
    }

    // ================================================================ bekor qilish

    /** Atomik UPDATE: faqat egasi, faqat CREATED yoki MATCHED. 0 qator o'zgarsa, xato. */
    public void cancel(@NotNull Long senderId, @NotNull Long id, String reason) {
        requireSender(senderId);

        String cleanReason = singleLine(reason);
        if (cleanReason.length() > MAX_CANCEL_REASON) {
            throw new BusinessException("Sabab " + MAX_CANCEL_REASON + " belgidan oshmasligi kerak");
        }

        int updated = deliveryRepository.cancelIfAllowed(
                id,
                senderId,
                CANCELLABLE_STATUSES,
                DeliveryStatus.CANCELLED,
                LocalDateTime.now(),
                cleanReason.isEmpty() ? null : cleanReason);

        if (updated == 0) {
            throw new BusinessException(CANCEL_FAILED);
        }
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(senderId, id, DeliveryStatus.CANCELLED));
    }

    // ================================================================ kuryer amallari

    /**
     * CREATED → MATCHED. Yuk hali kuryersiz bo'lishi shart, ikki kuryer bir vaqtda olsa bittasi yutadi.
     * Kuryer qatori qulflanadi, shuning uchun faol yuklar limiti aylanib o'tilmaydi.
     */
    public void accept(@NotNull Long courierId, @NotNull Long id) {
        UserEntity courier = requireCourier(courierId);
        entityManager.lock(courier, LockModeType.PESSIMISTIC_WRITE);

        long active = deliveryRepository.countByCourierIdAndStatusIn(courierId, COURIER_ACTIVE_STATUSES);
        if (active >= MAX_ACTIVE_COURIER_DELIVERIES) {
            throw new BusinessException("Bir vaqtda ko'pi bilan " + MAX_ACTIVE_COURIER_DELIVERIES
                    + " ta faol yuk olishingiz mumkin. Avval olgan yuklaringizni yetkazing.");
        }

        int updated = deliveryRepository.acceptIfCreated(
                id, courier, DeliveryStatus.CREATED, DeliveryStatus.MATCHED, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException(ACCEPT_FAILED);
        }
        publishChange(id, DeliveryStatus.MATCHED);
    }

    /** Kuryer "Olib ketdim" deydi. Holat hali MATCHED; jo'natuvchi tasdiqlagach PICKED_UP bo'ladi. */
    public void pickUp(@NotNull Long courierId, @NotNull Long id) {
        requireCourier(courierId);
        DeliveryEntity d = lockDelivery(id);

        if (d.getStatus() != DeliveryStatus.MATCHED
                || d.getCourier() == null
                || !courierId.equals(d.getCourier().getId())) {
            throw new BusinessException(PICK_UP_FAILED);
        }
        if (d.getCourierPickupAt() != null) {
            throw new BusinessException(PICK_UP_ALREADY);
        }

        d.setCourierPickupAt(LocalDateTime.now());
        finishPickupIfReady(d);
        publishChange(d.getId(), d.getStatus());
    }

    /** Jo'natuvchi kuryer yukni olganini tasdiqlaydi. Ikkala tomon tasdiqlagach holat PICKED_UP bo'ladi. */
    public void confirmPickupBySender(@NotNull Long senderId, @NotNull Long id) {
        requireSender(senderId);
        DeliveryEntity d = lockDelivery(id);

        if (d.getSender() == null || !senderId.equals(d.getSender().getId())) {
            throw new BusinessException(NOT_FOUND);
        }
        if (d.getStatus() != DeliveryStatus.MATCHED || d.getCourierPickupAt() == null) {
            throw new BusinessException(SENDER_CONFIRM_FAILED);
        }
        if (d.getSenderPickupConfirmedAt() != null) {
            throw new BusinessException("Siz allaqachon tasdiqlagansiz.");
        }

        d.setSenderPickupConfirmedAt(LocalDateTime.now());
        finishPickupIfReady(d);
        publishChange(d.getId(), d.getStatus());
    }

    /**
     * Kuryer "Yetkazdim" deydi. Holat hali PICKED_UP; jo'natuvchi tasdiqlagach DELIVERED bo'ladi.
     */
    public void deliver(@NotNull Long courierId, @NotNull Long id) {
        requireCourier(courierId);
        DeliveryEntity d = lockDelivery(id);

        if (d.getStatus() != DeliveryStatus.PICKED_UP
                || d.getCourier() == null
                || !courierId.equals(d.getCourier().getId())) {
            throw new BusinessException(DELIVER_FAILED);
        }
        if (d.getCourierDeliveryAt() != null) {
            throw new BusinessException(DELIVER_ALREADY);
        }

        d.setCourierDeliveryAt(LocalDateTime.now());
        finishDeliveryIfReady(d);
        publishChange(d.getId(), d.getStatus());
    }

    /** Jo'natuvchi yukning yetkazilganini tasdiqlaydi. Ikkala tomon tasdiqlagach holat DELIVERED bo'ladi. */
    public void confirmDeliveryBySender(@NotNull Long senderId, @NotNull Long id) {
        requireSender(senderId);
        DeliveryEntity d = lockDelivery(id);

        if (d.getSender() == null || !senderId.equals(d.getSender().getId())) {
            throw new BusinessException(NOT_FOUND);
        }
        if (d.getStatus() != DeliveryStatus.PICKED_UP || d.getCourierDeliveryAt() == null) {
            throw new BusinessException(SENDER_DELIVERY_CONFIRM_FAILED);
        }
        if (d.getSenderDeliveryConfirmedAt() != null) {
            throw new BusinessException("Siz allaqachon tasdiqlagansiz.");
        }

        d.setSenderDeliveryConfirmedAt(LocalDateTime.now());
        finishDeliveryIfReady(d);
        publishChange(d.getId(), d.getStatus());
    }

    /** Yuk qatorini qulflaydi, shunda ikki tomonning bir vaqtdagi tasdig'i to'qnashmaydi. */
    private DeliveryEntity lockDelivery(Long id) {
        DeliveryEntity d = entityManager.find(DeliveryEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (d == null) {
            throw new BusinessException(NOT_FOUND);
        }
        return d;
    }

    /** Ikkala tomon ham belgilagan bo'lsa, holat PICKED_UP ga o'tadi. */
    private static void finishPickupIfReady(DeliveryEntity d) {
        if (d.getStatus() == DeliveryStatus.MATCHED
                && d.getCourierPickupAt() != null
                && d.getSenderPickupConfirmedAt() != null) {
            d.setStatus(DeliveryStatus.PICKED_UP);
        }
    }

    /** Ikkala tomon ham belgilagan bo'lsa, holat DELIVERED ga o'tadi. */
    private static void finishDeliveryIfReady(DeliveryEntity d) {
        if (d.getStatus() == DeliveryStatus.PICKED_UP
                && d.getCourierDeliveryAt() != null
                && d.getSenderDeliveryConfirmedAt() != null) {
            d.setStatus(DeliveryStatus.DELIVERED);
        }
    }

    private void publishChange(Long deliveryId, DeliveryStatus status) {
        deliveryRepository.findSenderIdById(deliveryId).ifPresent(senderId ->
                eventPublisher.publishEvent(new DeliveryStatusChangedEvent(senderId, deliveryId, status)));
    }

    // ================================================================ yordamchilar

    private UserEntity requireSender(Long senderId) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException("Foydalanuvchi topilmadi"));
        if (sender.getRole() != Role.SENDER) {
            throw new BusinessException("Yuk joylash uchun jo'natuvchi hisobi kerak");
        }
        return sender;
    }

    private UserEntity requireCourier(Long courierId) {
        UserEntity courier = userRepository.findById(courierId)
                .orElseThrow(() -> new BusinessException("Foydalanuvchi topilmadi"));
        if (courier.getRole() != Role.COURIER) {
            throw new BusinessException("Bu amal uchun kuryer hisobi kerak");
        }
        return courier;
    }

    /** Chaqiruvchiga ishonmaymiz: sahifa hajmi cheklanadi, tartib serverda belgilanadi (yangisi birinchi). */
    private static Pageable safePageable(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        int page = Math.max(pageable.getPageNumber(), 0);
        return PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    /** Yaratish va tahrirlash uchun umumiy: matnni tozalaydi, tekshiradi va maydonlarni o'rnatadi. */
    private void applyRequest(DeliveryEntity entity, CreateDeliveryRequestDto request) {
        String pickupAddress = singleLine(request.pickupAddress());
        String dropoffAddress = singleLine(request.dropoffAddress());
        String recipientName = singleLine(request.recipientName());
        String description = multiLine(request.description());

        if (pickupAddress.length() < 5 || dropoffAddress.length() < 5) {
            throw new BusinessException("Manzil juda qisqa. Aniqroq kiriting");
        }
        if (recipientName.length() < 2) {
            throw new BusinessException("Qabul qiluvchi ismi juda qisqa");
        }

        validateCoordinates(request.pickupLat(), request.pickupLng(), "Olib ketish");
        validateCoordinates(request.dropoffLat(), request.dropoffLng(), "Yetkazish");

        entity.setFromRegion(request.fromRegion());
        entity.setToRegion(request.toRegion());
        entity.setDeliveryType(request.fromRegion() == request.toRegion()
                ? DeliveryType.INTRACITY
                : DeliveryType.INTERCITY);
        entity.setPickupAddress(pickupAddress);
        entity.setDropoffAddress(dropoffAddress);
        entity.setPickupLat(request.pickupLat());
        entity.setPickupLng(request.pickupLng());
        entity.setDropoffLat(request.dropoffLat());
        entity.setDropoffLng(request.dropoffLng());
        entity.setPackageSize(request.packageSize());
        entity.setWeightKg(request.weightKg());
        entity.setDescription(description.isEmpty() ? null : description);
        entity.setRecipientName(recipientName);
        entity.setRecipientPhone(normalizeRecipientPhone(request.recipientPhone()));
        entity.setReward(request.reward());
    }

    /**
     * Jo'natuvchi kontakti. Hisobida telefon bo'lsa, o'sha raqam olinadi (forma yuborgan qiymat e'tiborga olinmaydi).
     * Aks holda telefon yoki Telegram username majburiy. Hozircha hamma kontakt "tasdiqlanmagan".
     */
    private void applyContact(DeliveryEntity entity, UserEntity sender, CreateDeliveryRequestDto request) {
        String own = tryNormalizePhone(sender.getPhone());
        if (own != null) {
            entity.setSenderContactPhone(own);
            entity.setSenderContactTelegram(null);
            // Hisob telefoni ro'yxatdan o'tishda Telegram orqali tasdiqlangan bo'ladi
            entity.setSenderContactVerified(sender.getTelegramId() != null);
            return;
        }

        String phone = null;
        String rawPhone = singleLine(request.contactPhone());
        if (!rawPhone.isEmpty()) {
            phone = tryNormalizePhone(rawPhone);
            if (phone == null) {
                throw new BusinessException("Aloqa telefon raqami noto'g'ri");
            }
        }

        String telegram = singleLine(request.contactTelegram());
        if (telegram.startsWith("@")) {
            telegram = telegram.substring(1);
        }
        if (!telegram.isEmpty() && !TELEGRAM_PATTERN.matcher(telegram).matches()) {
            throw new BusinessException("Telegram username noto'g'ri");
        }

        if (phone == null && telegram.isEmpty()) {
            boolean hasExisting = notBlank(entity.getSenderContactPhone())
                    || notBlank(entity.getSenderContactTelegram());
            if (hasExisting) {
                return; // tahrirlashda avvalgi kontakt saqlanadi
            }
            throw new BusinessException(CONTACT_REQUIRED);
        }

        entity.setSenderContactPhone(phone);
        entity.setSenderContactTelegram(telegram.isEmpty() ? null : telegram);
        entity.setSenderContactVerified(false);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static void validateCoordinates(Double lat, Double lng, String what) {
        if (lat == null && lng == null) {
            return;
        }
        if (lat == null || lng == null) {
            throw new BusinessException(what + " joylashuvi to'liq emas. Qayta belgilang");
        }
        boolean finite = !lat.isNaN() && !lat.isInfinite() && !lng.isNaN() && !lng.isInfinite();
        boolean inside = finite && lat >= MIN_LAT && lat <= MAX_LAT && lng >= MIN_LNG && lng <= MAX_LNG;
        if (!inside) {
            throw new BusinessException(what + " joylashuvi O'zbekiston hududidan tashqarida");
        }
    }

    private static String normalizeRecipientPhone(String raw) {
        String phone = tryNormalizePhone(raw);
        if (phone == null) {
            throw new BusinessException("Qabul qiluvchi telefon raqami noto'g'ri");
        }
        return phone;
    }

    /** Raqamni +998XXXXXXXXX ko'rinishiga keltiradi, bo'lmasa null. */
    private static String tryNormalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String phone = AuthService.normalizePhone(raw);
            return phone != null && PHONE_PATTERN.matcher(phone).matches() ? phone : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Bir qatorli matn: bo'shliqlar yagona probelga, boshqaruv va ko'rinmas format belgilari olib tashlanadi. */
    private static String singleLine(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", " ")
                .replaceAll("[\\p{Cc}\\p{Cf}]", "")
                .trim();
    }

    /** Ko'p qatorli matn: yangi qatorlar saqlanadi, boshqa boshqaruv belgilari olib tashlanadi. */
    private static String multiLine(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\r\\n?", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("[\\p{Cc}\\p{Cf}&&[^\\n]]", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}