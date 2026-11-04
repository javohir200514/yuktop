package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.dependency.CssImport;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.formatDate;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.formatMoney;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.infoBlock;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.sizeLabel;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.tag;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.text;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.typeLabel;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.weightText;

/** Kuryer qabul qilgan yuklar: to'liq ma'lumot, "Olib ketdim" / "Yetkazdim" va jo'natuvchini baholash. */
@Route(value = "courier/deliveries", layout = PageLayout.class)
@PageTitle("Mening yetkazishlarim | YukBor")
@RolesAllowed("COURIER")
@CssImport("./styles/my-deliveries.css")
@CssImport("./styles/deliveries-feed.css")
public class MyCourierDeliveriesView extends Div {

    private static final Logger log = LoggerFactory.getLogger(MyCourierDeliveriesView.class);

    private static final int PAGE_SIZE = 10;
    private static final String GENERIC_ERROR =
            "Kutilmagan xato yuz berdi. Birozdan keyin qayta urinib ko'ring.";

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

    /** Foydalanuvchi ID'si view yaratilganda olinadi (push oqimida SecurityContext bo'lmasligi mumkin). */
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

        addClassName("md-page");

        H1 title = new H1("Mening yetkazishlarim");
        title.addClassName("md-title");
        Paragraph subtitle = new Paragraph("Siz qabul qilgan yuklar va ularning to'liq ma'lumoti.");
        subtitle.addClassName("md-subtitle");
        Div titleBox = new Div(title, subtitle);

        Anchor browse = new Anchor("/deliveries", "Ochiq yuklar");
        browse.addClassNames("md-primary", "df-link-btn");

        Div head = new Div(titleBox, browse);
        head.addClassName("md-head");

        alert.addClassName("md-alert");
        alert.getElement().setAttribute("role", "alert");
        alert.setVisible(false);

        totalLabel.addClassName("md-total");
        listBox.addClassName("md-list");
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
            // Jo'natuvchi tasdiqlasa, bekor qilsa yoki holat o'zgarsa, ro'yxat o'zi yangilanadi
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
        bar.addClassName("md-filters");
        bar.getElement().setAttribute("role", "group");
        bar.getElement().setAttribute("aria-label", "Holat bo'yicha filtr");

