package uz.onetimecourier.one_time_courier.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.onetimecourier.one_time_courier.dto.CourierRank;
import uz.onetimecourier.one_time_courier.dto.RatingSummary;
import uz.onetimecourier.one_time_courier.dto.ReputationDto;
import uz.onetimecourier.one_time_courier.dto.TopCourierDto;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.DeliveryRatingEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.repository.DeliveryRatingRepository;
import uz.onetimecourier.one_time_courier.repository.DeliveryRepository;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

import java.util.List;
import java.util.Objects;

@Service
public class RatingService {

    /** Eng ishonchli ro'yxatga kirish uchun kamida shuncha baho kerak. */
    public static final int MIN_RATINGS_FOR_TOP = 1;
    public static final int MAX_COMMENT = 500;

    private final DeliveryRatingRepository ratings;
    private final DeliveryRepository deliveries;
    private final UserRepository users;

    public RatingService(DeliveryRatingRepository ratings,
                         DeliveryRepository deliveries,
                         UserRepository users) {
        this.ratings = ratings;
        this.deliveries = deliveries;
        this.users = users;
    }

    /** Yetkazilgan yukda ishtirokchi (jo'natuvchi yoki kuryer) ikkinchi tomonni baholaydi. */
    @Transactional
    public void rate(Long raterId, Long deliveryId, int stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new BusinessException("Baho 1 dan 5 gacha bo'lishi kerak");
        }
        String clean = comment == null ? null : comment.trim();
        if (clean != null && clean.length() > MAX_COMMENT) {
            throw new BusinessException("Izoh " + MAX_COMMENT + " belgidan oshmasligi kerak");
        }
        if (clean != null && clean.isEmpty()) {
            clean = null;
        }

        DeliveryEntity d = deliveries.findById(deliveryId)
                .orElseThrow(() -> new BusinessException("Yuk topilmadi"));
        if (d.getStatus() != DeliveryStatus.DELIVERED) {
            throw new BusinessException("Faqat yetkazilgan yukni baholash mumkin");
        }

        UserEntity rater = users.findById(raterId)
                .orElseThrow(() -> new BusinessException("Foydalanuvchi topilmadi"));

        UserEntity ratee;
        if (d.getSender() != null && raterId.equals(d.getSender().getId())) {
            ratee = d.getCourier();
        } else if (d.getCourier() != null && raterId.equals(d.getCourier().getId())) {
            ratee = d.getSender();
        } else {
            throw new BusinessException("Bu yukni baholash huquqingiz yo'q");
        }
        if (ratee == null) {
            throw new BusinessException("Baholanadigan tomon topilmadi");
        }
        if (ratings.existsByDeliveryIdAndRaterId(deliveryId, raterId)) {
            throw new BusinessException("Siz bu yukni allaqachon baholagansiz");
        }

        try {
            ratings.saveAndFlush(new DeliveryRatingEntity(d, rater, ratee, stars, clean));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Siz bu yukni allaqachon baholagansiz");
        }
    }

    @Transactional(readOnly = true)
    public boolean hasRated(Long deliveryId, Long raterId) {
        return ratings.existsByDeliveryIdAndRaterId(deliveryId, raterId);
    }

    @Transactional(readOnly = true)
    public ReputationDto reputation(Long userId, Role role) {
        RatingSummary s = ratings.summaryFor(userId);
        double avg = s.avg() == null ? 0 : s.avg();
        long count = s.count() == null ? 0 : s.count();

        long delivered;
        long failed;
        if (role == Role.COURIER) {
            delivered = ratings.countAsCourier(userId, DeliveryStatus.DELIVERED);
            failed = ratings.countAsCourier(userId, DeliveryStatus.DISPUTED);
        } else {
            delivered = ratings.countAsSender(userId, DeliveryStatus.DELIVERED);
            failed = ratings.countAsSender(userId, DeliveryStatus.CANCELLED)
                    + ratings.countAsSender(userId, DeliveryStatus.DISPUTED);
        }
        return new ReputationDto(avg, count, delivered, failed);
    }

    @Transactional(readOnly = true)
    public List<TopCourierDto> topCouriers(int limit) {
        List<CourierRank> ranks = ratings.topRated(
                Role.COURIER, MIN_RATINGS_FOR_TOP, PageRequest.of(0, Math.max(1, limit)));

        return ranks.stream()
                .map(r -> {
                    UserEntity u = users.findById(r.userId()).orElse(null);
                    if (u == null) {
                        return null;
                    }
                    long delivered = ratings.countAsCourier(u.getId(), DeliveryStatus.DELIVERED);
                    String vehicle = u.getVehicleType() == null ? null : u.getVehicleType().getLabel();
                    return new TopCourierDto(
                            u.getId(),
                            u.getFullName(),
                            vehicle,
                            r.avg() == null ? 0 : r.avg(),
                            r.count() == null ? 0 : r.count(),
                            delivered);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}