package uz.onetimecourier.one_time_courier.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.onetimecourier.one_time_courier.service.CourierTripService;

@Component
public class TripArchiveJob {

    private static final Logger log = LoggerFactory.getLogger(TripArchiveJob.class);

    private final CourierTripService tripService;

    public TripArchiveJob(CourierTripService tripService) {
        this.tripService = tripService;
    }

    @Scheduled(fixedDelayString = "${trips.archive.interval-ms:60000}")
    public void archiveExpiredTrips() {
        int count = tripService.archiveExpired();
        if (count > 0) {
            log.info("Muddati o'tgan {} ta safar arxivga o'tkazildi", count);
        }
    }
}