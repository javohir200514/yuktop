package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.onetimecourier.one_time_courier.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, unique = true) private DeliveryEntity delivery;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status = PaymentStatus.PENDING;
    @Column(name = "provider_tx_id", length = 100) private String providerTxId;
    @Column(name = "held_at") private LocalDateTime heldAt;
    @Column(name = "released_at") private LocalDateTime releasedAt;
}