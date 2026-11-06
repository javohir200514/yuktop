package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import uz.onetimecourier.one_time_courier.dto.PendingRegistration;
import uz.onetimecourier.one_time_courier.enums.Role;
import uz.onetimecourier.one_time_courier.layout.AuthLayout;
import uz.onetimecourier.one_time_courier.service.AuthService;
import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService;

@Route(value = "register", layout = AuthLayout.class)
@PageTitle("Ro'yxatdan o'tish | YukBor")
@AnonymousAllowed
@CssImport("./styles/login-view.css")
public class RegisterView extends Div implements BeforeEnterObserver {

    private static final String SUBMIT_LABEL = "Ro'yxatdan o'tish";

    private final AuthService authService;
    private final TelegramPhoneVerificationService phoneVerification;
    private final String botUsername;

    private final TextField fullName = new TextField("Ism familiya");
    private final TextField phone = new TextField("Telefon raqam");
    private final PasswordField password = new PasswordField("Parol");
    private final PasswordField confirm = new PasswordField("Parolni takrorlang");
    private final Select<Role> role = new Select<>();

    private final NativeButton submit = new NativeButton();
    private final Span submitLabel = new Span(SUBMIT_LABEL);
    private final Icon submitIcon = VaadinIcon.ARROW_RIGHT.create();

    public RegisterView(AuthService authService,
                        TelegramPhoneVerificationService phoneVerification,
                        @Value("${telegram.bot.username:}") String botUsername) {
        this.authService = authService;
        this.phoneVerification = phoneVerification;
        this.botUsername = botUsername;
        addClassNames("login-page", "register-page");

        VerticalLayout card = new VerticalLayout();
        card.addClassNames("login-card", "register-card");
        card.setPadding(false);
        card.setSpacing(false);
        card.setWidthFull();
        card.setAlignItems(VerticalLayout.Alignment.STRETCH);

        // ---- Brend
        Span yuk = new Span("YUK");
        yuk.addClassName("brand-yuk");
        Span bor = new Span("BOR");
        bor.addClassName("brand-bor");
        Div brandName = new Div(yuk, bor);
        brandName.addClassName("brand-name");
        Span tagline = new Span("Yukingiz manziliga ishonch bilan");
        tagline.addClassName("brand-tagline");
        Div brand = new Div(brandName, tagline);
        brand.addClassName("yukbor-brand");

        // ---- Sarlavha
        H1 title = new H1();
        title.addClassName("login-title");
        Span plain = new Span("Hisob ");
        Span accent = new Span("yarating");
        accent.addClassName("login-title-highlight");
        title.add(plain, accent);

        Paragraph description = new Paragraph("Bir necha soniyada ro'yxatdan o'ting.");
        description.addClassName("login-description");

        // ---- Bosqichlar: hozir 1-bosqich
        Div steps = new Div(
                registerStep("01", "Ma'lumot", true),
                registerStep("02", "Telegram", false),
                registerStep("03", "Tasdiq", false)
        );
        steps.addClassName("register-steps");

        // ---- Maydonlar
        fullName.setPlaceholder("Aliyev Vali");
        fullName.setPrefixComponent(VaadinIcon.USER.create());
        fullName.setMaxLength(120);

        phone.setValue("+998");
        phone.setPlaceholder("+998901234567");
        phone.setPrefixComponent(VaadinIcon.PHONE.create());
        phone.setAllowedCharPattern("[0-9+]");
        phone.setMaxLength(13);

        password.setPlaceholder("Kamida 8 ta belgi");
        password.setPrefixComponent(VaadinIcon.LOCK.create());
        password.setRevealButtonVisible(true);
        password.setMaxLength(72);

        confirm.setPlaceholder("Parolni qayta kiriting");
        confirm.setPrefixComponent(VaadinIcon.LOCK.create());
        confirm.setRevealButtonVisible(true);
        confirm.setMaxLength(72);

        role.setLabel("Men kimman?");
        role.setItems(Role.SENDER, Role.COURIER);
        role.setItemLabelGenerator(r -> r == Role.SENDER ? "Yuk jo'natuvchi" : "Kuryer (yuk yetkazuvchi)");
        role.setValue(Role.SENDER);

        fullName.setWidthFull();
        phone.setWidthFull();
        password.setWidthFull();
        confirm.setWidthFull();
        role.setWidthFull();

        fullName.addClassName("login-input");
        phone.addClassName("login-input");
        password.addClassName("login-input");
        confirm.addClassName("login-input");
        role.addClassName("login-input");

        VerticalLayout form = new VerticalLayout(fullName, phone, password, confirm, role);
        form.addClassName("register-form");
        form.setPadding(false);
        form.setSpacing(false);
        form.setWidthFull();
        form.setAlignItems(VerticalLayout.Alignment.STRETCH);

        // ---- Tugma va izoh
        submit.addClassName("login-submit");
        submit.getElement().setAttribute("type", "button");
        submitIcon.addClassName("login-submit-icon");
        submit.add(submitLabel, submitIcon);
        submit.addClickListener(e -> register());
        confirm.addKeyPressListener(Key.ENTER, e -> register());

        Paragraph hint = new Paragraph("Keyingi qadamda telefon raqamingizni Telegram botda tasdiqlaysiz.");
        hint.addClassName("register-hint");

        // ---- Pastki qism
        RouterLink loginLink = new RouterLink("Kirish", LoginView.class);
        loginLink.addClassName("login-register-link");
        Div footer = new Div(new Span("Hisobingiz bormi? "), loginLink);
        footer.addClassName("login-footer");

        card.add(brand, title, description, steps, form, submit, hint, footer);
        add(card);
    }

