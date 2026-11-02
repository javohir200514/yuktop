package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import uz.onetimecourier.one_time_courier.dto.VehicleInfo;
import uz.onetimecourier.one_time_courier.enums.VehicleType;
import uz.onetimecourier.one_time_courier.service.VehicleVerificationService;
import uz.onetimecourier.one_time_courier.service.VehicleVerificationService.VerificationResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Ro'yxatdan o'tgandan keyin transport ma'lumotlarini so'raydi va tekshiradi.
 * - Velosiped: marka, model va raqam so'ralmaydi, faqat rang.
 * - Avtomobil va boshqa transport: marka va model ro'yxatdan tanlanadi, "Boshqa" bilan qo'lda kiritiladi.
 * - Davlat raqami: jismoniy yoki yuridik shaxs, rangli plastinka ko'rinishida.
 */
@CssImport("./styles/vehicle-dialog.css")
public class VehicleVerificationDialog extends Dialog {

    private static final String OTHER = "Boshqa";

    /** Marka -> modellar. Ro'yxatda yo'q marka "Boshqa" orqali kiritiladi. */
    private static final Map<String, List<String>> CAR_MODELS = new LinkedHashMap<>();

    static {
        CAR_MODELS.put("Chevrolet", List.of("Cobalt", "Nexia", "Gentra", "Lacetti", "Malibu", "Spark", "Tracker", "Captiva", "Onix"));
        CAR_MODELS.put("Kia", List.of("Rio", "K5", "Sportage", "Sorento", "Cerato", "Carnival"));
        CAR_MODELS.put("Hyundai", List.of("Accent", "Elantra", "Sonata", "Tucson", "Santa Fe", "Creta"));
        CAR_MODELS.put("Toyota", List.of("Camry", "Corolla", "RAV4", "Prado", "Land Cruiser", "Yaris"));
        CAR_MODELS.put("Lada", List.of("Vesta", "Granta", "Niva", "Largus"));
        CAR_MODELS.put("Ravon", List.of("R2", "R4", "Nexia R3", "Gentra"));
        CAR_MODELS.put("Daewoo", List.of("Nexia", "Matiz", "Lanos"));
        CAR_MODELS.put("BYD", List.of("Song Plus", "Han", "Tang", "Qin"));
        CAR_MODELS.put("Nissan", List.of("Qashqai", "X-Trail", "Almera", "Juke"));
        CAR_MODELS.put("Volkswagen", List.of("Polo", "Passat", "Tiguan", "Jetta"));
        CAR_MODELS.put("Honda", List.of("Civic", "Accord", "CR-V"));
        CAR_MODELS.put("Renault", List.of("Logan", "Duster", "Sandero"));
        CAR_MODELS.put("Skoda", List.of("Octavia", "Superb", "Rapid"));
        CAR_MODELS.put("Ford", List.of("Focus", "Mondeo", "Transit"));
        CAR_MODELS.put("Mercedes-Benz", List.of("C-Class", "E-Class", "S-Class", "GLE"));
        CAR_MODELS.put("BMW", List.of("3 Series", "5 Series", "X5"));
    }

    private static final List<String> COLORS = List.of(
            "Oq", "Qora", "Kumush", "Kulrang", "Ko'k", "Qizil", "Yashil", "Sariq", "Jigarrang");

    /** Ikki xil format: 01A123BC yoki 01123AAA (yuridik shaxs raqami ham shu formatlardan biri). */
    private static final Pattern PLATE_A = Pattern.compile("^\\d{2}[A-Z]\\d{3}[A-Z]{2}$");
    private static final Pattern PLATE_B = Pattern.compile("^\\d{2}\\d{3}[A-Z]{3}$");

    private final VehicleVerificationService verifier;
    private final Consumer<VehicleInfo> onVerified;

    private final Select<VehicleType> type = new Select<>();
    private final Select<String> brandSelect = new Select<>();
    private final TextField brandCustom = new TextField("Marka nomi");
    private final Select<String> modelSelect = new Select<>();
    private final TextField modelCustom = new TextField("Model nomi");
    private final Select<String> colorSelect = new Select<>();
    private final TextField plate = new TextField("Davlat raqami");
    private final Checkbox legal = new Checkbox("Yuridik shaxs raqami");

