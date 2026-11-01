package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.onetimecourier.one_time_courier.enums.OtpType;

public record OtpVerifyRequestDto(@NotNull OtpType type, @NotBlank @Size(min = 4, max = 6) String code) {}