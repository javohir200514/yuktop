
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
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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
            "/actuator/health",
            "/api/public/**",
            "/styles.css",
            "/images/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/login",
            "/register",
            "/about"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth ->
                auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
        );

        // Google OAuth2 login
        http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(info ->
                        info.userService(googleOAuth2UserService))
                .successHandler(oauthLoginSuccessHandler)
        );

        // Vaadin security
        http.with(VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class));

        // Security headers
        http.headers(headers -> headers

                // HSTS: faqat HTTPS orqali ulanish
                .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000)
                )

                // MIME sniffingni o'chirish
                .contentTypeOptions(options -> {})

                // Faqat shu domen ichidagi iframe
                .frameOptions(frame -> frame.sameOrigin())

                // Referrer ma'lumotini cheklash
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy
                                .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )

                // Brauzer imkoniyatlarini cheklash
                .addHeaderWriter(new StaticHeadersWriter(
                        "Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()"
                ))

                // CSP: hozircha kuzatish rejimi
                .addHeaderWriter(new StaticHeadersWriter(
                        "Content-Security-Policy-Report-Only",
                        "default-src 'self'; " +
                                "object-src 'none'; " +
                                "base-uri 'self'; " +
                                "frame-ancestors 'self'; " +
                                "img-src 'self' data: blob:; " +
                                "font-src 'self' data:; " +
                                "connect-src 'self' wss:; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "style-src 'self' 'unsafe-inline'"
                ))
        );

        return http.build();
    }
}
