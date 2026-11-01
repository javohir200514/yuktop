package uz.onetimecourier.one_time_courier.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.onetimecourier.one_time_courier.dto.LocationDto;
import uz.onetimecourier.one_time_courier.dto.LocationUpdateRequestDto;
import uz.onetimecourier.one_time_courier.entity.DeliveryEntity;
import uz.onetimecourier.one_time_courier.entity.LocationPingEntity;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.repository.DeliveryRepository;
import uz.onetimecourier.one_time_courier.repository.LocationPingRepository;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private static final long MIN_INTERVAL_SECONDS = 5;

    private final DeliveryRepository deliveryRepository;
    private final LocationPingRepository pingRepository;
    private final LocationBroadcaster broadcaster;

    @Transactional
    public void updateLocation(Long courierUserId, LocationUpdateRequestDto dto) {
        DeliveryEntity d = deliveryRepository.findById(dto.deliveryId())
                .orElseThrow(() -> new BusinessException("Yetkazma topilmadi"));

        if (d.getCourier() == null || !d.getCourier().getId().equals(courierUserId)) {
            throw new BusinessException("Bu yetkazma sizga biriktirilmagan");
        }
        if (d.getStatus() != DeliveryStatus.MATCHED && d.getStatus() != DeliveryStatus.PICKED_UP) {
            throw new BusinessException("Hozir joylashuv yuborib bo'lmaydi");
        }
        LocalDateTime now = LocalDateTime.now();
        if (d.getCourierUpdatedAt() != null
                && Duration.between(d.getCourierUpdatedAt(), now).getSeconds() < MIN_INTERVAL_SECONDS) {
            return;
        }

        d.setCourierLat(dto.lat());
        d.setCourierLng(dto.lng());
        d.setCourierUpdatedAt(now);

        LocationPingEntity ping = new LocationPingEntity();
        ping.setDelivery(d);
        ping.setCourier(d.getCourier());
        ping.setLat(dto.lat());
        ping.setLng(dto.lng());
        pingRepository.save(ping);

        broadcaster.publish(d.getId(), new LocationDto(dto.lat(), dto.lng(), now, null));
    }

    @Transactional(readOnly = true)
    public Optional<LocationDto> lastLocation(Long deliveryId, Long viewerUserId) {
        DeliveryEntity d = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException("Yetkazma topilmadi"));
        boolean isSender = d.getSender().getId().equals(viewerUserId);
        boolean isCourier = d.getCourier() != null && d.getCourier().getId().equals(viewerUserId);
        if (!isSender && !isCourier) {
            throw new BusinessException("Ruxsat yo'q");
        }
        if (d.getCourierLat() == null) return Optional.empty();
        return Optional.of(new LocationDto(d.getCourierLat(), d.getCourierLng(), d.getCourierUpdatedAt(), null));
    }


}