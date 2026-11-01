package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.OtpType;

import java.time.LocalDateTime;

public record OtpDto(OtpType type, String code, LocalDateTime expiresAt) {}