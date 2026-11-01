package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;

import java.time.LocalDateTime;

public record CreateTripRequestDto(
        Region fromRegion,
        Region toRegion,
        LocalDateTime departureAt,
        PackageSize maxPackageSize,
        String note
) {
}