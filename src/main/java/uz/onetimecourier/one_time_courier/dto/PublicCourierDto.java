package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.VehicleType;

public record PublicCourierDto(
        Long id,
        String fullName,
        String phone,
        VehicleType vehicleType,
        String vehicleBrand,
        String vehicleColor,
        long deliveredCount
) {
}