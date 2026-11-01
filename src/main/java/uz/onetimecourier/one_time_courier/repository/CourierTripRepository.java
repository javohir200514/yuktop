package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.onetimecourier.one_time_courier.entity.CourierTripEntity;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.enums.TripStatus;

import java.time.LocalDateTime;

public interface CourierTripRepository extends JpaRepository<CourierTripEntity, Long> {

    // ---------------------------------------------------------------- kelajakdagi faol safarlar

    @EntityGraph(attributePaths = "courier")
    Page<CourierTripEntity> findByStatusAndDepartureAtAfter(TripStatus status,
                                                            LocalDateTime now,
                                                            Pageable pageable);

    @EntityGraph(attributePaths = "courier")
    Page<CourierTripEntity> findByStatusAndDepartureAtAfterAndFromRegion(TripStatus status,
                                                                         LocalDateTime now,
                                                                         Region fromRegion,
                                                                         Pageable pageable);

    @EntityGraph(attributePaths = "courier")
    Page<CourierTripEntity> findByStatusAndDepartureAtAfterAndToRegion(TripStatus status,
                                                                       LocalDateTime now,
                                                                       Region toRegion,
                                                                       Pageable pageable);

    @EntityGraph(attributePaths = "courier")
    Page<CourierTripEntity> findByStatusAndDepartureAtAfterAndFromRegionAndToRegion(TripStatus status,
                                                                                    LocalDateTime now,
                                                                                    Region fromRegion,
                                                                                    Region toRegion,
                                                                                    Pageable pageable);

    // ---------------------------------------------------------------- kuryer limiti

    @Query("""
            select count(t) from CourierTripEntity t
            where t.courier.id = :courierId and t.status = :active and t.departureAt > :now
            """)
    long countUpcomingByCourier(@Param("courierId") Long courierId,
                                @Param("active") TripStatus active,
                                @Param("now") LocalDateTime now);

    // ---------------------------------------------------------------- arxivlash

    @Modifying
    @Query("""
            update CourierTripEntity t
            set t.status = :archived, t.archivedAt = :now
            where t.status = :active and t.departureAt <= :now
            """)
    int archiveExpired(@Param("active") TripStatus active,
                       @Param("archived") TripStatus archived,
                       @Param("now") LocalDateTime now);
}