    private final Div plateView = new Div();
    private final Span plateText = new Span();
    private final Div plateRow = new Div();
    private final Paragraph error = new Paragraph();

    public VehicleVerificationDialog(VehicleVerificationService verifier,
                                     Consumer<VehicleInfo> onVerified) {
        this.verifier = verifier;
        this.onVerified = onVerified;

        addClassName("vd-dialog");
        setHeaderTitle("Transport ma'lumotlari");
        setCloseOnOutsideClick(false);
        setWidth("min(520px, 92vw)");

        Div body = new Div(buildFields());
        body.addClassName("vd-body");
        add(body);

        Button cancel = new Button("Bekor qilish", e -> close());
        cancel.addClassName("vd-cancel");

        Button verify = new Button("Tasdiqlash", e -> submit());
        verify.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verify.addClassName("vd-submit");

        getFooter().add(cancel, verify);
        refreshVisibility();
    }

    // ---------------------------------------------------------------- maydonlar

    private Div buildFields() {
        Div grid = new Div();
        grid.addClassName("vd-grid");

        type.setLabel("Transport turi");
        type.setItems(VehicleType.values());
        type.setItemLabelGenerator(VehicleType::getLabel);
        type.setWidthFull();
        type.addValueChangeListener(e -> refreshVisibility());
        type.addClassName("vd-full");

        List<String> brands = new ArrayList<>(CAR_MODELS.keySet());
        brands.add(OTHER);
        brandSelect.setLabel("Marka");
        brandSelect.setItems(brands);
        brandSelect.setWidthFull();
        brandSelect.addValueChangeListener(e -> onBrandChanged());

        brandCustom.setPlaceholder("Masalan: Tesla");
        brandCustom.setWidthFull();
        brandCustom.addValueChangeListener(e -> refreshVisibility());

        modelSelect.setLabel("Model");
        modelSelect.setWidthFull();
        modelSelect.addValueChangeListener(e -> refreshVisibility());

        modelCustom.setPlaceholder("Model nomini kiriting");
        modelCustom.setWidthFull();

        colorSelect.setLabel("Rang");
        colorSelect.setItems(COLORS);
        colorSelect.setWidthFull();

        // Davlat raqami va plastinka ko'rinishi
        plate.setPlaceholder("01 A 123 BC");
        plate.setValueChangeMode(ValueChangeMode.EAGER);
        plate.setMaxLength(12);
        plate.setWidthFull();
        plate.addValueChangeListener(e -> updatePlatePreview());

        legal.addValueChangeListener(e -> updatePlatePreview());

        Span band = new Span("UZ");
        band.addClassName("vd-plate-band");
        plateText.addClassName("vd-plate-text");
        plateView.addClassName("vd-plate");
        plateView.add(band, plateText);

        plateRow.addClassNames("vd-plate-row", "vd-full");
        plateRow.add(plate, legal, plateView);

        error.addClassName("vd-error");
        error.setVisible(false);

        grid.add(type, brandSelect, brandCustom, modelSelect, modelCustom, colorSelect, plateRow, error);
        updatePlatePreview();
        return grid;
    }

    // ---------------------------------------------------------------- ko'rinish mantiqi

    private void onBrandChanged() {
        String brand = brandSelect.getValue();
        brandCustom.clear();
        modelSelect.clear();
        modelCustom.clear();

        if (brand == null) {
            modelSelect.setItems(List.of());
        } else if (OTHER.equals(brand)) {
            modelSelect.setItems(List.of(OTHER));
        } else {
            List<String> models = new ArrayList<>(CAR_MODELS.get(brand));
            models.add(OTHER);
            modelSelect.setItems(models);
        }
        refreshVisibility();
    }

