package uz.onetimecourier.one_time_courier.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    /** Foydalanuvchini joriy sessiyaga kirgizadi. Hamma kirish usullarida principal bir xil (CustomUserDetails). */
    public void establish(UserEntity user, HttpServletRequest request, HttpServletResponse response) {
        CustomUserDetails details = new CustomUserDetails(user);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }
}