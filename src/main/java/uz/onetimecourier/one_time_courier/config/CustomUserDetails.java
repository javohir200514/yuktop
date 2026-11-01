package uz.onetimecourier.one_time_courier.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import uz.onetimecourier.one_time_courier.entity.UserEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Joriy foydalanuvchi. Oddiy login (telefon/Telegram) va Google (OAuth2) uchun bir xil tur.
 * OAuth2User ni ham implement qiladi: Google login'da principal shu obyekt bo'ladi.
 */
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Long id;
    private final String fullName;
    private final String phone;
    private final String username;
    private final String contactLabel;
    private final String passwordHash;
    private final String role;
    private final Map<String, Object> attributes;

    public CustomUserDetails(UserEntity user) {
        this(user, Map.<String, Object>of());
    }

    public CustomUserDetails(UserEntity user, Map<String, Object> attributes) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();
        this.attributes = attributes == null
                ? Map.<String, Object>of()
                : Collections.unmodifiableMap(new HashMap<>(attributes));

        if (user.getPhone() != null) {
            this.username = user.getPhone();
            this.contactLabel = user.getPhone();
        } else if (user.getTelegramId() != null) {
            this.username = "tg:" + user.getTelegramId();
            this.contactLabel = user.getTelegramUsername() != null
                    ? "@" + user.getTelegramUsername() : "Telegram";
        } else {
            this.username = "google:" + user.getGoogleId();
            this.contactLabel = user.getEmail() != null ? user.getEmail() : "Google";
        }
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    /** Navbar menyusida ko'rsatish uchun: telefon, @telegram yoki email. */
    public String getContactLabel() { return contactLabel; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /** Paroli yo'q akkauntlar uchun hech qachon mos kelmaydigan qiymat. */
    @Override public String getPassword() { return passwordHash != null ? passwordHash : "!"; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // ---- OAuth2User ----

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}