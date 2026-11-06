package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;

import uz.onetimecourier.one_time_courier.dto.PendingRegistration;
import uz.onetimecourier.one_time_courier.dto.RegisterRequestDto;
import uz.onetimecourier.one_time_courier.dto.VehicleInfo;
import uz.onetimecourier.one_time_courier.enums.Language;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.layout.AuthLayout;
import uz.onetimecourier.one_time_courier.service.AuthService;
import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService;
import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService.Status;
import uz.onetimecourier.one_time_courier.service.VehicleVerificationService;

import java.util.ArrayList;
import java.util.List;

@Route(value = "register/verify", layout = AuthLayout.class)
@PageTitle("Telefon tasdiqlash | YukBor")
@AnonymousAllowed
@CssImport("./styles/telegram-verify.css")
public class TelegramVerifyView extends VerticalLayout implements BeforeEnterObserver {

    private static final int CODE_LENGTH = 6;
    private static final int POLL_MS = 1500;
    private static final int CODE_SECONDS = 300; // kod 5 daqiqa amal qiladi
    private static final String BOT_OPENED_KEY = "yukbor_bot_opened_token";

    private final AuthService authService;
    private final TelegramPhoneVerificationService phoneVerification;
    private final VehicleVerificationService vehicleVerifier;
    private final String botUsername;

    private PendingRegistration pending;
    private boolean verified;
    private boolean fillingBoxes;
    private Registration pollRegistration;

    private final List<TextField> boxes = new ArrayList<>();
    private Span statusText;
    private Span timerText;
    private Span error;
    private VerticalLayout codeBlock;
    private NativeButton openBot;
    private NativeButton confirm;
    private NativeButton vehicleBtn;
    private NativeButton backBtn;

