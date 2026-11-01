package uz.onetimecourier.one_time_courier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.AuthProvider;
import uz.onetimecourier.one_time_courier.enums.Language;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class OAuthUserService {

    private final UserRepository userRepository;

    @Transactional
    public UserEntity findOrCreateGoogleUser(String googleId, String email, String fullName) {
        return userRepository.findByGoogleId(googleId).orElseGet(() -> {
            UserEntity u = newUser(fullName, AuthProvider.GOOGLE);
            u.setGoogleId(googleId);
            u.setEmail(email);
            return userRepository.saveAndFlush(u);
        });
    }

    @Transactional
    public UserEntity findOrCreateTelegramUser(long telegramId, String firstName, String lastName, String username) {
        return userRepository.findByTelegramId(telegramId).orElseGet(() -> {
            String name = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
            UserEntity u = newUser(name.isEmpty() ? username : name, AuthProvider.TELEGRAM);
            u.setTelegramId(telegramId);
            u.setTelegramUsername(username);
            return userRepository.saveAndFlush(u);
        });
    }

    private UserEntity newUser(String fullName, AuthProvider provider) {
        String name = (fullName == null || fullName.isBlank()) ? "Foydalanuvchi" : fullName.trim();
        UserEntity u = new UserEntity();
        u.setFullName(name.length() > 120 ? name.substring(0, 120) : name);
        u.setRole(Role.SENDER);
        u.setLanguage(Language.UZ);
        u.setAuthProvider(provider);
        return u;
    }
}