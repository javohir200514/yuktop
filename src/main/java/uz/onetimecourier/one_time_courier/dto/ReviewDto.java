package uz.onetimecourier.one_time_courier.dto;

import java.time.LocalDateTime;

public record ReviewDto(Long id, Long deliveryId, String authorName, int rating,
                        String comment, LocalDateTime createdAt) {}