    public TelegramVerifyView(AuthService authService,
                              TelegramPhoneVerificationService phoneVerification,
                              VehicleVerificationService vehicleVerifier,
                              @Value("${telegram.bot.username:}") String botUsername) {
        this.authService = authService;
        this.phoneVerification = phoneVerification;
        this.vehicleVerifier = vehicleVerifier;
        this.botUsername = botUsername;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        pending = (PendingRegistration) VaadinSession.getCurrent().getAttribute(PendingRegistration.class);
        if (pending == null) {
            event.forwardTo(RegisterView.class);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (pending == null) {
            return;
        }
        buildLayout();

        UI ui = attachEvent.getUI();
        autoOpenBot(ui);

        stopPolling();
        ui.setPollInterval(POLL_MS);
        pollRegistration = ui.addPollListener(e -> refresh());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
        UI ui = detachEvent.getUI();
        ui.setPollInterval(-1);
        clearCodeTimer();
        if (!verified && pending != null) {
            phoneVerification.discard(pending.token());
            VaadinSession.getCurrent().setAttribute(PendingRegistration.class, null);
            pending = null;
        }
        super.onDetach(detachEvent);
    }

    // ---------------------------------------------------------------- bot

    private String botUrl() {
        return "https://t.me/" + botUsername + "?start=" + pending.token();
    }

    /** Sahifa ochilganda bot bir marta avtomatik ochiladi. Brauzer bloklasa, tugma ishlaydi. */
    private void autoOpenBot(UI ui) {
        String token = pending.token();
        if (token.equals(VaadinSession.getCurrent().getAttribute(BOT_OPENED_KEY))) {
            return;
        }
        VaadinSession.getCurrent().setAttribute(BOT_OPENED_KEY, token);
        ui.getPage().executeJs("window.open($0, '_blank')", botUrl());
    }

    // ---------------------------------------------------------------- qurilish

    private void buildLayout() {
        removeAll();
        boxes.clear();

        // Sahifa to'liq ekran: uslubi telefon-tasdiqlash CSS da
        addClassName("vt-page");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout card = new VerticalLayout();
        card.addClassName("vt-card");
        card.setAlignItems(Alignment.CENTER);
        card.setSpacing(true);
        card.setPadding(false);

        // Telegram belgisi: ko'k doira ichida samolyotcha
        Div telegramIcon = new Div();
        telegramIcon.addClassName("vt-icon");
        telegramIcon.getElement().setProperty("innerHTML",
                "<div class='vt-tg'>"
                        + "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='currentColor' aria-hidden='true'>"
                        + "<path d='M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 "
                        + "12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023"
                        + ".465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 "
                        + "6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902"
                        + "-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 "
                        + "3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c"
                        + "-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252"
                        + "-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663"
                        + " 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z'/>"
                        + "</svg></div>");

        H2 title = new H2("Telefon raqamni tasdiqlang");
        title.addClassName("vt-title");

        Span phoneSpan = new Span(pending.phone());
        phoneSpan.addClassName("vt-phone");

        Span subtitle = new Span("Telegram botda raqamingizni yuboring. Botdan kelgan 6 xonali kodni shu yerga kiriting.");
        subtitle.addClassName("vt-sub");

        statusText = new Span("Telegram botda raqamingizni yuboring.");
        statusText.addClassName("vt-status");

        openBot = new NativeButton("Telegram botni ochish");
        openBot.addClassNames("vt-btn", "vt-btn--tg");
        openBot.getElement().setAttribute("type", "button");
        openBot.addClickListener(e -> UI.getCurrent().getPage().open(botUrl(), "_blank"));

        HorizontalLayout otpLayout = new HorizontalLayout();
        otpLayout.addClassName("vt-otp-row");
        otpLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        otpLayout.setSpacing(true);
        for (int i = 0; i < CODE_LENGTH; i++) {
            TextField box = createOtpField(i);
            boxes.add(box);
            otpLayout.add(box);
        }

        timerText = new Span();
        timerText.addClassName("vt-timer");

        confirm = buildPrimaryButton("Tasdiqlash");
        confirm.addClickListener(e -> submitCode());

        codeBlock = new VerticalLayout(otpLayout, timerText, confirm);
        codeBlock.addClassName("vt-code");
        codeBlock.setPadding(false);
        codeBlock.setSpacing(true);
        codeBlock.setAlignItems(Alignment.CENTER);
        codeBlock.setWidthFull();
        codeBlock.setVisible(false);

        error = new Span();
        error.addClassName("vt-error");
        error.setVisible(false);

        vehicleBtn = buildPrimaryButton("Transport ma'lumotlarini kiriting");
        vehicleBtn.setVisible(false);
        vehicleBtn.addClickListener(e -> openVehicleDialog());

        backBtn = new NativeButton("Qaytadan urinish", e -> UI.getCurrent().navigate(RegisterView.class));
        backBtn.addClassName("vt-back");
        backBtn.getElement().setAttribute("type", "button");
        backBtn.setVisible(false);

        card.add(telegramIcon, title, phoneSpan, subtitle, statusText, openBot,
                codeBlock, error, vehicleBtn, backBtn);
        add(card);
    }

    private TextField createOtpField(int index) {
        TextField tf = new TextField();
        tf.addClassName("vt-otp");
        tf.setAllowedCharPattern("[0-9]");
        tf.setValueChangeMode(ValueChangeMode.EAGER);
        tf.getElement().setAttribute("inputmode", "numeric");
        tf.getElement().setAttribute("autocomplete", "one-time-code");

        tf.addValueChangeListener(e -> onBoxChanged(index, e.getValue()));

        tf.getElement()
                .addEventListener("keydown", event -> {
                    String key = event.getEventData().get("event.key").asText();
                    if ("Backspace".equals(key) && tf.getValue().isEmpty() && index > 0) {
                        boxes.get(index - 1).focus();
                    }
                })
                .addEventData("event.key");
        return tf;
    }

    /** Bitta raqam yoki paste qilingan bir nechta raqamni kataklarga taqsimlaydi. */
    private void onBoxChanged(int index, String raw) {
        if (fillingBoxes) {
            return;
        }
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return;
        }
        int next = index + 1;
        if (digits.length() > 1) {
            fillingBoxes = true;
            try {
                for (int i = 0; i < digits.length() && index + i < CODE_LENGTH; i++) {
                    boxes.get(index + i).setValue(String.valueOf(digits.charAt(i)));
                }
            } finally {
                fillingBoxes = false;
            }
            next = Math.min(index + digits.length(), CODE_LENGTH - 1);
        }
        if (next < CODE_LENGTH) {
            boxes.get(next).focus();
        }
    }

    private String code() {
        StringBuilder sb = new StringBuilder();
        for (TextField box : boxes) {
            sb.append(box.getValue());
        }
        return sb.toString();
    }

