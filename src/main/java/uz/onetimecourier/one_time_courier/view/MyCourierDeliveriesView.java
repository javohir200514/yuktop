package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
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
import uz.onetimecourier.one_time_courier.dto.CourierDeliveryDto;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.DeliveryFeedBroadcaster;
import uz.onetimecourier.one_time_courier.service.DeliveryService;
import uz.onetimecourier.one_time_courier.service.RatingService;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.formatDate;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.formatMoney;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.sizeLabel;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.typeLabel;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.weightText;

/**
 * Kuryer qabul qilgan yuklar. Kartada faqat qisqa ma'lumot (holat, yo'nalish, yuk turi, narx),
 * to'liq ma'lumot "Batafsil" oynasida. Dizayn my-deliveries-corporate.css va my-deliveries-mobile.css da.
 */
@Route(value = "courier/deliveries", layout = PageLayout.class)
@PageTitle("Mening yetkazishlarim | YukBor")
@RolesAllowed("COURIER")
@CssImport("./styles/my-deliveries.css")
@CssImport("./styles/deliveries-feed.css")
@CssImport("./styles/my-deliveries-corporate.css")
@CssImport("./styles/my-deliveries-mobile.css")
public class MyCourierDeliveriesView extends Div {

    private static final Logger log = LoggerFactory.getLogger(MyCourierDeliveriesView.class);

    private static final int PAGE_SIZE = 10;
    private static final String GENERIC_ERROR =
            "Kutilmagan xato yuz berdi. Birozdan keyin qayta urinib ko'ring.";

    private static final String[] STEP_LABELS = {"Joylandi", "Kuryer topildi", "Yo'lda", "Yetkazildi"};

    private enum Tab {
        ACTIVE("Faol", Set.of(DeliveryStatus.MATCHED, DeliveryStatus.PICKED_UP)),
        DONE("Yakunlangan", Set.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED, DeliveryStatus.DISPUTED));

        private final String label;
        private final Set<DeliveryStatus> statuses;

