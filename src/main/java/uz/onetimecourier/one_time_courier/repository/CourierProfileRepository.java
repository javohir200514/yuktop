package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.onetimecourier.one_time_courier.entity.CourierProfileEntity;
import uz.onetimecourier.one_time_courier.enums.VerificationStatus;

import java.util.List;
import java.util.Optional;

// courier/CourierProfileRepository.java
public interface CourierProfileRepository extends JpaRepository<CourierProfileEntity, Long> {
    Optional<CourierProfileEntity> findByUserId(Long userId);
    List<CourierProfileEntity> findByVerificationStatus(VerificationStatus status);
    long countByVerificationStatus(VerificationStatus status);
}