package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.enums.TripStatus;
import uz.onetimecourier.one_time_courier.enums.VehicleType;

import java.time.LocalDateTime;

public record TripDto(
        Long id,
        String courierName,
        String courierPhone,
        VehicleType vehicleType,
        String vehicleBrand,
        String vehicleColor,
        Region fromRegion,
        Region toRegion,
        LocalDateTime departureAt,
        PackageSize maxPackageSize,
        String note,
        TripStatus status
) {
}