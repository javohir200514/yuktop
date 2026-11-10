package uz.onetimecourier.one_time_courier.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import uz.onetimecourier.one_time_courier.entity.UserEntity;

@Component
public class SessionAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthenticator.class);

    private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    /** Foydalanuvchini joriy sessiyaga kirgizadi. Hamma kirish usullarida principal bir xil (CustomUserDetails). */
    public void establish(UserEntity user, HttpServletRequest request, HttpServletResponse response) {
        CustomUserDetails details = new CustomUserDetails(user);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities());
        establish(auth, request, response);
    }

    /**
     * Tayyor Authentication ni sessiyaga saqlaydi. Avval sessiya ID si almashtiriladi
     * (session fixation: kirishdan oldingi ID kirgandan keyin yaroqsiz bo'ladi).
     */
    public void establish(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        rotateSessionId(request);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }

    private static void rotateSessionId(HttpServletRequest request) {
        try {
            if (request.getSession(false) != null) {
                request.changeSessionId();
            } else {
                request.getSession(true);
            }
        } catch (IllegalStateException | UnsupportedOperationException e) {
            log.warn("Sessiya ID sini almashtirib bo'lmadi: {}", e.getMessage());
        }
    }
}
