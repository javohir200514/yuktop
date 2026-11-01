package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequestDto(@NotNull Long deliveryId,
                                       @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
                                       @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {}