package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequestDto(@NotNull Long deliveryId, @Min(1) @Max(5) int rating,
                                     @Size(max = 500) String comment) {}