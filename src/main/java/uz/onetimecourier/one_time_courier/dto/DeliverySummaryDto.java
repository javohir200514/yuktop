package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Jo'natuvchining o'z yuklari ro'yxati uchun. */
public record DeliverySummaryDto(
        Long id,
        Region fromRegion,
        Region toRegion,
        DeliveryType deliveryType,
        PackageSize packageSize,
        BigDecimal weightKg,
        BigDecimal reward,
        DeliveryStatus status,
        LocalDateTime createdAt,
        String pickupAddress,
        String dropoffAddress,
        String recipientName
) {
}