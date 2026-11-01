package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.VerificationStatus;

import java.math.BigDecimal;

public record CourierProfileDto(Long userId, String fullName, VerificationStatus verificationStatus,
                                BigDecimal trustScore, int completedDeliveries) {}