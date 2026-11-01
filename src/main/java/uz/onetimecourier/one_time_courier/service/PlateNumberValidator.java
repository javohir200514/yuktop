package uz.onetimecourier.one_time_courier.service;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PlateNumberValidator {

    /** Standart format: 01 A 123 BC (bo'shliqsiz: 01A123BC). */
    private static final Pattern STANDARD = Pattern.compile("^\\d{2}[A-Z]\\d{3}[A-Z]{2}$");

    private PlateNumberValidator() {
    }

    /** Bo'shliq va chiziqchalarni olib tashlaydi, katta harfga o'tkazadi. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String normalized) {
        return normalized != null && STANDARD.matcher(normalized).matches();
    }
}