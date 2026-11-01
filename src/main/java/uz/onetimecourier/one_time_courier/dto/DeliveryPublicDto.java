package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Hammaga ochiq ro'yxat uchun: manzil, telefon va jo'natuvchi ma'lumoti BO'LMASIN. */
public record DeliveryPublicDto(
        Long id,
        Region fromRegion,
        Region toRegion,
        DeliveryType deliveryType,
        PackageSize packageSize,
        BigDecimal weightKg,
        BigDecimal reward,
        LocalDateTime createdAt
) {
}