package uz.onetimecourier.one_time_courier.exps;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import uz.onetimecourier.one_time_courier.config.SessionAuthenticator;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.service.OAuthUserService;

import java.io.IOException;

/**
 * Google (OIDC) orqali kirgandan keyin: foydalanuvchi faqat googleId (sub) bo'yicha topiladi yoki yaratiladi.
 * Email bo'yicha mavjud akkauntga ulanmaydi, shuning uchun email orqali akkaunt tortib olib bo'lmaydi.
 */
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthUserService oauthUserService;
    private final SessionAuthenticator sessionAuthenticator;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidc)
                || oidc.getSubject() == null
                || oidc.getEmail() == null
                || !Boolean.TRUE.equals(oidc.getEmailVerified())) {
            rejectLogin(request, response);
            return;
        }

        UserEntity user = oauthUserService.findOrCreateGoogleUser(
                oidc.getSubject(), oidc.getEmail(), oidc.getFullName());

        // Spring yozgan OIDC kontekstini bizning CustomUserDetails bilan almashtiradi va sessiya ID sini yangilaydi
        sessionAuthenticator.establish(user, request, response);
        response.sendRedirect("/");
    }

    /**
     * Spring OIDC kontekstini bu handler chaqirilishidan oldin sessiyaga yozib bo'lgan bo'ladi.
     * Uni tozalamasak, rad etilgan foydalanuvchi yarim-kirgan holatda qolib ketadi.
     */
    private static void rejectLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect("/login?error");
    }
}