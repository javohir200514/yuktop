package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import uz.onetimecourier.one_time_courier.config.CurrentUser;
import uz.onetimecourier.one_time_courier.config.CustomUserDetails;
import uz.onetimecourier.one_time_courier.dto.TripDto;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.CourierTripService;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.sizeLabel;
import static uz.onetimecourier.one_time_courier.view.DeliveryViewSupport.tag;

/**
 * Kuryer safarlari ro'yxati. Tuzilishi va dizayni ochiq yuklar sahifasi bilan bir xil:
 * tepada yo'nalish filtri, pastda kartalar. Kuryer hisobida "+ Safar e'lon qilish" tugmasi ko'rinadi.
 */
@Route(value = "couriers", layout = PageLayout.class)
@RouteAlias(value = "trips", layout = PageLayout.class)
@PageTitle("Kuryerlar | YukBor")
@AnonymousAllowed
@CssImport("./styles/page-head.css")
@CssImport("./styles/deliveries-responsive.css")
@CssImport("./styles/couriers-page.css")
public class CouriersView extends Div {

    private static final Logger log = LoggerFactory.getLogger(CouriersView.class);

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static final String GENERIC_ERROR =
            "Ma'lumotni yuklashda xato yuz berdi. Birozdan keyin qayta urinib ko'ring.";

    private static final String BOX_SVG =
            "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" "
                    + "stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"
                    + "<path d=\"M21 8l-9-5-9 5 9 5 9-5z\"/><path d=\"M3 8v8l9 5 9-5V8\"/><path d=\"M12 13v8\"/>"
                    + "</svg>";

    private final CourierTripService tripService;
    private final boolean isCourier;

    private final Select<Region> fromFilter = new Select<>();
    private final Select<Region> toFilter = new Select<>();
    private final NativeButton clearButton = new NativeButton("Tozalash");
    private final NativeButton createButton = new NativeButton("+ Safar e'lon qilish");
    private final Div alert = new Div();
    private final Span totalLabel = new Span();
    private final Div listBox = new Div();
    private final Div pager = new Div();
    private final NativeButton prevButton = new NativeButton("‹ Oldingi");
    private final NativeButton nextButton = new NativeButton("Keyingi ›");
    private final Span pageLabel = new Span();

    private int page = 0;

    public CouriersView(CourierTripService tripService) {
        this.tripService = tripService;

        // Google (OAuth2) foydalanuvchisi bo'lsa ham xato bermaydi: u holda kuryer emas
        CustomUserDetails user = CurrentUser.details().orElse(null);
        this.isCourier = user != null && "COURIER".equals(String.valueOf(user.getRole()));

        addClassNames("md-page", "dm-page");

        // ---- sarlavha: sahifa nomi va qisqa yozuv
        H2 title = new H2("Kuryerlar");
        title.addClassName("dm-page-title");

        Paragraph subtitle = new Paragraph(
                "Yo'nalishingizga mos kuryer safarlarini toping va to'g'ridan-to'g'ri bog'laning.");
        subtitle.addClassName("dm-page-sub");

        Div head = new Div(title, subtitle);
        head.addClassName("dm-page-head");

        // ---- filtr paneli (ochiq yuklar bilan bir xil)
        configureFilters();
        Span filterArrow = new Span("⇄");
        filterArrow.addClassName("dm-filter-arrow");
        filterArrow.getElement().setAttribute("aria-hidden", "true");
        Div filters = new Div(fromFilter, filterArrow, toFilter, clearButton);
        filters.addClassName("dm-filter-bar");

        // ---- sarlavha qatori: jami soni va kuryer uchun tugma
        totalLabel.addClassName("dm-total");

        Div toolbar = new Div(totalLabel);
        toolbar.addClassName("tv-head");
        if (isCourier) {
            createButton.addClassNames("dm-btn", "dm-btn--solid", "tv-create");
            createButton.getElement().setAttribute("type", "button");
            createButton.addClickListener(e -> UI.getCurrent().navigate("trips/new"));
            toolbar.add(createButton);
        }

        alert.addClassName("dm-alert");
        alert.getElement().setAttribute("role", "alert");
        alert.setVisible(false);

        listBox.addClassName("dm-list");
        buildPager();

        add(head, filters, toolbar, alert, listBox, pager);
        load();
    }

