package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import uz.onetimecourier.one_time_courier.enums.Region;
import uz.onetimecourier.one_time_courier.layout.AuthLayout;

@Route(value = "", layout = AuthLayout.class)
@PageTitle("YukBor | Zamonaviy yetkazib berish")
@AnonymousAllowed
@CssImport("./styles/home.css")
public class HomeView extends Div {

    // =====================================================================
    //  IKONKALAR: yagona uslub, rangi currentColor dan olinadi
    // =====================================================================

    private static final String ICON_PACKAGE = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M7 14.5 24 6l17 8.5v19L24 42 7 33.5z" fill="currentColor" fill-opacity=".2"/>
              <g fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M7 14.5 24 6l17 8.5v19L24 42 7 33.5z"/>
                <path d="M7 14.5 24 23l17-8.5"/>
                <path d="M24 23v19"/>
                <path d="M15.5 10.2l17 8.6"/>
              </g>
            </svg>
            """;

    private static final String ICON_TRUCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M3 12h26v23H3z" fill="currentColor" fill-opacity=".2"/>
              <g fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 12h26v23H3z"/>
                <path d="M29 19h8.5l7 8.5V35H29"/>
                <path d="M33 22.5h3.7l3.8 4.5H33z"/>
                <path d="M9 20h12"/>
              </g>
              <g fill="none" stroke="currentColor" stroke-width="3">
                <circle cx="13" cy="36" r="4.8" fill="#121212"/>
                <circle cx="36" cy="36" r="4.8" fill="#121212"/>
              </g>
            </svg>
            """;

    private static final String ICON_SHIELD = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M24 5 40 11v11c0 9.5-6.5 16.5-16 20C14.5 38.5 8 31.5 8 22V11z"
                    fill="currentColor" fill-opacity=".2"/>
              <g fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M24 5 40 11v11c0 9.5-6.5 16.5-16 20C14.5 38.5 8 31.5 8 22V11z"/>
                <path d="M16.5 24l5.5 5.5L32 19"/>
              </g>
            </svg>
            """;

    private static final String ICON_PIN = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M24 5c-7.2 0-13 5.4-13 12.3C11 27.5 24 43 24 43s13-15.5 13-25.7C37 10.4 31.2 5 24 5z"
                    fill="currentColor" fill-opacity=".2"/>
              <g fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M24 5c-7.2 0-13 5.4-13 12.3C11 27.5 24 43 24 43s13-15.5 13-25.7C37 10.4 31.2 5 24 5z"/>
                <circle cx="24" cy="17.5" r="5"/>
              </g>
            </svg>
            """;

    private static final String ICON_CLOCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <circle cx="24" cy="24" r="18" fill="currentColor" fill-opacity=".2"/>
              <g fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="24" cy="24" r="18"/>
                <path d="M24 13v11l7.5 4.5"/>
                <path d="M24 3.5V6M44.5 24H42M24 44.5V42M3.5 24H6"/>
              </g>
            </svg>
            """;

    private static final String ICON_BOLT = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M27 4 10 27h12l-2 17 18-25H26z" fill="currentColor" fill-opacity=".2"/>
              <path d="M27 4 10 27h12l-2 17 18-25H26z" fill="none" stroke="currentColor"
                    stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            """;

    private static final String ICON_TELEGRAM = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M43 6 6 21l12 5 5 13z" fill="#2aabee" fill-opacity=".25"/>
              <g fill="none" stroke="#2aabee" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M43 6 6 21l12 5 5 13z"/>
                <path d="M18 26 43 6"/>
                <path d="M23 39l5-8"/>
              </g>
            </svg>
            """;

    private static final String ICON_SEARCH = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.8"
                 stroke-linecap="round" xmlns="http://www.w3.org/2000/svg">
              <circle cx="11" cy="11" r="7"/>
              <path d="M20 20l-4.2-4.2"/>
            </svg>
            """;

    private static final String ICON_SEND = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 19V5M5 12l7-7 7 7"/>
            </svg>
            """;

    private static final String ICON_ARROW = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.8"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 12h15M13 6l6 6-6 6"/>
            </svg>
            """;

