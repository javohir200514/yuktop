package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.onetimecourier.one_time_courier.entity.DisputeEvidenceEntity;

import java.util.List;

public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidenceEntity, Long> {
    List<DisputeEvidenceEntity> findByDisputeId(Long disputeId);
}