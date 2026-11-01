package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.onetimecourier.one_time_courier.entity.PhoneVerificationEntity;

import java.util.Optional;


public interface PhoneVerificationRepository extends JpaRepository<PhoneVerificationEntity, Long> {
    Optional<PhoneVerificationEntity> findTopByPhoneOrderByCreatedAtDesc(String phone);
}