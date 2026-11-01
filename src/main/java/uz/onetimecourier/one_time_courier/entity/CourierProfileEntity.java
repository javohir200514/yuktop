package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.onetimecourier.one_time_courier.enums.VerificationStatus;

import java.math.BigDecimal;

@Entity
@Table(name = "courier_profiles")
@Getter
@Setter
@NoArgsConstructor
public class CourierProfileEntity extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true) private UserEntity user;
    @Column(name = "document_url", length = 500) private String documentUrl;
    @Column(name = "selfie_url", length = 500) private String selfieUrl;
    @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    @Column(name = "rejection_reason", length = 300) private String rejectionReason;
    @Column(name = "trust_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal trustScore = BigDecimal.ZERO;
    @Column(name = "completed_deliveries", nullable = false) private int completedDeliveries = 0;
}