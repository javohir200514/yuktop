package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uz.onetimecourier.one_time_courier.enums.Language;
import uz.onetimecourier.one_time_courier.enums.Role;

public record RegisterRequestDto(
        @NotBlank @Pattern(regexp = "^\\+998\\d{9}$", message = "Telefon +998XXXXXXXXX ko'rinishida bo'lsin")
        String phone,
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Language language) {}