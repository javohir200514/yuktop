package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.onetimecourier.one_time_courier.config.CustomUserDetails;
import uz.onetimecourier.one_time_courier.dto.CreateTripRequestDto;
import uz.onetimecourier.one_time_courier.dto.TripDto;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.CourierTripService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

@Route(value = "trips/new", layout = PageLayout.class)
@PageTitle("Safar e'lon qilish | YukBor")
@RolesAllowed("COURIER")
@CssImport("./styles/create-delivery.css")
public class CreateTripView extends Div {

    private static final Logger log = LoggerFactory.getLogger(CreateTripView.class);
    private static final String GENERIC_ERROR =
            "Kutilmagan xato yuz berdi. Birozdan keyin qayta urinib ko'ring.";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");

    private final CourierTripService tripService;
    private final AuthenticationContext authContext;

    private final H1 title = new H1("Safar e'lon qilish");
    private final Paragraph lead = new Paragraph(
            "Qayerdan qayerga, qachon ketayotganingizni kiriting. Jo'nash vaqti o'tgach, "
                    + "e'lon avtomatik arxivga o'tadi.");

    private final Select<Region> fromRegion = new Select<>();
    private final Select<Region> toRegion = new Select<>();
    private final DatePicker departDate = new DatePicker("Sana *");
    private final TimePicker departTime = new TimePicker("Vaqt *");
    private final TextArea note = new TextArea("Izoh (ixtiyoriy)");

    private final Map<PackageSize, NativeButton> sizeButtons = new EnumMap<>(PackageSize.class);
    private final Div sizeError = new Div();
    private PackageSize selectedSize;

    private final Div formAlert = new Div();
    private final NativeButton submitButton = new NativeButton("E'lon qilish");

    private final Span summaryRoute = new Span();
    private final Span summaryDeparture = new Span();
    private final Span summarySize = new Span();
    private final Span summaryNote = new Span();

    private final Div layoutBox = new Div();
    private final Div successPanel = new Div();
    private final H2 successTitle = new H2("E'lon joylandi");
    private final Span successRoute = new Span();
    private final Span successDeparture = new Span();
    private final Span successSize = new Span();

    private boolean submitting;

    public CreateTripView(CourierTripService tripService, AuthenticationContext authContext) {
        this.tripService = tripService;
        this.authContext = authContext;

        addClassName("cd-page");
        title.addClassName("cd-title");
        lead.addClassName("cd-lead");

        layoutBox.addClassName("cd-layout");
        layoutBox.add(buildForm(), buildSummary());

        buildSuccessPanel();

        add(title, lead, layoutBox, successPanel);
        updateSummary();
    }

    // ================================================================ forma

    private Div buildForm() {
        Div form = new Div();
        form.addClassName("cd-form");
        form.add(routeSection(), timeSection(), packageSection(), noteSection(), actionsBlock());
        return form;
    }

    private Div routeSection() {
        Div section = section("1", "Yo'nalish");

        configureRegion(fromRegion, "Qayerdan (viloyat)");
        configureRegion(toRegion, "Qayerga (viloyat)");
        fromRegion.addValueChangeListener(e -> updateSummary());
        toRegion.addValueChangeListener(e -> updateSummary());

        Div row = new Div(fromRegion, toRegion);
        row.addClassNames("cd-row", "cd-row--2");

        NativeButton swap = new NativeButton("⇄ Yo'nalishni almashtirish");
        swap.addClassNames("cd-btn", "cd-btn--link");
        swap.getElement().setAttribute("type", "button");
        swap.addClickListener(e -> {
            Region a = fromRegion.getValue();
            fromRegion.setValue(toRegion.getValue());
            toRegion.setValue(a);
        });
        Div swapBox = new Div(swap);
        swapBox.addClassName("cd-swap");

        section.add(row, swapBox);
        return section;
    }

    private Div timeSection() {
        Div section = section("2", "Jo'nash vaqti");

        departDate.setWidthFull();
        departDate.setRequiredIndicatorVisible(true);
        departDate.setMin(LocalDate.now());
        departDate.setMax(LocalDate.now().plusDays(MAX_DAYS));
        departDate.setHelperText("Bugundan boshlab 7 kungacha");
        departDate.addValueChangeListener(e -> updateSummary());

        departTime.setWidthFull();
        departTime.setRequiredIndicatorVisible(true);
        departTime.setStep(Duration.ofMinutes(30));
        departTime.setHelperText("Kamida 30 daqiqa keyin");
        departTime.addValueChangeListener(e -> updateSummary());

        Div row = new Div(departDate, departTime);
        row.addClassNames("cd-row", "cd-row--2");
        section.add(row);
        return section;
    }

