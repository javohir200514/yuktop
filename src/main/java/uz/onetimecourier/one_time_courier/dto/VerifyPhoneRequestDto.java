package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPhoneRequestDto(@NotBlank String phone, @NotBlank @Size(min = 4, max = 6) String code) {}