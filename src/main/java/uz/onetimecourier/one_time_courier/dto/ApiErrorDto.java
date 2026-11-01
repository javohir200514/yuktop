package uz.onetimecourier.one_time_courier.dto;


import java.time.LocalDateTime;

public record ApiErrorDto(String code, String message, LocalDateTime timestamp) {}