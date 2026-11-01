package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.AuthProvider;
import uz.onetimecourier.one_time_courier.enums.Language;
import uz.onetimecourier.one_time_courier.enums.Role;

public record UserDto(
        Long id,
        String fullName,
        String phone,              // Google/Telegram foydalanuvchilarida null
        String email,              // Google orqali kirganlarda bor
        Role role,
        Language language,
        boolean phoneVerified,
        AuthProvider authProvider,
        Long telegramId,           // Telegram orqali kirganlarda bor
        String telegramUsername) { // @ belgisiz saqlanadi

    /** Menyu va profilda ko'rsatish uchun: telefon, @telegram yoki email. */
    public String contactLabel() {
        if (phone != null) return phone;
        if (telegramUsername != null) return "@" + telegramUsername;
        if (email != null) return email;
        return authProvider == AuthProvider.TELEGRAM ? "Telegram" : "Google";
    }
}