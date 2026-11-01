package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.onetimecourier.one_time_courier.enums.DisputeReason;
import uz.onetimecourier.one_time_courier.enums.DisputeStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
public class DisputeEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryEntity delivery;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "opened_by_id", nullable = false)
    private UserEntity openedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private DisputeReason reason;
    @Column(nullable = false, length = 1000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private DisputeStatus status = DisputeStatus.OPEN;
    @Column(length = 1000) private String resolution;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resolved_by_id") private UserEntity resolvedBy;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
}