package uz.onetimecourier.one_time_courier.service;

import org.springframework.stereotype.Service;
import uz.onetimecourier.one_time_courier.dto.VehicleInfo;
import uz.onetimecourier.one_time_courier.enums.VehicleType;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Hozircha faqat format tekshiradi. Haqiqiy tasdiqlash uchun
 * shu interfeysning boshqa implementatsiyasini yozib, uni @Primary qiling.
 *
 * Velosiped va mototsikl: faqat tur va rang kiritiladi.
 * Avtomobil va boshqa transport: marka, model, rang va davlat raqami tekshiriladi.
 */
@Service
public class FormatVehicleVerificationService implements VehicleVerificationService {

    /** 01A123BC yoki 01123AAA (yuridik shaxs raqami ham shu formatlardan biri). */
    private static final Pattern PLATE_A = Pattern.compile("^\\d{2}[A-Z]\\d{3}[A-Z]{2}$");
    private static final Pattern PLATE_B = Pattern.compile("^\\d{2}\\d{3}[A-Z]{3}$");

    /** Enum nomida shu so'zlardan biri bo'lsa, u yengil transport hisoblanadi. */
    private static final List<String> LIGHT_NAME_HINTS = List.of("BIKE", "BICYCLE", "VELO", "MOTO", "MOPED", "SCOOTER");

    /** O'zbekcha yoki inglizcha nomida shu so'zlardan biri bo'lsa, yengil transport. */
    private static final List<String> LIGHT_LABEL_HINTS = List.of("velo", "bike", "bicycle", "moto", "mopед", "skuter", "scooter");

    @Override
    public VerificationResult verify(VehicleInfo draft) {
        if (draft == null || draft.type() == null) {
            return VerificationResult.failure("Transport turini tanlang");
        }
        if (isBlank(draft.color())) {
            return VerificationResult.failure("Rangni tanlang");
        }

        if (isLightVehicle(draft.type())) {
            return VerificationResult.success(new VehicleInfo(
                    draft.type(), null, null, draft.color().trim(), null));
        }

        if (isBlank(draft.brand())) {
            return VerificationResult.failure("Markani tanlang");
        }
        if (isBlank(draft.model())) {
            return VerificationResult.failure("Modelni tanlang");
        }
        if (isBlank(draft.plateNumber())) {
            return VerificationResult.failure("Davlat raqamini kiriting");
        }

        String plate = normalizePlate(draft.plateNumber());
        if (!PLATE_A.matcher(plate).matches() && !PLATE_B.matcher(plate).matches()) {
            return VerificationResult.failure("Raqam formati noto'g'ri. Masalan: 01 A 123 BC yoki 01 123 AAA");
        }

        return VerificationResult.success(new VehicleInfo(
                draft.type(),
                draft.brand().trim(),
                draft.model().trim(),
                draft.color().trim(),
                formatPlate(plate)));
    }

    /**
     * Velosiped va mototsikl uchun marka, model va raqam so'ralmaydi.
     * Dialog ham shu metodni ishlatadi, shuning uchun qoida bir joyda.
     */
    public static boolean isLightVehicle(VehicleType type) {
        if (type == null) {
            return false;
        }
        String name = type.name().toUpperCase(Locale.ROOT);
        for (String hint : LIGHT_NAME_HINTS) {
            if (name.contains(hint)) {
                return true;
            }
        }
        String label = type.getLabel() == null ? "" : type.getLabel().toLowerCase(Locale.ROOT);
        for (String hint : LIGHT_LABEL_HINTS) {
            if (label.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizePlate(String raw) {
        return raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    /** 01A123BC -> 01 A 123 BC, 01123AAA -> 01 123 AAA. Format mos kelmasa, o'zgarishsiz qaytaradi. */
    private static String formatPlate(String n) {
        if (PLATE_A.matcher(n).matches()) {
            return n.substring(0, 2) + " " + n.charAt(2) + " " + n.substring(3, 6) + " " + n.substring(6);
        }
        if (PLATE_B.matcher(n).matches()) {
            return n.substring(0, 2) + " " + n.substring(2, 5) + " " + n.substring(5);
        }
        return n;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}