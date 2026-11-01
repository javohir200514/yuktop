package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.VehicleType;

/** Jo'natuvchi uchun: yukka biriktirilgan kuryer va olish bosqichi holati. */
public record CourierInfoDto(
        String fullName,
        String phone,
        String telegramUsername,
        VehicleType vehicleType,
        String vehicleBrand,
        String vehicleModel,
        String vehicleColor,
        String plateNumber,
        boolean courierPickupRequested,
        boolean senderPickupConfirmed
) {
}