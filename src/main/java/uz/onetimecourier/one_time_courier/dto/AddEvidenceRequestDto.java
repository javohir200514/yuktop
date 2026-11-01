package uz.onetimecourier.one_time_courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddEvidenceRequestDto(@NotBlank String fileUrl, @Size(max = 300) String note) {}