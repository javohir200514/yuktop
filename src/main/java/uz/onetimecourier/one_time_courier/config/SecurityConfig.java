package uz.onetimecourier.one_time_courier.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import uz.onetimecourier.one_time_courier.exps.OAuthLoginSuccessHandler;
import uz.onetimecourier.one_time_courier.view.LoginView;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final OAuthLoginSuccessHandler oauthLoginSuccessHandler;
    private final GoogleOAuth2UserService googleOAuth2UserService;

    public static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health", "/api/public/**", "/styles.css", "/images/**",
            "/oauth2/**", "/login/oauth2/**",
            "/login", "/register", "/about"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll());

        // Google login: foydalanuvchi CustomUserDetails sifatida yuklanadi
        http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(info -> info.userService(googleOAuth2UserService))
                .successHandler(oauthLoginSuccessHandler));

        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
                configurer.loginView(LoginView.class));

        return http.build();
    }
}