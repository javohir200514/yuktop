package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerificationDecisionRequestDto(@NotNull Boolean approved, @Size(max = 300) String rejectionReason) {}