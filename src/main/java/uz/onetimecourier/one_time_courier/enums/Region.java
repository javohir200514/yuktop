package uz.onetimecourier.one_time_courier.enums;

public enum Region {
    TOSHKENT_SHAHRI("Toshkent shahri"), TOSHKENT_VILOYATI("Toshkent viloyati"),
    ANDIJON("Andijon"), BUXORO("Buxoro"), FARGONA("Farg'ona"), JIZZAX("Jizzax"),
    XORAZM("Xorazm"), NAMANGAN("Namangan"), NAVOIY("Navoiy"), QASHQADARYO("Qashqadaryo"),
    QORAQALPOGISTON("Qoraqalpog'iston Respublikasi"), SAMARQAND("Samarqand"),
    SIRDARYO("Sirdaryo"), SURXONDARYO("Surxondaryo");

    private final String label;
    Region(String label) { this.label = label; }
    public String getLabel() { return label; }
}