package uz.onetimecourier.one_time_courier.service;


import org.springframework.data.domain.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.onetimecourier.one_time_courier.dto.PublicCourierDto;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

import org.springframework.data.domain.Pageable;

@Service
public class CourierDirectoryService {

    private final UserRepository userRepository;

    public CourierDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<PublicCourierDto> findCouriers(Pageable pageable) {
        return userRepository.findPublicCouriers(Role.COURIER, DeliveryStatus.DELIVERED, pageable);
    }
}