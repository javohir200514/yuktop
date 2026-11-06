package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.onetimecourier.one_time_courier.dto.TopCourierDto;
import uz.onetimecourier.one_time_courier.layout.PageLayout;
import uz.onetimecourier.one_time_courier.service.RatingService;

import java.util.List;
import java.util.Locale;

/**
 * Eng ishonchli kuryerlar: sarlavha, statistika, top-3 podium, ro'yxat va qanday ishlashi.
 * Sahifa ochiq turganda ma'lumot davriy yangilanadi.
 */
@Route(value = "reyting/kuryerlar", layout = PageLayout.class)
@PageTitle("Eng ishonchli kuryerlar | YukBor")
@AnonymousAllowed
@CssImport("./styles/top-couriers.css")
public class TopCouriersView extends Div {

    private static final Logger log = LoggerFactory.getLogger(TopCouriersView.class);

    private static final int LIMIT = 20;
    private static final int POLL_MS = 10_000;

    private static final String[] MEDALS = {"1", "2", "3"};
    private static final String[] TONES = {"gold", "silver", "bronze"};

    /**
     * Ikonkalar: ingichka kontur (currentColor) + yengil to'ldirma (.f).
     * Tashqi kutubxonasiz, rangni CSS boshqaradi.
     */
    private static final String ICON_TRUCK = """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M1 6h13v10H1z"/>
              <g fill="none" stroke="currentColor" stroke-width="1.9"
                 stroke-linecap="round" stroke-linejoin="round">
                <rect x="1" y="6" width="13" height="10" rx="1"/>
                <path d="M14 9h4l3 3v4h-7"/>
                <circle cx="5.5" cy="17.5" r="2" fill="#fff"/>
                <circle cx="17.5" cy="17.5" r="2" fill="#fff"/>
              </g>
            </svg>
            """;

    private static final String ICON_STAR = """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5-4.8-4.6 6.6-.9z"/>
              <path d="M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5-4.8-4.6 6.6-.9z"
                    fill="none" stroke="currentColor" stroke-width="1.9" stroke-linejoin="round"/>
            </svg>
            """;

    private static final String ICON_TROPHY = """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M7 4h10v5a5 5 0 0 1-10 0z"/>
              <g fill="none" stroke="currentColor" stroke-width="1.9"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="M8 21h8M12 17v4"/>
                <path d="M7 4h10v5a5 5 0 0 1-10 0z"/>
                <path d="M7 6H4v2a3 3 0 0 0 3 3M17 6h3v2a3 3 0 0 1-3 3"/>
              </g>
            </svg>
            """;

    private final RatingService service;

    /** Ro'yxat shu konteynerda qayta chiziladi. Footer bir marta qo'shiladi. */
    private final Div content = new Div();

    /** Oxirgi chizilgan ma'lumot. O'zgarmagan bo'lsa qayta chizilmaydi. */
    private List<TopCourierDto> shown;

    private Registration pollRegistration;

    public TopCouriersView(RatingService service) {
        this.service = service;
        addClassName("tc-page");
        content.addClassName("tc-content");
        add(content, new SiteFooter());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        load();

        UI ui = attachEvent.getUI();
        ui.setPollInterval(POLL_MS);
        pollRegistration = ui.addPollListener(e -> load());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        detachEvent.getUI().setPollInterval(-1);
        super.onDetach(detachEvent);
    }

    // ---------------------------------------------------------------- yuklash va chizish

    private void load() {
        List<TopCourierDto> list;
        try {
            list = service.topCouriers(LIMIT);
        } catch (RuntimeException e) {
            log.warn("Ishonchli kuryerlar ro'yxatini yuklashda xato: {}", e.getMessage());
            return;
        }
        if (list.equals(shown)) {
            return;
        }
        shown = list;
        render(list);
    }

