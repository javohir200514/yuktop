package uz.onetimecourier.one_time_courier.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.onetimecourier.one_time_courier.dto.PublicCourierDto;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Role;

import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByPhone(String phone);
    boolean existsByPhone(String phone);
    Optional<UserEntity> findByGoogleId(String googleId);
    Optional<UserEntity> findByTelegramId(Long telegramId);
    @Query(value = """
        select new uz.onetimecourier.one_time_courier.dto.PublicCourierDto(
            u.id, u.fullName, u.phone, u.vehicleType, u.vehicleBrand, u.vehicleColor, count(d.id))
        from UserEntity u
        left join DeliveryEntity d on d.courier = u and d.status = :delivered
        where u.role = :role and u.phoneVerified = true
        group by u.id, u.fullName, u.phone, u.vehicleType, u.vehicleBrand, u.vehicleColor
        order by count(d.id) desc, u.id asc
        """,
            countQuery = """
        select count(u) from UserEntity u
        where u.role = :role and u.phoneVerified = true
        """)
    Page<PublicCourierDto> findPublicCouriers(@Param("role") Role role,
                                              @Param("delivered") DeliveryStatus delivered,
                                              Pageable pageable);
}