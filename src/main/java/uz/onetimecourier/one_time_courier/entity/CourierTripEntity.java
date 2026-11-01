package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.enums.TripStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "courier_trips")
public class CourierTripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id", nullable = false)
    private UserEntity courier;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_region", nullable = false, length = 40)
    private Region fromRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_region", nullable = false, length = 40)
    private Region toRegion;

    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_package_size", nullable = false, length = 20)
    private PackageSize maxPackageSize;

    @Column(length = 300)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status = TripStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}