        for (Tab t : Tab.values()) {
            NativeButton b = new NativeButton(t.label);
            b.addClassName("md-filter");
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
            b.getClassNames().set("md-filter--active", active);
        });
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
        box.addClassName("md-empty");

        H2 h = new H2(currentTab == Tab.ACTIVE ? "Faol yuk yo'q" : "Yakunlangan yuk yo'q");
        h.addClassName("md-empty-title");
        Paragraph p = new Paragraph(currentTab == Tab.ACTIVE
                ? "Ochiq yuklardan birini qabul qiling, u shu yerda paydo bo'ladi."
                : "Yetkazilgan yoki bekor qilingan yuklar shu yerda ko'rinadi.");
        p.addClassName("md-empty-text");

        Anchor browse = new Anchor("/deliveries", "Ochiq yuklarga o'tish");
        browse.addClassNames("md-primary", "df-link-btn");

        box.add(h, p, browse);
        return box;
    }

    // ---------------------------------------------------------------- kartochka

    private Div card(CourierDeliveryDto d) {
        Div card = new Div();
        card.addClassNames("md-card", "md-card--" + d.status().name().toLowerCase());

        Span from = new Span(d.fromRegion().getLabel());
        Span arrow = new Span("→");
        arrow.addClassName("md-route-arrow");
        arrow.getElement().setAttribute("aria-hidden", "true");
        Span to = new Span(d.toRegion().getLabel());
        Div route = new Div(from, arrow, to);
        route.addClassName("md-route");

        Div top = new Div(route, DeliveryViewSupport.statusBadge(d.status()));
        top.addClassName("md-card-top");

        Div tags = new Div(
                tag(typeLabel(d.deliveryType())),
                tag(sizeLabel(d.packageSize())),
                tag(weightText(d.weightKg())));
        tags.addClassName("md-tags");

        Div blocks = new Div(
                senderBlock(d),
                infoBlock("Olib ketish", text(d.pickupAddress()),
                        DeliveryViewSupport.mapLink("Xaritada ko'rish", d.pickupLat(), d.pickupLng())),
                infoBlock("Yetkazish", text(d.dropoffAddress()),
                        DeliveryViewSupport.mapLink("Xaritada ko'rish", d.dropoffLat(), d.dropoffLng())),
                infoBlock("Qabul qiluvchi", text(d.recipientName()),
                        DeliveryViewSupport.telLink(d.recipientPhone())));
        blocks.addClassName("df-blocks");

        card.add(top, tags, DeliveryViewSupport.tracker(d.status(), "Jo'natuvchi bu yukni bekor qildi."), blocks);

        if (d.description() != null && !d.description().isBlank()) {
            Div desc = infoBlock("Tavsif", text(d.description()));
            desc.addClassName("df-desc");
            card.add(desc);
        }

        Span date = new Span("Qabul qilingan: " + formatDate(d.matchedAt()));
        date.addClassName("md-date");
        Span reward = new Span(formatMoney(d.reward()));
        reward.addClassName("md-reward");
        Div bottom = new Div(date, reward);
        bottom.addClassName("md-card-bottom");
        card.add(bottom);

        Div actions = new Div();
        actions.addClassName("md-actions");

        if (d.status() == DeliveryStatus.MATCHED) {
            if (d.courierPickupRequested()) {
                // So'rov allaqachon yuborilgan: tugma yo'q, faqat kutish holati
                Span wait = new Span("Olib ketganingizni bildirdingiz. Jo'natuvchi tasdiqlashini kuting.");
                wait.addClassName("md-pickup-wait");
                actions.add(wait);
            } else {
                NativeButton pickUp = new NativeButton("Olib ketdim");
                pickUp.addClassNames("md-btn", "md-btn--solid");
                pickUp.getElement().setAttribute("type", "button");
                pickUp.addClickListener(e -> confirmStep(d, true));
                actions.add(pickUp);
            }
        } else if (d.status() == DeliveryStatus.PICKED_UP) {
            NativeButton deliver = new NativeButton("Yetkazdim");
            deliver.addClassNames("md-btn", "md-btn--solid");
            deliver.getElement().setAttribute("type", "button");
            deliver.addClickListener(e -> confirmStep(d, false));
            actions.add(deliver);
        }

        // Yetkazilgan yuk: jo'natuvchini baholash (har bir yuk uchun bir marta)
        if (d.status() == DeliveryStatus.DELIVERED && userId != null
                && !ratingService.hasRated(d.id(), userId)) {
            NativeButton rate = new NativeButton("Jo'natuvchini baholash");
            rate.addClassNames("md-btn", "md-btn--ghost");
            rate.getElement().setAttribute("type", "button");
            rate.addClickListener(e ->
                    new RatingDialog(ratingService, userId, d.id(), this::load).open());
            actions.add(rate);
        }

        if (actions.getChildren().findAny().isPresent()) {
            card.add(actions);
        }
        return card;
    }

    /** Jo'natuvchi aloqa ma'lumoti. Telefon va Telegram yo'q bo'lsa, "Ko'rsatilmagan" chiqadi. */
    private Div senderBlock(CourierDeliveryDto d) {
        List<Component> items = new ArrayList<>();
        items.add(text(d.senderName()));

        Component phone = DeliveryViewSupport.telLink(d.senderContactPhone());
        Component telegram = DeliveryViewSupport.telegramLink(d.senderContactTelegram());
        if (phone != null) {
            items.add(phone);
        }
        if (telegram != null) {
            items.add(telegram);
        }
        if (phone == null && telegram == null) {
            items.add(text("Ko'rsatilmagan"));
        }

        Div block = infoBlock("Jo'natuvchi", items.toArray(Component[]::new));
        block.add(DeliveryViewSupport.verifyBadge(d.senderContactVerified()));
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
            // Ro'yxat bu yerda qayta yuklanmaydi: o'zgarishni DeliveryFeedBroadcaster
            // push orqali yetkazadi. Ikki marta yuklash brauzerdagi DOM ni buzadi.
            return null;
        });
    }

    private void showAlert(String message) {
        alert.setText(message);
        alert.setVisible(true);
    }
}