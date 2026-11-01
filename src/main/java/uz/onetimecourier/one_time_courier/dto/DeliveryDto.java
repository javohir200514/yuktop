package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Jo'natuvchi o'z yuki uchun to'liq ma'lumot. */
public record DeliveryDto(
        Long id,
        Region fromRegion,
        Region toRegion,
        DeliveryType deliveryType,
        String pickupAddress,
        String dropoffAddress,
        Double pickupLat,
        Double pickupLng,
        Double dropoffLat,
        Double dropoffLng,
        Double courierLat,
        Double courierLng,
        LocalDateTime courierUpdatedAt,
        PackageSize packageSize,
        BigDecimal weightKg,
        String description,
        String recipientName,
        String recipientPhone,
        BigDecimal reward,
        DeliveryStatus status,
        LocalDateTime createdAt,
        LocalDateTime matchedAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        String senderContactPhone,
        String senderContactTelegram,
        boolean senderContactVerified
) {
}