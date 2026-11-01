package uz.onetimecourier.one_time_courier.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.onetimecourier.one_time_courier.enums.OtpType;

import java.time.LocalDateTime;


@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
public class OtpCodeEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryEntity delivery;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OtpType type;
    @Column(name = "code_hash", nullable = false, length = 100) private String codeHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private int attempts = 0;
    @Column(nullable = false) private boolean used = false;
}