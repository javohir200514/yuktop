package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.onetimecourier.one_time_courier.entity.ReviewEntity;

import java.util.List;

// review/ReviewRepository.java
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByTargetIdOrderByCreatedAtDesc(Long targetId);
    boolean existsByDeliveryIdAndAuthorId(Long deliveryId, Long authorId);

    @Query("select coalesce(avg(r.rating), 0) from ReviewEntity r where r.target.id = :targetId")
    double averageRatingByTargetId(@Param("targetId") Long targetId);

    @Query("select coalesce(avg(r.rating), 0) from ReviewEntity r")
    double averageRating();
}