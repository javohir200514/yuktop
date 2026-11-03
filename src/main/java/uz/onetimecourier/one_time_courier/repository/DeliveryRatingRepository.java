package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.onetimecourier.one_time_courier.dto.CourierRank;
import uz.onetimecourier.one_time_courier.dto.RatingSummary;
import uz.onetimecourier.one_time_courier.entity.DeliveryRatingEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Role;

import java.util.List;

public interface DeliveryRatingRepository extends JpaRepository<DeliveryRatingEntity, Long> {

    boolean existsByDeliveryIdAndRaterId(Long deliveryId, Long raterId);

    @Query("select new uz.onetimecourier.one_time_courier.dto.RatingSummary(avg(r.stars), count(r)) "
            + "from DeliveryRatingEntity r where r.ratee.id = :userId")
    RatingSummary summaryFor(@Param("userId") Long userId);

    @Query("select new uz.onetimecourier.one_time_courier.dto.CourierRank(r.ratee.id, avg(r.stars), count(r)) "
            + "from DeliveryRatingEntity r where r.ratee.role = :role "
            + "group by r.ratee.id having count(r) >= :minRatings "
            + "order by avg(r.stars) desc, count(r) desc")
    List<CourierRank> topRated(@Param("role") Role role,
                               @Param("minRatings") long minRatings,
                               Pageable pageable);

    @Query("select count(d) from DeliveryEntity d where d.courier.id = :id and d.status = :status")
    long countAsCourier(@Param("id") Long id, @Param("status") DeliveryStatus status);

    @Query("select count(d) from DeliveryEntity d where d.sender.id = :id and d.status = :status")
    long countAsSender(@Param("id") Long id, @Param("status") DeliveryStatus status);
}