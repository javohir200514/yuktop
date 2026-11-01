package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.onetimecourier.one_time_courier.entity.OtpCodeEntity;
import uz.onetimecourier.one_time_courier.enums.OtpType;

import java.util.Optional;

// otp/OtpCodeRepository.java
public interface OtpCodeRepository extends JpaRepository<OtpCodeEntity, Long> {
    Optional<OtpCodeEntity> findTopByDeliveryIdAndTypeAndUsedFalseOrderByCreatedAtDesc(Long deliveryId, OtpType type);
}