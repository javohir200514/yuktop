package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.math.BigDecimal;

/**
 * Koordinatalar ixtiyoriy: yo ikkalasi (lat+lng) berilishi, yo ikkalasi ham null bo'lishi kerak.
 * contactPhone/contactTelegram faqat hisobida telefon yo'q (OAuth) jo'natuvchilar uchun ishlatiladi.
 * Juftlik, hudud va kontakt qoidalari DeliveryService'da tekshiriladi.
 */
public record CreateDeliveryRequestDto(

        @NotNull(message = "Qayerdan jo'natilishini tanlang")
        Region fromRegion,

        @NotNull(message = "Qayerga yuborilishini tanlang")
        Region toRegion,

        @NotBlank(message = "Olib ketish manzilini kiriting")
        @Size(min = 5, max = 300, message = "Manzil 5 dan 300 gacha belgidan iborat bo'lishi kerak")
        String pickupAddress,

        @NotBlank(message = "Yetkazish manzilini kiriting")
        @Size(min = 5, max = 300, message = "Manzil 5 dan 300 gacha belgidan iborat bo'lishi kerak")
        String dropoffAddress,

        Double pickupLat,
        Double pickupLng,
        Double dropoffLat,
        Double dropoffLng,

        @NotNull(message = "Yuk turini tanlang")
        PackageSize packageSize,

        @NotNull(message = "Og'irlikni kiriting")
        @DecimalMin(value = "0.1", message = "Og'irlik kamida 0.1 kg bo'lishi kerak")
        @DecimalMax(value = "50.00", message = "Og'irlik 50 kg dan oshmasligi kerak")
        @Digits(integer = 2, fraction = 2, message = "Og'irlikda kg dan keyin ko'pi bilan 2 xona bo'lishi mumkin")
        BigDecimal weightKg,

        @Size(max = 500, message = "Tavsif 500 belgidan oshmasligi kerak")
        String description,

        @NotBlank(message = "Qabul qiluvchi ismini kiriting")
        @Size(min = 2, max = 120, message = "Ism 2 dan 120 gacha belgidan iborat bo'lishi kerak")
        String recipientName,

        @NotBlank(message = "Qabul qiluvchi telefonini kiriting")
        @Pattern(regexp = "^\\+998\\d{9}$", message = "Telefonni +998901234567 ko'rinishida kiriting")
        String recipientPhone,

        @NotNull(message = "Mukofotni kiriting")
        @DecimalMin(value = "1000", message = "Mukofot kamida 1 000 so'm bo'lishi kerak")
        @DecimalMax(value = "100000000", message = "Mukofot 100 000 000 so'mdan oshmasligi kerak")
        @Digits(integer = 9, fraction = 2, message = "Mukofotda tiyindan keyin ko'pi bilan 2 xona bo'lishi mumkin")
        BigDecimal reward,

        @Pattern(regexp = "^\\+998\\d{9}$", message = "Telefonni +998901234567 ko'rinishida kiriting")
        String contactPhone,

        @Pattern(regexp = "^[A-Za-z0-9_]{5,32}$",
                message = "Telegram username 5–32 belgi: lotin harflari, raqam va _ bo'lishi mumkin")
        String contactTelegram
) {

    /** Kontaktsiz qisqa konstruktor (kontakt hisobdagi telefondan olinadigan holat). */
    public CreateDeliveryRequestDto(Region fromRegion, Region toRegion,
                                    String pickupAddress, String dropoffAddress,
                                    Double pickupLat, Double pickupLng, Double dropoffLat, Double dropoffLng,
                                    PackageSize packageSize, BigDecimal weightKg, String description,
                                    String recipientName, String recipientPhone, BigDecimal reward) {
        this(fromRegion, toRegion, pickupAddress, dropoffAddress,
                pickupLat, pickupLng, dropoffLat, dropoffLng,
                packageSize, weightKg, description, recipientName, recipientPhone, reward,
                null, null);
    }
}