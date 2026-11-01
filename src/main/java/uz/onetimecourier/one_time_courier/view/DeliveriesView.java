package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import uz.onetimecourier.one_time_courier.config.CurrentUser;
import uz.onetimecourier.one_time_courier.config.CustomUserDetails;
import uz.onetimecourier.one_time_courier.dto.CourierOpenDeliveryDto;
import uz.onetimecourier.one_time_courier.dto.DeliveryPublicDto;
import uz.onetimecourier.one_time_courier.enums.DeliveryStatus;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.exps.BusinessException;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.DeliveryFeedBroadcaster;
import uz.onetimecourier.one_time_courier.service.DeliveryService;

import java.util.ArrayList;
import java.util.List;

import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.formatDate;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.formatMoney;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.sizeLabel;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.tag;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.typeLabel;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.weightText;

/**
 * Ochiq yuklar ro'yxati. Kim ko'rayotganiga qarab ma'lumot turlicha:
 * kirmagan va jo'natuvchi: faqat ochiq maydonlar; kuryer: jo'natuvchi ismi, niqoblangan kontakt, "Qabul qilish".
 */
@Route(value = "deliveries", layout = PageLayout.class)
@PageTitle("Yuklar | YukBor")
@AnonymousAllowed
@CssImport("./styles/my-deliveries.css")
@CssImport("./styles/deliveries-feed.css")
@CssImport("./styles/deliveries-modern.css")
@CssImport("./styles/deliveries-responsive.css")
@CssImport("./styles/page-head.css")
public class DeliveriesView extends Div {

    private static final Logger log = LoggerFactory.getLogger(DeliveriesView.class);

    private static final int PAGE_SIZE = 10;
    private static final String GENERIC_ERROR =
            "Kutilmagan xato yuz berdi. Birozdan keyin qayta urinib ko'ring.";

    /** Karta chapidagi quti ikonkasi (inline SVG, tashqi kutubxonasiz). */
    private static final String BOX_ICON =
            "<span class=\"dm-card-icon\" aria-hidden=\"true\">"
                    + "<svg viewBox=\"0 0 24 24\" width=\"26\" height=\"26\" fill=\"none\" stroke=\"currentColor\" "
                    + "stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"
                    + "<path d=\"M21 8l-9-5-9 5 9 5 9-5z\"/><path d=\"M3 8v8l9 5 9-5V8\"/><path d=\"M12 13v8\"/>"
                    + "</svg></span>";

    private enum Viewer { ANONYMOUS, SENDER, COURIER, OTHER }

    private final DeliveryService deliveryService;
    private final DeliveryFeedBroadcaster feed;
    private final Viewer viewer;
    private final Long userId;

    private final Select<Region> fromFilter = new Select<>();
    private final Select<Region> toFilter = new Select<>();
    private final NativeButton clearButton = new NativeButton("Tozalash");
    private final Div alert = new Div();
    private final Div updateBanner = new Div();
    private final Span totalLabel = new Span();
    private final Div listBox = new Div();
    private final Div pager = new Div();
    private final NativeButton prevButton = new NativeButton("‹ Oldingi");
    private final NativeButton nextButton = new NativeButton("Keyingi ›");
    private final Span pageLabel = new Span();

    private int page = 0;
    private Registration feedRegistration;

