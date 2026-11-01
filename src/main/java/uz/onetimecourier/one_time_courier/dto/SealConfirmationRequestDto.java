package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotNull;

public record SealConfirmationRequestDto(@NotNull Boolean sealed, String photoUrl) {}
