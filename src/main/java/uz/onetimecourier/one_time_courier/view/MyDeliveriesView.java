package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import uz.onetimecourier.one_time_courier.config.CustomUserDetails;
import uz.onetimecourier.one_time_courier.dto.CourierInfoDto;
import uz.onetimecourier.one_time_courier.dto.DeliveryStatusChangedEvent;
import uz.onetimecourier.one_time_courier.dto.DeliverySummaryDto;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.DeliveryType;
import uz.onetimecourier.one_time_courier.enums.PackageSize;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.DeliveryService;
import uz.onetimecourier.one_time_courier.service.DeliveryStatusBroadcaster;
import uz.onetimecourier.one_time_courier.service.RatingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Route(value = "deliveries/mine", layout = PageLayout.class)
@PageTitle("Mening yuklarim | YukBor")
@RolesAllowed("SENDER")
@CssImport("./styles/my-deliveries.css")
public class MyDeliveriesView extends Div {

    private static final Logger log = LoggerFactory.getLogger(MyDeliveriesView.class);

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static final String GENERIC_ERROR =
            "Kutilmagan xato yuz berdi. Birozdan keyin qayta urinib ko'ring.";

    private static final String[] STEP_LABELS = {"Joylandi", "Kuryer topildi", "Yo'lda", "Yetkazildi"};

    private enum Filter {
        ALL("Hammasi", null),
        WAITING("Kutilmoqda", Set.of(DeliveryStatus.CREATED)),
        ON_THE_WAY("Yo'lda", Set.of(DeliveryStatus.MATCHED, DeliveryStatus.PICKED_UP)),
        DELIVERED("Yetkazildi", Set.of(DeliveryStatus.DELIVERED)),
        CANCELLED("Bekor qilindi", Set.of(DeliveryStatus.CANCELLED));

        private final String label;
        private final Set<DeliveryStatus> statuses;

        Filter(String label, Set<DeliveryStatus> statuses) {
            this.label = label;
            this.statuses = statuses;
        }
    }

    private final DeliveryService deliveryService;
    private final DeliveryStatusBroadcaster broadcaster;
    private final RatingService ratingService;

    /**
     * Foydalanuvchi ID'si view yaratilganda (so'rov oqimida) olinadi va saqlanadi.
     * Push oqimida SecurityContext bo'lmasligi mumkin, shu sababli u yerda authContext ishlatilmaydi.
     */
    private final Long userId;

    private final Map<Filter, NativeButton> filterButtons = new EnumMap<>(Filter.class);
    private final Div alert = new Div();
    private final Span totalLabel = new Span();
    private final Span liveRegion = new Span();
    private final Div listBox = new Div();
    private final Div pager = new Div();
    private final NativeButton prevButton = new NativeButton("‹ Oldingi");
    private final NativeButton nextButton = new NativeButton("Keyingi ›");
    private final Span pageLabel = new Span();

    private Filter currentFilter = Filter.ALL;
    private int page = 0;
    private Registration pushRegistration;
    private Long highlightId;

    public MyDeliveriesView(DeliveryService deliveryService,
                            AuthenticationContext authContext,
                            DeliveryStatusBroadcaster broadcaster,
                            RatingService ratingService) {
        this.deliveryService = deliveryService;
        this.broadcaster = broadcaster;
        this.ratingService = ratingService;
        this.userId = authContext.getAuthenticatedUser(CustomUserDetails.class)
                .map(CustomUserDetails::getId)
                .orElse(null);

        addClassName("md-page");

        H1 title = new H1("Mening yuklarim");
        title.addClassName("md-title");
        Paragraph subtitle = new Paragraph("Joylagan yuklaringiz holatini shu yerda kuzatib boring.");
        subtitle.addClassName("md-subtitle");
        Div titleBox = new Div(title, subtitle);
        titleBox.addClassName("md-title-box");

        NativeButton create = new NativeButton("+ Yuk joylash");
        create.addClassName("md-primary");
        create.getElement().setAttribute("type", "button");
        create.addClickListener(e -> UI.getCurrent().navigate("deliveries/new"));

        Div head = new Div(titleBox, create);
        head.addClassName("md-head");

        Div filters = buildFilters();

        alert.addClassName("md-alert");
        alert.getElement().setAttribute("role", "alert");
        alert.setVisible(false);

        totalLabel.addClassName("md-total");

        // Ekran o'qish dasturlari uchun: holat o'zgarganda e'lon qilinadi
        liveRegion.addClassName("md-sr-only");
        liveRegion.getElement().setAttribute("role", "status");
        liveRegion.getElement().setAttribute("aria-live", "polite");

        listBox.addClassName("md-list");

        buildPager();

        add(head, filters, alert, totalLabel, liveRegion, listBox, pager);
        updateFilterButtons();
        load();
    }

