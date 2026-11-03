package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.service.RatingService;

import java.util.ArrayList;
import java.util.List;

/** Yetkazilgan yukni 1–5 yulduz bilan baholash oynasi. */
public class RatingDialog extends Dialog {

    private static final String STAR_ON = "#f59e0b";
    private static final String STAR_OFF = "#d1d5db";
    private static final String TEXT = "#1e293b";
    private static final String MUTED = "#64748b";

    private final RatingService service;
    private final Long userId;
    private final Long deliveryId;
    private final Runnable onDone;

    private final List<NativeButton> starButtons = new ArrayList<>();
    private final TextArea comment = new TextArea("Izoh (ixtiyoriy)");
    private final Paragraph error = new Paragraph();
    private int stars = 0;

    public RatingDialog(RatingService service, Long userId, Long deliveryId, Runnable onDone) {
        this.service = service;
        this.userId = userId;
        this.deliveryId = deliveryId;
        this.onDone = onDone;

        addClassName("rating-dialog");
        setHeaderTitle("Yetkazishni baholang");
        setWidth("min(440px, 92vw)");
        setCloseOnOutsideClick(false);

        Paragraph hint = new Paragraph("Yuk #" + deliveryId + " bo'yicha tajribangizni baholang.");
        hint.getStyle().set("margin", "0").set("color", MUTED).set("font-size", "0.95rem");

        comment.setPlaceholder("Masalan: tez va ehtiyotkor yetkazdi");
        comment.setMaxLength(RatingService.MAX_COMMENT);
        comment.setWidthFull();

        error.getStyle().set("color", "#dc2626").set("margin", "0").set("font-size", "0.9rem");
        error.setVisible(false);

        VerticalLayout body = new VerticalLayout(hint, buildStars(), comment, error);
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();
        add(body);

        Button cancel = new Button("Bekor qilish", e -> close());
        Button send = new Button("Yuborish", e -> submit());
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, send);
    }

    private Div buildStars() {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("gap", "8px")
                .set("padding", "8px 0");

        for (int i = 1; i <= 5; i++) {
            final int value = i;
            NativeButton star = new NativeButton("★");
            star.getElement().setAttribute("type", "button");
            star.getElement().setAttribute("aria-label", value + " yulduz");
            star.getStyle()
                    .set("font-size", "40px")
                    .set("line-height", "1")
                    .set("border", "none")
                    .set("background", "transparent")
                    .set("cursor", "pointer")
                    .set("padding", "2px")
                    .set("transition", "transform 0.15s ease");
            star.addClickListener(e -> {
                stars = value;
                paintStars();
            });
            starButtons.add(star);
            row.add(star);
        }
        paintStars();
        return row;
    }

    private void paintStars() {
        for (int i = 0; i < starButtons.size(); i++) {
            starButtons.get(i).getStyle().set("color", i < stars ? STAR_ON : STAR_OFF);
        }
    }

    private void submit() {
        if (stars == 0) {
            showError("Iltimos, yulduz tanlang");
            return;
        }
        try {
            service.rate(userId, deliveryId, stars, comment.getValue());
            Notification.show("Rahmat! Bahoyingiz saqlandi", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
            onDone.run();
        } catch (BusinessException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
    }
}