package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private UserEntity courier;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_region", nullable = false)
    private Region fromRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_region", nullable = false)
    private Region toRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType;

    @Column(name = "pickup_address", nullable = false, length = 300)
    private String pickupAddress;

    @Column(name = "dropoff_address", nullable = false, length = 300)
    private String dropoffAddress;

    @Column(name = "pickup_lat")
    private Double pickupLat;

    @Column(name = "pickup_lng")
    private Double pickupLng;

    @Column(name = "dropoff_lat")
    private Double dropoffLat;

    @Column(name = "dropoff_lng")
    private Double dropoffLng;

    @Column(name = "courier_lat")
    private Double courierLat;

    @Column(name = "courier_lng")
    private Double courierLng;

    @Column(name = "courier_updated_at")
    private LocalDateTime courierUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_size", nullable = false)
    private PackageSize packageSize;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "reward", nullable = false, precision = 12, scale = 2)
    private BigDecimal reward;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Column(name = "sender_contact_phone", length = 20)
    private String senderContactPhone;

    @Column(name = "sender_contact_telegram", length = 32)
    private String senderContactTelegram;

    @Column(name = "sender_contact_verified", nullable = false)
    private boolean senderContactVerified;

    /** Kuryer "Olib ketdim" degan vaqt. */
    @Column(name = "courier_pickup_at")
    private LocalDateTime courierPickupAt;

    /** Jo'natuvchi kuryer olib ketganini tasdiqlagan vaqt. */
    @Column(name = "sender_pickup_confirmed_at")
    private LocalDateTime senderPickupConfirmedAt;

    /** Kuryer "Yetkazdim" degan vaqt. */
    @Column(name = "courier_delivery_at")
    private LocalDateTime courierDeliveryAt;

    /** Jo'natuvchi yetkazilganini tasdiqlagan vaqt. */
    @Column(name = "sender_delivery_confirmed_at")
    private LocalDateTime senderDeliveryConfirmedAt;
}