    /** Bosqich belgisi: raqam kvadrati va nomi. Faol bosqich sariq bo'ladi. */
    private Div registerStep(String number, String label, boolean active) {
        Span num = new Span(number);
        num.addClassName("register-step-num");

        Span text = new Span(label);
        text.addClassName("register-step-label");

        Div step = new Div(num, text);
        step.addClassName("register-step");
        if (active) {
            step.addClassName("register-step--active");
        }
        return step;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken)) {
            event.forwardTo("");
        }
    }

    private boolean validate() {
        fullName.setInvalid(false);
        phone.setInvalid(false);
        password.setInvalid(false);
        confirm.setInvalid(false);

        if (fullName.getValue().trim().length() < 2) {
            fullName.setErrorMessage("Ism familiyangizni kiriting");
            fullName.setInvalid(true);
            fullName.focus();
            return false;
        }
        if (!AuthService.normalizePhone(phone.getValue()).matches("^\\+998\\d{9}$")) {
            phone.setErrorMessage("Raqam +998901234567 ko'rinishida bo'lsin");
            phone.setInvalid(true);
            phone.focus();
            return false;
        }
        if (password.getValue().length() < 8) {
            password.setErrorMessage("Parol kamida 8 ta belgi bo'lsin");
            password.setInvalid(true);
            password.focus();
            return false;
        }
        if (!password.getValue().equals(confirm.getValue())) {
            confirm.setErrorMessage("Parollar bir xil emas");
            confirm.setInvalid(true);
            confirm.focus();
            return false;
        }
        return true;
    }

    /** Ma'lumotlarni tekshirib, sessiyaga saqlaydi va Telegram tasdiqlash sahifasiga o'tkazadi. */
    private void register() {
        if (!validate()) {
            return;
        }
        if (botUsername == null || botUsername.isBlank()) {
            notifyError("Telegram bot sozlanmagan. Administratorga murojaat qiling");
            return;
        }

        String phoneNorm = AuthService.normalizePhone(phone.getValue());
        if (authService.isPhoneTaken(phoneNorm)) {
            phone.setErrorMessage("Bu telefon raqam allaqachon ro'yxatdan o'tgan");
            phone.setInvalid(true);
            phone.focus();
            return;
        }

        String token;
        try {
            token = phoneVerification.createSession(phoneNorm);
        } catch (IllegalStateException ex) {
            notifyError(ex.getMessage());
            return;
        }

        VaadinSession.getCurrent().setAttribute(PendingRegistration.class, new PendingRegistration(
                token,
                phoneNorm,
                fullName.getValue().trim(),
                password.getValue(),
                role.getValue()));
        UI.getCurrent().navigate(TelegramVerifyView.class);
    }

    private void notifyError(String message) {
        Notification.show(message, 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}