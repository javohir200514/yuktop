package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import uz.onetimecourier.one_time_courier.config.CurrentUser;
import uz.onetimecourier.one_time_courier.config.CustomUserDetails;
import uz.onetimecourier.one_time_courier.entity.CourierRouteSubscription;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.CourierNotificationService;
import uz.onetimecourier.one_time_courier.telegram.TelegramUpdateHandler;

import java.util.ArrayList;
import java.util.List;

@Route(value = "kuryer/bildirishnoma", layout = PageLayout.class)
@PageTitle("Bildirishnomalar | YukBor")
@RolesAllowed("COURIER")
public class CourierNotificationsView extends VerticalLayout implements BeforeEnterObserver {

    private static final String ANY = "*";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CourierNotificationsView.class);

    // Yumshoq palitra
    private static final String TEXT = "#1e293b";
    private static final String MUTED = "#64748b";
    private static final String BORDER = "#e5e7eb";
    private static final String ACCENT = "#3b82f6";
    private static final String ACCENT_BG = "#eff6ff";

    private final CourierNotificationService service;
    private Long courierId;

    private final Div telegramBox = new Div();
    private final Div routeList = new Div();
    private final Span routeCount = new Span();
    private final Select<String> fromSelect = new Select<>();
    private final Select<String> toSelect = new Select<>();

    public CourierNotificationsView(CourierNotificationService service) {
        this.service = service;

        setWidthFull();
        setMaxWidth("760px");
        setPadding(false);
        setSpacing(false);
        getStyle().set("margin", "0 auto").set("padding", "32px 16px 48px").set("gap", "20px");

        // --- Sarlavha ---
        H2 title = new H2("Bildirishnomalar");
        title.getStyle().set("margin", "0").set("color", TEXT).set("font-size", "1.75rem");
        Paragraph intro = new Paragraph(
                "Tanlagan yo'nalishingizda yangi yuk chiqsa, Telegram botdan xabar olasiz.");
        intro.getStyle().set("margin", "4px 0 0").set("color", MUTED);
        Div header = new Div(title, intro);

        // --- Telegram holati ---
        telegramBox.setWidthFull();

        // --- Yo'nalishlar ro'yxati ---
        Div routesHeader = sectionHeader("Yo'nalishlarim", routeCount);
        routeList.setWidthFull();
        routeList.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "8px");
        Div routesCard = card(routesHeader, routeList);

        // --- Yangi yo'nalish ---
        Div addCard = card(sectionHeader("Yangi yo'nalish", null), buildAddForm());

        add(header, telegramBox, routesCard, addCard);
    }

    // ---------- Yordamchi UI metodlar ----------

    private static Div card(com.vaadin.flow.component.Component... children) {
        Div card = new Div(children);
        card.setWidthFull();
        card.getStyle()
                .set("box-sizing", "border-box")
                .set("background", "#ffffff")
                .set("border", "1px solid " + BORDER)
                .set("border-radius", "14px")
                .set("padding", "20px")
                .set("box-shadow", "0 1px 2px rgba(15, 23, 42, 0.04)");
        return card;
    }

    private static Div sectionHeader(String text, Span badge) {
        H3 h = new H3(text);
        h.getStyle().set("margin", "0").set("font-size", "1.05rem").set("color", TEXT);
        Div row = new Div(h);
        if (badge != null) {
            badge.getStyle()
                    .set("background", ACCENT_BG)
                    .set("color", ACCENT)
                    .set("font-size", "0.8rem")
                    .set("font-weight", "600")
                    .set("padding", "2px 10px")
                    .set("border-radius", "999px");
            row.add(badge);
        }
        row.getStyle().set("display", "flex").set("align-items", "center")
                .set("gap", "10px").set("margin-bottom", "16px");
        return row;
    }

    private Div statusBanner(VaadinIcon iconType, String text, String bg, String border, String iconColor) {
        Icon icon = iconType.create();
        icon.setSize("18px");
        icon.getStyle().set("color", iconColor).set("flex-shrink", "0").set("margin-top", "2px");

        Span msg = new Span(text);
        msg.getStyle().set("color", TEXT).set("line-height", "1.5").set("font-size", "0.95rem");

        Div banner = new Div(icon, msg);
        banner.setWidthFull();
        banner.getStyle()
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("align-items", "flex-start")
                .set("gap", "10px")
                .set("background", bg)
                .set("border", "1px solid " + border)
                .set("border-radius", "12px")
                .set("padding", "14px 18px");
        return banner;
    }

    // ---------- Ma'lumotlarni yangilash ----------

    private void refresh() {
        telegramBox.removeAll();
        if (service.isTelegramLinked(courierId)) {
            telegramBox.add(statusBanner(VaadinIcon.CHECK_CIRCLE,
                    "Telegram ulangan. Bildirishnomalar shu yerga keladi.",
                    "#f0fdf4", "#bbf7d0", "#16a34a"));
        } else {
            telegramBox.add(statusBanner(VaadinIcon.INFO_CIRCLE,
                    "Telegram hali bog'lanmagan. Ro'yxatdan o'tishda telefon raqamingizni tasdiqlang.",
                    "#fefce8", "#fde68a", "#ca8a04"));
        }

        routeList.removeAll();
        List<CourierRouteSubscription> subs = service.list(courierId);
        routeCount.setText(String.valueOf(subs.size()));
        routeCount.setVisible(!subs.isEmpty());

        if (subs.isEmpty()) {
            Div empty = new Div(new Span("Hozircha yo'nalish yo'q. Quyidan yangisini qo'shing."));
            empty.getStyle()
                    .set("text-align", "center")
                    .set("color", MUTED)
                    .set("font-size", "0.95rem")
                    .set("padding", "24px 12px")
                    .set("border", "1px dashed #d1d5db")
                    .set("border-radius", "10px");
            routeList.add(empty);
        }

        for (CourierRouteSubscription s : subs) {
            Checkbox enabled = new Checkbox(
                    TelegramUpdateHandler.routeLabel(s.getFromRegion(), s.getToRegion()),
                    s.isEnabled());
            enabled.getStyle().set("color", TEXT);
            enabled.addValueChangeListener(e -> service.setEnabled(courierId, s.getId(), e.getValue()));

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
                service.delete(courierId, s.getId());
                refresh();
            });
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            delete.getElement().setAttribute("aria-label", "O'chirish");

            HorizontalLayout row = new HorizontalLayout(enabled, delete);
            row.setWidthFull();
            row.setPadding(false);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            row.getStyle()
                    .set("box-sizing", "border-box")
                    .set("padding", "6px 8px 6px 14px")
                    .set("border", "1px solid " + BORDER)
                    .set("border-radius", "10px")
                    .set("background", "#f9fafb");
            routeList.add(row);
        }
    }

    // ---------- Forma ----------

    private FormLayout buildAddForm() {
        List<String> items = new ArrayList<>();
        items.add(ANY);
        for (Region r : Region.values()) {
            items.add(r.name());
        }

        fromSelect.setLabel("Jo'nash hududi");
        fromSelect.setItems(items);
        fromSelect.setItemLabelGenerator(this::label);
        fromSelect.setValue(ANY);
        fromSelect.setWidthFull();

        toSelect.setLabel("Yetkazish hududi");
        toSelect.setItems(items);
        toSelect.setItemLabelGenerator(this::label);
        toSelect.setValue(ANY);
        toSelect.setWidthFull();

        Button add = new Button("Qo'shish", VaadinIcon.PLUS.create(), e -> addRoute());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.addClickShortcut(Key.ENTER);
        add.setWidthFull();

        FormLayout form = new FormLayout(fromSelect, toSelect, add);
        form.setWidthFull();
        // Tor ekranda 1 ustun, keng ekranda 2 ustun
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("480px", 2));
        form.setColspan(add, 2);
        return form;
    }

    private void addRoute() {
        try {
            service.add(courierId, toRegion(fromSelect.getValue()), toRegion(toSelect.getValue()));
            Notification.show("Yo'nalish qo'shildi", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refresh();
        } catch (BusinessException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String label(String code) {
        return ANY.equals(code) ? "Istalgan" : TelegramUpdateHandler.regionLabel(Region.valueOf(code));
    }

    private static Region toRegion(String code) {
        return ANY.equals(code) || code == null ? null : Region.valueOf(code);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        CustomUserDetails user = CurrentUser.details().orElse(null);
        log.info("Bildirishnoma sahifasi: role={}", user == null ? "null" : user.getRole());

        if (user == null || !"COURIER".equals(user.getRole())) {
            event.forwardTo("");
            return;
        }
        courierId = user.getId();
        refresh();
    }
}