    // ---------------------------------------------------------------- qurilish

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
        try {
            Page<TripDto> result = tripService.upcoming(
                    fromFilter.getValue(), toFilter.getValue(), PageRequest.of(page, PAGE_SIZE));

            if (result.getTotalPages() > 0 && page >= result.getTotalPages()) {
                page = result.getTotalPages() - 1;
                load();
                return;
            }
            render(result);
        } catch (RuntimeException e) {
            log.error("Safarlar ro'yxatini olishda xato", e);
            showAlert(GENERIC_ERROR);
        }
    }

    private void render(Page<TripDto> result) {
        listBox.removeAll();

        if (result.isEmpty()) {
            totalLabel.setText("");
            pager.setVisible(false);
            listBox.add(emptyState());
            return;
        }

        totalLabel.setText("Jami: " + result.getTotalElements() + " ta safar");
        result.getContent().forEach(t -> listBox.add(card(t)));

        int totalPages = result.getTotalPages();
        pager.setVisible(totalPages > 1);
        pageLabel.setText("Sahifa " + (page + 1) + " / " + totalPages);
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < totalPages);
    }

    private Div emptyState() {
        Div box = new Div();
        box.addClassName("dm-empty");

        boolean filtered = fromFilter.getValue() != null || toFilter.getValue() != null;
        H2 h = new H2(filtered ? "Bu yo'nalishda safar yo'q" : "Hozircha faol safar yo'q");
        h.addClassName("dm-empty-title");

        Paragraph p = new Paragraph(filtered
                ? "Boshqa viloyatni tanlang yoki filtrni tozalang."
                : (isCourier
                ? "Birinchi safaringizni e'lon qiling, jo'natuvchilar uni ko'radi."
                : "Yangi safar e'lonlari paydo bo'lganda, ro'yxat shu yerda ko'rinadi."));
        p.addClassName("dm-empty-text");

        box.add(h, p);
        return box;
    }

    // ---------------------------------------------------------------- kartochka

    private Div card(TripDto t) {
        Div card = new Div();
        card.addClassName("dm-card");

        Div icon = iconBox();

        Span from = new Span(t.fromRegion().getLabel());
        Span line = new Span();
        line.addClassName("dm-route-line");
        line.getElement().setAttribute("aria-hidden", "true");
        Span to = new Span(t.toRegion().getLabel());
        Div route = new Div(from, line, to);
        route.addClassName("dm-route");

        Div tags = new Div(tag("Sig'im: " + sizeLabel(t.maxPackageSize())));
        if (t.vehicleType() != null) {
            tags.add(tag(t.vehicleType().getLabel()));
        }
        if (!isBlank(t.vehicleBrand())) {
            tags.add(tag(t.vehicleBrand()));
        }
        if (!isBlank(t.vehicleColor())) {
            tags.add(tag(t.vehicleColor()));
        }
        tags.addClassName("dm-tags");

        Span date = new Span("Jo'nash: " + DATE_FORMAT.format(t.departureAt()));
        date.addClassName("dm-date");

        Div main = new Div(route, tags, date);
        main.addClassName("dm-card-main");

        String courierName = isBlank(t.courierName()) ? "Kuryer" : t.courierName();
        Span name = new Span(courierName);
        name.addClassName("tv-side-name");

        Component contact;
        if (!isBlank(t.courierPhone())) {
            NativeButton call = new NativeButton("Qo'ng'iroq qilish");
            call.addClassNames("dm-btn", "dm-btn--solid", "dm-deliver");
            call.getElement().setAttribute("type", "button");
            call.getElement().setAttribute("aria-label", "Kuryer raqamini ko'rsatish: " + courierName);
            call.addClickListener(e -> showPhone(courierName, t.courierPhone()));
            contact = call;
        } else {
            Span none = new Span("Telefon ko'rsatilmagan");
            none.addClassName("dm-date");
            contact = none;
        }

        Div side = new Div(name, contact);
        side.addClassName("dm-card-side");

        card.add(icon, main, side);

        if (!isBlank(t.note())) {
            Paragraph note = new Paragraph(t.note());
            note.addClassName("dm-extra");
            card.add(note);
        }
        return card;
    }

    /** Kuryer raqamini katta qilib ko'rsatadi. Nusxalash va qo'ng'iroq tugmalari bilan. */
    private void showPhone(String courierName, String phone) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(true);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                dialog.removeFromParent();
            }
        });

        H2 title = new H2(courierName);
        title.addClassName("tv-phone-title");

        Span number = new Span(phone);
        number.addClassName("tv-phone-number");

        Anchor call = new Anchor("tel:" + phone.replaceAll("[^+0-9]", ""), "Qo'ng'iroq qilish");
        call.addClassNames("dm-btn", "dm-btn--solid");

        NativeButton copy = new NativeButton("Nusxalash");
        copy.addClassNames("dm-btn", "dm-btn--ghost");
        copy.getElement().setAttribute("type", "button");
        copy.addClickListener(e -> {
            copy.getElement().executeJs("navigator.clipboard.writeText($0)", phone);
            Notification.show("Raqam nusxalandi", 2000, Notification.Position.TOP_CENTER);
        });

        NativeButton close = new NativeButton("Yopish");
        close.addClassNames("dm-btn", "dm-btn--ghost");
        close.getElement().setAttribute("type", "button");
        close.addClickListener(e -> dialog.close());

        Div actions = new Div(call, copy, close);
        actions.addClassName("tv-phone-actions");

        Div content = new Div(title, number, actions);
        content.addClassName("tv-phone-box");

        dialog.add(content);
        dialog.open();
    }

    /** Quti ikonkasi: oddiy Div, SVG ichida. Shu sababli klass doim karta elementiga tegadi. */
    private static Div iconBox() {
        Div box = new Div();
        box.addClassName("dm-card-icon");
        box.getElement().setAttribute("aria-hidden", "true");
        box.getElement().setProperty("innerHTML", BOX_SVG);
        return box;
    }

    private void showAlert(String message) {
        alert.setText(message);
        alert.setVisible(true);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}