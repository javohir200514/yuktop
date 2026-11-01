package uz.onetimecourier.one_time_courier.mapper;

import uz.onetimecourier.one_time_courier.dto.UserDto;
import uz.onetimecourier.one_time_courier.entity.UserEntity;

public final class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(UserEntity e) {
        return new UserDto(
                e.getId(),
                e.getFullName(),
                e.getPhone(),
                e.getEmail(),
                e.getRole(),
                e.getLanguage(),
                e.isPhoneVerified(),
                e.getAuthProvider(),
                e.getTelegramId(),
                e.getTelegramUsername());
    }
}