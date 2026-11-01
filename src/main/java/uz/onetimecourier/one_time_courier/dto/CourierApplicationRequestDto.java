package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;

public record CourierApplicationRequestDto(@NotBlank String documentUrl, @NotBlank String selfieUrl) {}