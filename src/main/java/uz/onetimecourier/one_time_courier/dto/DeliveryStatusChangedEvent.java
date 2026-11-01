package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;

/** Yuk holati o'zgaganini bildiradi. Maxfiy ma'lumot (manzil, telefon) yo'q. */
public record DeliveryStatusChangedEvent(Long senderId, Long deliveryId, DeliveryStatus status) {
}