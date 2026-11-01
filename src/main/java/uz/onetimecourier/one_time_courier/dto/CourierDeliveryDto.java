package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Yukni qabul qilgan kuryer uchun to'liq ma'lumot.
 * Maydonlar tartibi DeliveryMapper.toCourierDto dagi konstruktor chaqiruvi bilan bir xil.
 */
public record CourierDeliveryDto(
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
        PackageSize packageSize,
        BigDecimal weightKg,
        String description,
        String recipientName,
        String recipientPhone,
        BigDecimal reward,
        DeliveryStatus status,
        LocalDateTime createdAt,
        LocalDateTime matchedAt,
        String senderName,
        String senderContactPhone,
        String senderContactTelegram,
        boolean senderContactVerified,
        boolean courierPickupRequested
) {
}