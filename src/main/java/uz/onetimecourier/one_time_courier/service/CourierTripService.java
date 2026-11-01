package uz.onetimecourier.one_time_courier.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.onetimecourier.one_time_courier.dto.CreateTripRequestDto;
import uz.onetimecourier.one_time_courier.dto.TripDto;
import uz.onetimecourier.one_time_courier.entity.CourierTripEntity;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.enums.TripStatus;
import uz.onetimecourier.one_time_courier.repository.CourierTripRepository;
import uz.onetimecourier.one_time_courier.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class CourierTripService {

    static final int MAX_ACTIVE_TRIPS = 5;
    static final int MAX_NOTE_LENGTH = 300;
    static final int MAX_PAGE_SIZE = 50;
    static final Duration MIN_LEAD = Duration.ofMinutes(30);
    static final Duration MAX_LEAD = Duration.ofDays(7);

    private final CourierTripRepository tripRepository;
    private final UserRepository userRepository;

    public CourierTripService(CourierTripRepository tripRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TripDto create(Long courierId, CreateTripRequestDto dto) {
        validate(dto);

        UserEntity courier = userRepository.findById(courierId)
                .orElseThrow(() -> new IllegalStateException("Foydalanuvchi topilmadi. Qaytadan kiring."));
        if (courier.getRole() != Role.COURIER) {
            throw new IllegalStateException("Faqat kuryerlar safar e'lon qila oladi.");
        }

        LocalDateTime now = LocalDateTime.now();
        long active = tripRepository.countUpcomingByCourier(courierId, TripStatus.ACTIVE, now);
        if (active >= MAX_ACTIVE_TRIPS) {
            throw new IllegalStateException("Sizda allaqachon " + MAX_ACTIVE_TRIPS
                    + " ta faol safar bor. Avval eski e'lonlardan birini bekor qiling.");
        }

        CourierTripEntity trip = new CourierTripEntity();
        trip.setCourier(courier);
        trip.setFromRegion(dto.fromRegion());
        trip.setToRegion(dto.toRegion());
        trip.setDepartureAt(dto.departureAt());
        trip.setMaxPackageSize(dto.maxPackageSize());
        trip.setNote(blankToNull(dto.note()));
        trip.setStatus(TripStatus.ACTIVE);
        trip.setCreatedAt(now);

        CourierTripEntity saved = tripRepository.save(trip);
        return toDto(saved, courier);
    }

    /**
     * Kelajakdagi faol safarlar. from/to null bo'lsa, o'sha yo'nalish bo'yicha filtr qo'yilmaydi.
     * Eng yaqin jo'nash birinchi.
     */
    @Transactional(readOnly = true)
    public Page<TripDto> upcoming(Region from, Region to, Pageable pageable) {
        Pageable safe = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.asc("departureAt"), Sort.Order.asc("id")));

        LocalDateTime now = LocalDateTime.now();
        TripStatus active = TripStatus.ACTIVE;

        Page<CourierTripEntity> result;
        if (from != null && to != null) {
            result = tripRepository.findByStatusAndDepartureAtAfterAndFromRegionAndToRegion(active, now, from, to, safe);
        } else if (from != null) {
            result = tripRepository.findByStatusAndDepartureAtAfterAndFromRegion(active, now, from, safe);
        } else if (to != null) {
            result = tripRepository.findByStatusAndDepartureAtAfterAndToRegion(active, now, to, safe);
        } else {
            result = tripRepository.findByStatusAndDepartureAtAfter(active, now, safe);
        }

        return result.map(t -> toDto(t, t.getCourier()));
    }

    /**
     * Jo'nash vaqti o'tgan faol e'lonlarni arxivga o'tkazadi.
     * Scheduler tomonidan muntazam chaqiriladi.
     */
    @Transactional
    public int archiveExpired() {
        return tripRepository.archiveExpired(TripStatus.ACTIVE, TripStatus.ARCHIVED, LocalDateTime.now());
    }

    private static void validate(CreateTripRequestDto dto) {
        if (dto.fromRegion() == null || dto.toRegion() == null) {
            throw new IllegalArgumentException("Yo'nalishni tanlang");
        }
        if (dto.departureAt() == null) {
            throw new IllegalArgumentException("Jo'nash sanasi va vaqtini kiriting");
        }
        if (dto.maxPackageSize() == null) {
            throw new IllegalArgumentException("Yuk o'lchamini tanlang");
        }
        if (dto.note() != null && dto.note().strip().length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Izoh " + MAX_NOTE_LENGTH + " belgidan oshmasin");
        }

        LocalDateTime now = LocalDateTime.now();
        if (dto.departureAt().isBefore(now.plus(MIN_LEAD))) {
            throw new IllegalArgumentException("Jo'nash vaqti kamida 30 daqiqa keyin bo'lishi kerak");
        }
        if (dto.departureAt().isAfter(now.plus(MAX_LEAD))) {
            throw new IllegalArgumentException("Safarni 7 kundan ko'p oldin e'lon qilib bo'lmaydi");
        }
    }

    private static TripDto toDto(CourierTripEntity t, UserEntity courier) {
        return new TripDto(
                t.getId(),
                courier.getFullName(),
                courier.getPhone(),
                courier.getVehicleType(),
                courier.getVehicleBrand(),
                courier.getVehicleColor(),
                t.getFromRegion(),
                t.getToRegion(),
                t.getDepartureAt(),
                t.getMaxPackageSize(),
                t.getNote(),
                t.getStatus());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}