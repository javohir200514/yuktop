package uz.onetimecourier.one_time_courier.dto;

import java.time.LocalDateTime;

public record LocationDto(double lat, double lng, LocalDateTime recordedAt, Integer etaMinutes) {}