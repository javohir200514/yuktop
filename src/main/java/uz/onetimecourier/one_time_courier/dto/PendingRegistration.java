package uz.onetimecourier.one_time_courier.dto;

import uz.onetimecourier.one_time_courier.enums.Role;

/** Ro'yxatdan o'tish ma'lumotlari, Telegram tasdiqlash sahifasigacha server sessiyasida saqlanadi. */
public record PendingRegistration(String token, String phone, String fullName, String password, Role role) {
}