    private void render(List<TopCourierDto> list) {
        content.removeAll();
        content.add(hero(list));

        if (list.isEmpty()) {
            content.add(emptyState());
        } else {
            int topCount = Math.min(3, list.size());

            Div podium = new Div();
            podium.addClassNames("tc-podium", "tc-podium--" + topCount);
            for (int i = 0; i < topCount; i++) {
                podium.add(podiumCard(i, list.get(i)));
            }
            content.add(podium);

            if (list.size() > 3) {
                Span section = new Span("Boshqa kuryerlar");
                section.addClassName("tc-section-title");

                Div rows = new Div();
                rows.addClassName("tc-list");
                for (int i = 3; i < list.size(); i++) {
                    rows.add(row(i + 1, list.get(i)));
                }
                content.add(section, rows);
            }
        }

        content.add(howItWorks());
    }

    // ---------------------------------------------------------------- sarlavha va statistika

    private Div hero(List<TopCourierDto> list) {
        Div hero = new Div();
        hero.addClassName("tc-hero");

        Span kicker = new Span("Reyting");
        kicker.addClassName("tc-hero-kicker");

        H1 title = new H1("Eng ishonchli kuryerlar");
        title.addClassName("tc-hero-title");

        Paragraph sub = new Paragraph("Mijozlar bahosi bo'yicha saralangan. Kamida "
                + RatingService.MIN_RATINGS_FOR_TOP + " ta baho olgan kuryerlar ko'rsatiladi.");
        sub.addClassName("tc-hero-sub");

        double avg = list.stream().mapToDouble(TopCourierDto::avgStars).average().orElse(0);
        long delivered = list.stream().mapToLong(TopCourierDto::delivered).sum();

        Div stats = new Div(
                stat(String.valueOf(list.size()), "Reytingdagi kuryer"),
                stat(list.isEmpty() ? "—" : String.format(Locale.US, "%.1f", avg), "O'rtacha baho"),
                stat(String.valueOf(delivered), "Yetkazilgan yuk"));
        stats.addClassName("tc-stats");

        hero.add(kicker, title, sub, stats);
        return hero;
    }

    private Div stat(String value, String label) {
        Span v = new Span(value);
        v.addClassName("tc-stat-value");
        Span l = new Span(label);
        l.addClassName("tc-stat-label");

        Div box = new Div(v, l);
        box.addClassName("tc-stat");
        return box;
    }

    // ---------------------------------------------------------------- podium (top 3)

    private Div podiumCard(int index, TopCourierDto c) {
        Div card = new Div();
        card.addClassNames("tc-card", "tc-podium-card");
        if (index == 0) {
            card.addClassName("tc-podium-card--first");
        }

        Span medal = new Span(MEDALS[index]);
        medal.addClassNames("tc-medal", "tc-medal--" + TONES[index]);

        String name = maskName(c.name());
        Div avatar = avatar(name, TONES[index]);
        avatar.addClassName("tc-avatar--lg");

        Span nameSpan = new Span(name);
        nameSpan.addClassName("tc-name");

        Span vehicle = new Span(vehicleText(c));
        vehicle.addClassName("tc-vehicle");

        Span stars = stars(c.avgStars());
        stars.addClassName("tc-stars--lg");

        Span meta = new Span(c.ratingCount() + " ta baho · " + c.delivered() + " ta yuk");
        meta.addClassName("tc-meta");

        card.add(medal, avatar, nameSpan, vehicle, stars, meta);
        return card;
    }

    // ---------------------------------------------------------------- ro'yxat qatori

    private Div row(int rank, TopCourierDto c) {
        Div row = new Div();
        row.addClassNames("tc-card", "tc-row");

        Span rankSpan = new Span("#" + rank);
        rankSpan.addClassName("tc-rank");

        String name = maskName(c.name());
        Div avatar = avatar(name, "plain");

        Span nameSpan = new Span(name);
        nameSpan.addClassName("tc-row-name");

        Span vehicle = new Span(vehicleText(c));
        vehicle.addClassName("tc-chip");

        Div main = new Div(nameSpan, vehicle);
        main.addClassName("tc-row-main");

        Span stars = stars(c.avgStars());

        Div bar = new Div();
        bar.addClassName("tc-bar");
        Div fill = new Div();
        fill.addClassName("tc-bar-fill");
        double pct = Math.max(0, Math.min(100, c.avgStars() / 5.0 * 100));
        fill.getStyle().set("width", String.format(Locale.US, "%.0f%%", pct));
        bar.add(fill);

        Span meta = new Span(c.ratingCount() + " baho · " + c.delivered() + " yuk");
        meta.addClassName("tc-meta");

        Div stats = new Div(stars, bar, meta);
        stats.addClassName("tc-row-stats");

        row.add(rankSpan, avatar, main, stats);
        return row;
    }

