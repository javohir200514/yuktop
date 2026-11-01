package uz.onetimecourier.one_time_courier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        UserEntity u = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("Foydalanuvchi topilmadi"));
        return User.withUsername(u.getPhone())
                .password(u.getPasswordHash())
                .roles(u.getRole().name())
                .build();
    }
}