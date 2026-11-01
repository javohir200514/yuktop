package uz.onetimecourier.one_time_courier.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Joriy foydalanuvchini xavfsiz olish.
 * Oddiy login bo'lsa CustomUserDetails, Google (OAuth2) bo'lsa bo'sh qaytaradi.
 * Vaadin AuthenticationContext.getAuthenticatedUser(...) dan farqli, ClassCastException bermaydi.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<CustomUserDetails> details() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        return principal instanceof CustomUserDetails user
                ? Optional.of(user)
                : Optional.empty();
    }
}