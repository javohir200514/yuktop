package uz.onetimecourier.one_time_courier.dto;

public record LandingStatsDto(long successfulDeliveries, long verifiedCouriers,
                              double averageDeliveryMinutes, double averageRating) {}