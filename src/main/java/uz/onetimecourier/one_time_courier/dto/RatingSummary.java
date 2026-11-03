package uz.onetimecourier.one_time_courier.dto;

/** Repozitoriy so'rovi uchun: o'rtacha baho va baholar soni. avg null bo'lishi mumkin. */
public record RatingSummary(Double avg, Long count) {
}