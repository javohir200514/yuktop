package uz.onetimecourier.one_time_courier.enums;

public enum VehicleType {
    YENGIL_AVTO("Yengil avtomobil"),
    YUK_AVTO("Yuk avtomobili"),
    MOTOSIKL("Mototsikl"),
    VELOSIPED("Velosiped");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}