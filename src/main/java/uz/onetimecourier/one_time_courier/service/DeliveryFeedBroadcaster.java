package uz.onetimecourier.one_time_courier.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.shared.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uz.onetimecourier.one_time_courier.dto.DeliveryStatusChangedEvent;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Yuklar ro'yxati o'zgarganini sahifalarga bildiradi (Vaadin Push, ya'ni WebSocket orqali).
 * Hodisada yuk ma'lumoti yo'q: sahifa o'zi servisdan ruxsati doirasida qayta o'qiydi.
 * Bitta server uchun. Bir nechta serverda Redis pub/sub kabi vosita kerak bo'ladi.
 */
@Component
public class DeliveryFeedBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(DeliveryFeedBroadcaster.class);

    /** Ochiq yuklar ro'yxati uchun: faqat ro'yxatga ta'sir qiladigan holatlar. */
    public static final Predicate<DeliveryStatus> OPEN_FEED = status ->
            status == DeliveryStatus.CREATED
                    || status == DeliveryStatus.MATCHED
                    || status == DeliveryStatus.CANCELLED;

    /** Kuryerning o'z yuklari uchun: barcha holat o'zgarishlari. */
    public static final Predicate<DeliveryStatus> ALL_CHANGES = status -> true;

    private record Subscriber(UI ui, Predicate<DeliveryStatus> filter, Runnable listener) {
    }

    private final Set<Subscriber> subscribers = ConcurrentHashMap.newKeySet();

    /** Barcha o'zgarishlarni tinglaydi (kuryer sahifasi uchun). */
    public Registration register(UI ui, Runnable listener) {
        return register(ui, ALL_CHANGES, listener);
    }

    /** Berilgan filtr bo'yicha tinglaydi. */
    public Registration register(UI ui, Predicate<DeliveryStatus> filter, Runnable listener) {
        Subscriber subscriber = new Subscriber(ui, filter, listener);
        subscribers.add(subscriber);
        return () -> subscribers.remove(subscriber);
    }

    /** Faqat tranzaksiya COMMIT bo'lgandan keyin ishlaydi, shunda yangi ma'lumot o'qiladi. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(DeliveryStatusChangedEvent event) {
        if (event == null || event.status() == null) {
            return;
        }
        for (Subscriber subscriber : subscribers) {
            if (!subscriber.filter().test(event.status())) {
                continue;
            }
            try {
                subscriber.ui().access(subscriber.listener()::run);
            } catch (UIDetachedException e) {
                subscribers.remove(subscriber);
            } catch (RuntimeException e) {
                log.warn("Ro'yxat yangilanishini yetkazib bo'lmadi: {}", e.getClass().getSimpleName());
            }
        }
    }
}