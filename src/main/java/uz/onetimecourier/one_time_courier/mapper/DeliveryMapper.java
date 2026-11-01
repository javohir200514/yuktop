package uz.onetimecourier.one_time_courier.mapper;

import uz.onetimecourier.one_time_courier.dto.*;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeliveryMapper {

    private static final Pattern UZ_PHONE = Pattern.compile("^\\+998(\\d{2})(\\d{3})(\\d{2})(\\d{2})$");

    private DeliveryMapper() {
    }

    public static DeliveryDto toDto(DeliveryEntity e) {
        if (e == null) {
            return null;
        }
        return new DeliveryDto(
                e.getId(),
                e.getFromRegion(),
                e.getToRegion(),
                e.getDeliveryType(),
                e.getPickupAddress(),
                e.getDropoffAddress(),
                e.getPickupLat(),
                e.getPickupLng(),
                e.getDropoffLat(),
                e.getDropoffLng(),
                e.getCourierLat(),
                e.getCourierLng(),
                e.getCourierUpdatedAt(),
                e.getPackageSize(),
                e.getWeightKg(),
                e.getDescription(),
                e.getRecipientName(),
                e.getRecipientPhone(),
                e.getReward(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getMatchedAt(),
                e.getCancelledAt(),
                e.getCancelReason(),
                e.getSenderContactPhone(),
                e.getSenderContactTelegram(),
                e.isSenderContactVerified()
        );
    }

    public static DeliverySummaryDto toSummaryDto(DeliveryEntity e) {
        if (e == null) {
            return null;
        }
        return new DeliverySummaryDto(
                e.getId(),
                e.getFromRegion(),
                e.getToRegion(),
                e.getDeliveryType(),
                e.getPackageSize(),
                e.getWeightKg(),
                e.getReward(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getPickupAddress(),
                e.getDropoffAddress(),
                e.getRecipientName()
        );
    }

    /** Hammaga ochiq: faqat viloyatlar, yuk turi, og'irlik, mukofot, vaqt. */
    public static DeliveryPublicDto toPublicDto(DeliveryEntity e) {
        if (e == null) {
            return null;
        }
        return new DeliveryPublicDto(
                e.getId(),
                e.getFromRegion(),
                e.getToRegion(),
                e.getDeliveryType(),
                e.getPackageSize(),
                e.getWeightKg(),
                e.getReward(),
                e.getCreatedAt()
        );
    }

    /** Kirgan kuryer uchun: ochiq ma'lumot + jo'natuvchining ismi (birinchi so'z) va niqoblangan kontakt. */
    public static CourierOpenDeliveryDto toCourierOpenDto(DeliveryEntity e) {
        if (e == null) {
            return null;
        }
        UserEntity sender = e.getSender();
        return new CourierOpenDeliveryDto(
                toPublicDto(e),
                firstName(sender == null ? null : sender.getFullName()),
                contactHint(e),
                e.isSenderContactVerified()
        );
    }

    /** Yukni qabul qilgan kuryer uchun to'liq ma'lumot. */
    public static CourierDeliveryDto toCourierDto(DeliveryEntity e) {
        if (e == null) {
            return null;
        }
        UserEntity sender = e.getSender();
        return new CourierDeliveryDto(
                e.getId(),
                e.getFromRegion(),
                e.getToRegion(),
                e.getDeliveryType(),
                e.getPickupAddress(),
                e.getDropoffAddress(),
                e.getPickupLat(),
                e.getPickupLng(),
                e.getDropoffLat(),
                e.getDropoffLng(),
                e.getPackageSize(),
                e.getWeightKg(),
                e.getDescription(),
                e.getRecipientName(),
                e.getRecipientPhone(),
                e.getReward(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getMatchedAt(),
                sender == null ? null : sender.getFullName(),
                contactPhone(e),
                e.getSenderContactTelegram(),
                e.isSenderContactVerified(),
                e.getCourierPickupAt() != null
        );
    }

    /** Yukda kontakt telefoni bo'lmasa (eski yuklar), jo'natuvchining hisobidagi raqam ishlatiladi. */
    static String contactPhone(DeliveryEntity e) {
        String own = e.getSenderContactPhone();
        if (own != null && !own.isBlank()) {
            return own;
        }
        UserEntity sender = e.getSender();
        String fromAccount = sender == null ? null : sender.getPhone();
        return fromAccount == null || fromAccount.isBlank() ? null : fromAccount;
    }

    static String contactHint(DeliveryEntity e) {
        String phone = contactPhone(e);
        if (phone != null) {
            return maskPhone(phone);
        }
        String telegram = e.getSenderContactTelegram();
        if (telegram != null && !telegram.isBlank()) {
            return "Telegram orqali";
        }
        return "Ko'rsatilmagan";
    }

    /** +998901234567 → +998 90 *** ** 67 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "***";
        }
        Matcher m = UZ_PHONE.matcher(phone.trim());
        if (m.matches()) {
            return "+998 " + m.group(1) + " *** ** " + m.group(4);
        }
        return "***";
    }

    static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Jo'natuvchi";
        }
        return fullName.trim().split("\\s+")[0];
    }
}