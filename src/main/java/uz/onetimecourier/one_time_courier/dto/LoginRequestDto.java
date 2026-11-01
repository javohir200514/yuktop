package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank String phone, @NotBlank String password) {}