package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "location_pings")
@Getter
@Setter
@NoArgsConstructor
public class LocationPingEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryEntity delivery;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "courier_id", nullable = false)
    private UserEntity courier;
    @Column(nullable = false) private double lat;
    @Column(nullable = false) private double lng;
}