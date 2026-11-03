package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.time.LocalDateTime;

/** Kuryerning bildirishnoma yo'nalishi. fromRegion yoki toRegion null bo'lsa, "istalgan" degani. */
@Entity
@Table(name = "courier_route_subscriptions",
       indexes = @Index(name = "idx_route_sub_courier", columnList = "courier_id"))
public class CourierRouteSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id", nullable = false)
    private UserEntity courier;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_region", length = 40)
    private Region fromRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_region", length = 40)
    private Region toRegion;

    /** false bo'lsa, yo'nalish saqlanadi, lekin bildirishnoma yuborilmaydi. */
    @Column(name = "enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CourierRouteSubscription() {
    }

    public CourierRouteSubscription(UserEntity courier, Region fromRegion, Region toRegion) {
        this.courier = courier;
        this.fromRegion = fromRegion;
        this.toRegion = toRegion;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public UserEntity getCourier() { return courier; }

    public Region getFromRegion() { return fromRegion; }

    public Region getToRegion() { return toRegion; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}