    private void clearBoxes() {
        boxes.forEach(b -> b.setValue(""));
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
    }

    private NativeButton buildPrimaryButton(String text) {
        NativeButton btn = new NativeButton(text);
        btn.addClassNames("vt-btn", "vt-btn--solid");
        btn.getElement().setAttribute("type", "button");
        return btn;
    }

    // ---------------------------------------------------------------- polling va taymer

    private void refresh() {
        if (pending == null || verified) {
            return;
        }
        Status s = phoneVerification.poll(pending.token());
        if (s == Status.WAITING_CODE && !codeBlock.isVisible()) {
            statusText.setText("Botdan kelgan 6 xonali kodni kiriting.");
            codeBlock.setVisible(true);
            startCodeTimer(CODE_SECONDS);
            boxes.get(0).focus();
        } else if (s == Status.EXPIRED) {
            stopPolling();
            clearCodeTimer();
            statusText.setText("Vaqt tugadi. Qaytadan urinib ko'ring.");
            codeBlock.setVisible(false);
            openBot.setVisible(false);
            backBtn.setVisible(true);
        }
    }

    private void stopPolling() {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
    }

    private void startCodeTimer(int seconds) {
        timerText.getElement().executeJs(
                "var sec = $0; var el = this; var expired = $1; var label = $2;" +
                        "if (window._yukborCodeTimer) clearInterval(window._yukborCodeTimer);" +
                        "function paint() {" +
                        "  var m = Math.floor(sec / 60), s = sec % 60;" +
                        "  el.textContent = label + ': ' + (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s);" +
                        "  el.style.color = sec <= 30 ? '#ff7a6b' : '#a9a59b';" +
                        "}" +
                        "paint();" +
                        "window._yukborCodeTimer = setInterval(function() {" +
                        "  sec--;" +
                        "  if (sec <= 0) {" +
                        "    clearInterval(window._yukborCodeTimer);" +
                        "    el.textContent = expired;" +
                        "    el.style.color = '#ff7a6b';" +
                        "    return;" +
                        "  }" +
                        "  paint();" +
                        "}, 1000);",
                seconds, "Kod muddati tugadi", "Kod amal qilish muddati");
    }

    private void clearCodeTimer() {
        if (timerText != null) {
            timerText.getElement().executeJs(
                    "if (window._yukborCodeTimer) { clearInterval(window._yukborCodeTimer); window._yukborCodeTimer = null; }");
        }
    }

    // ---------------------------------------------------------------- tasdiqlash

    private void submitCode() {
        error.setVisible(false);
        String code = code();
        if (code.length() != CODE_LENGTH) {
            showError("6 xonali kodni to'liq kiriting");
            return;
        }
        if (!phoneVerification.checkCode(pending.token(), code)) {
            showError("Kod noto'g'ri yoki muddati o'tgan");
            clearBoxes();
            boxes.get(0).focus();
            return;
        }
        onVerified();
    }

    private void onVerified() {
        verified = true;
        stopPolling();
        clearCodeTimer();
        codeBlock.setVisible(false);
        openBot.setVisible(false);
        statusText.setText("Telefon raqam tasdiqlandi.");

        if (pending.role() == Role.COURIER) {
            vehicleBtn.setVisible(true);
            openVehicleDialog();
        } else {
            finish(null);
        }
    }

    private void openVehicleDialog() {
        new VehicleVerificationDialog(vehicleVerifier, this::finish).open();
    }

    private void finish(VehicleInfo vehicle) {
        try {
            Long telegramId = phoneVerification.telegramIdOf(pending.token()); // discard dan oldin
            authService.registerVerified(new RegisterRequestDto(
                    pending.phone(),
                    pending.fullName(),
                    pending.password(),
                    pending.role(),
                    Language.UZ), vehicle, telegramId);
        } catch (BusinessException ex) {
            showError(ex.getMessage());
            return;
        } catch (ConstraintViolationException ex) {
            showError("Ma'lumotlarni tekshirib, qayta urinib ko'ring");
            return;
        }

        phoneVerification.discard(pending.token());
        VaadinSession.getCurrent().setAttribute(PendingRegistration.class, null);
        pending = null;

        Notification.show("Muvaffaqiyatli ro'yxatdan o'tdingiz!", 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate(LoginView.class);
    }
}