    // ---------------------------------------------------------------- bo'sh holat

    private Div emptyState() {
        Div icon = iconBox(ICON_TROPHY, "tc-empty-icon");

        Span title = new Span("Hozircha reyting yo'q");
        title.addClassName("tc-empty-title");

        Span text = new Span("Yetkazishlar yakunlanib, kamida "
                + RatingService.MIN_RATINGS_FOR_TOP
                + " ta baho to'plangach, eng ishonchli kuryerlar shu yerda ko'rinadi.");
        text.addClassName("tc-empty-text");

        Div box = new Div(icon, title, text);
        box.addClassNames("tc-card", "tc-empty");
        return box;
    }

    // ---------------------------------------------------------------- qanday ishlaydi

    private Div howItWorks() {
        Div box = new Div();
        box.addClassName("tc-steps");

        Span kicker = new Span("Qanday ishlaydi");
        kicker.addClassName("tc-steps-kicker");

        H2 title = new H2("Reytingga 3 qadamda chiqing");
        title.addClassName("tc-steps-title");

        Div grid = new Div(
                step(1, "blue", ICON_TRUCK, "Yuk yetkazing",
                        "Kuryer sifatida yuk qabul qilib, manzilga yetkazing."),
                step(2, "amber", ICON_STAR, "Baho oling",
                        "Yetkazishdan so'ng jo'natuvchi sizni 1 dan 5 gacha yulduz bilan baholaydi."),
                step(3, "green", ICON_TROPHY, "Top ro'yxatga chiqing",
                        "Kamida " + RatingService.MIN_RATINGS_FOR_TOP
                                + " ta baho to'plansangiz, reytingda ko'rinasiz."));
        grid.addClassName("tc-steps-grid");

        box.add(kicker, title, grid);
        return box;
    }

    private Div step(int number, String tone, String svg, String heading, String text) {
        Div icon = iconBox(svg, "tc-step-icon");

        Span num = new Span(String.valueOf(number));
        num.addClassName("tc-step-num");

        Div top = new Div(icon, num);
        top.addClassName("tc-step-top");

        Span titleSpan = new Span(heading);
        titleSpan.addClassName("tc-step-title");

        Paragraph desc = new Paragraph(text);
        desc.addClassName("tc-step-text");

        Div card = new Div(top, titleSpan, desc);
        card.addClassNames("tc-step", "tc-step--" + tone);
        return card;
    }

    // ---------------------------------------------------------------- yordamchilar

    /** Inline SVG ni konteynerga joylaydi. SVG matni faqat kod ichidagi konstantalardan keladi. */
    private static Div iconBox(String svg, String cssClass) {
        Div box = new Div();
        box.addClassName(cssClass);
        box.getElement().setProperty("innerHTML", svg);
        box.getElement().setAttribute("aria-hidden", "true");
        return box;
    }

    private Div avatar(String maskedName, String tone) {
        Div avatar = new Div();
        avatar.setText(initials(maskedName));
        avatar.addClassNames("tc-avatar", "tc-avatar--" + tone);
        return avatar;
    }

    private Span stars(double avg) {
        Span s = new Span(String.format(Locale.US, "★ %.1f", avg));
        s.addClassName("tc-stars");
        return s;
    }

    private static String vehicleText(TopCourierDto c) {
        return c.vehicleLabel() == null || c.vehicleLabel().isBlank()
                ? "Transport ko'rsatilmagan"
                : c.vehicleLabel();
    }

    /** Maxfiylik: "Javohir Komilov" -> "Javohir K." */
    static String maskName(String full) {
        if (full == null || full.isBlank()) {
            return "Kuryer";
        }
        String[] parts = full.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }

    /** "Javohir K." -> "JK" */
    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}