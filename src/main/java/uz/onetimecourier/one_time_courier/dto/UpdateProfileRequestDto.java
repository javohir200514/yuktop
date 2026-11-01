package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import uz.onetimecourier.one_time_courier.enums.Language;

public record UpdateProfileRequestDto(@NotBlank @Size(max = 120) String fullName, Language language) {}