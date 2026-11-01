package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(Long id, Long deliveryId, BigDecimal amount, PaymentStatus status,
                         LocalDateTime heldAt, LocalDateTime releasedAt) {}