        Tab(String label, Set<DeliveryStatus> statuses) {
            this.label = label;
            this.statuses = statuses;
        }
    }

    private final DeliveryService deliveryService;
    private final DeliveryFeedBroadcaster feed;
    private final RatingService ratingService;
    private final Long userId;

    private final Map<Tab, NativeButton> tabButtons = new EnumMap<>(Tab.class);
    private final Div alert = new Div();
    private final Span totalLabel = new Span();
    private final Div listBox = new Div();
    private final Div pager = new Div();
    private final NativeButton prevButton = new NativeButton("‹ Oldingi");
    private final NativeButton nextButton = new NativeButton("Keyingi ›");
    private final Span pageLabel = new Span();

    private Tab currentTab = Tab.ACTIVE;
    private int page = 0;
    private Registration feedRegistration;

    public MyCourierDeliveriesView(DeliveryService deliveryService,
                                   AuthenticationContext authContext,
                                   DeliveryFeedBroadcaster feed,
                                   RatingService ratingService) {
        this.deliveryService = deliveryService;
        this.feed = feed;
        this.ratingService = ratingService;
        this.userId = authContext.getAuthenticatedUser(CustomUserDetails.class)
                .map(CustomUserDetails::getId)
                .orElse(null);

        addClassName("mdv-page");

        Span kicker = new Span("Kuryer paneli");
        kicker.addClassName("mdv-kicker");

        H1 title = new H1("Mening yetkazishlarim");
        title.addClassName("mdv-title");
        Paragraph subtitle = new Paragraph("Siz qabul qilgan yuklar va ularning to'liq ma'lumoti.");
        subtitle.addClassName("mdv-subtitle");
        Div titleBox = new Div(kicker, title, subtitle);
        titleBox.addClassName("mdv-title-box");

        Anchor browse = new Anchor("/deliveries", "Ochiq yuklar");
        browse.addClassName("mdv-primary");

        Div head = new Div(titleBox, browse);
        head.addClassName("mdv-head");

        alert.addClassName("mdv-alert");
        alert.getElement().setAttribute("role", "alert");
        alert.setVisible(false);

        totalLabel.addClassName("mdv-total");
        listBox.addClassName("mdv-list");
        buildPager();

        add(head, buildTabs(), alert, totalLabel, listBox, pager);
        updateTabButtons();
        load();
    }

    // ---------------------------------------------------------------- jonli yangilanish

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (feedRegistration == null) {
            feedRegistration = feed.register(attachEvent.getUI(), this::load);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (feedRegistration != null) {
            feedRegistration.remove();
            feedRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    // ---------------------------------------------------------------- qurilish

    private Div buildTabs() {
        Div bar = new Div();
        bar.addClassName("mdv-filters");
        bar.getElement().setAttribute("role", "group");
        bar.getElement().setAttribute("aria-label", "Holat bo'yicha filtr");

        for (Tab t : Tab.values()) {
            NativeButton b = new NativeButton(t.label);
            b.addClassName("mdv-filter");
            b.getElement().setAttribute("type", "button");
            b.addClickListener(e -> {
                currentTab = t;
                page = 0;
                updateTabButtons();
                load();
            });
            tabButtons.put(t, b);
            bar.add(b);
        }
        return bar;
    }

    private void updateTabButtons() {
        tabButtons.forEach((t, b) -> {
            boolean active = t == currentTab;
            b.getElement().setAttribute("aria-pressed", String.valueOf(active));
            b.getClassNames().set("mdv-filter--active", active);
        });
    }

    private void buildPager() {
        pager.addClassName("mdv-pager");

        prevButton.addClassName("mdv-page-btn");
        prevButton.getElement().setAttribute("type", "button");
        prevButton.addClickListener(e -> {
            if (page > 0) {
                page--;
                load();
            }
        });

        nextButton.addClassName("mdv-page-btn");
        nextButton.getElement().setAttribute("type", "button");
        nextButton.addClickListener(e -> {
            page++;
            load();
        });

        pageLabel.addClassName("mdv-page-label");
        pageLabel.getElement().setAttribute("aria-live", "polite");

        pager.add(prevButton, pageLabel, nextButton);
        pager.setVisible(false);
    }

    // ---------------------------------------------------------------- yuklash

    private void load() {
        alert.setVisible(false);

        if (userId == null) {
            showAlert("Sessiya tugagan. Iltimos, qaytadan kiring.");
            return;
        }

        try {
            Page<CourierDeliveryDto> result = deliveryService.courierDeliveries(
                    userId, currentTab.statuses, PageRequest.of(page, PAGE_SIZE));

            if (result.getTotalPages() > 0 && page >= result.getTotalPages()) {
                page = result.getTotalPages() - 1;
                load();
                return;
            }
            render(result);
        } catch (BusinessException e) {
            showAlert(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Kuryer yuklarini olishda xato", e);
            showAlert(GENERIC_ERROR);
        }
    }

    private void render(Page<CourierDeliveryDto> result) {
        listBox.removeAll();

        if (result.isEmpty()) {
            totalLabel.setText("");
            pager.setVisible(false);
            listBox.add(emptyState());
            return;
        }

        totalLabel.setText("Jami: " + result.getTotalElements() + " ta yuk");
        result.getContent().forEach(d -> listBox.add(card(d)));

        int totalPages = result.getTotalPages();
        pager.setVisible(totalPages > 1);
        pageLabel.setText("Sahifa " + (page + 1) + " / " + totalPages);
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < totalPages);
    }

    private Div emptyState() {
        Div box = new Div();
        box.addClassName("mdv-empty");

        H2 h = new H2(currentTab == Tab.ACTIVE ? "Faol yuk yo'q" : "Yakunlangan yuk yo'q");
        h.addClassName("mdv-empty-title");
        Paragraph p = new Paragraph(currentTab == Tab.ACTIVE
                ? "Ochiq yuklardan birini qabul qiling, u shu yerda paydo bo'ladi."
                : "Yetkazilgan yoki bekor qilingan yuklar shu yerda ko'rinadi.");
        p.addClassName("mdv-empty-text");

        Anchor browse = new Anchor("/deliveries", "Ochiq yuklarga o'tish");
        browse.addClassName("mdv-primary");

        box.add(h, p, browse);
        return box;
    }

    // ---------------------------------------------------------------- kartochka (qisqa)

    private Div card(CourierDeliveryDto d) {
        Div card = new Div();
        card.addClassNames("mdv-card", "mdv-card--" + d.status().name().toLowerCase());

        Div top = new Div(route(d), statusBadge(d.status()));
        top.addClassName("mdv-card-top");

        Div tags = new Div(
                tag(typeLabel(d.deliveryType())),
                tag("Yuk: " + sizeLabel(d.packageSize())),
                tag(weightText(d.weightKg())));
        tags.addClassName("mdv-tags");

        Span date = new Span("Qabul qilingan: " + formatDate(d.matchedAt()));
        date.addClassName("mdv-date");
        Span reward = new Span(formatMoney(d.reward()));
        reward.addClassName("mdv-reward");
        Div bottom = new Div(date, reward);
        bottom.addClassName("mdv-card-bottom");

        card.add(top, tags, bottom);

        Div actions = new Div();
        actions.addClassName("mdv-actions");

        if (d.status() == DeliveryStatus.MATCHED) {
            if (d.courierPickupRequested()) {
                Span wait = new Span("Olib ketganingizni bildirdingiz. Jo'natuvchi tasdiqlashini kuting.");
                wait.addClassName("mdv-pickup-wait");
                actions.add(wait);
            } else {
                NativeButton pickUp = new NativeButton("Olib ketdim");
                pickUp.addClassNames("mdv-btn", "mdv-btn--solid");
                pickUp.getElement().setAttribute("type", "button");
                pickUp.addClickListener(e -> confirmStep(d, true));
                actions.add(pickUp);
            }
        } else if (d.status() == DeliveryStatus.PICKED_UP) {
            NativeButton deliver = new NativeButton("Yetkazdim");
            deliver.addClassNames("mdv-btn", "mdv-btn--solid");
            deliver.getElement().setAttribute("type", "button");
            deliver.addClickListener(e -> confirmStep(d, false));
            actions.add(deliver);
        }

        NativeButton details = new NativeButton("Batafsil ›");
        details.addClassNames("mdv-btn", "mdv-btn--outline");
        details.getElement().setAttribute("type", "button");
        details.getElement().setAttribute("aria-label",
                "Batafsil: " + d.fromRegion().getLabel() + " → " + d.toRegion().getLabel());
        details.addClickListener(e -> openDetails(d));
        actions.add(details);

        card.add(actions);
        return card;
    }

    private Div route(CourierDeliveryDto d) {
        Span from = new Span(d.fromRegion().getLabel());
        Span arrow = new Span("→");
        arrow.addClassName("mdv-route-arrow");
        arrow.getElement().setAttribute("aria-hidden", "true");
        Span to = new Span(d.toRegion().getLabel());
        Div route = new Div(from, arrow, to);
        route.addClassName("mdv-route");
        return route;
    }

    // ---------------------------------------------------------------- batafsil oynasi

    private void openDetails(CourierDeliveryDto d) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(true);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                dialog.removeFromParent();
            }
        });

        Div content = new Div();
        content.addClassNames("mdv-dialog", "mdv-details");

        H2 heading = new H2(d.fromRegion().getLabel() + " → " + d.toRegion().getLabel());
        heading.addClassName("mdv-dialog-title");

        Div statusRow = new Div(statusBadge(d.status()));
        statusRow.addClassName("mdv-details-status");

        Div blocks = new Div(
                senderBlock(d),
                block("Olib ketish", plain(d.pickupAddress()), mapLink(d.pickupLat(), d.pickupLng())),
                block("Yetkazish", plain(d.dropoffAddress()), mapLink(d.dropoffLat(), d.dropoffLng())),
                block("Qabul qiluvchi", plain(d.recipientName()), telLink(d.recipientPhone())),
                block("Yuk", plain(typeLabel(d.deliveryType()) + " · " + sizeLabel(d.packageSize())
                        + " · " + weightText(d.weightKg()))),
                block("Mukofot", plain(formatMoney(d.reward()))));
        blocks.addClassName("mdv-blocks");

        content.add(heading, statusRow, tracker(d.status()), blocks);

        if (d.description() != null && !d.description().isBlank()) {
            Div desc = block("Tavsif", plain(d.description()));
            desc.addClassName("mdv-desc");
            content.add(desc);
        }

        Span date = new Span("Qabul qilingan: " + formatDate(d.matchedAt()));
        date.addClassName("mdv-date");
        content.add(date);

        NativeButton close = new NativeButton("Yopish");
        close.addClassNames("mdv-btn", "mdv-btn--solid");
        close.getElement().setAttribute("type", "button");
        close.addClickListener(e -> dialog.close());
        Div actions = new Div(close);
        actions.addClassNames("mdv-actions", "mdv-details-actions");
        content.add(actions);

        dialog.add(content);
        dialog.open();
    }

    private Div senderBlock(CourierDeliveryDto d) {
        Component phone = telLink(d.senderContactPhone());
        Component telegram = telegramLink(d.senderContactTelegram());

        Div block = block("Jo'natuvchi", plain(d.senderName()));

        Div links = new Div();
        links.addClassName("mdv-block-links");
        if (phone != null) {
            links.add(phone);
        }
        if (telegram != null) {
            links.add(telegram);
        }
        if (phone == null && telegram == null) {
            links.add(plain("Ko'rsatilmagan"));
        }
        block.add(links);

        boolean verified = Boolean.TRUE.equals(d.senderContactVerified());
        Span verify = new Span(verified ? "Tasdiqlangan" : "Tasdiqlanmagan");
        verify.addClassNames("mdv-tag", "mdv-verify", verified ? "mdv-verify--yes" : "mdv-verify--no");
        block.add(verify);
        return block;
    }

    // ---------------------------------------------------------------- amallar

    private void confirmStep(CourierDeliveryDto d, boolean pickUp) {
        String route = d.fromRegion().getLabel() + " → " + d.toRegion().getLabel();
        String title = pickUp ? "Yukni olib ketdingizmi?" : "Yukni yetkazdingizmi?";
        String body = route + (pickUp
                ? " yo'nalishidagi yuk uchun jo'natuvchiga bildirishnoma yuboriladi. "
                + "Jo'natuvchi tasdiqlagach yuk «Yo'lda» holatiga o'tadi."
                : " yo'nalishidagi yuk «Yetkazildi» holatiga o'tadi. Buni qaytarib bo'lmaydi.");

        DeliveryViewSupport.confirm(title, body, pickUp ? "Ha, olib ketdim" : "Ha, yetkazdim", false, () -> {
            if (userId == null) {
                return "Sessiya tugagan. Iltimos, qaytadan kiring.";
            }
            try {
                if (pickUp) {
                    deliveryService.pickUp(userId, d.id());
                } else {
                    deliveryService.deliver(userId, d.id());
                }
            } catch (BusinessException e) {
                load();
                return e.getMessage();
            } catch (RuntimeException e) {
                log.error("Yuk holatini o'zgartirishda xato", e);
                return GENERIC_ERROR;
            }
            Notification.show(pickUp
                            ? "Jo'natuvchiga bildirildi. U tasdiqlashini kuting"
                            : "Yuk yetkazildi",
                    4000, Notification.Position.TOP_CENTER);
            // Ro'yxatni DeliveryFeedBroadcaster push orqali yangilaydi.
            return null;
        });
    }

    private void showAlert(String message) {
        alert.setText(message);
        alert.setVisible(true);
    }

    // ---------------------------------------------------------------- bosqichlar va belgilar

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
            note.addClassNames("mdv-note", "mdv-note--" + status.name().toLowerCase());
            note.setText(status == DeliveryStatus.CANCELLED
                    ? "Jo'natuvchi bu yukni bekor qildi."
                    : "Bu yuk bo'yicha nizo ko'rib chiqilmoqda.");
            return note;
        }

        Div track = new Div();
        track.addClassName("mdv-track");
        track.getElement().setAttribute("role", "list");
        track.getElement().setAttribute("aria-label",
                "Yetkazish bosqichlari. Hozirgi holat: " + STEP_LABELS[current]);

        for (int i = 0; i < STEP_LABELS.length; i++) {
            Div step = new Div(new Span(STEP_LABELS[i]));
            step.addClassName("mdv-step");
            step.getElement().setAttribute("role", "listitem");
            if (i <= current) {
                step.addClassName("mdv-step--done");
            }
            if (i == current) {
                step.addClassName("mdv-step--current");
                step.getElement().setAttribute("aria-current", "step");
            }
            track.add(step);
        }
        return track;
    }

    private static Span statusBadge(DeliveryStatus status) {
        Span dot = new Span();
        dot.addClassName("mdv-dot");
        dot.getElement().setAttribute("aria-hidden", "true");
        Span badge = new Span(dot, new Span(statusLabel(status)));
        badge.addClassNames("mdv-badge", "mdv-badge--" + status.name().toLowerCase());
        return badge;
    }

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

    // ---------------------------------------------------------------- kichik elementlar

    private static Span tag(String text) {
        Span t = new Span(text);
        t.addClassName("mdv-tag");
        return t;
    }

    private static Span plain(String value) {
        Span s = new Span(value == null || value.isBlank() ? "—" : value);
        s.addClassName("mdv-block-value");
        return s;
    }

    private static Div block(String label, Component... values) {
        Span l = new Span(label);
        l.addClassName("mdv-block-label");
        Div b = new Div(l);
        b.addClassName("mdv-block");
        for (Component v : values) {
            if (v != null) {
                b.add(v);
            }
        }
        return b;
    }

    private static Component mapLink(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }
        Anchor a = new Anchor(String.format(Locale.ROOT,
                "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f",
                lat, lng, lat, lng), "Xaritada ko'rish");
        a.setTarget("_blank");
        a.getElement().setAttribute("rel", "noopener noreferrer");
        a.addClassName("mdv-link");
        return a;
    }

    private static Component telLink(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        Anchor a = new Anchor("tel:" + phone.replaceAll("[^+0-9]", ""), phone.strip());
        a.addClassName("mdv-link");
        return a;
    }

    private static Component telegramLink(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String clean = username.strip();
        if (clean.startsWith("@")) {
            clean = clean.substring(1);
        }
        if (clean.isEmpty()) {
            return null;
        }
        Anchor a = new Anchor("https://t.me/" + clean, "@" + clean);
        a.setTarget("_blank");
        a.getElement().setAttribute("rel", "noopener noreferrer");
        a.addClassName("mdv-link");
        return a;
    }
}