package uz.onetimecourier.one_time_courier.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.onetimecourier.one_time_courier.entity.CourierRouteSubscription;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.repository.CourierRouteSubscriptionRepository;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

import java.util.List;

/** Sayt sahifasi uchun: kuryerning bildirishnoma yo'nalishlarini boshqaradi. */
@Service
public class CourierNotificationService {

    public static final int MAX_SUBSCRIPTIONS = 10;

    private final CourierRouteSubscriptionRepository subscriptions;
    private final UserRepository users;

    public CourierNotificationService(CourierRouteSubscriptionRepository subscriptions,
                                      UserRepository users) {
        this.subscriptions = subscriptions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public boolean isTelegramLinked(Long courierId) {
        return users.findById(courierId).map(u -> u.getTelegramId() != null).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<CourierRouteSubscription> list(Long courierId) {
        return subscriptions.findByCourierIdOrderByIdAsc(courierId);
    }

    @Transactional
    public void add(Long courierId, Region from, Region to) {
        UserEntity courier = users.findById(courierId)
                .orElseThrow(() -> new BusinessException("Foydalanuvchi topilmadi"));
        if (subscriptions.countByCourierId(courierId) >= MAX_SUBSCRIPTIONS) {
            throw new BusinessException("Ko'pi bilan " + MAX_SUBSCRIPTIONS + " ta yo'nalish qo'shish mumkin");
        }
        if (subscriptions.findFirstByCourierIdAndFromRegionAndToRegion(courierId, from, to).isPresent()) {
            throw new BusinessException("Bu yo'nalish allaqachon qo'shilgan");
        }
        subscriptions.save(new CourierRouteSubscription(courier, from, to));
    }

    @Transactional
    public void setEnabled(Long courierId, Long subscriptionId, boolean enabled) {
        subscriptions.findByIdAndCourierId(subscriptionId, courierId)
                .ifPresent(s -> s.setEnabled(enabled));
    }

    @Transactional
    public void delete(Long courierId, Long subscriptionId) {
        subscriptions.findByIdAndCourierId(subscriptionId, courierId)
                .ifPresent(subscriptions::delete);
    }
}