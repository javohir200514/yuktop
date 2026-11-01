package uz.onetimecourier.one_time_courier.service;

import org.springframework.stereotype.Service;
import uz.onetimecourier.one_time_courier.dto.VehicleInfo;

/**
 * Hozircha faqat format tekshiradi. Haqiqiy tasdiqlash uchun
 * shu interfeysning boshqa implementatsiyasini yozib, uni @Primary qiling.
 */
@Service
public class FormatVehicleVerificationService implements VehicleVerificationService {

    @Override
    public VerificationResult verify(VehicleInfo draft) {
        if (draft == null || draft.type() == null) {
            return VerificationResult.failure("Transport turini tanlang");
        }
        if (isBlank(draft.brand()) || isBlank(draft.model()) || isBlank(draft.color())) {
            return VerificationResult.failure("Marka, model va rangni kiriting");
        }

        String plate = PlateNumberValidator.normalize(draft.plateNumber());
        if (!PlateNumberValidator.isValid(plate)) {
            return VerificationResult.failure("Davlat raqami noto'g'ri. Masalan: 01 A 123 BC");
        }

        return VerificationResult.success(new VehicleInfo(
                draft.type(),
                draft.brand().trim(),
                draft.model().trim(),
                draft.color().trim(),
                plate));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}