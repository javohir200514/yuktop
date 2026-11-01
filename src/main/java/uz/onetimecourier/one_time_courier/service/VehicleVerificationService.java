package uz.onetimecourier.one_time_courier.service;

import uz.onetimecourier.one_time_courier.dto.VehicleInfo;

public interface VehicleVerificationService {

    VerificationResult verify(VehicleInfo draft);

    record VerificationResult(boolean ok, VehicleInfo verified, String error) {

        public static VerificationResult success(VehicleInfo verified) {
            return new VerificationResult(true, verified, null);
        }

        public static VerificationResult failure(String error) {
            return new VerificationResult(false, null, error);
        }
    }
}