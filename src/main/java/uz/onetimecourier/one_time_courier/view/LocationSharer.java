package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import uz.onetimecourier.one_time_courier.dto.LocationUpdateRequestDto;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.service.TrackingService;

public class LocationSharer extends Div {

    private final Long deliveryId;
    private final Long courierUserId;
    private final TrackingService trackingService;

    public LocationSharer(Long deliveryId, Long courierUserId, TrackingService trackingService) {
        this.deliveryId = deliveryId;
        this.courierUserId = courierUserId;
        this.trackingService = trackingService;
        setText("Joylashuv yuborilmoqda...");
    }

    @Override
    protected void onAttach(AttachEvent e) {
        getElement().executeJs("""
            const el = this;
            if (!navigator.geolocation) { el.$server.onGeoError(0); return; }
            this._watch = navigator.geolocation.watchPosition(
              p => el.$server.onPosition(p.coords.latitude, p.coords.longitude, p.coords.accuracy),
              err => el.$server.onGeoError(err.code),
              { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 });
            """);
    }

    @Override
    protected void onDetach(DetachEvent e) {
        getElement().executeJs("if (this._watch != null) navigator.geolocation.clearWatch(this._watch);");
    }

    @ClientCallable
    private void onPosition(double lat, double lng, double accuracy) {
        try {
            trackingService.updateLocation(courierUserId,
                    new LocationUpdateRequestDto(deliveryId, lat, lng));
        } catch (BusinessException ex) {
            setText(ex.getMessage());
        }
    }

    @ClientCallable
    private void onGeoError(int code) {
        setText(code == 1 ? "Joylashuvga ruxsat bering" : "Joylashuvni aniqlab bo'lmadi");
    }
}