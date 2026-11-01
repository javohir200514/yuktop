package uz.onetimecourier.one_time_courier.exps;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import uz.onetimecourier.one_time_courier.config.SessionAuthenticator;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.service.OAuthUserService;

import java.io.IOException;

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
                || !Boolean.TRUE.equals(oidc.getEmailVerified())) {
            response.sendRedirect("/login?error");
            return;
        }
        UserEntity user = oauthUserService.findOrCreateGoogleUser(
                oidc.getSubject(), oidc.getEmail(), oidc.getFullName());
        sessionAuthenticator.establish(user, request, response);
        response.sendRedirect("/");
    }
}