    /** Transport turiga qarab keraksiz maydonlarni yashiradi. */
    private void refreshVisibility() {
        VehicleType selected = type.getValue();
        boolean chosen = selected != null;
        boolean bicycle = isBicycle(selected);
        boolean car = chosen && !bicycle;

        colorSelect.setVisible(chosen);
        brandSelect.setVisible(car);
        modelSelect.setVisible(car);
        plateRow.setVisible(car);

        brandCustom.setVisible(car && OTHER.equals(brandSelect.getValue()));
        modelCustom.setVisible(car && (OTHER.equals(brandSelect.getValue())
                || OTHER.equals(modelSelect.getValue())));
    }

    /** Velosiped turini aniqlaydi. Enum nomida BIKE, BICYCLE yoki VELO bo'lsa velosiped hisoblanadi. */
    private static boolean isBicycle(VehicleType t) {
        return t != null && t.name().toUpperCase(Locale.ROOT).matches(".*(BIKE|BICYCLE|VELO).*");
    }

    // ---------------------------------------------------------------- davlat raqami

    private void updatePlatePreview() {
        String raw = plate.getValue() == null ? "" : plate.getValue();
        boolean empty = raw.isBlank();

        plateText.setText(empty ? "01 A 123 BC" : formatPlate(raw));
        plateText.getClassNames().set("vd-plate-empty", empty);
        plateView.getClassNames().set("vd-plate--legal", legal.getValue());
    }

    /** Raqamni ko'rinish formatiga keltiradi: 01 A 123 BC yoki 01 123 AAA. Format mos kelmasa, bor holatini qaytaradi. */
    private static String formatPlate(String raw) {
        String n = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (PLATE_A.matcher(n).matches()) {
            return n.substring(0, 2) + " " + n.charAt(2) + " " + n.substring(3, 6) + " " + n.substring(6);
        }
        if (PLATE_B.matcher(n).matches()) {
            return n.substring(0, 2) + " " + n.substring(2, 5) + " " + n.substring(5);
        }
        return n;
    }

    // ---------------------------------------------------------------- tekshirish va yuborish

    private void submit() {
        String problem = validate();
        if (problem != null) {
            showError(problem);
            return;
        }

        VerificationResult result = verifier.verify(buildInfo());
        if (!result.ok()) {
            showError(result.error());
            return;
        }

        close();
        onVerified.accept(result.verified());
    }

    private String validate() {
        VehicleType selected = type.getValue();
        if (selected == null) {
            return "Transport turini tanlang";
        }
        if (colorSelect.getValue() == null) {
            return "Rangni tanlang";
        }
        if (isBicycle(selected)) {
            return null;
        }

        if (brandSelect.getValue() == null) {
            return "Markani tanlang";
        }
        if (OTHER.equals(brandSelect.getValue()) && isBlank(brandCustom.getValue())) {
            return "Marka nomini kiriting";
        }
        if (modelSelect.getValue() == null) {
            return "Modelni tanlang";
        }
        if (OTHER.equals(modelSelect.getValue()) && isBlank(modelCustom.getValue())) {
            return "Model nomini kiriting";
        }

        String n = normalizePlate(plate.getValue());
        if (n.isEmpty()) {
            return "Davlat raqamini kiriting";
        }
        if (!PLATE_A.matcher(n).matches() && !PLATE_B.matcher(n).matches()) {
            return "Raqam formati noto'g'ri. Masalan: 01 A 123 BC yoki 01 123 AAA";
        }
        return null;
    }

    private VehicleInfo buildInfo() {
        VehicleType selected = type.getValue();
        String color = colorSelect.getValue();

        if (isBicycle(selected)) {
            return new VehicleInfo(selected, null, null, color, null);
        }

        String brand = OTHER.equals(brandSelect.getValue())
                ? brandCustom.getValue().trim()
                : brandSelect.getValue();
        String model = OTHER.equals(modelSelect.getValue())
                ? modelCustom.getValue().trim()
                : modelSelect.getValue();
        String formattedPlate = formatPlate(normalizePlate(plate.getValue()));

        return new VehicleInfo(selected, brand, model, color, formattedPlate);
    }

    private static String normalizePlate(String raw) {
        return raw == null ? "" : raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
    }
}