    private static final String ICON_CHEVRON = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 9.5l6 5.5 6-5.5"/>
            </svg>
            """;

    private static final String ICON_BIG_TRUCK = """
            <svg viewBox="0 0 230 96" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="10" width="140" height="58" rx="5" fill="#f5f3ee"/>
              <rect x="4" y="52" width="140" height="8" fill="#ffd21f"/>
              <text x="74" y="40" text-anchor="middle" font-family="'Barlow Condensed', Impact, sans-serif"
                    font-size="28" font-weight="800" fill="#121212" letter-spacing="1.5">YUKBOR</text>
              <path d="M148 24h36c5 0 8 2 11 6l14 18c2 3 3 5 3 9v13H148z" fill="#ffd21f"/>
              <path d="M156 31h24l13 17h-37z" fill="#2b3a4c"/>
              <rect x="4" y="68" width="210" height="8" rx="3" fill="#2a2a2a"/>
              <rect x="207" y="56" width="8" height="9" rx="2" fill="#fff6c2"/>
              <g fill="#0b0b0b" stroke="#f5f3ee" stroke-width="3">
                <circle cx="36" cy="78" r="13"/>
                <circle cx="104" cy="78" r="13"/>
                <circle cx="182" cy="78" r="13"/>
              </g>
              <g fill="#f5f3ee">
                <circle cx="36" cy="78" r="4.5"/>
                <circle cx="104" cy="78" r="4.5"/>
                <circle cx="182" cy="78" r="4.5"/>
              </g>
            </svg>
            """;

    private static final String[][] SAMPLE_ROUTES = {
            {"Toshkent", "Samarqand"},
            {"Samarqand", "Buxoro"},
            {"Andijon", "Toshkent"},
            {"Farg'ona", "Namangan"},
            {"Qarshi", "Toshkent"}
    };

    public HomeView() {
        addClassName("home-page");

        add(
                hero(),
                ticker(),
                road(),
                steps(),
                features(),
                faq(),
                ctaBand(),
                new SiteFooter(),
                mobileBar()
        );
    }

    /** Telefonda: hero tugmalari ekrandan chiqqach, pastda doimiy panel chiqadi. */
    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        getElement().executeJs("""
                const actions = this.querySelector('.home-actions');
                const bar = this.querySelector('.home-mobile-bar');
                if (!actions || !bar || !('IntersectionObserver' in window)) { return; }
                if (this._ybObserver) { this._ybObserver.disconnect(); }
                this._ybObserver = new IntersectionObserver(function (entries) {
                    bar.classList.toggle('is-visible', !entries[0].isIntersecting);
                });
                this._ybObserver.observe(actions);
                """);
    }

    // ================= HERO =================

    private Div hero() {
        Div hero = new Div();
        hero.addClassName("home-hero");

        Div container = new Div();
        container.addClassName("home-hero-container");

        Div grid = new Div();
        grid.addClassName("home-hero-grid");

        // ---- chap: sarlavha, matn, tugmalar ----
        Div copy = new Div();
        copy.addClassName("home-hero-copy");



        H1 title = new H1();
        title.addClassName("home-title");
        title.add(
                line("Yo'lingizda"),
                line("yuk olib keting."),
                line("Qo'shimcha"),
                markLine("daromad", " oling.")
        );

        Paragraph description = new Paragraph(
                "Bir shahardan boshqasiga ketayotgan bo'lsangiz, yo'lingizdagi yukni yetkazib qo'ying. "
                        + "Jo'natuvchilar esa o'z yuklari uchun ishonchli kuryer topadi.");
        description.addClassName("home-lead");

        Anchor findButton = new Anchor("/deliveries");
        findButton.addClassNames("home-btn", "home-btn-primary");
        findButton.add(svgBox(ICON_SEARCH, "home-btn-icon"), new Span("Yuklarni ko'rish"));

        Anchor sendButton = new Anchor("/deliveries/new");
        sendButton.addClassNames("home-btn", "home-btn-secondary");
        sendButton.add(svgBox(ICON_SEND, "home-btn-icon"), new Span("Yuk joylashtirish"));

        Div actions = new Div(findButton, sendButton);
        actions.addClassName("home-actions");

        Div stats = new Div(
                stat(null, String.valueOf(Region.values().length), "hudud bo'yicha"),
                stat(null, "2×", "tomonlama tasdiqlash"),
                stat(ICON_TELEGRAM, null, "Telegram orqali tasdiq")
        );
        stats.addClassName("home-stats");

        copy.add(title, description, actions, stats);

        // ---- o'ng: yo'nalishlar paneli ----
        Div visual = new Div(board());
        visual.addClassName("home-hero-visual");

        grid.add(copy, visual);
        container.add(grid);
        hero.add(container);
        return hero;
    }

    private Span line(String text) {
        Span s = new Span(text);
        s.addClassName("home-title-line");
        return s;
    }

    private Span markLine(String highlighted, String rest) {
        Span wrap = new Span();
        wrap.addClassNames("home-title-line", "home-title-accent");
        Span mark = new Span(highlighted);
        mark.addClassName("home-title-mark");
        wrap.add(mark, new Span(rest));
        return wrap;
    }

    private Div stat(String iconSvg, String value, String label) {
        Div box = new Div();
        box.addClassName("home-stat");

        if (iconSvg != null) {
            Div valueBox = new Div();
            valueBox.addClassName("home-stat-value");
            valueBox.add(svgBox(iconSvg, "home-stat-icon"));
            box.add(valueBox);
        } else {
            Span number = new Span(value);
            number.addClassName("home-stat-value");
            box.add(number);
        }

        Span description = new Span(label);
        description.addClassName("home-stat-label");
        box.add(description);
        return box;
    }

    private Div board() {
        Div board = new Div();
        board.addClassName("home-board");

        Div head = new Div();
        head.addClassName("home-board-head");
        head.add(new Span("Yo'nalishlar"));
        Span sample = new Span("namuna");
        sample.addClassName("home-board-sample");
        head.add(sample);
        board.add(head);

        for (int i = 0; i < SAMPLE_ROUTES.length; i++) {
            Div row = new Div();
            row.addClassName("home-board-row");

            Span num = new Span(String.format("%02d", i + 1));
            num.addClassName("home-board-num");

            Span from = new Span(SAMPLE_ROUTES[i][0]);
            from.addClassName("home-board-city");

            Span to = new Span(SAMPLE_ROUTES[i][1]);
            to.addClassName("home-board-city");

            row.add(num, from, svgBox(ICON_ARROW, "home-board-arrow"), to);
            board.add(row);
        }
        return board;
    }

    // ================= LENTA VA YO'L =================

    /** Sariq lenta: yo'nalishlar doimiy aylanib turadi. Ikki nusxa, shuning uchun silliq takrorlanadi. */
    private Div ticker() {
        Div band = new Div();
        band.addClassName("home-ticker");
        band.getElement().setAttribute("aria-hidden", "true");

        Div track = new Div();
        track.addClassName("home-ticker-track");
        for (int pass = 0; pass < 2; pass++) {
            for (String[] route : SAMPLE_ROUTES) {
                Span item = new Span(route[0] + " → " + route[1]);
                item.addClassName("home-ticker-item");
                track.add(item);
            }
        }

        band.add(track);
        return band;
    }

    private Div road() {
        Div road = new Div();
        road.addClassName("home-road");
        road.getElement().setAttribute("aria-hidden", "true");
        road.add(svgBox(ICON_BIG_TRUCK, "home-truck"));
        return road;
    }

    // ================= QADAMLAR =================

    private Div steps() {
        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(sectionHeader(
                "Qanday ishlaydi",
                "Yetkazish endi ancha oson",
                "Jo'natuvchilar va kuryerlar uchun yagona qulay platforma. Uch qadam, ortiqcha gap yo'q."));

        Div grid = new Div();
        grid.addClassName("home-steps");
        grid.add(
                step("1", ICON_PACKAGE, "Yuk joylashtiring",
                        "Yukingiz haqida ma'lumot kiriting va o'zingizga mos kuryerni toping."),
                step("2", ICON_TRUCK, "Kuryer toping",
                        "O'z yo'nalishingizga mos keladigan yuklarni tanlang."),
                step("3", ICON_SHIELD, "Ishonchli yetkazish",
                        "Har bir yetkazma ikki tomonlama tasdiqlash tizimi orqali nazorat qilinadi.")
        );

        container.add(grid);
        section.add(container);
        return section;
    }

    private Div step(String number, String iconSvg, String title, String description) {
        Div step = new Div();
        step.addClassName("home-step");

        Span num = new Span(number);
        num.addClassName("home-step-number");
        num.getElement().setAttribute("aria-hidden", "true");

        Div top = new Div(svgBox(iconSvg, "home-step-icon"));
        top.addClassName("home-step-top");

        step.add(num, top, new H3(title), new Paragraph(description));
        return step;
    }

    // ================= AFZALLIKLAR =================

    private Div features() {
        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(sectionHeader(
                "Bizning ustunligimiz",
                "Nega aynan YukBor?",
                "Vaqtingizni tejang, ishonchli kuryer toping va yetkazishni osonlashtiring."));

        Div grid = new Div();
        grid.addClassName("home-features-grid");
        grid.add(
                feature(ICON_CLOCK, "Tezkor xizmat",
                        "Kerakli yo'nalishdagi yuklarni tezda topish imkoniyati."),
                feature(ICON_PIN, "Qulay yo'nalish",
                        "Shahar ichida va viloyatlararo yuk yetkazish."),
                feature(ICON_SHIELD, "Xavfsizlik",
                        "Telegram orqali telefon raqamini tasdiqlash va ikki tomonlama nazorat."),
                feature(ICON_BOLT, "Jonli yangilanish",
                        "Yuk holati o'zgarganda ma'lumotlar avtomatik yangilanadi.")
        );

        container.add(grid);
        section.add(container);
        return section;
    }

    private Div feature(String iconSvg, String title, String description) {
        Div card = new Div();
        card.addClassName("home-feature");

        Div body = new Div();
        body.addClassName("home-feature-body");
        body.add(new H3(title), new Paragraph(description));

        card.add(svgBox(iconSvg, "home-feature-icon"), body);
        return card;
    }

    // ================= FAQ =================

    private Div faq() {
        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        Div wrap = new Div();
        wrap.addClassName("home-faq-wrap");

        wrap.add(sectionHeader(
                "Savollar",
                "Ko'p so'raladigan savollar",
                "Javobini topa olmadingizmi? Qo'llab-quvvatlash xizmati bilan bog'laning."));

        Div list = new Div();
        list.addClassName("home-faq-list");
        list.add(
                faqItem("Telefon raqamim kimga ko'rinadi?",
                        "Jo'natuvchi yuk joylaganda telefon raqami umumiy ro'yxatda ko'rinmaydi. "
                                + "Yuk kuryer tomonidan qabul qilinib, biriktirilgandan keyin "
                                + "raqamlar ikki tomonga ochiladi. Kuryer safar e'lon qilganda "
                                + "esa raqami Kuryerlar sahifasida ko'rinadi.",
                        true),
                faqItem("Yuk yo'qolsa yoki shikastlansa nima bo'ladi?",
                        "Yetkazish jarayonida nizo chiqsa, buyurtmani \"Nizoli\" holatiga o'tkazish mumkin. "
                                + "Olib ketish va yetkazish vaqtlari tizimda qayd etiladi, shu asosida holat "
                                + "tekshiriladi. Yuk qiymati bo'yicha kafolat va kompensatsiya tartibi "
                                + "alohida shartlarda belgilanadi.",
                        false),
                faqItem("Kuryer bo'lish uchun nima kerak?",
                        "Ro'yxatdan o'tish, Telegram orqali telefon raqamingizni tasdiqlash va "
                                + "transport vositangiz ma'lumotlarini (turi, markasi, rangi va davlat raqami) "
                                + "kiritish kifoya. Tasdiqlangandan so'ng yuklarni qabul qilish va safar "
                                + "e'lon qilish mumkin bo'ladi.",
                        false)
        );

        wrap.add(list);
        container.add(wrap);
        section.add(container);
        return section;
    }

    private Div faqItem(String question, String answer, boolean openByDefault) {
        Div item = new Div();
        item.addClassName("home-faq-item");

        NativeButton toggle = new NativeButton();
        toggle.addClassName("home-faq-question");
        toggle.getElement().setAttribute("type", "button");
        toggle.getElement().setAttribute("aria-expanded", String.valueOf(openByDefault));
        toggle.add(new Span(question), svgBox(ICON_CHEVRON, "home-faq-chevron"));

        Div body = new Div();
        body.addClassName("home-faq-answer");
        body.add(new Paragraph(answer));

        if (openByDefault) {
            item.getElement().getClassList().add("is-open");
        }

        toggle.addClickListener(e -> {
            boolean open = item.getElement().getClassList().contains("is-open");
            item.getElement().getClassList().set("is-open", !open);
            toggle.getElement().setAttribute("aria-expanded", String.valueOf(!open));
        });

        item.add(toggle, body);
        return item;
    }

    // ================= CTA =================

    private Div ctaBand() {
        Div section = new Div();
        section.addClassName("home-cta-section");

        Div container = new Div();
        container.addClassName("home-container");

        Div panel = new Div();
        panel.addClassName("home-cta-panel");

        Div content = new Div();
        content.addClassName("home-cta-content");
        content.add(
                new H2("Yo'lingizni imkoniyatga aylantiring!"),
                new Paragraph("Bugunoq YukBor platformasiga qo'shiling. "
                        + "Yukingizni joylang yoki o'z yo'nalishingizda kuryer bo'ling.")
        );

        Anchor primary = new Anchor("/deliveries/new", "Hoziroq boshlash");
        primary.addClassNames("home-cta-button", "home-cta-button--primary");

        Anchor ghost = new Anchor("/register", "Kuryer bo'lish");
        ghost.addClassNames("home-cta-button", "home-cta-button--ghost");

        Div actions = new Div(primary, ghost);
        actions.addClassName("home-cta-actions");

        panel.add(content, actions);
        container.add(panel);
        section.add(container);
        return section;
    }

    // ================= TELEFON PASTKI PANELI =================

    private Div mobileBar() {
        Div bar = new Div();
        bar.addClassName("home-mobile-bar");
        bar.getElement().setAttribute("aria-label", "Tezkor harakatlar");

        Anchor find = new Anchor("/deliveries");
        find.addClassNames("home-bar-btn", "home-bar-btn--primary");
        find.add(svgBox(ICON_SEARCH, "home-btn-icon"), new Span("Yuklar"));

        Anchor send = new Anchor("/deliveries/new");
        send.addClassNames("home-bar-btn", "home-bar-btn--ghost");
        send.add(svgBox(ICON_SEND, "home-btn-icon"), new Span("Yuk joylash"));

        bar.add(find, send);
        return bar;
    }

    // ================= YORDAMCHI =================

    /** Inline SVG konteynerga joylanadi. SVG matni faqat kod ichidagi konstantalardan keladi. */
    private static Div svgBox(String svg, String cssClass) {
        Div box = new Div();
        box.addClassName(cssClass);
        box.getElement().setProperty("innerHTML", svg);
        box.getElement().setAttribute("aria-hidden", "true");
        return box;
    }

    private Div sectionHeader(String kicker, String title, String description) {
        Div header = new Div();
        header.addClassName("home-section-header");

        Span label = new Span(kicker);
        label.addClassName("home-section-kicker");

        H2 heading = new H2(title);
        heading.addClassName("home-section-title");

        Paragraph text = new Paragraph(description);
        text.addClassName("home-section-description");

        header.add(label, heading, text);
        return header;
    }
}