    public DeliveriesView(DeliveryService deliveryService,
                          AuthenticationContext authContext,
                          DeliveryFeedBroadcaster feed) {
        this.deliveryService = deliveryService;
        this.feed = feed;

        // Google (OAuth2) foydalanuvchisi bo'lsa ham xato bermaydi: u holda user == null
        CustomUserDetails user = CurrentUser.details().orElse(null);
        this.userId = user == null ? null : user.getId();
        if (user != null) {
            String role = String.valueOf(user.getRole());
            this.viewer = "COURIER".equals(role) ? Viewer.COURIER
                    : "SENDER".equals(role) ? Viewer.SENDER : Viewer.OTHER;
        } else {
            this.viewer = authContext.isAuthenticated() ? Viewer.OTHER : Viewer.ANONYMOUS;
        }

        addClassNames("md-page", "dm-page");

        // ---- sahifa sarlavhasi (Kuryerlar sahifasidagi kabi)
        H2 title = new H2("Yuklar");
        title.addClassName("dm-page-title");
        Paragraph subtitle = new Paragraph("Siz shu yuklarni yetkazib berishingiz mumkin.");
        subtitle.addClassName("dm-page-sub");
        Div head = new Div(title, subtitle);
        head.addClassName("dm-page-head");
        add(head);

        // ---- filtr paneli
        configureFilters();
        Span filterArrow = new Span("⇄");
        filterArrow.addClassName("dm-filter-arrow");
        filterArrow.getElement().setAttribute("aria-hidden", "true");
        Div filters = new Div(fromFilter, filterArrow, toFilter, clearButton);
        filters.addClassName("dm-filter-bar");
        add(filters);

        /*// ---- faqat kirmagan foydalanuvchiga: kuryer bo'lib ro'yxatdan o'tishga chaqiriq
        if (viewer == Viewer.ANONYMOUS) {
            add(gate());
        }*/

        alert.addClassName("dm-alert");
        alert.getElement().setAttribute("role", "alert");
        alert.setVisible(false);

        buildUpdateBanner();

        totalLabel.addClassName("dm-total");
        listBox.addClassName("dm-list");
        buildPager();

        add(alert, updateBanner, totalLabel, listBox, pager);
        load();
    }

