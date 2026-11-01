package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.onetimecourier.one_time_courier.config.SessionAuthenticator;
import uz.onetimecourier.one_time_courier.config.TelegramLoginSessionStore;
import uz.onetimecourier.one_time_courier.entity.UserEntity;
import uz.onetimecourier.one_time_courier.layout.AuthLayout;
import uz.onetimecourier.one_time_courier.service.AuthService;
import uz.onetimecourier.one_time_courier.service.TelegramLoginService;


@Route(value = "login", layout = AuthLayout.class)
@PageTitle("Kirish | YukBor")
@AnonymousAllowed
@CssImport("./styles/login-view.css")
public class LoginView extends Div implements BeforeEnterObserver {

    // O'lcham (width/height) SVG'ning o'zida berilgan: CSS yuklanmasa ham katta bo'lib ketmaydi
    private static final String GOOGLE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="20" height="20"
                 style="flex:none;display:block" aria-hidden="true">
              <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
              <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
              <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
              <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
            </svg>""";

    // Telegram: dumaloq belgi ichida samolyotcha
    private static final String TELEGRAM_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="22" height="22"
                 fill="currentColor" style="flex:none;display:block" aria-hidden="true">
              <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/>
            </svg>""";

    private static final String GOOGLE_LABEL = "Google orqali kirish";
    private static final String TELEGRAM_LABEL = "Telegram orqali kirish";

    private final AuthenticationManager authenticationManager;
    private final TelegramLoginService telegramLoginService;
    private final SessionAuthenticator sessionAuthenticator;

    private final TextField phone = new TextField("Telefon raqam");
    private final PasswordField password = new PasswordField("Parol");
    private final Button submit = new Button("Kirish");
    private final Button googleBtn = new Button();
    private final Button telegramBtn = new Button();

    private String telegramToken;
    private Registration pollRegistration;

    public LoginView(AuthenticationManager authenticationManager,
                     TelegramLoginService telegramLoginService,
                     SessionAuthenticator sessionAuthenticator) {
        this.authenticationManager = authenticationManager;
        this.telegramLoginService = telegramLoginService;
        this.sessionAuthenticator = sessionAuthenticator;

        addClassName("login-page");

        VerticalLayout card = new VerticalLayout();
        card.addClassName("login-card");
        card.setPadding(false);
        card.setSpacing(false);
        card.setWidthFull();
        card.setAlignItems(VerticalLayout.Alignment.STRETCH);

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

        H1 title = new H1();
        title.addClassName("login-title");
        title.add(new Span("Hisobingizga "));
        Span accent = new Span("kiring");
        accent.addClassName("login-title-highlight");
        title.add(accent);

        Paragraph description = new Paragraph("Buyurtmalaringizni qulay va xavfsiz boshqaring.");
        description.addClassName("login-description");

        phone.setValue("+998");
        phone.setPlaceholder("+998901234567");
        phone.setPrefixComponent(VaadinIcon.PHONE.create());
        phone.setAllowedCharPattern("[0-9+]");
        phone.setMaxLength(13);
        phone.setWidthFull();
        phone.addClassName("login-input");

        password.setPlaceholder("Parolingizni kiriting");
        password.setPrefixComponent(VaadinIcon.LOCK.create());
        password.setRevealButtonVisible(true);
        password.setWidthFull();
        password.addClassName("login-input");

        VerticalLayout form = new VerticalLayout(phone, password);
        form.addClassName("login-form");
        form.setPadding(false);
        form.setSpacing(false);
        form.setWidthFull();
        form.setAlignItems(VerticalLayout.Alignment.STRETCH);

        submit.addClassName("login-submit");
        submit.setWidthFull();
        submit.setIcon(VaadinIcon.ARROW_RIGHT.create());
        submit.setIconAfterText(true);
        submit.addClickListener(e -> login());
        phone.addKeyPressListener(Key.ENTER, e -> login());
        password.addKeyPressListener(Key.ENTER, e -> login());

        card.add(brand, title, description, form, submit,
                buildDivider(), buildSocialButtons(), buildFooter());
        add(card);
    }

    // ---------- Yasash ----------

    /** "yoki" ajratgichi: chiziq — yozuv — chiziq (CSS'ga bog'liq emas). */
    private Div buildDivider() {
        Span left = line();
        Span text = new Span("yoki");
        text.getStyle()
                .set("color", "#8793a8")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", ".08em");
        Span right = line();

        Div divider = new Div(left, text, right);
        divider.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("width", "100%")
                .set("margin", "0 0 16px");
        return divider;
    }

    private static Span line() {
        Span s = new Span();
        s.getStyle()
                .set("flex", "1")
                .set("height", "1px")
                .set("background", "#dde6f5");
        return s;
    }

    private Div buildSocialButtons() {
        styleSocial(googleBtn, "social-google", "#ffffff", "1px solid #d6e4fb");
        setSocialContent(googleBtn, GOOGLE_SVG, GOOGLE_LABEL, "#374151");
        googleBtn.addClickListener(e ->
                UI.getCurrent().getPage().setLocation("/oauth2/authorization/google"));

        styleSocial(telegramBtn, "social-telegram", "#229ed9", "none");
        setSocialContent(telegramBtn, TELEGRAM_SVG, TELEGRAM_LABEL, "#ffffff");
        telegramBtn.addClickListener(e -> startTelegramLogin());

        Div social = new Div(googleBtn, telegramBtn);
        social.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px")
                .set("width", "100%")
                .set("margin", "0 0 18px");
        return social;
    }

