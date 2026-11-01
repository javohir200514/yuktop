package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.onetimecourier.one_time_courier.entity.PaymentEntity;

import java.util.Optional;

// payment/PaymentRepository.java
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByDeliveryId(Long deliveryId);
}