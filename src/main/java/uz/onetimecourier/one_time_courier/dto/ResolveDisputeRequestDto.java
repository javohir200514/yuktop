package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.onetimecourier.one_time_courier.enums.DisputeStatus;

public record ResolveDisputeRequestDto(@NotNull DisputeStatus status, @NotBlank @Size(max = 1000) String resolution) {}