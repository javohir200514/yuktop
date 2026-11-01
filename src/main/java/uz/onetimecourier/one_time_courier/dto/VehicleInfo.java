package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.VehicleType;

public record VehicleInfo(
        VehicleType type,
        String brand,
        String model,
        String color,
        String plateNumber
) {
}