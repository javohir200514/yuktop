package uz.onetimecourier.one_time_courier.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uz.onetimecourier.one_time_courier.entity.UserEntity;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String fullName;
    private final String phone;
    private final String username;
    private final String contactLabel;
    private final String passwordHash;
    private final String role;

    public CustomUserDetails(UserEntity user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();

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
}