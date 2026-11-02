package uz.onetimecourier.one_time_courier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.onetimecourier.one_time_courier.entity.CourierRouteSubscription;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.enums.Role;

import java.util.List;
import java.util.Optional;

public interface CourierRouteSubscriptionRepository extends JpaRepository<CourierRouteSubscription, Long> {

    long countByCourierId(Long courierId);

    List<CourierRouteSubscription> findByCourierIdOrderByIdAsc(Long courierId);

    /** Null qiymatlar "istalgan" sifatida saqlanadi, shuning uchun Spring Data "is null" qiladi. */
    Optional<CourierRouteSubscription> findFirstByCourierIdAndFromRegionAndToRegion(Long courierId,
                                                                                    Region fromRegion,
                                                                                    Region toRegion);

    Optional<CourierRouteSubscription> findByIdAndCourierId(Long id, Long courierId);

    /** Berilgan yo'nalishga mos, bildirishnoma yoqqan kuryerlar (takrorlanmasdan). */
    @Query("""
            select distinct s.courier from CourierRouteSubscription s
             where (s.fromRegion is null or s.fromRegion = :fromRegion)
               and (s.toRegion is null or s.toRegion = :toRegion)
               and s.courier.role = :role
               and s.courier.telegramId is not null
            """)
    List<UserEntity> findCouriersForRoute(@Param("fromRegion") Region fromRegion,
                                          @Param("toRegion") Region toRegion,
                                          @Param("role") Role role);
}