    // ---------------------------------------------------------------- jonli yangilanish

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (feedRegistration == null) {
            feedRegistration = feed.register(attachEvent.getUI(), () -> updateBanner.setVisible(true));
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

    private void buildUpdateBanner() {
        updateBanner.addClassName("dm-banner");
        updateBanner.getElement().setAttribute("role", "status");
        updateBanner.getElement().setAttribute("aria-live", "polite");
        updateBanner.setVisible(false);

        Span text = new Span("Ro'yxat yangilandi.");
        NativeButton refresh = new NativeButton("Yangilash");
        refresh.addClassNames("dm-btn", "dm-btn--ghost");
        refresh.getElement().setAttribute("type", "button");
        refresh.addClickListener(e -> load());
        updateBanner.add(text, refresh);
    }

    // ---------------------------------------------------------------- qurilish

    private Div gate() {
        Div gate = new Div();
        gate.addClassName("dm-gate");

        Span title = new Span("Jo'natuvchi ma'lumotlari yashirin");
        title.addClassName("dm-gate-title");
        Paragraph text = new Paragraph("Kuryer sifatida ro'yxatdan o'ting: jo'natuvchining ismi va aloqa "
                + "ma'lumotini ko'rasiz, yukni qabul qila olasiz.");
        text.addClassName("dm-gate-text");
        Div info = new Div(title, text);

        Anchor login = new Anchor("/login", "Kirish");
        login.addClassNames("dm-btn", "dm-btn--ghost");
        Anchor register = new Anchor("/register", "Ro'yxatdan o'tish");
        register.addClassNames("dm-btn", "dm-btn--solid");
        Div actions = new Div(login, register);
        actions.addClassName("dm-gate-actions");

        gate.add(info, actions);
        return gate;
    }

    private void configureFilters() {
        for (Select<Region> s : List.of(fromFilter, toFilter)) {
            s.setItems(Region.values());
            s.setItemLabelGenerator(r -> r == null ? "Hammasi" : r.getLabel());
            s.setEmptySelectionAllowed(true);
            s.setEmptySelectionCaption("Hammasi");
            s.setWidthFull();
            s.addValueChangeListener(e -> {
                page = 0;
                updateClearButton();
                load();
            });
        }
        fromFilter.setLabel("Qayerdan");
        toFilter.setLabel("Qayerga");

        clearButton.addClassNames("dm-btn", "dm-btn--solid", "dm-clear");
        clearButton.getElement().setAttribute("type", "button");
        clearButton.addClickListener(e -> {
            fromFilter.clear();
            toFilter.clear();
        });
        clearButton.setVisible(false);
    }

    private void updateClearButton() {
        clearButton.setVisible(fromFilter.getValue() != null || toFilter.getValue() != null);
    }

    private void buildPager() {
        pager.addClassName("dm-pager");

        prevButton.addClassName("dm-page-btn");
        prevButton.getElement().setAttribute("type", "button");
        prevButton.addClickListener(e -> {
            if (page > 0) {
                page--;
                load();
            }
        });

        nextButton.addClassName("dm-page-btn");
        nextButton.getElement().setAttribute("type", "button");
        nextButton.addClickListener(e -> {
            page++;
            load();
        });

        pageLabel.addClassName("dm-page-label");
        pageLabel.getElement().setAttribute("aria-live", "polite");

        pager.add(prevButton, pageLabel, nextButton);
        pager.setVisible(false);
    }

    // ---------------------------------------------------------------- yuklash

    private void load() {
        alert.setVisible(false);
        updateBanner.setVisible(false);

        try {
            PageRequest request = PageRequest.of(page, PAGE_SIZE);
            Region from = fromFilter.getValue();
            Region to = toFilter.getValue();

            long total;
            int totalPages;
            List<Component> cards = new ArrayList<>();

            if (viewer == Viewer.COURIER && userId != null) {
                Page<CourierOpenDeliveryDto> result = deliveryService.openForCourier(userId, from, to, request);
                if (fixPage(result)) {
                    return;
                }
                total = result.getTotalElements();
                totalPages = result.getTotalPages();
                result.getContent().forEach(d -> cards.add(courierCard(d)));
            } else {
                Page<DeliveryPublicDto> result = deliveryService.open(from, to, request);
                if (fixPage(result)) {
                    return;
                }
                total = result.getTotalElements();
                totalPages = result.getTotalPages();
                result.getContent().forEach(d -> cards.add(publicCard(d)));
            }

            render(total, totalPages, cards);
        } catch (BusinessException e) {
            showAlert(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Ochiq yuklar ro'yxatini olishda xato", e);
            showAlert(GENERIC_ERROR);
        }
    }

    /** Sahifa raqami oxirgisidan oshib ketgan bo'lsa, tuzatib qayta yuklaydi. */
    private boolean fixPage(Page<?> result) {
        if (result.getTotalPages() > 0 && page >= result.getTotalPages()) {
            page = result.getTotalPages() - 1;
            load();
            return true;
        }
        return false;
    }

    private void render(long total, int totalPages, List<Component> cards) {
        listBox.removeAll();

        if (cards.isEmpty()) {
            totalLabel.setText("");
            pager.setVisible(false);
            listBox.add(emptyState());
            return;
        }

        totalLabel.setText("Jami: " + total + " ta yuk");
        cards.forEach(listBox::add);

        pager.setVisible(totalPages > 1);
        pageLabel.setText("Sahifa " + (page + 1) + " / " + totalPages);
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < totalPages);
    }

    private Div emptyState() {
        Div box = new Div();
        box.addClassName("dm-empty");

        boolean filtered = fromFilter.getValue() != null || toFilter.getValue() != null;
        H2 h = new H2(filtered ? "Bu yo'nalishda yuk yo'q" : "Hozircha ochiq yuk yo'q");
        h.addClassName("dm-empty-title");
        Paragraph p = new Paragraph(filtered
                ? "Boshqa viloyatni tanlang yoki filtrni tozalang."
                : "Yangi yuklar paydo bo'lganda, ro'yxat shu yerda yangilanadi.");
        p.addClassName("dm-empty-text");
        box.add(h, p);
        return box;
    }

    // ---------------------------------------------------------------- kartochkalar

    private Div publicCard(DeliveryPublicDto d) {
        return card(d, null, this::promptCourierRegistration);
    }

    private Div courierCard(CourierOpenDeliveryDto d) {
        Span sender = new Span(d.senderName());
        Span contact = new Span(d.senderContactHint());
        Div row = new Div(contact, DeliveryViewSupport.verifyBadge(d.senderContactVerified()));
        row.addClassName("df-contact-row");

        Div info = new Div(
                DeliveryViewSupport.infoBlock("Jo'natuvchi", sender),
                DeliveryViewSupport.infoBlock("Aloqa", row));
        info.addClassName("df-contact");

        Paragraph hint = new Paragraph("Aniq manzillar, qabul qiluvchi va to'liq aloqa ma'lumoti "
                + "yukni qabul qilgach ochiladi.");
        hint.addClassName("df-hint");

        Div extra = new Div(info, hint);
        return card(d.delivery(), extra, () -> confirmAccept(d));
    }

    /** Uzun gorizontal karta: ikonka | yo'nalish, teglar, sana | status va narx. */
    private Div card(DeliveryPublicDto d, Div extra, Runnable onDeliver) {
        Div card = new Div();
        card.addClassName("dm-card");

        Html icon = new Html(BOX_ICON);

        Span from = new Span(d.fromRegion().getLabel());
        Span line = new Span();
        line.addClassName("dm-route-line");
        line.getElement().setAttribute("aria-hidden", "true");
        Span to = new Span(d.toRegion().getLabel());
        Div route = new Div(from, line, to);
        route.addClassName("dm-route");

        Div tags = new Div(
                tag(typeLabel(d.deliveryType())),
                tag(sizeLabel(d.packageSize())),
                tag(weightText(d.weightKg())));
        tags.addClassName("dm-tags");

        Span date = new Span("Joylangan: " + formatDate(d.createdAt()));
        date.addClassName("dm-date");

        Div main = new Div(route, tags, date);
        main.addClassName("dm-card-main");

        Component badge = DeliveryViewSupport.statusBadge(DeliveryStatus.CREATED);
        Span reward = new Span(formatMoney(d.reward()));
        reward.addClassName("dm-reward");
        NativeButton deliver = new NativeButton("Yetkazib berish");
        deliver.addClassNames("dm-btn", "dm-btn--solid", "dm-deliver");
        deliver.getElement().setAttribute("type", "button");
        deliver.getElement().setAttribute("aria-label", "Yukni yetkazib berish: "
                + d.fromRegion().getLabel() + " → " + d.toRegion().getLabel());
        deliver.addClickListener(e -> onDeliver.run());
        Div side = new Div(badge, reward, deliver);
        side.addClassName("dm-card-side");

        card.add(icon, main, side);
        if (extra != null) {
            extra.addClassName("dm-extra");
            card.add(extra);
        }
        return card;
    }

    // ---------------------------------------------------------------- qabul qilish

    private void confirmAccept(CourierOpenDeliveryDto d) {
        String route = d.delivery().fromRegion().getLabel() + " → " + d.delivery().toRegion().getLabel();
        String text = route + " yo'nalishidagi yuk (" + formatMoney(d.delivery().reward())
                + ") sizga biriktiriladi va aniq manzillar ochiladi.";

        DeliveryViewSupport.confirm("Yukni qabul qilasizmi?", text, "Ha, qabul qilaman", false, () -> {
            if (userId == null) {
                return "Sessiya tugagan. Iltimos, qaytadan kiring.";
            }
            try {
                deliveryService.accept(userId, d.delivery().id());
            } catch (BusinessException e) {
                load();
                return e.getMessage();
            } catch (RuntimeException e) {
                log.error("Yukni qabul qilishda xato", e);
                return GENERIC_ERROR;
            }
            Notification.show("Yuk qabul qilindi", 4000, Notification.Position.TOP_CENTER);
            UI.getCurrent().navigate("courier/deliveries");
            return null;
        });
    }

    /** Kuryer bo'lmaganlar (kirmagan yoki jo'natuvchi) uchun: xabar ko'rsatib, ro'yxatdan o'tishga yo'naltiradi. */
    private void promptCourierRegistration() {
        DeliveryViewSupport.confirm(
                "Avval kuryer sifatida ro'yxatdan o'ting",
                "Yukni yetkazib berish uchun kuryer hisobi kerak. Ro'yxatdan o'tish sahifasiga o'tasiz.",
                "Ro'yxatdan o'tish", false, () -> {
                    UI.getCurrent().getPage().setLocation("/register");
                    return null;
                });
    }

    private void showAlert(String message) {
        alert.setText(message);
        alert.setVisible(true);
    }
}