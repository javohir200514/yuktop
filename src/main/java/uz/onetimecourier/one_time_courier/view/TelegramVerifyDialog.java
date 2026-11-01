package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService;
import uz.onetimecourier.one_time_courier.service.TelegramPhoneVerificationService.Status;

/** Telegram botda raqamni yuborgandan keyin, botdan kelgan kodni kiritish oynasi. */
public class TelegramVerifyDialog extends Dialog {

    private final TelegramPhoneVerificationService service;
    private final String token;
    private final Runnable onVerified;
    private final Runnable onFail;

    private final Paragraph info = new Paragraph(
            "1) Telegram botda pastdagi tugmani bosing va raqamingizni yuboring.\n"
                    + "2) Botdan kelgan 6 xonali kodni shu yerga kiriting.");
    private final TextField code = new TextField("Tasdiqlash kodi");
    private final Paragraph error = new Paragraph();
    private final Button confirm = new Button("Tasdiqlash");
    private final Registration pollRegistration;
    private boolean finished;

    public TelegramVerifyDialog(TelegramPhoneVerificationService service,
                                String token,
                                String botUsername,
                                Runnable onVerified,
                                Runnable onFail) {
        this.service = service;
        this.token = token;
        this.onVerified = onVerified;
        this.onFail = onFail;

        setHeaderTitle("Telegram orqali tasdiqlash");
        setCloseOnOutsideClick(false);

        code.setPlaceholder("123456");
        code.setMaxLength(6);
        code.setAllowedCharPattern("[0-9]");
        code.setVisible(false);

        error.getStyle().set("color", "var(--lumo-error-text-color)");
        error.setVisible(false);

        Button openBot = new Button("Telegram botni ochish",
                e -> UI.getCurrent().getPage().open(
                        "https://t.me/" + botUsername + "?start=" + token, "_blank"));
        openBot.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        confirm.setVisible(false);
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirm.addClickListener(e -> confirmCode());

        Button cancel = new Button("Bekor qilish", e -> close());

        add(info, openBot, code, error);
        getFooter().add(cancel, confirm);

        UI ui = UI.getCurrent();
        ui.setPollInterval(1500);
        pollRegistration = ui.addPollListener(e -> refresh());
        addDetachListener(e -> {
            pollRegistration.remove();
            ui.setPollInterval(-1);
            if (!finished) {
                service.discard(token);
                onFail.run();
            }
        });
    }

    private void refresh() {
        Status status = service.poll(token);
        if (status == Status.WAITING_CODE && !code.isVisible()) {
            code.setVisible(true);
            confirm.setVisible(true);
            info.setText("Botdan kelgan 6 xonali kodni kiriting.");
        } else if (status == Status.EXPIRED) {
            Notification.show("Vaqt tugadi. Qaytadan urinib ko'ring", 4000, Notification.Position.TOP_CENTER);
            close();
        }
    }

    private void confirmCode() {
        if (service.checkCode(token, code.getValue())) {
            finished = true;
            close();
            onVerified.run();
        } else {
            error.setText("Kod noto'g'ri yoki muddati o'tgan");
            error.setVisible(true);
        }
    }
}