    private Div packageSection() {
        Div section = section("3", "Sig'im");

        Span label = new Span("Eng katta yuk o'lchami *");
        label.addClassName("cd-label");

        Div group = new Div();
        group.addClassName("cd-size-group");
        group.getElement().setAttribute("role", "radiogroup");

        for (PackageSize size : PackageSize.values()) {
            NativeButton b = new NativeButton();
            b.addClassName("cd-size");
            b.getElement().setAttribute("type", "button");
            b.getElement().setAttribute("role", "radio");
            b.getElement().setAttribute("aria-checked", "false");

            Span name = new Span(sizeLabel(size));
            name.addClassName("cd-size-name");
            Span hint = new Span(sizeHint(size));
            hint.addClassName("cd-size-hint");
            b.add(name, hint);

            b.addClickListener(e -> selectSize(size));
            sizeButtons.put(size, b);
            group.add(b);
        }

        sizeError.addClassName("cd-error");
        sizeError.getElement().setAttribute("role", "alert");
        sizeError.setVisible(false);

        section.add(label, group, sizeError);
        return section;
    }

    private Div noteSection() {
        Div section = section("4", "Qo'shimcha");

        note.setWidthFull();
        note.setMaxLength(MAX_NOTE);
        note.setValueChangeMode(ValueChangeMode.EAGER);
        note.setMinHeight("96px");
        note.setHelperText("Masalan: faqat hujjat va kichik qutilar olaman");
        note.addValueChangeListener(e -> updateSummary());

        section.add(note);
        return section;
    }

    private Div actionsBlock() {
        Div actions = new Div();
        actions.addClassName("cd-actions");

        Paragraph info = new Paragraph(
                "E'londa hisobingizdagi telefon raqami va transport ma'lumoti ko'rinadi.");
        info.addClassName("cd-contact-note");

        formAlert.addClassName("cd-alert");
        formAlert.getElement().setAttribute("role", "alert");
        formAlert.setVisible(false);

        submitButton.addClassName("cd-submit");
        submitButton.getElement().setAttribute("type", "button");
        submitButton.addClickListener(e -> submit());

        actions.add(info, formAlert, submitButton);
        return actions;
    }

    // ================================================================ xulosa

    private Div buildSummary() {
        Div box = new Div();
        box.addClassName("cd-summary");

        H2 summaryTitle = new H2("Xulosa");
        summaryTitle.addClassName("cd-summary-title");

        Div list = new Div(
                row("Yo'nalish", summaryRoute),
                row("Jo'nash", summaryDeparture),
                row("Sig'im", summarySize),
                row("Izoh", summaryNote));
        list.addClassName("cd-summary-list");

        Paragraph noteText = new Paragraph("E'lon jo'nash vaqtidan so'ng arxivga o'tadi va ro'yxatdan chiqadi.");
        noteText.addClassName("cd-summary-note");

        box.add(summaryTitle, list, noteText);
        return box;
    }

    private Div row(String label, Span value) {
        Span l = new Span(label);
        l.addClassName("cd-summary-label");
        value.addClassName("cd-summary-value");
        Div row = new Div(l, value);
        row.addClassName("cd-summary-row");
        return row;
    }

    private void updateSummary() {
        Region from = fromRegion.getValue();
        Region to = toRegion.getValue();
        summaryRoute.setText(from == null && to == null
                ? "—"
                : (from == null ? "?" : from.getLabel()) + " → " + (to == null ? "?" : to.getLabel()));

        LocalDateTime departure = combineDeparture();
        summaryDeparture.setText(departure == null ? "—" : DATE_FORMAT.format(departure));

        summarySize.setText(selectedSize == null ? "—" : sizeLabel(selectedSize));

        String n = note.getValue() == null ? "" : note.getValue().strip();
        summaryNote.setText(n.isEmpty() ? "—" : n);
    }

    // ================================================================ yuborish

