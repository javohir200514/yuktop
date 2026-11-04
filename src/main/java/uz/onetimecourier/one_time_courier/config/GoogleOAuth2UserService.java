package uz.onetimecourier.one_time_courier.config;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

import java.util.Optional;

/**
 * Google login: foydalanuvchini bazadan topadi (googleId, keyin email bo'yicha) yoki yangi yaratadi.
 * Qaytaradigan principal CustomUserDetails bo'ladi.
 */
@Service
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public GoogleOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        OAuth2User google = delegate.loadUser(request);

        String googleId = google.getAttribute("sub");
        String email = google.getAttribute("email");
        String name = google.getAttribute("name");

        if (googleId == null || googleId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"), "Google foydalanuvchi ID si topilmadi");
        }

        Optional<UserEntity> found = userRepository.findByGoogleId(googleId);
        if (found.isEmpty() && email != null) {
            found = userRepository.findByEmail(email);
        }

        UserEntity user;
        if (found.isPresent()) {
            user = found.get();
        } else {
            // Yangi Google foydalanuvchi. Rol hozircha SENDER; keyin o'zgartirish mumkin.
            user = new UserEntity();
            user.setRole(Role.SENDER);
            user.setFullName(name != null && !name.isBlank() ? name : "Foydalanuvchi");
        }

        user.setGoogleId(googleId);

        if (user.getEmail() == null && email != null) {
            user.setEmail(email);
        }

        Boolean emailVerified = google.getAttribute("email_verified");

        if (Boolean.TRUE.equals(emailVerified)) {
            user.setPhoneVerified(true);
        }

        user = userRepository.save(user);

        return new CustomUserDetails(user, google.getAttributes());
    }
}