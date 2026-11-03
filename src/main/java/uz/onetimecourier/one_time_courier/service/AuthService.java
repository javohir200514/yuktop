package uz.onetimecourier.one_time_courier.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import uz.onetimecourier.one_time_courier.dto.RegisterRequestDto;
import uz.onetimecourier.one_time_courier.dto.UserDto;
import uz.onetimecourier.one_time_courier.dto.VehicleInfo;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.Language;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.mapper.UserMapper;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

@Service
@Validated
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** "+998 90 123-45-67" -> "+998901234567". Register va login bir xil ishlatadi. */
    public static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.isEmpty() ? "" : "+" + digits;
    }

    /** Bo'sh yoki null matnni null qiladi, aks holda probelsiz qaytaradi. */
    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Transactional
    public UserDto register(@Valid RegisterRequestDto dto) {
        if (dto.role() == Role.ADMIN) {
            throw new BusinessException("Bu rolni tanlab bo'lmaydi");
        }
        String phone = normalizePhone(dto.phone());
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("Bu telefon raqam allaqachon ro'yxatdan o'tgan");
        }

        UserEntity user = new UserEntity();
        user.setPhone(phone);
        user.setFullName(dto.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        user.setLanguage(dto.language() != null ? dto.language() : Language.UZ);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Bu telefon raqam allaqachon ro'yxatdan o'tgan");
        }
        return UserMapper.toDto(user);
    }

    /** Kod yuborishdan oldin: telefon allaqachon band bo'lsa true. */
    @Transactional(readOnly = true)
    public boolean isPhoneTaken(String rawPhone) {
        return userRepository.existsByPhone(normalizePhone(rawPhone));
    }

    /** Eski chaqiruvlar uchun: Telegram ID siz ro'yxatdan o'tkazish. */
    @Transactional
    public UserDto registerVerified(@Valid RegisterRequestDto dto, VehicleInfo vehicle) {
        return registerVerified(dto, vehicle, null);
    }

    /**
     * Telefon tasdiqlangandan keyin ro'yxatdan o'tkazadi.
     * telegramId botdagi kontakt orqali olingan Telegram ID; bildirishnomalar shu orqali yuboriladi.
     */
    @Transactional
    public UserDto registerVerified(@Valid RegisterRequestDto dto, VehicleInfo vehicle, Long telegramId) {
        if (dto.role() == Role.ADMIN) {
            throw new BusinessException("Bu rolni tanlab bo'lmaydi");
        }
        if (dto.role() == Role.COURIER && vehicle == null) {
            throw new BusinessException("Kuryer uchun transport ma'lumoti kerak");
        }
        if (dto.role() != Role.COURIER && vehicle != null) {
            throw new BusinessException("Transport ma'lumoti faqat kuryer uchun");
        }

        String phone = normalizePhone(dto.phone());
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("Bu telefon raqam allaqachon ro'yxatdan o'tgan");
        }

        if (telegramId != null && userRepository.findByTelegramId(telegramId).isPresent()) {
            throw new BusinessException("Bu Telegram hisob boshqa akkauntga bog'langan");
        }

        UserEntity user = new UserEntity();
        user.setPhone(phone);
        user.setFullName(dto.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        user.setLanguage(dto.language() != null ? dto.language() : Language.UZ);
        user.setPhoneVerified(true);
        user.setTelegramId(telegramId);

        if (vehicle != null) {
            // Velosiped va mototsikl uchun marka/model/raqam bo'lmasligi mumkin
            user.setVehicleType(vehicle.type());
            user.setVehicleBrand(clean(vehicle.brand()));
            user.setVehicleModel(clean(vehicle.model()));
            user.setVehicleColor(clean(vehicle.color()));
            user.setPlateNumber(clean(vehicle.plateNumber()));
        }

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Bu telefon raqam allaqachon ro'yxatdan o'tgan");
        }
        return UserMapper.toDto(user);
    }
}