    // ---------------------------------------------------------------- jonli yangilanish

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (userId != null && pushRegistration == null) {
            pushRegistration = broadcaster.register(userId, attachEvent.getUI(), this::onStatusChanged);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pushRegistration != null) {
            pushRegistration.remove();
            pushRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    private void onStatusChanged(DeliveryStatusChangedEvent event) {
        try {
            if (event.status() == DeliveryStatus.MATCHED && isPickupAwaitingSender(event.deliveryId())) {
                highlightId = event.deliveryId();
                showPickupToast(event.deliveryId());
            } else if (event.status() == DeliveryStatus.PICKED_UP
                    && userId != null
                    && deliveryService.awaitingDeliveryConfirmation(userId, event.deliveryId())) {
                highlightId = event.deliveryId();
                showDeliveryToast(event.deliveryId());
            } else if (isCourierDriven(event.status())) {
                highlightId = event.deliveryId();
                String text = "Yuk holati yangilandi: " + statusLabel(event.status());
                liveRegion.setText(text);
                Notification.show(text, 5000, Notification.Position.BOTTOM_START);
            }
            load();
        } catch (RuntimeException e) {
            log.warn("Jonli yangilanishda xato: {}", e.getClass().getSimpleName());
        }
    }
    private void showDeliveryToast(Long deliveryId) {
        Notification n = new Notification();
        n.setDuration(0);
        n.setPosition(Notification.Position.TOP_CENTER);

        Span text = new Span("Kuryer yukni yetkazganini bildirdi. Tasdiqlaysizmi?");

        NativeButton ok = new NativeButton("Ha, yetkazildi");
        ok.addClassNames("md-btn", "md-btn--solid");
        ok.getElement().setAttribute("type", "button");
        ok.addClickListener(e -> {
            n.close();
            confirmDelivery(deliveryId);
        });

        NativeButton later = new NativeButton("Keyinroq");
        later.addClassNames("md-btn", "md-btn--outline");
        later.getElement().setAttribute("type", "button");
        later.addClickListener(e -> n.close());

        n.add(new Div(text, new Div(ok, later)));
        n.open();
    }

    private void confirmDelivery(Long deliveryId) {
        try {
            deliveryService.confirmDeliveryBySender(userId, deliveryId);
            Notification.show("Tasdiqlandi. Yuk «Yetkazildi» holatiga o'tdi",
                    4000, Notification.Position.TOP_CENTER);
        } catch (BusinessException e) {
            Notification.show(e.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (RuntimeException e) {
            log.error("Yetkazish tasdiqlashda xato", e);
            Notification.show(GENERIC_ERROR, 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        load();
    }

    private static boolean isCourierDriven(DeliveryStatus status) {
        return status == DeliveryStatus.MATCHED
                || status == DeliveryStatus.PICKED_UP
                || status == DeliveryStatus.DELIVERED;
    }

    // ---------------------------------------------------------------- kuryer bilan bog'liq

    /** Kuryer ma'lumoti. Xato bo'lsa yoki kuryer yo'q bo'lsa, bo'sh qaytadi. */
    private Optional<CourierInfoDto> fetchCourier(Long deliveryId) {
        if (userId == null) {
            return Optional.empty();
        }
        try {
            return deliveryService.courierInfo(userId, deliveryId);
        } catch (RuntimeException e) {
            log.warn("Kuryer ma'lumotini olishda xato, yuk id={}", deliveryId);
            return Optional.empty();
        }
    }

    /** Kuryer yukni olganini bildirgan-u, jo'natuvchi hali tasdiqlamagan bo'lsa true. */
    private static boolean awaitingSender(CourierInfoDto c) {
        return c.courierPickupRequested() && !c.senderPickupConfirmed();
    }

    private boolean isPickupAwaitingSender(Long deliveryId) {
        return fetchCourier(deliveryId).map(MyDeliveriesView::awaitingSender).orElse(false);
    }

    /** Jo'natuvchiga bildirishnoma: "Ha, tasdiqlayman" tugmasi bilan. */
    private void showPickupToast(Long deliveryId) {
        Notification n = new Notification();
        n.setDuration(0);
        n.setPosition(Notification.Position.TOP_CENTER);

        Span text = new Span("Kuryer yukni olganini bildirdi. Tasdiqlaysizmi?");

        NativeButton ok = new NativeButton("Ha, tasdiqlayman");
        ok.addClassNames("md-btn", "md-btn--solid");
        ok.getElement().setAttribute("type", "button");
        ok.addClickListener(e -> {
            n.close();
            confirmPickup(deliveryId);
        });

        NativeButton later = new NativeButton("Keyinroq");
        later.addClassNames("md-btn", "md-btn--outline");
        later.getElement().setAttribute("type", "button");
        later.addClickListener(e -> n.close());

        n.add(new Div(text, new Div(ok, later)));
        n.open();
    }

    /** Jo'natuvchi tasdiqlaydi. Natija xabar sifatida chiqadi va ro'yxat yangilanadi. */
    private void confirmPickup(Long deliveryId) {
        try {
            deliveryService.confirmPickupBySender(userId, deliveryId);
            Notification.show("Tasdiqlandi. Yuk «Yo'lda» holatiga o'tdi",
                    4000, Notification.Position.TOP_CENTER);
        } catch (BusinessException e) {
            Notification.show(e.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (RuntimeException e) {
            log.error("Olish tasdiqlashda xato", e);
            Notification.show(GENERIC_ERROR, 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        load();
    }

    // ---------------------------------------------------------------- qurilish

    private Div buildFilters() {
        Div bar = new Div();
        bar.addClassName("md-filters");
        bar.getElement().setAttribute("role", "group");
        bar.getElement().setAttribute("aria-label", "Holat bo'yicha filtr");

        for (Filter f : Filter.values()) {
            NativeButton b = new NativeButton(f.label);
            b.addClassName("md-filter");
            b.getElement().setAttribute("type", "button");
            b.addClickListener(e -> {
                currentFilter = f;
                page = 0;
                updateFilterButtons();
                load();
            });
            filterButtons.put(f, b);
            bar.add(b);
        }
        return bar;
    }

    private void buildPager() {
        pager.addClassName("md-pager");

        prevButton.addClassName("md-page-btn");
        prevButton.getElement().setAttribute("type", "button");
        prevButton.addClickListener(e -> {
            if (page > 0) {
                page--;
                load();
            }
        });

        nextButton.addClassName("md-page-btn");
        nextButton.getElement().setAttribute("type", "button");
        nextButton.addClickListener(e -> {
            page++;
            load();
        });

        pageLabel.addClassName("md-page-label");
        pageLabel.getElement().setAttribute("aria-live", "polite");

        pager.add(prevButton, pageLabel, nextButton);
        pager.setVisible(false);
    }

    private void updateFilterButtons() {
        filterButtons.forEach((f, b) -> {
            boolean active = f == currentFilter;
            b.getElement().setAttribute("aria-pressed", String.valueOf(active));
            b.getClassNames().set("md-filter--active", active);
        });
    }

    // ---------------------------------------------------------------- yuklash

    private void load() {
        alert.setVisible(false);

        if (userId == null) {
            showAlert("Sessiya tugagan. Iltimos, qaytadan kiring.");
            return;
        }

        try {
            Page<DeliverySummaryDto> result = deliveryService.mine(
                    userId, currentFilter.statuses, PageRequest.of(page, PAGE_SIZE));

            if (result.getTotalPages() > 0 && page >= result.getTotalPages()) {
                page = result.getTotalPages() - 1;
                load();
                return;
            }
            render(result);
        } catch (BusinessException e) {
            showAlert(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Yuklar ro'yxatini olishda xato", e);
            showAlert(GENERIC_ERROR);
        }
    }

    private void render(Page<DeliverySummaryDto> result) {
        listBox.removeAll();

        if (result.isEmpty()) {
            totalLabel.setText("");
            pager.setVisible(false);
            listBox.add(emptyState());
            highlightId = null;
            return;
        }

        totalLabel.setText("Jami: " + result.getTotalElements() + " ta yuk");
        result.getContent().forEach(d -> listBox.add(card(d)));
        highlightId = null;

        int totalPages = result.getTotalPages();
        pager.setVisible(totalPages > 1);
        pageLabel.setText("Sahifa " + (page + 1) + " / " + totalPages);
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < totalPages);
    }

    private Div emptyState() {
        Div box = new Div();
        box.addClassName("md-empty");

        boolean filtered = currentFilter != Filter.ALL;
        H2 h = new H2(filtered ? "Bu holatda yuk yo'q" : "Hali yuk joylamagansiz");
        h.addClassName("md-empty-title");
        Paragraph p = new Paragraph(filtered
                ? "Boshqa holatni tanlang yoki yangi yuk joylang."
                : "Birinchi yukingizni joylang, shu yo'nalishda ketayotgan kuryerlar uni ko'radi.");
        p.addClassName("md-empty-text");

        NativeButton create = new NativeButton("Yuk joylash");
        create.addClassName("md-primary");
        create.getElement().setAttribute("type", "button");
        create.addClickListener(e -> UI.getCurrent().navigate("deliveries/new"));

        box.add(h, p, create);
        return box;
    }

    // ---------------------------------------------------------------- kartochka

    private Div card(DeliverySummaryDto d) {
        Div card = new Div();
        card.addClassNames("md-card", "md-card--" + d.status().name().toLowerCase());
        if (Objects.equals(d.id(), highlightId)) {
            card.addClassName("md-card--updated");
        }

        // Yuqori qator: yo'nalish va holat belgisi
        Span from = new Span(d.fromRegion().getLabel());
        Span arrow = new Span("→");
        arrow.addClassName("md-route-arrow");
        arrow.getElement().setAttribute("aria-hidden", "true");
        Span to = new Span(d.toRegion().getLabel());
        Div route = new Div(from, arrow, to);
        route.addClassName("md-route");

        Span dot = new Span();
        dot.addClassName("md-dot");
        dot.getElement().setAttribute("aria-hidden", "true");
        Span badgeText = new Span(statusLabel(d.status()));
        Span badge = new Span(dot, badgeText);
        badge.addClassNames("md-badge", "md-badge--" + d.status().name().toLowerCase());

        Div top = new Div(route, badge);
        top.addClassName("md-card-top");

        // Teglar
        Div tags = new Div(
                tag(typeLabel(d.deliveryType())),
                tag(sizeLabel(d.packageSize())),
                tag(weightText(d.weightKg())));
        tags.addClassName("md-tags");

        // Manzillar
        Div blocks = new Div(
                block("Olib ketish", d.pickupAddress()),
                block("Yetkazish", d.dropoffAddress()),
                block("Qabul qiluvchi", d.recipientName()));
        blocks.addClassName("md-blocks");

        // Pastki qator
        Span date = new Span("Joylangan: " + formatDate(d.createdAt()));
        date.addClassName("md-date");
        Span reward = new Span(formatMoney(d.reward()));
        reward.addClassName("md-reward");
        Div bottom = new Div(date, reward);
        bottom.addClassName("md-card-bottom");

        card.add(top, tags, tracker(d.status()), blocks, bottom);

        // Kuryer ma'lumoti (faqat kuryer biriktirilgan holatlarda)
        Optional<CourierInfoDto> courier = isCourierDriven(d.status())
                ? fetchCourier(d.id())
                : Optional.empty();
        courier.ifPresent(c -> card.add(courierBlock(c)));

        // Amallar: olinganini tasdiqlash, baholash, tahrirlash (CREATED), bekor qilish (CREATED yoki MATCHED)
        Div actions = new Div();
        actions.addClassName("md-actions");
        boolean hasActions = false;

        if (courier.isPresent() && awaitingSender(courier.get())) {
            NativeButton confirmPickup = new NativeButton("Yuk olinganini tasdiqlayman");
            confirmPickup.addClassNames("md-btn", "md-btn--solid");
            confirmPickup.getElement().setAttribute("type", "button");
            confirmPickup.addClickListener(e -> confirmPickup(d.id()));
            actions.add(confirmPickup);
            hasActions = true;
        }

        // Yetkazilgan yuk: faqat jo'natuvchi kuryerni baholaydi (har bir yuk uchun bir marta)
        if (d.status() == DeliveryStatus.DELIVERED && userId != null
                && !ratingService.hasRated(d.id(), userId)) {
            NativeButton rate = new NativeButton("Kuryerni baholash");
            rate.addClassNames("md-btn", "md-btn--solid");
            rate.getElement().setAttribute("type", "button");
            rate.addClickListener(e ->
                    new RatingDialog(ratingService, userId, d.id(), this::load).open());
            actions.add(rate);
            hasActions = true;
        }
        if (d.status() == DeliveryStatus.PICKED_UP && userId != null
                && deliveryService.awaitingDeliveryConfirmation(userId, d.id())) {
            NativeButton confirmDelivery = new NativeButton("Yetkazilganini tasdiqlayman");
            confirmDelivery.addClassNames("md-btn", "md-btn--solid");
            confirmDelivery.getElement().setAttribute("type", "button");
            confirmDelivery.addClickListener(e -> confirmDelivery(d.id()));
            actions.add(confirmDelivery);
            hasActions = true;
        }

        if (canEdit(d.status())) {
            NativeButton edit = new NativeButton("Tahrirlash");
            edit.addClassNames("md-btn", "md-btn--outline");
            edit.getElement().setAttribute("type", "button");
            edit.getElement().setAttribute("aria-label",
                    "Yukni tahrirlash: " + d.fromRegion().getLabel() + " → " + d.toRegion().getLabel());
            edit.addClickListener(e -> UI.getCurrent().navigate("deliveries/edit/" + d.id()));
            actions.add(edit);
            hasActions = true;
        }
        if (canCancel(d.status())) {
            NativeButton cancel = new NativeButton("Bekor qilish");
            cancel.addClassNames("md-btn", "md-btn--danger");
            cancel.getElement().setAttribute("type", "button");
            cancel.getElement().setAttribute("aria-label",
                    "Yukni bekor qilish: " + d.fromRegion().getLabel() + " → " + d.toRegion().getLabel());
            cancel.addClickListener(e -> confirmCancel(d));
            actions.add(cancel);
            hasActions = true;
        }
        if (hasActions) {
            card.add(actions);
        }
        return card;
    }

    private static boolean canEdit(DeliveryStatus status) {
        return status == DeliveryStatus.CREATED;
    }

    private static boolean canCancel(DeliveryStatus status) {
        return status == DeliveryStatus.CREATED || status == DeliveryStatus.MATCHED;
    }

    /** Kuryer ma'lumoti: ism, telefon, Telegram, transport. Tugmalar bu yerda emas. */
    private Div courierBlock(CourierInfoDto c) {
        Div box = new Div();
        box.addClassName("md-courier");

        Span heading = new Span("Kuryer");
        heading.addClassName("md-courier-title");

        box.add(heading, block("Ism", c.fullName()), block("Telefon", c.phone()));
        if (c.telegramUsername() != null && !c.telegramUsername().isBlank()) {
            box.add(block("Telegram", "@" + c.telegramUsername()));
        }
        box.add(block("Transport", vehicleText(c)));

        if (awaitingSender(c)) {
            Span notice = new Span("Kuryer yukni olganini bildirdi. Tasdiqlang.");
            notice.addClassName("md-pickup-notice");
            box.add(notice);
        }
        return box;
    }

    private static String vehicleText(CourierInfoDto c) {
        List<String> parts = new ArrayList<>();
        if (c.vehicleType() != null) {
            parts.add(c.vehicleType().getLabel());
        }
        addIfPresent(parts, c.vehicleBrand());
        addIfPresent(parts, c.vehicleModel());
        addIfPresent(parts, c.vehicleColor());
        addIfPresent(parts, c.plateNumber());
        return parts.isEmpty() ? "—" : String.join(" · ", parts);
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }

    // ---------------------------------------------------------------- bekor qilish

    private void confirmCancel(DeliverySummaryDto d) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                dialog.removeFromParent();
            }
        });

        Div content = new Div();
        content.addClassName("md-dialog");

        H2 heading = new H2("Yukni bekor qilasizmi?");
        heading.addClassName("md-dialog-title");

        String routeText = d.fromRegion().getLabel() + " → " + d.toRegion().getLabel();
        String body = routeText + " yo'nalishidagi yuk bekor qilinadi."
                + (d.status() == DeliveryStatus.MATCHED ? " Bu yukka kuryer allaqachon topilgan." : "")
                + " Bu amalni qaytarib bo'lmaydi.";
        Paragraph text = new Paragraph(body);
        text.addClassName("md-dialog-text");

        TextField reason = new TextField("Sababi (ixtiyoriy)");
        reason.setMaxLength(DeliveryService.MAX_CANCEL_REASON);
        reason.setWidthFull();
        reason.setPlaceholder("Masalan: fikrim o'zgardi");

        Div error = new Div();
        error.addClassName("md-dialog-error");
        error.getElement().setAttribute("role", "alert");
        error.setVisible(false);

        NativeButton confirm = new NativeButton("Ha, bekor qilish");
        confirm.addClassNames("md-btn", "md-btn--danger-solid");
        confirm.getElement().setAttribute("type", "button");

        NativeButton back = new NativeButton("Yo'q, qaytish");
        back.addClassNames("md-btn", "md-btn--outline");
        back.getElement().setAttribute("type", "button");
        back.addClickListener(e -> dialog.close());

        confirm.addClickListener(e -> {
            confirm.setEnabled(false);
            doCancel(d, reason.getValue(), dialog, error, confirm);
        });

        Div actions = new Div(confirm, back);
        actions.addClassName("md-dialog-actions");

        content.add(heading, text, reason, error, actions);
        dialog.add(content);
        dialog.open();
        back.getElement().callJsFunction("focus");
    }

    private void doCancel(DeliverySummaryDto d, String reason, Dialog dialog, Div error, NativeButton confirm) {
        if (userId == null) {
            error.setText("Sessiya tugagan. Iltimos, qaytadan kiring.");
            error.setVisible(true);
            confirm.setEnabled(true);
            return;
        }

        try {
            deliveryService.cancel(userId, d.id(), reason);
            dialog.close();
            Notification.show("Yuk bekor qilindi", 4000, Notification.Position.TOP_CENTER);
            load();
        } catch (BusinessException e) {
            error.setText(e.getMessage());
            error.setVisible(true);
            confirm.setEnabled(true);
            load(); // holat o'zgargan bo'lishi mumkin, ro'yxatni yangilaymiz
        } catch (RuntimeException e) {
            log.error("Yukni bekor qilishda xato", e);
            error.setText(GENERIC_ERROR);
            error.setVisible(true);
            confirm.setEnabled(true);
        }
    }

    // ---------------------------------------------------------------- bosqichlar

    /** Bosqichlar chizig'i. Faqat yukning haqiqiy holatidan chiqariladi. */
    private Component tracker(DeliveryStatus status) {
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
                    ? "Bu yuk bekor qilingan."
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

    private Span tag(String text) {
        Span t = new Span(text);
        t.addClassName("md-tag");
        return t;
    }

    private Div block(String label, String value) {
        Span l = new Span(label);
        l.addClassName("md-block-label");
        Span v = new Span(value == null || value.isBlank() ? "—" : value);
        v.addClassName("md-block-value");
        Div b = new Div(l, v);
        b.addClassName("md-block");
        return b;
    }

    private void showAlert(String message) {
        alert.setText(message);
        alert.setVisible(true);
    }

    // ---------------------------------------------------------------- matnlar

    private static String statusLabel(DeliveryStatus status) {
        return switch (status) {
            case CREATED -> "Kuryer qidirilmoqda";
            case MATCHED -> "Kuryer topildi";
            case PICKED_UP -> "Yo'lda";
            case DELIVERED -> "Yetkazildi";
            case CANCELLED -> "Bekor qilindi";
            case DISPUTED -> "Nizo ko'rib chiqilmoqda";
        };
    }

    private static String typeLabel(DeliveryType type) {
        return type == DeliveryType.INTRACITY ? "Shahar ichida" : "Viloyatlararo";
    }

    private static String sizeLabel(PackageSize size) {
        return switch (size) {
            case DOCUMENT -> "Hujjat";
            case SMALL -> "Kichik";
            case MEDIUM -> "O'rta";
            case LARGE -> "Katta";
        };
    }

    private static String weightText(BigDecimal weight) {
        return weight == null ? "—" : weight.stripTrailingZeros().toPlainString() + " kg";
    }

    private static String formatDate(LocalDateTime t) {
        return t == null ? "—" : DATE_FORMAT.format(t);
    }

    private static String formatMoney(BigDecimal amount) {
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
}