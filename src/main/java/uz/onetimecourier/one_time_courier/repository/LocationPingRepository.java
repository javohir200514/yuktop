package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.onetimecourier.one_time_courier.entity.LocationPingEntity;

import java.util.Optional;

// location/LocationPingRepository.java
public interface LocationPingRepository extends JpaRepository<LocationPingEntity, Long> {
    Optional<LocationPingEntity> findTopByDeliveryIdOrderByCreatedAtDesc(Long deliveryId);
}