    private Div buildFooter() {
        Div footer = new Div(new Span("Hisobingiz yo'qmi? "));
        RouterLink registerLink = new RouterLink("Ro'yxatdan o'tish", RegisterView.class);
        registerLink.addClassName("login-register-link");
        footer.add(registerLink);
        footer.addClassName("login-footer");
        return footer;
    }

    private static void styleSocial(Button b, String cssClass, String background, String border) {
        b.addClassNames("social-btn", cssClass);
        b.setWidthFull();
        b.getStyle()
                .set("height", "52px")
                .set("min-height", "52px")
                .set("margin", "0")
                .set("padding", "0")
                .set("border-radius", "14px")
                .set("background", background)
                .set("border", border)
                .set("cursor", "pointer");
    }

    private static void setSocialContent(Button b, String svg, String label, String color) {
        b.getElement().setProperty("innerHTML",
                "<span style=\"display:inline-flex;align-items:center;justify-content:center;"
                        + "gap:10px;color:" + color + ";font-size:14px;font-weight:700;\">"
                        + svg + "<span>" + label + "</span></span>");
    }

    // ---------- Telefon + parol ----------

    private void login() {
        String phoneNumber = AuthService.normalizePhone(phone.getValue());
        String passwordValue = password.getValue();

        if (!phoneNumber.matches("^\\+998\\d{9}$")) {
            showError("Telefon raqamni +998XXXXXXXXX formatida kiriting.");
            phone.focus();
            return;
        }
        if (passwordValue == null || passwordValue.isBlank()) {
            showError("Parolni kiriting.");
            password.focus();
            return;
        }
        if (!submit.isEnabled()) {
            return;
        }

        submit.setEnabled(false);
        submit.setText("Tekshirilmoqda...");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(phoneNumber, passwordValue));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Sessiyaga saqlash (Google/Telegram bilan bir xil yo'l)
            org.springframework.security.core.context.SecurityContext context =
                    SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            new org.springframework.security.web.context.HttpSessionSecurityContextRepository().saveContext(
                    context,
                    VaadinServletRequest.getCurrent().getHttpServletRequest(),
                    VaadinServletResponse.getCurrent().getHttpServletResponse());

            UI.getCurrent().getPage().setLocation("/");
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            password.clear();
            showError("Telefon raqam yoki parol noto'g'ri.");
            resetSubmit();
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            showError("Kirishda xatolik yuz berdi. Qayta urinib ko'ring.");
            resetSubmit();
        }
    }

    private void resetSubmit() {
        submit.setEnabled(true);
        submit.setText("Kirish");
        submit.setIcon(VaadinIcon.ARROW_RIGHT.create());
        password.focus();
    }

    // ---------- Telegram ----------

    private void startTelegramLogin() {
        if (!telegramLoginService.isEnabled()) {
            showError("Telegram orqali kirish hozircha sozlanmagan.");
            return;
        }
        UI ui = UI.getCurrent();
        stopTelegramPolling(ui);

        TelegramLoginService.Started started = telegramLoginService.start();
        telegramToken = started.token();
        setTelegramLabel("Telegramda tasdiqlang...");

        ui.getPage().open(started.link(), "_blank");
        ui.setPollInterval(2000);
        pollRegistration = ui.addPollListener(e -> checkTelegram());
    }

    private void checkTelegram() {
        if (telegramToken == null) {
            return;
        }
        UI ui = UI.getCurrent();
        TelegramLoginSessionStore.PollResult result = telegramLoginService.poll(telegramToken);
        switch (result.state()) {
            case PENDING -> { }
            case EXPIRED -> {
                stopTelegramPolling(ui);
                showError("Vaqt tugadi. Telegram orqali qayta urinib ko'ring.");
            }
            case DONE -> {
                stopTelegramPolling(ui);
                try {
                    UserEntity user = telegramLoginService.resolveUser(result.profile());
                    sessionAuthenticator.establish(user,
                            VaadinServletRequest.getCurrent().getHttpServletRequest(),
                            VaadinServletResponse.getCurrent().getHttpServletResponse());
                    ui.getPage().setLocation("/");
                } catch (Exception ex) {
                    showError("Kirishda xatolik yuz berdi. Qayta urinib ko'ring.");
                }
            }
        }
    }

    private void stopTelegramPolling(UI ui) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        if (ui != null) {
            ui.setPollInterval(-1);
        }
        telegramToken = null;
        setTelegramLabel(TELEGRAM_LABEL);
    }

    private void setTelegramLabel(String label) {
        setSocialContent(telegramBtn, TELEGRAM_SVG, label, "#ffffff");
    }

    @Override
    protected void onDetach(DetachEvent event) {
        stopTelegramPolling(event.getUI());
    }

    // ---------- Umumiy ----------

    private void showError(String message) {
        Notification.show(message, 3500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            event.forwardTo("");
            return;
        }
        // Google'dan xato bilan qaytsa (?error)
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            showError("Kirish amalga oshmadi. Qayta urinib ko'ring.");
        }
    }
}