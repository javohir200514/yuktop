package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;

import java.util.Collection;
import java.util.List;

/** Eslatmalar uchun: faol yuklar, kuryer bilan birga. */
public interface TelegramDeliveryRepository extends JpaRepository<DeliveryEntity, Long> {

    @Query("""
            select d from DeliveryEntity d
              left join fetch d.courier
             where d.status in :statuses
            """)
    List<DeliveryEntity> findActiveWithCourier(@Param("statuses") Collection<DeliveryStatus> statuses);
}