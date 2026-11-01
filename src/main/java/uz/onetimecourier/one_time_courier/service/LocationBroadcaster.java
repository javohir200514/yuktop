package uz.onetimecourier.one_time_courier.service;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;
import uz.onetimecourier.one_time_courier.dto.LocationDto;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class LocationBroadcaster {

    private final Map<Long, Set<Consumer<LocationDto>>> listeners = new ConcurrentHashMap<>();

    public Registration subscribe(Long deliveryId, Consumer<LocationDto> listener) {
        listeners.computeIfAbsent(deliveryId, k -> ConcurrentHashMap.newKeySet()).add(listener);
        return () -> {
            Set<Consumer<LocationDto>> set = listeners.get(deliveryId);
            if (set != null) set.remove(listener);
        };
    }

    public void publish(Long deliveryId, LocationDto location) {
        Set<Consumer<LocationDto>> set = listeners.get(deliveryId);
        if (set != null) set.forEach(l -> l.accept(location));
    }
}