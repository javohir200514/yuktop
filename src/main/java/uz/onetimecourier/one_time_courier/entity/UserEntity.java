package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.onetimecourier.one_time_courier.enums.AuthProvider;
import uz.onetimecourier.one_time_courier.enums.Language;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.enums.VehicleType;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity {


    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;


    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(length = 190)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Language language = Language.UZ;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    // ---- Google ----
    @Column(name = "google_id", length = 64)
    private String googleId;

    // ---- Telegram ----
    @Column(name = "telegram_id")
    private Long telegramId;

    @Column(name = "telegram_username", length = 64)
    private String telegramUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType;

    @Column(name = "vehicle_brand", length = 60)
    private String vehicleBrand;

    @Column(name = "vehicle_model", length = 60)
    private String vehicleModel;

    @Column(name = "vehicle_color", length = 40)
    private String vehicleColor;

    @Column(name = "plate_number", length = 20)
    private String plateNumber;
}