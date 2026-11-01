package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.PackageSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Ochiq yuklar va kuryer sahifalari uchun umumiy matnlar va kichik komponentlar. */
final class DeliveryViewSupport {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static final String[] STEP_LABELS = {"Joylandi", "Kuryer topildi", "Yo'lda", "Yetkazildi"};

    private static final Pattern PHONE = Pattern.compile("^\\+998\\d{9}$");
    private static final Pattern TELEGRAM = Pattern.compile("^[A-Za-z0-9_]{5,32}$");

    private DeliveryViewSupport() {
    }

    // ---------------------------------------------------------------- matnlar

    static String statusLabel(DeliveryStatus status) {
        return switch (status) {
            case CREATED -> "Kuryer qidirilmoqda";
            case MATCHED -> "Kuryer topildi";
            case PICKED_UP -> "Yo'lda";
            case DELIVERED -> "Yetkazildi";
            case CANCELLED -> "Bekor qilindi";
            case DISPUTED -> "Nizo ko'rib chiqilmoqda";
        };
    }

    static String typeLabel(DeliveryType type) {
        return type == DeliveryType.INTRACITY ? "Shahar ichida" : "Viloyatlararo";
    }

    static String sizeLabel(PackageSize size) {
        return switch (size) {
            case DOCUMENT -> "Hujjat";
            case SMALL -> "Kichik";
            case MEDIUM -> "O'rta";
            case LARGE -> "Katta";
        };
    }

    static String weightText(BigDecimal weight) {
        return weight == null ? "—" : weight.stripTrailingZeros().toPlainString() + " kg";
    }

    static String formatDate(LocalDateTime t) {
        return t == null ? "—" : DATE_FORMAT.format(t);
    }

    static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        String plain = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = plain.length() - 1; i >= 0; i--) {
            sb.append(plain.charAt(i));
            if (++count % 3 == 0 && i > 0) {
                sb.append(' ');
            }
        }
        return sb.reverse() + " so'm";
    }

    // ---------------------------------------------------------------- havolalar (qiymat qayta tekshiriladi)

    static Anchor telLink(String phone) {
        if (phone == null || !PHONE.matcher(phone).matches()) {
            return null;
        }
        Anchor a = new Anchor("tel:" + phone, phone);
        a.addClassName("df-link");
        return a;
    }

    static Anchor telegramLink(String username) {
        if (username == null || !TELEGRAM.matcher(username).matches()) {
            return null;
        }
        Anchor a = new Anchor("https://t.me/" + username, "@" + username);
        a.setTarget("_blank");
        a.getElement().setAttribute("rel", "noopener noreferrer");
        a.addClassName("df-link");
        return a;
    }

    static Anchor mapLink(String text, Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }
        String href = String.format(Locale.ROOT,
                "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f", lat, lng, lat, lng);
        Anchor a = new Anchor(href, text);
        a.setTarget("_blank");
        a.getElement().setAttribute("rel", "noopener noreferrer");
        a.addClassName("df-link");
        return a;
    }

    // ---------------------------------------------------------------- komponentlar

    static Span tag(String text) {
        Span t = new Span(text);
        t.addClassName("md-tag");
        return t;
    }

    static Span statusBadge(DeliveryStatus status) {
        Span dot = new Span();
        dot.addClassName("md-dot");
        dot.getElement().setAttribute("aria-hidden", "true");
        Span badge = new Span(dot, new Span(statusLabel(status)));
        badge.addClassNames("md-badge", "md-badge--" + status.name().toLowerCase());
        return badge;
    }

    static Span verifyBadge(boolean verified) {
        Span s = new Span(verified ? "Tasdiqlangan" : "Tasdiqlanmagan");
        s.addClassNames("df-verify", verified ? "df-verify--yes" : "df-verify--no");
        return s;
    }

    /** Yorliq va qiymat(lar)dan iborat blok. Qiymatlar faqat setText bilan qo'yiladi. */
    static Div infoBlock(String label, Component... values) {
        Span l = new Span(label);
        l.addClassName("md-block-label");
        Div block = new Div(l);
        block.addClassName("md-block");
        for (Component v : values) {
            if (v != null) {
                v.getElement().getClassList().add("md-block-value");
                block.add(v);
            }
        }
        return block;
    }

    static Span text(String value) {
        return new Span(value == null || value.isBlank() ? "—" : value);
    }

    /** Bosqichlar chizig'i. Faqat yukning haqiqiy holatidan chiqariladi. */
    static Component tracker(DeliveryStatus status, String cancelledText) {
        int current = switch (status) {
            case CREATED -> 0;
            case MATCHED -> 1;
            case PICKED_UP -> 2;
            case DELIVERED -> 3;
            default -> -1;
        };

        if (current < 0) {
            Div note = new Div();
            note.addClassNames("md-note", "md-note--" + status.name().toLowerCase());
            note.setText(status == DeliveryStatus.CANCELLED
                    ? cancelledText
                    : "Bu yuk bo'yicha nizo ko'rib chiqilmoqda.");
            return note;
        }

        Div track = new Div();
        track.addClassName("md-track");
        track.getElement().setAttribute("role", "list");
        track.getElement().setAttribute("aria-label",
                "Yetkazish bosqichlari. Hozirgi holat: " + STEP_LABELS[current]);

        for (int i = 0; i < STEP_LABELS.length; i++) {
            Div step = new Div(new Span(STEP_LABELS[i]));
            step.addClassName("md-step");
            step.getElement().setAttribute("role", "listitem");
            if (i <= current) {
                step.addClassName("md-step--done");
            }
            if (i == current) {
                step.addClassName("md-step--current");
                step.getElement().setAttribute("aria-current", "step");
            }
            track.add(step);
        }
        return track;
    }

    /**
     * Tasdiqlash oynasi. action xato matnini qaytaradi; null qaytsa, oyna yopiladi.
     */
    static void confirm(String title, String text, String confirmLabel, boolean danger,
                        Supplier<String> action) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                dialog.removeFromParent();
            }
        });

        Div content = new Div();
        content.addClassName("md-dialog");

        H2 heading = new H2(title);
        heading.addClassName("md-dialog-title");
        Paragraph body = new Paragraph(text);
        body.addClassName("md-dialog-text");

        Div error = new Div();
        error.addClassName("md-dialog-error");
        error.getElement().setAttribute("role", "alert");
        error.setVisible(false);

        NativeButton confirmBtn = new NativeButton(confirmLabel);
        confirmBtn.addClassNames("md-btn", danger ? "md-btn--danger-solid" : "md-btn--solid");
        confirmBtn.getElement().setAttribute("type", "button");

        NativeButton back = new NativeButton("Yo'q, qaytish");
        back.addClassNames("md-btn", "md-btn--outline");
        back.getElement().setAttribute("type", "button");
        back.addClickListener(e -> dialog.close());

        confirmBtn.addClickListener(e -> {
            confirmBtn.setEnabled(false);
            String err = action.get();
            if (err == null) {
                dialog.close();
            } else {
                error.setText(err);
                error.setVisible(true);
                confirmBtn.setEnabled(true);
            }
        });

        Div actions = new Div(confirmBtn, back);
        actions.addClassName("md-dialog-actions");

        content.add(heading, body, error, actions);
        dialog.add(content);
        dialog.open();
        back.getElement().callJsFunction("focus");
    }
}