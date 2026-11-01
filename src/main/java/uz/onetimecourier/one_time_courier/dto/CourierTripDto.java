package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.TripStatus;

import java.time.LocalDateTime;

public record CourierTripDto(Long id, String fromAddress, String toAddress,
                             LocalDateTime departureTime, TripStatus status) {}