package uz.onetimecourier.one_time_courier.dto;

/** Foydalanuvchi ishonchliligi. failed: kuryer uchun nizo, jo'natuvchi uchun bekor qilingan + nizo. */
public record ReputationDto(double avgStars, long ratingCount, long delivered, long failed) {

    public long total() {
        return delivered + failed;
    }

    /** Bajarilgan yuklar foizi (0..100). Yuk bo'lmasa 0. */
    public double completionPercent() {
        return total() == 0 ? 0 : delivered * 100.0 / total();
    }
}