    private void submit() {
        if (submitting) {
            return;
        }
        clearErrors();

        boolean valid = true;
        if (fromRegion.getValue() == null) {
            markInvalid(fromRegion, "Qayerdanni tanlang");
            valid = false;
        }
        if (toRegion.getValue() == null) {
            markInvalid(toRegion, "Qayerga ni tanlang");
            valid = false;
        }
        if (departDate.getValue() == null) {
            markInvalid(departDate, "Sanani tanlang");
            valid = false;
        }
        if (departTime.getValue() == null) {
            markInvalid(departTime, "Vaqtni tanlang");
            valid = false;
        }
        if (selectedSize == null) {
            sizeError.setText("Yuk o'lchamini tanlang");
            sizeError.setVisible(true);
            valid = false;
        }
        if (!valid) {
            showAlert("Iltimos, belgilangan maydonlarni tekshiring.");
            return;
        }

        CustomUserDetails user = authContext.getAuthenticatedUser(CustomUserDetails.class).orElse(null);
        if (user == null) {
            showAlert("Sessiya tugagan. Iltimos, qaytadan kiring.");
            return;
        }

        CreateTripRequestDto dto = new CreateTripRequestDto(
                fromRegion.getValue(),
                toRegion.getValue(),
                combineDeparture(),
                selectedSize,
                note.getValue() == null ? "" : note.getValue().strip());

        submitting = true;
        submitButton.setEnabled(false);
        try {
            TripDto saved = tripService.create(user.getId(), dto);
            showSuccess(saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            showAlert(e.getMessage());
            resetSubmitState();
        } catch (RuntimeException e) {
            log.error("SAFAR YARATISHDA XATO:", e);

            showAlert(
                    "Xatolik: " +
                            (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );

            Notification.show(
                    "Xato: " + e.getClass().getSimpleName(),
                    7000,
                    Notification.Position.MIDDLE
            );

            resetSubmitState();
        }
    }

    private void showSuccess(TripDto saved) {
        successRoute.setText(saved.fromRegion().getLabel() + " → " + saved.toRegion().getLabel());
        successDeparture.setText(DATE_FORMAT.format(saved.departureAt()));
        successSize.setText(sizeLabel(saved.maxPackageSize()));

        layoutBox.setVisible(false);
        successPanel.setVisible(true);
        Notification.show("Safar e'loni joylandi", 4000, Notification.Position.TOP_CENTER);
    }

    private void buildSuccessPanel() {
        successPanel.addClassName("cd-success");
        successPanel.setVisible(false);
        successPanel.getElement().setAttribute("role", "status");

        successTitle.addClassName("cd-success-title");

        Div list = new Div(
                row("Yo'nalish", successRoute),
                row("Jo'nash", successDeparture),
                row("Sig'im", successSize));
        list.addClassName("cd-summary-list");

        NativeButton again = new NativeButton("Yana e'lon qilish");
        again.addClassName("cd-submit");
        again.getElement().setAttribute("type", "button");
        again.addClickListener(e -> resetForm());

        NativeButton toList = new NativeButton("Safarlarni ko'rish");
        toList.addClassNames("cd-btn", "cd-btn--ghost");
        toList.getElement().setAttribute("type", "button");
        toList.addClickListener(e -> UI.getCurrent().navigate("trips"));

        Div actions = new Div(again, toList);
        actions.addClassName("cd-success-actions");

        successPanel.add(successTitle, list, actions);
    }

    private void resetForm() {
        fromRegion.clear();
        toRegion.clear();
        departDate.clear();
        departTime.clear();
        note.clear();
        selectSize(null);
        clearErrors();
        resetSubmitState();
        updateSummary();
        successPanel.setVisible(false);
        layoutBox.setVisible(true);
        fromRegion.focus();
    }

    private void resetSubmitState() {
        submitting = false;
        submitButton.setEnabled(true);
    }

    // ================================================================ yordamchilar

    private LocalDateTime combineDeparture() {
        if (departDate.getValue() == null || departTime.getValue() == null) {
            return null;
        }
        return LocalDateTime.of(departDate.getValue(), departTime.getValue());
    }

    private void selectSize(PackageSize size) {
        selectedSize = size;
        sizeButtons.forEach((k, b) ->
                b.getElement().setAttribute("aria-checked", String.valueOf(k == size)));
        sizeError.setVisible(false);
        updateSummary();
    }

    private void markInvalid(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
    }

    private void clearErrors() {
        fromRegion.setInvalid(false);
        toRegion.setInvalid(false);
        departDate.setInvalid(false);
        departTime.setInvalid(false);
        sizeError.setVisible(false);
        formAlert.setVisible(false);
    }

    private void showAlert(String message) {
        formAlert.setText(message);
        formAlert.setVisible(true);
    }

    private void configureRegion(Select<Region> select, String label) {
        select.setLabel(label);
        select.setItems(Region.values());
        select.setItemLabelGenerator(r -> r == null ? "" : r.getLabel());
        select.setPlaceholder("Tanlang");
        select.setRequiredIndicatorVisible(true);
        select.setWidthFull();
    }

    private Div section(String number, String name) {
        Span num = new Span(number);
        num.addClassName("cd-section-num");
        H2 h = new H2(name);
        h.addClassName("cd-section-title");
        Div head = new Div(num, h);
        head.addClassName("cd-section-head");
        Div section = new Div(head);
        section.addClassName("cd-section");
        return section;
    }

    private static String sizeLabel(PackageSize size) {
        return switch (size) {
            case DOCUMENT -> "Hujjat";
            case SMALL -> "Kichik";
            case MEDIUM -> "O'rta";
            case LARGE -> "Katta";
        };
    }

    private static String sizeHint(PackageSize size) {
        return switch (size) {
            case DOCUMENT -> "Konvert, qog'oz";
            case SMALL -> "Qo'lga sig'adigan";
            case MEDIUM -> "Sumka yoki quti";
            case LARGE -> "Katta quti, yirik buyum";
        };
    }

    private static final long MAX_DAYS = 7;
    private static final int MAX_NOTE = 300;
}