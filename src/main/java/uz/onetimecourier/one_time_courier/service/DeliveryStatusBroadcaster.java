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


import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Yuk holati o'zgarishini jo'natuvchining ochiq sahifalariga yetkazadi (Vaadin Push).
 * Har bir obunachi faqat o'z jo'natuvchi ID'si bo'yicha hodisa oladi.
 * Eslatma: bitta server uchun. Bir nechta serverda Redis pub/sub kabi narsa kerak bo'ladi.
 */
@Component
public class DeliveryStatusBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStatusBroadcaster.class);

    private record Subscriber(UI ui, Consumer<DeliveryStatusChangedEvent> listener) {
    }

    private final Map<Long, Set<Subscriber>> subscribers = new ConcurrentHashMap<>();

    public Registration register(Long senderId, UI ui, Consumer<DeliveryStatusChangedEvent> listener) {
        Subscriber subscriber = new Subscriber(ui, listener);
        subscribers.computeIfAbsent(senderId, k -> ConcurrentHashMap.newKeySet()).add(subscriber);
        return () -> unregister(senderId, subscriber);
    }

    private void unregister(Long senderId, Subscriber subscriber) {
        subscribers.computeIfPresent(senderId, (k, set) -> {
            set.remove(subscriber);
            return set.isEmpty() ? null : set;
        });
    }

    /** Faqat tranzaksiya commit bo'lgandan keyin: bekor bo'lgan o'zgarish haqida xabar chiqmasin. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(DeliveryStatusChangedEvent event) {
        if (event == null || event.senderId() == null) {
            return;
        }
        Set<Subscriber> set = subscribers.get(event.senderId());
        if (set == null) {
            return;
        }
        for (Subscriber subscriber : set) {
            try {
                subscriber.ui().access(() -> subscriber.listener().accept(event));
            } catch (UIDetachedException e) {
                unregister(event.senderId(), subscriber);
            } catch (RuntimeException e) {
                log.warn("Holat o'zgarishini yetkazib bo'lmadi: {}", e.getClass().getSimpleName());
            }
        }
    }
}