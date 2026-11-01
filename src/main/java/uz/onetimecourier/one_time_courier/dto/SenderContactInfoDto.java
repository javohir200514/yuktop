package uz.onetimecourier.one_time_courier.dto;

/**
 * Yuk formasi uchun: required = true bo'lsa, hisobida telefon yo'q va kontakt so'raladi.
 * phone/telegram oxirgi yukdagi kontakt (oldindan to'ldirish uchun).
 */
public record SenderContactInfoDto(boolean required, String phone, String telegram) {
}