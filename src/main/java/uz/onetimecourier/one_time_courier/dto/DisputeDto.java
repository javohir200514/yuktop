package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.DisputeReason;
import uz.onetimecourier.one_time_courier.enums.DisputeStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DisputeDto(Long id, Long deliveryId, UserDto openedBy, DisputeReason reason,
                         String description, DisputeStatus status, String resolution,
                         List<DisputeEvidenceDto> evidence, LocalDateTime createdAt, LocalDateTime resolvedAt) {}