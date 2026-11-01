package uz.onetimecourier.one_time_courier.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {

    // ---------------------------------------------------------------- jo'natuvchi

    long countBySenderIdAndStatusIn(Long senderId, Collection<DeliveryStatus> statuses);

    Page<DeliveryEntity> findBySenderId(Long senderId, Pageable pageable);

    Page<DeliveryEntity> findBySenderIdAndStatusIn(Long senderId,
                                                   Collection<DeliveryStatus> statuses,
                                                   Pageable pageable);

    /** Faqat egasining yuki. Boshqaniki yoki mavjud bo'lmasa, bo'sh. */
    Optional<DeliveryEntity> findByIdAndSenderId(Long id, Long senderId);

    /** Tahrirlash uchun qatorni qulflab o'qiydi (bekor qilish va kuryer biriktirish bilan to'qnashmasin). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryEntity> findForUpdateByIdAndSenderId(Long id, Long senderId);

    /** Hodisa yuborish uchun yuk egasining ID'si. */
    @Query("select d.sender.id from DeliveryEntity d where d.id = :id")
    Optional<Long> findSenderIdById(@Param("id") Long id);

    /** Oxirgi kiritilgan kontaktni formaga oldindan to'ldirish uchun (eng yangisi birinchi). */
    @Query("""
            select d from DeliveryEntity d
             where d.sender.id = :senderId
               and (d.senderContactPhone is not null or d.senderContactTelegram is not null)
             order by d.id desc
            """)
    List<DeliveryEntity> findWithContact(@Param("senderId") Long senderId, Pageable pageable);

    // ---------------------------------------------------------------- ochiq ro'yxat

    @EntityGraph(attributePaths = "sender")
    Page<DeliveryEntity> findByStatus(DeliveryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Page<DeliveryEntity> findByStatusAndFromRegion(DeliveryStatus status, Region fromRegion, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Page<DeliveryEntity> findByStatusAndToRegion(DeliveryStatus status, Region toRegion, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Page<DeliveryEntity> findByStatusAndFromRegionAndToRegion(DeliveryStatus status,
                                                              Region fromRegion,
                                                              Region toRegion,
                                                              Pageable pageable);

    // ---------------------------------------------------------------- kuryer

    long countByCourierIdAndStatusIn(Long courierId, Collection<DeliveryStatus> statuses);

    @EntityGraph(attributePaths = "sender")
    Page<DeliveryEntity> findByCourierId(Long courierId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Page<DeliveryEntity> findByCourierIdAndStatusIn(Long courierId,
                                                    Collection<DeliveryStatus> statuses,
                                                    Pageable pageable);

    // ---------------------------------------------------------------- atomik o'zgartirishlar

    /**
     * Atomik bekor qilish: faqat egasi, faqat ruxsat etilgan holatlarda.
     * O'zgargan qatorlar soni qaytadi (0 bo'lsa, bekor qilinmadi).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DeliveryEntity d
               set d.status = :newStatus,
                   d.cancelledAt = :now,
                   d.cancelReason = :reason
             where d.id = :id
               and d.sender.id = :senderId
               and d.status in :allowed
            """)
    int cancelIfAllowed(@Param("id") Long id,
                        @Param("senderId") Long senderId,
                        @Param("allowed") Collection<DeliveryStatus> allowed,
                        @Param("newStatus") DeliveryStatus newStatus,
                        @Param("now") LocalDateTime now,
                        @Param("reason") String reason);

    /** Kuryer yukni qabul qiladi: faqat hali kuryersiz CREATED yuk. Ikki kuryer bir vaqtda olsa, bittasi yutadi. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DeliveryEntity d
               set d.status = :newStatus,
                   d.courier = :courier,
                   d.matchedAt = :now
             where d.id = :id
               and d.status = :expected
               and d.courier is null
            """)
    int acceptIfCreated(@Param("id") Long id,
                        @Param("courier") UserEntity courier,
                        @Param("expected") DeliveryStatus expected,
                        @Param("newStatus") DeliveryStatus newStatus,
                        @Param("now") LocalDateTime now);

    /** Biriktirilgan kuryer holatni keyingi bosqichga o'tkazadi (faqat kutilgan holatdan). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DeliveryEntity d
               set d.status = :newStatus
             where d.id = :id
               and d.courier.id = :courierId
               and d.status = :expected
            """)
    int advanceByCourier(@Param("id") Long id,
                         @Param("courierId") Long courierId,
                         @Param("expected") DeliveryStatus expected,
                         @Param("newStatus") DeliveryStatus newStatus);
}