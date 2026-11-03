package uz.onetimecourier.one_time_courier.dto;

/** Eng ishonchli kuryerlar sahifasi uchun. */
public record TopCourierDto(Long id, String name, String vehicleLabel,
                            double avgStars, long ratingCount, long delivered) {
}