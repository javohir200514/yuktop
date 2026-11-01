package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "phone_verifications")
@Getter
@Setter
@NoArgsConstructor
public class PhoneVerificationEntity extends BaseEntity {
    @Column(nullable = false, length = 20) private String phone;
    @Column(name = "code_hash", nullable = false, length = 100) private String codeHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private int attempts = 0;
    @Column(nullable = false) private boolean verified = false;
}