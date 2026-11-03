package uz.onetimecourier.one_time_courier.view;


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

    // ================= RANGLI IKONKALAR (inline SVG) =================

    private static final String ICON_PACKAGE = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <polygon points="24,5 42,14 24,23 6,14" fill="#fcd34d"/>
              <polygon points="6,14 24,23 24,43 6,34" fill="#d97706"/>
              <polygon points="42,14 24,23 24,43 42,34" fill="#f59e0b"/>
              <polygon points="20,9.5 30,14.5 30,17.5 20,12.5" fill="#ffffff" opacity=".65"/>
            </svg>
            """;

    private static final String ICON_TRUCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <rect x="3" y="12" width="27" height="21" rx="3" fill="#2563eb"/>
              <rect x="3" y="12" width="27" height="4" rx="2" fill="#60a5fa"/>
              <path d="M30 18h7.5l6.5 8v7H30z" fill="#1e40af"/>
              <path d="M32.5 20.5h4.5l4 5h-8.5z" fill="#bfdbfe"/>
              <circle cx="13" cy="36" r="5" fill="#0f172a"/>
              <circle cx="13" cy="36" r="2" fill="#94a3b8"/>
              <circle cx="36" cy="36" r="5" fill="#0f172a"/>
              <circle cx="36" cy="36" r="2" fill="#94a3b8"/>
            </svg>
            """;

    private static final String ICON_SHIELD = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M24 4 41 10v13c0 10.5-7.2 18.6-17 21-9.8-2.4-17-10.5-17-21V10z" fill="#16a34a"/>
              <path d="M24 4 41 10v13c0 10.5-7.2 18.6-17 21z" fill="#22c55e"/>
              <path d="M15.5 24.5l6 6 11-12" fill="none" stroke="#ffffff" stroke-width="4"
                    stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            """;

    private static final String ICON_PIN = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M24 4c-8.3 0-15 6.5-15 14.6C9 29.6 24 44 24 44s15-14.4 15-25.4C39 10.5 32.3 4 24 4z"
                    fill="#ef4444"/>
              <circle cx="24" cy="18" r="6" fill="#ffffff"/>
            </svg>
            """;

    private static final String ICON_CLOCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <circle cx="24" cy="24" r="20" fill="#e0f2fe" stroke="#0284c7" stroke-width="3"/>
              <path d="M24 13v11.5l7.5 5" fill="none" stroke="#0c4a6e" stroke-width="3.5"
                    stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="24" cy="24" r="2.5" fill="#0c4a6e"/>
            </svg>
            """;

    private static final String ICON_TELEGRAM = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <circle cx="24" cy="24" r="22" fill="#2aabee"/>
              <path d="M9.5 23.5l25-10c1.2-.5 2.3.3 1.9 2.1l-4.3 20.2c-.3 1.4-1.2 1.7-2.4 1.1l-6.6-4.9-3.2 3.1c-.4.4-.7.7-1.4.7l.5-6.7 12.2-11c.5-.5-.1-.7-.8-.3l-15 9.5-6.5-2c-1.4-.4-1.4-1.4.6-2.2z"
                    fill="#ffffff"/>
            </svg>
            """;

    private static final String ICON_BOLT = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M27.5 4 9 27.5h12.5L19.5 44 39 19.5H26.5z" fill="#facc15"
                    stroke="#eab308" stroke-width="1.5" stroke-linejoin="round"/>
            </svg>
            """;

    // Tugma va accordion ichidagi monoxrom ikonkalar (currentColor)
    private static final String ICON_SEARCH = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
                 stroke-linecap="round" xmlns="http://www.w3.org/2000/svg">
              <circle cx="11" cy="11" r="7"/>
              <path d="M20 20l-4.2-4.2"/>
            </svg>
            """;

    private static final String ICON_ARROW = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M5 12h14M13 6l6 6-6 6"/>
            </svg>
            """;

    private static final String ICON_PLANE = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M21.5 2.5 10.5 13.5"/>
              <path d="M21.5 2.5l-6.5 19-4-8-8-4z"/>
            </svg>
            """;

    private static final String ICON_CHEVRON = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 9l6 6 6-6"/>
            </svg>
            """;

    public HomeView() {
        addClassName("home-page");

        add(
                hero(),
                advantages(),
                features(),
                faq(),
                ctaBand(),
                new SiteFooter()
        );
    }

    // ================= HERO =================

    private Div hero() {

        Div hero = new Div();
        hero.addClassName("home-hero");

        Image background = new Image("/images/home-hero.png", "");
        background.addClassName("home-hero-bg");
        background.getElement().setAttribute("aria-hidden", "true");

        Div overlay = new Div();
        overlay.addClassName("home-hero-overlay");

        Div container = new Div();
        container.addClassName("home-hero-container");

        Div content = new Div();
        content.addClassName("home-hero-content");

        Span badge = new Span("YUKBOR — ZAMONAVIY YETKAZIB BERISH PLATFORMASI");
        badge.addClassName("home-eyebrow");

        H1 title = new H1();
        title.addClassName("home-title");

        Span mainTitle = new Span("Yo'lingizda yuk olib keting.");
        Span accent = new Span("Qo'shimcha daromad oling.");
        accent.addClassName("home-title-accent");
        title.add(mainTitle, accent);

        Paragraph description = new Paragraph(
                "Bir shahardan boshqasiga ketayotgan bo'lsangiz, "
                        + "yo'lingizdagi yukni yetkazib qo'ying. "
                        + "Jo'natuvchilar esa o'z yuklari uchun "
                        + "ishonchli kuryer topadi."
        );
        description.addClassName("home-lead");

        Anchor findButton = new Anchor("/deliveries");
        findButton.addClassNames("home-btn", "home-btn-primary");
        findButton.add(
                svgBox(ICON_SEARCH, "home-btn-icon"),
                new Span("Yuklarni ko'rish"),
                svgBox(ICON_ARROW, "home-btn-icon")
        );

        Anchor sendButton = new Anchor("/deliveries/new");
        sendButton.addClassNames("home-btn", "home-btn-secondary");
        sendButton.add(
                svgBox(ICON_PLANE, "home-btn-icon"),
                new Span("Yuk joylashtirish")
        );

        Div actions = new Div(findButton, sendButton);
        actions.addClassName("home-actions");

        Div stats = new Div(
                stat(ICON_PIN, String.valueOf(Region.values().length), "Hudud bo'yicha"),
                stat(ICON_SHIELD, "2 tomonlama", "Tasdiqlash tizimi"),
                stat(ICON_TELEGRAM, "Telegram", "Orqali tasdiqlash")
        );
        stats.addClassName("home-stats");

        content.add(badge, title, description, actions, stats);
        container.add(content);

        hero.add(background, overlay, container);
        return hero;
    }

    // ================= STATISTIKA =================

    private Div stat(String iconSvg, String value, String label) {

        Div box = new Div();
        box.addClassName("home-stat");

        Div icon = svgBox(iconSvg, "home-stat-icon");

        Div text = new Div();
        text.addClassName("home-stat-text");

        Span number = new Span(value);
        number.addClassName("home-stat-value");

        Span description = new Span(label);
        description.addClassName("home-stat-label");

        text.add(number, description);
        box.add(icon, text);
        return box;
    }

    // ================= AFZALLIKLAR =================

    private Div advantages() {

        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(
                sectionHeader(
                        "YUKBOR IMKONIYATLARI",
                        "Yetkazish endi ancha oson",
                        "Jo'natuvchilar va kuryerlar uchun yagona qulay platforma."
                )
        );

        Div grid = new Div();
        grid.addClassName("home-advantages-grid");

        grid.add(
                advantage(
                        ICON_PACKAGE,
                        "Yuk joylashtiring",
                        "Yukingiz haqida ma'lumot kiriting va o'zingizga mos kuryerni toping."
                ),
                advantage(
                        ICON_TRUCK,
                        "Kuryer toping",
                        "O'z yo'nalishingizga mos keladigan yuklarni tanlang."
                ),
                advantage(
                        ICON_SHIELD,
                        "Ishonchli yetkazish",
                        "Har bir yetkazma ikki tomonlama tasdiqlash tizimi orqali nazorat qilinadi."
                )
        );

        container.add(grid);
        section.add(container);
        return section;
    }

    private Div advantage(String iconSvg, String title, String description) {

        Div card = new Div();
        card.addClassName("home-advantage-card");

        Div iconBox = svgBox(iconSvg, "home-advantage-icon");

        H3 heading = new H3(title);
        Paragraph text = new Paragraph(description);

        card.add(iconBox, heading, text);
        return card;
    }

    // ================= QO'SHIMCHA IMKONIYATLAR =================

    private Div features() {

        Div section = new Div();
        section.addClassNames("home-section", "home-section-light");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(
                sectionHeader(
                        "BIZNING USTUNLIGIMIZ",
                        "Nega aynan YukBor?",
                        "Vaqtingizni tejang, ishonchli kuryer toping va yetkazishni osonlashtiring."
                )
        );

        Div grid = new Div();
        grid.addClassName("home-features-grid");

        grid.add(
                feature(
                        ICON_CLOCK,
                        "Tezkor xizmat",
                        "Kerakli yo'nalishdagi yuklarni tezda topish imkoniyati."
                ),
                feature(
                        ICON_PIN,
                        "Qulay yo'nalish",
                        "Shahar ichida va viloyatlararo yuk yetkazish."
                ),
                feature(
                        ICON_SHIELD,
                        "Xavfsizlik",
                        "Telegram orqali telefon raqamini tasdiqlash va ikki tomonlama nazorat."
                ),
                feature(
                        ICON_BOLT,
                        "Jonli yangilanish",
                        "Yuk holati o'zgarganda ma'lumotlar avtomatik yangilanadi."
                )
        );

        container.add(grid);
        section.add(container);
        return section;
    }

    private Div feature(String iconSvg, String title, String description) {

        Div card = new Div();
        card.addClassName("home-feature-card");

        Div iconBox = svgBox(iconSvg, "home-feature-icon");

        H3 heading = new H3(title);
        Paragraph text = new Paragraph(description);

        card.add(iconBox, heading, text);
        return card;
    }

    // ================= FAQ =================

    private Div faq() {

        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(
                sectionHeader(
                        "SAVOLLAR",
                        "Ko'p so'raladigan savollar",
                        "Javobini topa olmadingizmi? Qo'llab-quvvatlash xizmati bilan bog'laning."
                )
        );

        Div list = new Div();
        list.addClassName("home-faq-list");

        list.add(
                faqItem(
                        "Telefon raqamim kimga ko'rinadi?",
                        "Jo'natuvchi yuk joylaganda telefon raqami umumiy ro'yxatda ko'rinmaydi. "
                                + "Yuk kuryer tomonidan qabul qilinib, biriktirilgandan keyin "
                                + "raqamlar ikki tomonga ochiladi. Kuryer safar e'lon qilganda "
                                + "esa raqami Kuryerlar sahifasida ko'rinadi."
                ),
                faqItem(
                        "Yuk yo'qolsa yoki shikastlansa nima bo'ladi?",
                        "Yetkazish jarayonida nizo chiqsa, buyurtmani \"Nizoli\" holatiga o'tkazish mumkin. "
                                + "Olib ketish va yetkazish vaqtlari tizimda qayd etiladi, shu asosida holat "
                                + "tekshiriladi. Yuk qiymati bo'yicha kafolat va kompensatsiya tartibi "
                                + "alohida shartlarda belgilanadi."
                ),
                faqItem(
                        "Kuryer bo'lish uchun nima kerak?",
                        "Ro'yxatdan o'tish, Telegram orqali telefon raqamingizni tasdiqlash va "
                                + "transport vositangiz ma'lumotlarini (turi, markasi, rangi va davlat raqami) "
                                + "kiritish kifoya. Tasdiqlangandan so'ng yuklarni qabul qilish va safar "
                                + "e'lon qilish mumkin bo'ladi."
                )
        );

        container.add(list);
        section.add(container);
        return section;
    }

    private Div faqItem(String question, String answer) {

        Div item = new Div();
        item.addClassName("home-faq-item");

        NativeButton toggle = new NativeButton();
        toggle.addClassName("home-faq-question");
        toggle.getElement().setAttribute("aria-expanded", "false");

        Span text = new Span(question);
        Div chevron = svgBox(ICON_CHEVRON, "home-faq-chevron");

        toggle.add(text, chevron);

        Div body = new Div();
        body.addClassName("home-faq-answer");
        body.add(new Paragraph(answer));

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

        H2 title = new H2("Yo'lingizni imkoniyatga aylantiring!");

        Paragraph description = new Paragraph(
                "Bugunoq YukBor platformasiga qo'shiling. "
                        + "Yukingizni joylang yoki o'z yo'nalishingizda kuryer bo'ling."
        );

        content.add(title, description);

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

    // ================= YORDAMCHI =================

    /** Inline SVG ni konteynerga joylaydi. SVG matni faqat kod ichidagi konstantalardan keladi. */
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