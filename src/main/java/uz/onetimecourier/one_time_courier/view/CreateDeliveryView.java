package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uz.onetimecourier.one_time_courier.config.CustomUserDetails;
import uz.onetimecourier.one_time_courier.dto.CreateDeliveryRequestDto;
import uz.onetimecourier.one_time_courier.dto.DeliveryDto;
import uz.onetimecourier.one_time_courier.dto.SenderContactInfoDto;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.AuthService;
import uz.onetimecourier.one_time_courier.service.DeliveryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Yuk joylash (/deliveries/new) va tahrirlash (/deliveries/edit/:id) formasi.
 * Tahrirlash rejimida yuk oldindan to'ldiriladi va faqat CREATED holatida ochiladi.
 * Hisobida telefon yo'q (OAuth) jo'natuvchidan aloqa ma'lumoti so'raladi.
 */
@Route(value = "deliveries/new", layout = PageLayout.class)
@RouteAlias(value = "deliveries/edit/:id", layout = PageLayout.class)
@RolesAllowed("SENDER")
@CssImport("./styles/create-delivery.css")
public class CreateDeliveryView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private static final Logger log = LoggerFactory.getLogger(CreateDeliveryView.class);

    private static final String GENERIC_ERROR =
            "Kutilmagan xato yuz berdi. Birozdan keyin qayta urinib ko'ring.";

    private final DeliveryService deliveryService;
    private final AuthenticationContext authContext;
    private final Validator validator;

    /** true bo'lsa, hisobida telefon yo'q va "Aloqa" bo'limi ko'rsatiladi. */
    private final boolean contactRequired;

    private final H1 title = new H1("Yuk joylash");
    private final Paragraph lead = new Paragraph("Yo'nalish, yuk va qabul qiluvchi ma'lumotlarini kiriting. "
            + "Yulduzcha (*) bilan belgilangan maydonlar majburiy.");

    private final Select<Region> fromRegion = new Select<>();
    private final TextField pickupAddress = new TextField("Olib ketish manzili");
    private final Select<Region> toRegion = new Select<>();
    private final TextField dropoffAddress = new TextField("Yetkazish manzili");
    private final TextField weightField = new TextField("Og'irligi");
    private final TextArea descriptionField = new TextArea("Tavsif (ixtiyoriy)");
    private final TextField recipientName = new TextField("Qabul qiluvchi ismi");
    private final TextField recipientPhone = new TextField("Qabul qiluvchi telefoni");
    private final TextField rewardField = new TextField("Mukofot");
    private final TextField contactPhone = new TextField("Telefon raqamingiz");
    private final TextField contactTelegram = new TextField("Telegram username");
    private final Checkbox forbiddenConfirm = new Checkbox();
    private final Span confirmHint = new Span("Yuborish tugmasi faollashishi uchun yuqoridagi belgini qo'ying");

    private final LocationBox pickupBox =
            new LocationBox("Olib ketish nuqtasini belgilang", this::updateSummary);
    private final LocationBox dropoffBox =
            new LocationBox("Yetkazish nuqtasini belgilang", this::updateSummary);

    private final Map<PackageSize, NativeButton> sizeButtons = new EnumMap<>(PackageSize.class);
    private final Div sizeError = new Div();
    private PackageSize selectedSize;

    private final Div formAlert = new Div();
    private final NativeButton submitButton = new NativeButton("Yukni joylash");

    private final Span summaryKind = new Span();
    private final Span summaryRoute = new Span();
    private final Span summarySize = new Span();
    private final Span summaryWeight = new Span();
    private final Span summaryPickupPoint = new Span();
    private final Span summaryDropoffPoint = new Span();
    private final Span summaryReward = new Span();

    private final Div layoutBox = new Div();

    private final Div successPanel = new Div();
    private final H2 successTitle = new H2();
    private final Paragraph successText = new Paragraph();
    private final Span successRoute = new Span();
    private final Span successReward = new Span();
    private final Span successNumber = new Span();
    private final NativeButton successAgain = new NativeButton("Yana yuk joylash");

    private final Div blockedPanel = new Div();
    private final H2 blockedTitle = new H2();
    private final Paragraph blockedText = new Paragraph();

    private final Map<String, FieldRef> fields = new LinkedHashMap<>();
    private boolean submitting;

    /** null bo'lsa, yangi yuk yaratilmoqda; aks holda shu ID'li yuk tahrirlanmoqda. */
    private Long editingId;

    private record FieldRef(Consumer<String> show, Runnable clear, Runnable focus) {
    }

    public CreateDeliveryView(DeliveryService deliveryService,
                              AuthenticationContext authContext,
                              Validator validator) {
        this.deliveryService = deliveryService;
        this.authContext = authContext;
        this.validator = validator;

        SenderContactInfoDto contactInfo = loadContactInfo();
        this.contactRequired = contactInfo.required();

        addClassName("cd-page");

        title.addClassName("cd-title");
        lead.addClassName("cd-lead");

        layoutBox.addClassName("cd-layout");
        layoutBox.add(buildForm(), buildSummary());

        if (contactRequired) {
            contactPhone.setValue(contactInfo.phone() == null ? "" : contactInfo.phone());
            contactTelegram.setValue(contactInfo.telegram() == null ? "" : contactInfo.telegram());
        }

        buildSuccessPanel();
        buildBlockedPanel();

        add(title, lead, layoutBox, successPanel, blockedPanel);
        updateSummary();
        updateSubmitEnabled();
    }

    /** Aloqa ma'lumoti so'ralishi kerakmi? Xato bo'lsa, bo'lim ko'rsatiladi (server baribir qoidani tekshiradi). */
    private SenderContactInfoDto loadContactInfo() {
        CustomUserDetails user = authContext.getAuthenticatedUser(CustomUserDetails.class).orElse(null);
        if (user == null) {
            return new SenderContactInfoDto(true, null, null);
        }
        try {
            return deliveryService.contactInfo(user.getId());
        } catch (RuntimeException e) {
            log.warn("Kontakt ma'lumotini olib bo'lmadi: {}", e.getClass().getSimpleName());
            return new SenderContactInfoDto(true, null, null);
        }
    }

    // ================================================================ yo'nalish va sarlavha

    @Override
    public String getPageTitle() {
        return editingId == null ? "Yuk joylash | YukBor" : "Yukni tahrirlash | YukBor";
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String raw = event.getRouteParameters().get("id").orElse(null);
        if (raw == null) {
            return; // yangi yuk rejimi
        }

        Long id;
        try {
            id = Long.valueOf(raw);
        } catch (NumberFormatException e) {
            showBlocked("Yuk topilmadi", "Bunday yuk mavjud emas yoki u sizga tegishli emas.");
            return;
        }

        CustomUserDetails user = authContext.getAuthenticatedUser(CustomUserDetails.class).orElse(null);
        if (user == null) {
            showBlocked("Sessiya tugagan", "Iltimos, qaytadan kiring.");
            return;
        }

        try {
            DeliveryDto delivery = deliveryService.getMine(user.getId(), id);
            if (delivery.status() != DeliveryStatus.CREATED) {
                showBlocked("Tahrirlab bo'lmaydi",
                        "Faqat «Kuryer qidirilmoqda» holatidagi yukni tahrirlash mumkin. "
                                + "Kerak bo'lsa, yukni bekor qilib, qaytadan joylang.");
                return;
            }
            editingId = id;
            prefill(delivery);
            title.setText("Yukni tahrirlash");
            lead.setText("O'zgartirishlar faqat kuryer topilgunga qadar mumkin. "
                    + "Yulduzcha (*) bilan belgilangan maydonlar majburiy.");
            submitButton.setText("O'zgarishlarni saqlash");
        } catch (BusinessException e) {
            showBlocked("Yuk topilmadi", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Yukni tahrirlash uchun yuklashda xato", e);
            showBlocked("Xato yuz berdi", GENERIC_ERROR);
        }
    }

    private void prefill(DeliveryDto d) {
        fromRegion.setValue(d.fromRegion());
        toRegion.setValue(d.toRegion());
        pickupAddress.setValue(nullToEmpty(d.pickupAddress()));
        dropoffAddress.setValue(nullToEmpty(d.dropoffAddress()));
        pickupBox.setPoint(new LocationBox.Point(d.pickupLat(), d.pickupLng()));
        dropoffBox.setPoint(new LocationBox.Point(d.dropoffLat(), d.dropoffLng()));
        selectSize(d.packageSize());
        weightField.setValue(d.weightKg() == null ? "" : d.weightKg().stripTrailingZeros().toPlainString());
        descriptionField.setValue(nullToEmpty(d.description()));
        recipientName.setValue(nullToEmpty(d.recipientName()));
        recipientPhone.setValue(nullToEmpty(d.recipientPhone()));
        rewardField.setValue(d.reward() == null ? "" : formatNumber(d.reward()));
        if (contactRequired) {
            contactPhone.setValue(nullToEmpty(d.senderContactPhone()));
            contactTelegram.setValue(nullToEmpty(d.senderContactTelegram()));
        }
        updateSummary();
    }

    // ================================================================ forma

    private Div buildForm() {
        Div form = new Div();
        form.addClassName("cd-form");
        form.add(routeSection(), packageSection(), recipientSection(), rewardSection());
        if (contactRequired) {
            form.add(contactSection());
        }
        form.add(actionsBlock());
        return form;
    }

    private Div routeSection() {
        Div section = section("1", "Yo'nalish");

        configureRegion(fromRegion, "Qayerdan (viloyat)");
        configureText(pickupAddress, "Ko'cha, uy, mo'ljal", 300);
        pickupAddress.setHelperText("Kuryer yukni aynan shu manzildan oladi");

        configureRegion(toRegion, "Qayerga (viloyat)");
        configureText(dropoffAddress, "Ko'cha, uy, mo'ljal", 300);
        dropoffAddress.setHelperText("Yuk shu manzilga yetkaziladi");

        register("fromRegion", fromRegion, fromRegion::focus);
        register("pickupAddress", pickupAddress, pickupAddress::focus);
        register("toRegion", toRegion, toRegion::focus);
        register("dropoffAddress", dropoffAddress, dropoffAddress::focus);

        Div from = column("Qayerdan", fromRegion, pickupAddress, pickupBox);
        Div to = column("Qayerga", toRegion, dropoffAddress, dropoffBox);

        Div row = new Div(from, to);
        row.addClassNames("cd-row", "cd-row--2");

        NativeButton swap = new NativeButton("⇄ Manzillarni almashtirish");
        swap.addClassNames("cd-btn", "cd-btn--link");
        swap.getElement().setAttribute("type", "button");
        swap.addClickListener(e -> swapRoute());
        Div swapBox = new Div(swap);
        swapBox.addClassName("cd-swap");

        section.add(row, swapBox);

        fromRegion.addValueChangeListener(e -> updateSummary());
        toRegion.addValueChangeListener(e -> updateSummary());
        return section;
    }

    private Div packageSection() {
        Div section = section("2", "Yuk");

        Span label = new Span("Yuk turi *");
        label.addClassName("cd-label");
        label.setId("cd-size-label");

        Div group = new Div();
        group.addClassName("cd-size-group");
        group.getElement().setAttribute("role", "radiogroup");
        group.getElement().setAttribute("aria-labelledby", "cd-size-label");

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

        register("packageSize", new FieldRef(
                msg -> {
                    sizeError.setText(msg);
                    sizeError.setVisible(true);
                },
                () -> sizeError.setVisible(false),
                () -> sizeButtons.values().stream().findFirst()
                        .ifPresent(b -> b.getElement().callJsFunction("focus"))));

        configureText(weightField, "Masalan: 2.5", 6);
        weightField.setAllowedCharPattern("[0-9.,]");
        weightField.setHelperText("0.1 dan 50 kg gacha");
        weightField.setSuffixComponent(new Span("kg"));
        weightField.setRequiredIndicatorVisible(true);
        weightField.setValueChangeMode(ValueChangeMode.LAZY);
        weightField.addValueChangeListener(e -> updateSummary());
        register("weightKg", weightField, weightField::focus);

        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
        descriptionField.setValueChangeMode(ValueChangeMode.EAGER);
        descriptionField.setMinHeight("96px");
        descriptionField.setHelperText("Kuryerga muhim ma'lumot: mo'rt, nam bo'lmasin va hokazo");
        register("description", descriptionField, descriptionField::focus);

        Div weightBox = new Div(weightField);
        weightBox.addClassName("cd-weight");

        section.add(label, group, sizeError, weightBox, descriptionField);
        return section;
    }

    private Div recipientSection() {
        Div section = section("3", "Qabul qiluvchi");

        configureText(recipientName, "Ism familiya", 120);
        recipientName.setRequiredIndicatorVisible(true);
        configureText(recipientPhone, "+998 90 123 45 67", 20);
        recipientPhone.setRequiredIndicatorVisible(true);
        recipientPhone.setAllowedCharPattern("[0-9+ ()\\-]");
        recipientPhone.setHelperText("Yetkazish vaqtida kuryer shu raqamga qo'ng'iroq qiladi");

        register("recipientName", recipientName, recipientName::focus);
        register("recipientPhone", recipientPhone, recipientPhone::focus);

        Div row = new Div(recipientName, recipientPhone);
        row.addClassNames("cd-row", "cd-row--2");
        section.add(row);
        return section;
    }

    private Div rewardSection() {
        Div section = section("4", "Mukofot");

        configureText(rewardField, "Masalan: 50 000", 14);
        rewardField.setAllowedCharPattern("[0-9 ]");
        rewardField.setSuffixComponent(new Span("so'm"));
        rewardField.setRequiredIndicatorVisible(true);
        rewardField.setHelperText("1 000 dan 100 000 000 so'mgacha. Kuryerga yetkazib berganingiz uchun to'lanadi");
        rewardField.setValueChangeMode(ValueChangeMode.LAZY);
        rewardField.addValueChangeListener(e -> updateSummary());
        register("reward", rewardField, rewardField::focus);

        Span chipsLabel = new Span("Tezkor tanlash:");
        chipsLabel.addClassName("cd-chips-label");
        Div chips = new Div(chipsLabel);
        chips.addClassName("cd-chips");
        for (String amount : new String[]{"20 000", "50 000", "100 000"}) {
            NativeButton chip = new NativeButton(amount);
            chip.addClassName("cd-chip");
            chip.getElement().setAttribute("type", "button");
            chip.addClickListener(e -> rewardField.setValue(amount));
            chips.add(chip);
        }

        Div box = new Div(rewardField);
        box.addClassName("cd-reward");
        section.add(box, chips);
        return section;
    }

    /** Faqat hisobida telefon yo'q jo'natuvchilar uchun. */
    private Div contactSection() {
        Div section = section("5", "Aloqa");

        Paragraph info = new Paragraph("Hisobingizda telefon raqami yo'q. Kuryer siz bilan bog'lanishi uchun "
                + "telefon yoki Telegram username'dan kamida bittasini kiriting. Ma'lumot yukni qabul qilgan "
                + "kuryerga ko'rinadi va hozircha «tasdiqlanmagan» deb belgilanadi.");
        info.addClassName("cd-contact-info");

        configureText(contactPhone, "+998 90 123 45 67", 20);
        contactPhone.setAllowedCharPattern("[0-9+ ()\\-]");

        configureText(contactTelegram, "username", 33);
        contactTelegram.setAllowedCharPattern("[A-Za-z0-9_@]");
        contactTelegram.setPrefixComponent(new Span("@"));
        contactTelegram.setHelperText("5–32 belgi: lotin harflari, raqam va _");

        register("contactPhone", contactPhone, contactPhone::focus);
        register("contactTelegram", contactTelegram, contactTelegram::focus);

        Div row = new Div(contactPhone, contactTelegram);
        row.addClassNames("cd-row", "cd-row--2");
        section.add(info, row);
        return section;
    }

    private Div actionsBlock() {
        Div actions = new Div();
        actions.addClassName("cd-actions");

        if (!contactRequired) {
            Paragraph note = new Paragraph("Hisobingizdagi telefon raqami yukni qabul qilgan kuryerga ko'rinadi.");
            note.addClassName("cd-contact-note");
            actions.add(note);
        }

        forbiddenConfirm.setLabel("Taqiqlangan buyumlar (qurol, giyohvand moddalar, portlovchi va yonuvchi "
                + "moddalar, jonli hayvonlar va hokazo) yuborilmasligini tasdiqlayman");
        forbiddenConfirm.addValueChangeListener(e -> updateSubmitEnabled());
        confirmHint.addClassName("cd-confirm-hint");
        Div confirm = new Div(forbiddenConfirm, confirmHint);
        confirm.addClassName("cd-confirm");

        formAlert.addClassName("cd-alert");
        formAlert.getElement().setAttribute("role", "alert");
        formAlert.setVisible(false);

        submitButton.addClassName("cd-submit");
        submitButton.getElement().setAttribute("type", "button");
        submitButton.addClickListener(e -> submit());

        actions.add(confirm, formAlert, submitButton);
        return actions;
    }

    // ================================================================ xulosa

    private Div buildSummary() {
        Div box = new Div();
        box.addClassName("cd-summary");

        H2 summaryTitle = new H2("Xulosa");
        summaryTitle.addClassName("cd-summary-title");

        summaryKind.addClassName("cd-summary-kind");
        summaryKind.getElement().setAttribute("aria-live", "polite");

        summaryReward.addClassName("cd-summary-reward");
        Div list = new Div(
                row("Yo'nalish", summaryRoute),
                row("Yuk turi", summarySize),
                row("Og'irlik", summaryWeight),
                row("Olib ketish nuqtasi", summaryPickupPoint),
                row("Yetkazish nuqtasi", summaryDropoffPoint),
                row("Mukofot", summaryReward));
        list.addClassName("cd-summary-list");

        Paragraph note = new Paragraph("Yukingiz holati «Kuryer qidirilmoqda» bo'ladi.");
        note.addClassName("cd-summary-note");

        box.add(summaryTitle, summaryKind, list, note);
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

        if (from == null || to == null) {
            summaryKind.setText("Viloyatlarni tanlang");
        } else {
            summaryKind.setText(from == to ? "Shahar ichida" : "Viloyatlararo");
        }

        summaryRoute.setText(from == null && to == null
                ? "—"
                : (from == null ? "?" : from.getLabel()) + " → " + (to == null ? "?" : to.getLabel()));

        summarySize.setText(selectedSize == null ? "—" : sizeLabel(selectedSize));

        BigDecimal weight = parseWeight(weightField.getValue());
        summaryWeight.setText(weight == null ? "—" : weight.stripTrailingZeros().toPlainString() + " kg");

        summaryPickupPoint.setText(pickupBox.isSet() ? "Belgilangan" : "Belgilanmagan");
        summaryDropoffPoint.setText(dropoffBox.isSet() ? "Belgilangan" : "Belgilanmagan");

        BigDecimal reward = parseReward(rewardField.getValue());
        summaryReward.setText(reward == null ? "—" : formatMoney(reward));
    }

    // ================================================================ yuborish

    private void submit() {
        if (submitting || !forbiddenConfirm.getValue()) {
            return;
        }
        clearErrors();

        Map<String, String> errors = new LinkedHashMap<>();

        BigDecimal weight = parseWeight(weightField.getValue());
        if (weight == null) {
            errors.put("weightKg", weightField.getValue().isBlank()
                    ? "Og'irlikni kiriting"
                    : "Og'irlikni raqam bilan kiriting, masalan 2.5 (ko'pi bilan 2 xona)");
        }
        BigDecimal reward = parseReward(rewardField.getValue());
        if (reward == null) {
            errors.put("reward", rewardField.getValue().isBlank()
                    ? "Mukofotni kiriting"
                    : "Mukofotni butun son bilan kiriting, masalan 50 000");
        }

        String cPhone = null;
        String cTelegram = null;
        if (contactRequired) {
            String rawPhone = contactPhone.getValue().strip();
            cPhone = rawPhone.isEmpty() ? null : normalizePhoneForForm(rawPhone);
            String rawTelegram = contactTelegram.getValue().strip();
            if (rawTelegram.startsWith("@")) {
                rawTelegram = rawTelegram.substring(1);
            }
            cTelegram = rawTelegram.isEmpty() ? null : rawTelegram;
            if (cPhone == null && cTelegram == null) {
                errors.put("contactPhone", "Telefon raqami yoki Telegram username kiriting");
            }
        }

        CreateDeliveryRequestDto dto = new CreateDeliveryRequestDto(
                fromRegion.getValue(),
                toRegion.getValue(),
                pickupAddress.getValue().strip(),
                dropoffAddress.getValue().strip(),
                pickupBox.lat(),
                pickupBox.lng(),
                dropoffBox.lat(),
                dropoffBox.lng(),
                selectedSize,
                weight,
                descriptionField.getValue().strip(),
                recipientName.getValue().strip(),
                normalizePhoneForForm(recipientPhone.getValue()),
                reward,
                cPhone,
                cTelegram);

        List<ConstraintViolation<CreateDeliveryRequestDto>> violations = validator.validate(dto).stream()
                .sorted(Comparator.comparing(ConstraintViolation::getMessage))
                .toList();
        for (ConstraintViolation<CreateDeliveryRequestDto> v : violations) {
            errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
        }

        if (!errors.isEmpty()) {
            showFieldErrors(errors);
            return;
        }

        CustomUserDetails user = authContext.getAuthenticatedUser(CustomUserDetails.class).orElse(null);
        if (user == null) {
            showAlert("Sessiya tugagan. Iltimos, qaytadan kiring.");
            return;
        }

        setSubmitting(true);
        boolean succeeded = false;
        try {
            boolean edit = editingId != null;
            DeliveryDto saved = edit
                    ? deliveryService.update(user.getId(), editingId, dto)
                    : deliveryService.create(user.getId(), dto);
            succeeded = true;
            showSuccess(saved, edit);
        } catch (BusinessException e) {
            showAlert(e.getMessage());
        } catch (ConstraintViolationException e) {
            showAlert("Ma'lumotlarda xato bor. Tekshirib, qayta urinib ko'ring.");
        } catch (RuntimeException e) {
            log.error("Yukni saqlashda kutilmagan xato", e);
            showAlert(GENERIC_ERROR);
        } finally {
            if (!succeeded) {
                setSubmitting(false);
            }
        }
    }

    private void showFieldErrors(Map<String, String> errors) {
        boolean focused = false;
        StringBuilder unknown = new StringBuilder();

        for (Map.Entry<String, String> e : errors.entrySet()) {
            if (!fields.containsKey(e.getKey())) {
                unknown.append(e.getValue()).append(' ');
            }
        }
        for (Map.Entry<String, FieldRef> f : fields.entrySet()) {
            String msg = errors.get(f.getKey());
            if (msg != null) {
                f.getValue().show().accept(msg);
                if (!focused) {
                    f.getValue().focus().run();
                    focused = true;
                }
            }
        }
        showAlert(unknown.isEmpty()
                ? "Iltimos, belgilangan maydonlarni tekshiring."
                : unknown.toString().trim());
    }

    private void showSuccess(DeliveryDto saved, boolean edited) {
        successTitle.setText(edited ? "O'zgarishlar saqlandi" : "Yuk joylandi");
        successText.setText(edited
                ? "Yuk ma'lumotlari yangilandi. Holati: «Kuryer qidirilmoqda»."
                : "Yukingiz e'lon qilindi. Holati: «Kuryer qidirilmoqda».");
        successAgain.setVisible(!edited);

        successNumber.setText(saved.id() == null ? "—" : "#" + saved.id());
        successRoute.setText(saved.fromRegion().getLabel() + " → " + saved.toRegion().getLabel());
        successReward.setText(formatMoney(saved.reward()));

        layoutBox.setVisible(false);
        successPanel.setVisible(true);
        Notification.show(edited ? "O'zgarishlar saqlandi" : "Yuk muvaffaqiyatli joylandi",
                4000, Notification.Position.TOP_CENTER);
    }

    private void buildSuccessPanel() {
        successPanel.addClassName("cd-success");
        successPanel.setVisible(false);
        successPanel.getElement().setAttribute("role", "status");

        successTitle.addClassName("cd-success-title");
        successText.addClassName("cd-lead");

        Div list = new Div(
                row("Yuk raqami", successNumber),
                row("Yo'nalish", successRoute),
                row("Mukofot", successReward));
        list.addClassName("cd-summary-list");

        successAgain.addClassName("cd-submit");
        successAgain.getElement().setAttribute("type", "button");
        successAgain.addClickListener(e -> resetForm());

        NativeButton toMine = new NativeButton("Yuklarimga o'tish");
        toMine.addClassNames("cd-btn", "cd-btn--ghost");
        toMine.getElement().setAttribute("type", "button");
        toMine.addClickListener(e -> UI.getCurrent().navigate("deliveries/mine"));

        Div actions = new Div(successAgain, toMine);
        actions.addClassName("cd-success-actions");

        successPanel.add(successTitle, successText, list, actions);
    }

    private void buildBlockedPanel() {
        blockedPanel.addClassNames("cd-success", "cd-blocked");
        blockedPanel.setVisible(false);
        blockedPanel.getElement().setAttribute("role", "alert");

        blockedTitle.addClassName("cd-success-title");
        blockedText.addClassName("cd-lead");

        NativeButton toMine = new NativeButton("Yuklarimga qaytish");
        toMine.addClassName("cd-submit");
        toMine.getElement().setAttribute("type", "button");
        toMine.addClickListener(e -> UI.getCurrent().navigate("deliveries/mine"));

        blockedPanel.add(blockedTitle, blockedText, toMine);
    }

    private void showBlocked(String heading, String text) {
        blockedTitle.setText(heading);
        blockedText.setText(text);
        title.setVisible(false);
        lead.setVisible(false);
        layoutBox.setVisible(false);
        successPanel.setVisible(false);
        blockedPanel.setVisible(true);
    }

    private void resetForm() {
        fromRegion.clear();
        toRegion.clear();
        pickupAddress.clear();
        dropoffAddress.clear();
        weightField.clear();
        descriptionField.clear();
        recipientName.clear();
        recipientPhone.clear();
        rewardField.clear();
        forbiddenConfirm.setValue(false);
        pickupBox.clear();
        dropoffBox.clear();
        selectSize(null);
        clearErrors();
        submitting = false;
        updateSubmitEnabled();
        updateSummary();
        successPanel.setVisible(false);
        layoutBox.setVisible(true);
        fromRegion.focus();
        // aloqa maydonlari (agar bor bo'lsa) oxirgi kiritilgan qiymatda qoladi
    }

    // ================================================================ yordamchilar

    private void swapRoute() {
        Region a = fromRegion.getValue();
        Region b = toRegion.getValue();
        fromRegion.setValue(b);
        toRegion.setValue(a);

        String addr = pickupAddress.getValue();
        pickupAddress.setValue(dropoffAddress.getValue());
        dropoffAddress.setValue(addr);

        LocationBox.Point p = pickupBox.point();
        pickupBox.setPoint(dropoffBox.point());
        dropoffBox.setPoint(p);

        clearErrors();
        updateSummary();
    }

    private void selectSize(PackageSize size) {
        selectedSize = size;
        sizeButtons.forEach((k, b) ->
                b.getElement().setAttribute("aria-checked", String.valueOf(k == size)));
        sizeError.setVisible(false);
        updateSummary();
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        updateSubmitEnabled();
    }

    private void updateSubmitEnabled() {
        boolean confirmed = forbiddenConfirm.getValue();
        submitButton.setEnabled(confirmed && !submitting);
        confirmHint.setVisible(!confirmed);
    }

    private void clearErrors() {
        fields.values().forEach(f -> f.clear().run());
        formAlert.setVisible(false);
    }

    private void showAlert(String message) {
        formAlert.setText(message);
        formAlert.setVisible(true);
    }

    private void register(String key, HasValidation field, Runnable focus) {
        fields.put(key, new FieldRef(
                msg -> {
                    field.setErrorMessage(msg);
                    field.setInvalid(true);
                },
                () -> field.setInvalid(false),
                focus));
    }

    private void register(String key, FieldRef ref) {
        fields.put(key, ref);
    }

    private void configureRegion(Select<Region> select, String label) {
        select.setLabel(label);
        select.setItems(Region.values());
        select.setItemLabelGenerator(Region::getLabel);
        select.setPlaceholder("Tanlang");
        select.setRequiredIndicatorVisible(true);
        select.setWidthFull();
    }

    private void configureText(TextField field, String placeholder, int maxLength) {
        field.setPlaceholder(placeholder);
        field.setMaxLength(maxLength);
        field.setWidthFull();
        field.setClearButtonVisible(false);
        if (field == pickupAddress || field == dropoffAddress) {
            field.setRequiredIndicatorVisible(true);
        }
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

    private Div column(String heading, Component... children) {
        Span h = new Span(heading);
        h.addClassName("cd-subhead");
        Div col = new Div(h);
        col.addClassName("cd-col");
        col.add(children);
        return col;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String normalizePhoneForForm(String raw) {
        String value = raw == null ? "" : raw.strip();
        if (value.isEmpty()) {
            return value;
        }
        try {
            String normalized = AuthService.normalizePhone(value);
            return normalized == null ? value : normalized;
        } catch (RuntimeException e) {
            return value; // DTO validatsiyasi tushunarli xabar beradi
        }
    }

    private static BigDecimal parseWeight(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.strip().replace(',', '.');
        if (!s.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            return null;
        }
        return new BigDecimal(s);
    }

    private static BigDecimal parseReward(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.replaceAll("\\s", "");
        if (!s.matches("\\d{1,10}")) {
            return null;
        }
        return new BigDecimal(s);
    }

    private static String formatNumber(BigDecimal amount) {
        String plain = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = plain.length() - 1; i >= 0; i--) {
            sb.append(plain.charAt(i));
            if (++count % 3 == 0 && i > 0) {
                sb.append(' ');
            }
        }
        return sb.reverse().toString();
    }

    private static String formatMoney(BigDecimal amount) {
        return formatNumber(amount) + " so'm";
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

    // ================================================================ xaritadan nuqta tanlash

    /**
     * Manzil ostidagi blok: "Xaritadan belgilash" tugmasi xarita oynasini ochadi (Leaflet + OpenStreetMap,
     * brauzerda CDN'dan yuklanadi). Foydalanuvchi matni hech qachon HTML sifatida chiqarilmaydi.
     */
    private static final class LocationBox extends Div {

        record Point(Double lat, Double lng) {
        }

        private static final double DEFAULT_LAT = 41.3111; // Toshkent
        private static final double DEFAULT_LNG = 69.2797;

        private static final String INIT_JS = """
                const el = this;
                const startLat = $0;
                const startLng = $1;
                const hasPoint = $2;
                const bounds = [[$3, $5], [$4, $6]];
                const loadLeaflet = () => new Promise((resolve, reject) => {
                  if (window.L) { resolve(); return; }
                  if (!document.getElementById('cd-leaflet-css')) {
                    const link = document.createElement('link');
                    link.id = 'cd-leaflet-css';
                    link.rel = 'stylesheet';
                    link.href = 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css';
                    document.head.appendChild(link);
                  }
                  let script = document.getElementById('cd-leaflet-js');
                  if (!script) {
                    script = document.createElement('script');
                    script.id = 'cd-leaflet-js';
                    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js';
                    document.head.appendChild(script);
                  }
                  script.addEventListener('load', () => resolve());
                  script.addEventListener('error', () => { script.remove(); reject(new Error('leaflet')); });
                });
                return loadLeaflet().then(() => {
                  const map = L.map(el, {minZoom: 5, maxBounds: bounds, maxBoundsViscosity: 1.0})
                    .setView([startLat, startLng], hasPoint ? 16 : 11);
                  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap'
                  }).addTo(map);
                  el.__map = map;
                  el.__pick = null;
                  el.__marker = null;
                  el.__put = (la, ln) => {
                    el.__pick = {lat: la, lng: ln};
                    if (el.__marker) {
                      el.__marker.setLatLng([la, ln]);
                    } else {
                      el.__marker = L.marker([la, ln], {draggable: true}).addTo(map);
                      el.__marker.on('dragend', () => {
                        const p = el.__marker.getLatLng();
                        el.__pick = {lat: p.lat, lng: p.lng};
                      });
                    }
                  };
                  if (hasPoint) { el.__put(startLat, startLng); }
                  map.on('click', e => el.__put(e.latlng.lat, e.latlng.lng));
                  new ResizeObserver(() => map.invalidateSize()).observe(el);
                  setTimeout(() => map.invalidateSize(), 300);
                  return 'OK';
                }).catch(() => 'ERR');
                """;

        private static final String LOCATE_JS = """
                const el = this;
                if (!el.__map) { return 'ERR:9'; }
                if (!navigator.geolocation) { return 'ERR:0'; }
                return new Promise(resolve => {
                  navigator.geolocation.getCurrentPosition(p => {
                    const la = p.coords.latitude;
                    const ln = p.coords.longitude;
                    if (la < $0 || la > $1 || ln < $2 || ln > $3) { resolve('OUT'); return; }
                    el.__put(la, ln);
                    el.__map.setView([la, ln], 16);
                    resolve('OK');
                  }, e => resolve('ERR:' + e.code),
                  {enableHighAccuracy: true, timeout: 10000, maximumAge: 0});
                });
                """;

        private static final String READ_JS =
                "return this.__pick ? (this.__pick.lat + ';' + this.__pick.lng) : '';";

        private final String pickerTitle;
        private final Runnable onChange;

        private final NativeButton pickButton = new NativeButton();
        private final NativeButton clearButton = new NativeButton("Olib tashlash");
        private final Anchor mapLink = new Anchor();
        private final Span status = new Span();

        private Double lat;
        private Double lng;

        LocationBox(String pickerTitle, Runnable onChange) {
            this.pickerTitle = pickerTitle;
            this.onChange = onChange;
            addClassName("cd-loc");

            pickButton.addClassNames("cd-btn", "cd-btn--ghost");
            pickButton.getElement().setAttribute("type", "button");
            pickButton.addClickListener(e -> openPicker());

            clearButton.addClassNames("cd-btn", "cd-btn--link");
            clearButton.getElement().setAttribute("type", "button");
            clearButton.addClickListener(e -> clear());

            mapLink.setText("Xaritada ko'rish");
            mapLink.setTarget("_blank");
            mapLink.getElement().setAttribute("rel", "noopener noreferrer");
            mapLink.addClassName("cd-maplink");

            status.addClassName("cd-loc-status");
            status.getElement().setAttribute("aria-live", "polite");

            Div actions = new Div(pickButton, mapLink, clearButton);
            actions.addClassName("cd-loc-actions");

            Span help = new Span("Ixtiyoriy. Aniq nuqta kuryerga yo'lni topishda yordam beradi.");
            help.addClassName("cd-loc-help");

            add(actions, status, help);
            refresh();
        }

        boolean isSet() {
            return lat != null && lng != null;
        }

        Double lat() {
            return lat;
        }

        Double lng() {
            return lng;
        }

        Point point() {
            return new Point(lat, lng);
        }

        void setPoint(Point p) {
            lat = p == null ? null : p.lat();
            lng = p == null ? null : p.lng();
            refresh();
            onChange.run();
        }

        void clear() {
            setPoint(null);
        }

        private void refresh() {
            boolean set = isSet();
            pickButton.setText(set ? "Xaritada o'zgartirish" : "Xaritadan belgilash");
            clearButton.setVisible(set);
            mapLink.setVisible(set);
            status.setText(set ? "Nuqta xaritada belgilandi." : "");
            if (set) {
                mapLink.setHref(String.format(Locale.ROOT,
                        "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f",
                        lat, lng, lat, lng));
            }
        }

        private void openPicker() {
            Dialog dialog = new Dialog();
            dialog.setCloseOnOutsideClick(false);
            dialog.addOpenedChangeListener(e -> {
                if (!e.isOpened()) {
                    dialog.removeFromParent();
                }
            });

            Div content = new Div();
            content.addClassName("cd-dialog");

            H2 dialogTitle = new H2(pickerTitle);
            dialogTitle.addClassName("cd-dialog-title");
            Paragraph hint = new Paragraph(
                    "Xaritani bosib nuqtani belgilang. Kerak bo'lsa, markerni sudrab to'g'rilang.");
            hint.addClassName("cd-dialog-hint");

            Div mapDiv = new Div();
            mapDiv.addClassName("cd-map");

            Span dialogStatus = new Span();
            dialogStatus.addClassName("cd-loc-status");
            dialogStatus.getElement().setAttribute("role", "status");
            dialogStatus.getElement().setAttribute("aria-live", "polite");

            NativeButton locate = new NativeButton("Mening joylashuvim");
            locate.addClassNames("cd-btn", "cd-btn--ghost");
            locate.getElement().setAttribute("type", "button");

            NativeButton ok = new NativeButton("Tasdiqlash");
            ok.addClassName("cd-submit");
            ok.getElement().setAttribute("type", "button");

            NativeButton cancel = new NativeButton("Bekor qilish");
            cancel.addClassNames("cd-btn", "cd-btn--link");
            cancel.getElement().setAttribute("type", "button");

            Div actions = new Div(ok, locate, cancel);
            actions.addClassName("cd-dialog-actions");

            content.add(dialogTitle, hint, mapDiv, dialogStatus, actions);
            dialog.add(content);

            locate.addClickListener(e -> {
                dialogStatus.setText("Joylashuv aniqlanmoqda…");
                mapDiv.getElement()
                        .executeJs(LOCATE_JS,
                                DeliveryService.MIN_LAT, DeliveryService.MAX_LAT,
                                DeliveryService.MIN_LNG, DeliveryService.MAX_LNG)
                        .then(String.class,
                                r -> dialogStatus.setText(locateText(r)),
                                err -> dialogStatus.setText(locateText("ERR:0")));
            });

            ok.addClickListener(e -> mapDiv.getElement().executeJs(READ_JS)
                    .then(String.class, s -> confirm(s, dialog, dialogStatus)));

            cancel.addClickListener(e -> dialog.close());

            dialog.open();

            double startLat = isSet() ? lat : DEFAULT_LAT;
            double startLng = isSet() ? lng : DEFAULT_LNG;
            mapDiv.getElement()
                    .executeJs(INIT_JS, startLat, startLng, isSet(),
                            DeliveryService.MIN_LAT, DeliveryService.MAX_LAT,
                            DeliveryService.MIN_LNG, DeliveryService.MAX_LNG)
                    .then(String.class, r -> {
                        if (!"OK".equals(r)) {
                            dialogStatus.setText("Xarita yuklanmadi. Internetni tekshiring yoki "
                                    + "manzilni matn bilan kiriting.");
                        }
                    }, err -> dialogStatus.setText("Xarita yuklanmadi. Internetni tekshiring yoki "
                            + "manzilni matn bilan kiriting."));
        }

        private void confirm(String raw, Dialog dialog, Span dialogStatus) {
            if (raw == null || raw.isBlank()) {
                dialogStatus.setText("Avval xaritada nuqtani bosing.");
                return;
            }
            try {
                String[] parts = raw.split(";");
                double la = Math.round(Double.parseDouble(parts[0]) * 1_000_000d) / 1_000_000d;
                double ln = Math.round(Double.parseDouble(parts[1]) * 1_000_000d) / 1_000_000d;

                boolean inside = la >= DeliveryService.MIN_LAT && la <= DeliveryService.MAX_LAT
                        && ln >= DeliveryService.MIN_LNG && ln <= DeliveryService.MAX_LNG;
                if (!inside) {
                    dialogStatus.setText("Nuqta O'zbekiston hududidan tashqarida. Boshqa joyni tanlang.");
                    return;
                }
                setPoint(new Point(la, ln));
                dialog.close();
            } catch (RuntimeException ex) {
                dialogStatus.setText("Nuqtani o'qib bo'lmadi. Qayta bosing.");
            }
        }

        private static String locateText(String result) {
            if ("OK".equals(result)) {
                return "Joylashuvingiz belgilandi. Kerak bo'lsa, markerni sudrab to'g'rilang.";
            }
            if ("OUT".equals(result)) {
                return "Joylashuvingiz O'zbekiston hududidan tashqarida. Xaritadan tanlang.";
            }
            String code = result != null && result.startsWith("ERR:") ? result.substring(4) : "0";
            return switch (code) {
                case "1" -> "Joylashuvga ruxsat berilmadi. Xaritadan o'zingiz belgilang "
                        + "yoki brauzer sozlamalaridan ruxsat bering.";
                case "2" -> "Joylashuvni aniqlab bo'lmadi. Xaritadan o'zingiz belgilang.";
                case "3" -> "Vaqt tugadi. Qayta urinib ko'ring yoki xaritadan belgilang.";
                case "9" -> "Xarita hali yuklanmadi. Biroz kuting.";
                default -> "Brauzeringiz joylashuvni qo'llamaydi. Xaritadan belgilang.";
            };
        }
    }
}