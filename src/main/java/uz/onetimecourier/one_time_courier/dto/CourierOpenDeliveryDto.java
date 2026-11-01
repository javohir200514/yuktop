package uz.onetimecourier.one_time_courier.dto;

/**
 * Kirgan kuryer uchun ochiq yuk: ochiq ma'lumot + jo'natuvchining ismi (birinchi so'z)
 * va niqoblangan kontakt belgisi. To'liq kontakt, manzil va qabul qiluvchi ma'lumoti yukni qabul qilgach ochiladi.
 */
public record CourierOpenDeliveryDto(
        DeliveryPublicDto delivery,
        String senderName,
        String senderContactHint,
        boolean senderContactVerified
) {
}