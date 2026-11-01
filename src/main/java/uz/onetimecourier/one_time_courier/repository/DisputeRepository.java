package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.onetimecourier.one_time_courier.entity.DisputeEntity;
import uz.onetimecourier.one_time_courier.entity.DisputeEvidenceEntity;
import uz.onetimecourier.one_time_courier.enums.DisputeStatus;

import java.util.List;
import java.util.Optional;

// dispute/DisputeRepository.java
public interface DisputeRepository extends JpaRepository<DisputeEntity, Long> {
    List<DisputeEntity> findByStatusOrderByCreatedAtDesc(DisputeStatus status);
    List<DisputeEntity> findByOpenedByIdOrderByCreatedAtDesc(Long userId);
    Optional<DisputeEntity> findByDeliveryId(Long deliveryId);
}

