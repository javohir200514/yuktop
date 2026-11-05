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

    // =====================================================================
    //  QO'LDA CHIZILGANDEK IKONKALAR
    //  Uslub: to'q ko'k kontur + biroz siljitilgan rangli dog' (bosma xato effekti)
    // =====================================================================

    private static final String ICON_PACKAGE = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M11 19.5 27 11l16 8.5v18.5L27 46 11 37.5z" fill="#f4b942"/>
              <g fill="none" stroke="#102b50" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M8 15.5 24 7l16 8.5V34L24 42.5 8 34z"/>
                <path d="M8 15.5 24 24l16-8.5"/>
                <path d="M24 24v18.5"/>
                <path d="M16 11.2l16 8.6"/>
              </g>
            </svg>
            """;

    private static final String ICON_TRUCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M8 18h25v20H8z" fill="#9cc4ff"/>
              <path d="M33 24h8l5.5 6.5V38H33z" fill="#9cc4ff"/>
              <g fill="none" stroke="#102b50" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4.5 13.5h25.5v21H4.5z"/>
                <path d="M30 20h7.5l5 6v8.5H30"/>
                <path d="M33.5 22.5h3.3l3.4 3.8h-6.7z"/>
                <path d="M9 19.5h8"/>
              </g>
              <circle cx="14" cy="35.5" r="4.6" fill="#fffdf8" stroke="#102b50" stroke-width="2.4"/>
              <circle cx="36" cy="35.5" r="4.6" fill="#fffdf8" stroke="#102b50" stroke-width="2.4"/>
              <circle cx="14" cy="35.5" r="1.3" fill="#102b50"/>
              <circle cx="36" cy="35.5" r="1.3" fill="#102b50"/>
            </svg>
            """;

    private static final String ICON_SHIELD = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M27 9.5l15 6v11c0 9-6 16-15 20-9-4-15-11-15-20v-11z" fill="#8fd3a8"/>
              <g fill="none" stroke="#102b50" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M24 5l15 6v11c0 9-6 16-15 20C15 38 9 31 9 22V11z"/>
                <path d="M16.5 24l5.3 5.3L32 18.5"/>
              </g>
            </svg>
            """;

    private static final String ICON_PIN = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M27 8c-7 0-12 5-12 11.5C15 29 27 45 27 45s12-16 12-25.5C39 13 34 8 27 8z" fill="#f7a58a"/>
              <g fill="none" stroke="#102b50" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M24 5c-7 0-12 5-12 11.5C12 26 24 42 24 42s12-16 12-25.5C36 10 31 5 24 5z"/>
                <circle cx="24" cy="16.5" r="4.6" fill="#fffdf8"/>
              </g>
            </svg>
            """;

    private static final String ICON_CLOCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <circle cx="27" cy="27" r="17" fill="#ffd37a"/>
              <g fill="none" stroke="#102b50" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="24" cy="24" r="17"/>
                <path d="M24 13.5V24l7 4.2"/>
                <path d="M24 7.5v1.8M40.5 24h-1.8M24 40.5v-1.8M7.5 24h1.8"/>
              </g>
            </svg>
            """;

    private static final String ICON_BOLT = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path d="M30 8 14 30h11l-2 16 17-24H29z" fill="#ffd37a"/>
              <path d="M27 5 11 27h11l-2 16 17-24H26z" fill="none" stroke="#102b50"
                    stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            """;

    private static final String ICON_TELEGRAM = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <circle cx="26" cy="26" r="19" fill="#9cc4ff"/>
              <g fill="none" stroke="#102b50" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="24" cy="24" r="19" fill="#fffdf8"/>
                <path d="M11 24l24-9.5-4 19-7-5-3.5 3.5V27z"/>
                <path d="M16.5 24.5 31 17"/>
              </g>
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
              <path d="M4 12.5c5 .4 10-.4 15-.5M13 6l6 6-6 6"/>
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
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 9.5l6 5.5 6-5.5"/>
            </svg>
            """;

    // =====================================================================
    //  HERO RASMI: yo'l xaritasi, kuryer mashinasi, banka va pochta shtampi
    // =====================================================================

    private static final String HERO_ART = """
            <svg viewBox="0 0 560 520" xmlns="http://www.w3.org/2000/svg" role="img">
              <!-- qog'oz varaq -->
              <path d="M30 40 C 120 22, 330 30, 520 44 C 540 140, 534 330, 526 470
                       C 380 492, 190 486, 36 478 C 22 360, 26 160, 30 40z"
                    fill="#fffdf8" stroke="#102b50" stroke-width="2.6" stroke-linejoin="round"/>
              <path d="M44 56 C 150 44, 330 50, 506 60" fill="none" stroke="#efe6d3" stroke-width="2"/>

              <!-- xarita relyef chiziqlari -->
              <g fill="none" stroke="#e5dcc6" stroke-width="2" stroke-linecap="round">
                <path d="M60 130 C 110 100, 170 150, 230 118 S 340 90, 400 120"/>
                <path d="M70 420 C 140 390, 200 440, 280 410 S 400 400, 470 430"/>
                <path d="M440 170 C 470 220, 430 260, 470 310"/>
              </g>

              <!-- yo'l (uzuq chiziq) -->
              <path id="route" d="M96 396 C 150 300, 90 230, 190 210 S 330 250, 360 170 S 430 120, 462 106"
                    fill="none" stroke="#e8742c" stroke-width="5" stroke-linecap="round"
                    stroke-dasharray="2 14"/>

              <!-- shaharlar -->
              <g>
                <circle cx="96" cy="396" r="11" fill="#fffdf8" stroke="#102b50" stroke-width="3"/>
                <circle cx="96" cy="396" r="4" fill="#102b50"/>
                <circle cx="190" cy="210" r="11" fill="#fffdf8" stroke="#102b50" stroke-width="3"/>
                <circle cx="190" cy="210" r="4" fill="#102b50"/>
                <circle cx="462" cy="106" r="11" fill="#f4b942" stroke="#102b50" stroke-width="3"/>
                <circle cx="462" cy="106" r="4" fill="#102b50"/>
              </g>
              <g font-family="Caveat, 'Segoe Print', cursive" font-size="30" font-weight="700" fill="#102b50">
                <text x="112" y="428" transform="rotate(-3 112 428)">Buxoro</text>
                <text x="64" y="188" transform="rotate(-4 64 188)">Samarqand</text>
                <text x="392" y="82" transform="rotate(-3 392 82)">Toshkent</text>
              </g>

              <!-- yuk mashinasi -->
              <g transform="translate(250 262) rotate(-14)">
                <rect x="0" y="0" width="64" height="40" rx="4" fill="#9cc4ff" stroke="#102b50" stroke-width="3"/>
                <path d="M64 10h22l12 14v16H64z" fill="#fffdf8" stroke="#102b50"
                      stroke-width="3" stroke-linejoin="round"/>
                <path d="M72 15h11l7 9H72z" fill="#9cc4ff" stroke="#102b50" stroke-width="2.4"
                      stroke-linejoin="round"/>
                <path d="M10 12h28M10 22h18" stroke="#102b50" stroke-width="3" stroke-linecap="round"/>
                <circle cx="20" cy="42" r="9" fill="#fffdf8" stroke="#102b50" stroke-width="3"/>
                <circle cx="20" cy="42" r="2.6" fill="#102b50"/>
                <circle cx="84" cy="42" r="9" fill="#fffdf8" stroke="#102b50" stroke-width="3"/>
                <circle cx="84" cy="42" r="2.6" fill="#102b50"/>
              </g>

              <!-- banka (yuk) -->
              <g transform="translate(330 330) rotate(6)">
                <path d="M4 22 52 4l48 18v52L52 94 4 74z" fill="#f4b942" opacity=".0"/>
                <path d="M0 24 50 4l50 20v50L50 96 0 74z" fill="#f4b942" stroke="#102b50"
                      stroke-width="3" stroke-linejoin="round"/>
                <path d="M0 24l50 20 50-20" fill="none" stroke="#102b50" stroke-width="3"
                      stroke-linejoin="round"/>
                <path d="M50 44v52" stroke="#102b50" stroke-width="3"/>
                <path d="M25 14l50 20v14L25 28z" fill="#fffdf8" stroke="#102b50" stroke-width="2.6"
                      stroke-linejoin="round" opacity=".92"/>
                <path d="M62 62h22M62 72h14" stroke="#102b50" stroke-width="2.6" stroke-linecap="round"/>
              </g>

              <!-- pochta shtampi -->
              <g transform="translate(402 360) rotate(-14)" fill="none" stroke="#e8742c"
                 stroke-width="3.2" opacity=".92">
                <circle cx="0" cy="0" r="44" stroke-dasharray="3 0"/>
                <circle cx="0" cy="0" r="35" stroke-width="1.8"/>
                <path d="M-56 -6H-44M44 -6H56" stroke-linecap="round"/>
                <text x="0" y="-6" text-anchor="middle" font-family="Fraunces, Georgia, serif"
                      font-size="18" font-weight="800" fill="#e8742c" stroke="none">YUKBOR</text>
                <text x="0" y="14" text-anchor="middle" font-family="Inter, sans-serif"
                      font-size="9.5" font-weight="700" fill="#e8742c" stroke="none"
                      letter-spacing="1.6">YO'LDA YUK</text>
              </g>

              <!-- yopishtirilgan qog'oz (izoh) -->
              <g transform="translate(58 292) rotate(-5)">
                <rect x="0" y="0" width="150" height="62" fill="#fff3b8" stroke="#102b50" stroke-width="2.4"/>
                <rect x="52" y="-9" width="46" height="16" fill="#e8742c" opacity=".75"/>
                <text x="75" y="31" text-anchor="middle" font-family="Caveat, 'Segoe Print', cursive"
                      font-size="25" font-weight="700" fill="#102b50">yo'lingizdagi</text>
                <text x="75" y="53" text-anchor="middle" font-family="Caveat, 'Segoe Print', cursive"
                      font-size="25" font-weight="700" fill="#102b50">yuk ham daromad!</text>
              </g>
            </svg>
            """;

    public HomeView() {
        addClassName("home-page");

        add(
                hero(),
                routeTape(),
                steps(),
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

        Div container = new Div();
        container.addClassName("home-hero-container");

        Div grid = new Div();
        grid.addClassName("home-hero-grid");

        // ---- chap tomon (matn) ----
        Div copy = new Div();
        copy.addClassName("home-hero-copy");

        Span badge = new Span("yo'ldosh yuk, yo'ldosh kuryer");
        badge.addClassName("home-eyebrow");

        H1 title = new H1();
        title.addClassName("home-title");

        Span line1 = new Span("Yo'lingizda yuk olib keting.");
        line1.addClassName("home-title-line");

        Span line2 = new Span();
        line2.addClassNames("home-title-line", "home-title-accent");
        Span mark = new Span("daromad");
        mark.addClassName("home-title-mark");
        line2.add(new Span("Qo'shimcha "), mark, new Span(" oling."));

        title.add(line1, line2);

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

        UnorderedList facts = new UnorderedList(
                fact(ICON_PIN, String.valueOf(Region.values().length), " hudud bo'yicha"),
                fact(ICON_SHIELD, "2 tomonlama", " tasdiqlash"),
                fact(ICON_TELEGRAM, "Telegram", " orqali tasdiq")
        );
        facts.addClassName("home-facts");

        copy.add(badge, title, description, actions, facts);

        // ---- o'ng tomon (rasm) ----
        Div art = svgBox(HERO_ART, "home-hero-art");
        art.getElement().removeAttribute("aria-hidden");
        art.getElement().setAttribute("aria-label", "Shaharlararo yo'l xaritasi: yuk mashinasi va banka");

        grid.add(copy, art);
        container.add(grid);
        hero.add(container);
        return hero;
    }

    private ListItem fact(String iconSvg, String strong, String rest) {

        ListItem item = new ListItem();
        item.addClassName("home-fact");

        Div icon = svgBox(iconSvg, "home-fact-icon");

        Span text = new Span();
        Span bold = new Span(strong);
        bold.getElement().getStyle().set("font-weight", "700");
        bold.getElement().getStyle().set("color", "var(--ink)");
        text.add(bold, new Span(rest));

        item.add(icon, text);
        return item;
    }

    // ================= YO'NALISH LENTASI =================

    private Div routeTape() {

        Div wrap = new Div();
        wrap.addClassName("home-tape-wrap");

        Div tape = new Div();
        tape.addClassName("home-tape");

        String[] cities = {
                "Toshkent", "Samarqand", "Buxoro", "Andijon",
                "Farg'ona", "Namangan", "Qarshi", "Nukus"
        };

        for (int i = 0; i < cities.length; i++) {
            tape.add(new Span(cities[i]));
            if (i < cities.length - 1) {
                Span dot = new Span();
                dot.addClassName("home-tape-sep");
                tape.add(dot);
            }
        }

        wrap.add(tape);
        return wrap;
    }

    // ================= QADAMLAR =================

    private Div steps() {

        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(
                sectionHeader(
                        "qanday ishlaydi?",
                        "Yetkazish endi ancha oson",
                        "Jo'natuvchilar va kuryerlar uchun yagona qulay platforma. Uch qadam, ortiqcha gap yo'q."
                )
        );

        Div grid = new Div();
        grid.addClassName("home-steps");

        grid.add(
                step(
                        "1",
                        ICON_PACKAGE,
                        "Yuk joylashtiring",
                        "Yukingiz haqida ma'lumot kiriting va o'zingizga mos kuryerni toping."
                ),
                step(
                        "2",
                        ICON_TRUCK,
                        "Kuryer toping",
                        "O'z yo'nalishingizga mos keladigan yuklarni tanlang."
                ),
                step(
                        "3",
                        ICON_SHIELD,
                        "Ishonchli yetkazish",
                        "Har bir yetkazma ikki tomonlama tasdiqlash tizimi orqali nazorat qilinadi."
                )
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

        Div top = new Div(num, svgBox(iconSvg, "home-step-icon"));
        top.addClassName("home-step-top");

        H3 heading = new H3(title);
        Paragraph text = new Paragraph(description);

        step.add(top, heading, text);
        return step;
    }

    // ================= AFZALLIKLAR =================

    private Div features() {

        Div section = new Div();
        section.addClassNames("home-section", "home-section-paper");

        Div container = new Div();
        container.addClassName("home-container");

        Div layout = new Div();
        layout.addClassName("home-why");

        Div side = new Div();
        side.addClassName("home-why-side");

        side.add(
                sectionHeader(
                        "bizning ustunligimiz",
                        "Nega aynan YukBor?",
                        "Vaqtingizni tejang, ishonchli kuryer toping va yetkazishni osonlashtiring."
                )
        );

        Span note = new Span("yo'l bo'ylab, ortiqcha harajatsiz!");
        note.addClassName("home-why-note");
        side.add(note);

        Div list = new Div();
        list.addClassName("home-features-list");

        list.add(
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

        layout.add(side, list);
        container.add(layout);
        section.add(container);
        return section;
    }

    private Div feature(String iconSvg, String title, String description) {

        Div item = new Div();
        item.addClassName("home-feature");

        Div iconBox = svgBox(iconSvg, "home-feature-icon");

        H3 heading = new H3(title);
        Paragraph text = new Paragraph(description);

        item.add(iconBox, heading, text);
        return item;
    }

    // ================= FAQ =================

    private Div faq() {

        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        Div wrap = new Div();
        wrap.addClassName("home-faq-wrap");

        wrap.add(
                sectionHeader(
                        "savollar",
                        "Ko'p so'raladigan savollar",
                        "Javobini topa olmadingizmi? Qo'llab-quvvatlash xizmati bilan bog'laning."
                )
        );

        Div list = new Div();
        list.addClassName("home-faq-list");

        Div first = faqItem(
                "Telefon raqamim kimga ko'rinadi?",
                "Jo'natuvchi yuk joylaganda telefon raqami umumiy ro'yxatda ko'rinmaydi. "
                        + "Yuk kuryer tomonidan qabul qilinib, biriktirilgandan keyin "
                        + "raqamlar ikki tomonga ochiladi. Kuryer safar e'lon qilganda "
                        + "esa raqami Kuryerlar sahifasida ko'rinadi."
        );
        // Birinchi savol dastlab ochiq turadi
        first.getElement().getClassList().add("is-open");
        first.getChildren().findFirst().ifPresent(c -> c.getElement().setAttribute("aria-expanded", "true"));

        list.add(
                first,
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

        wrap.add(list);
        container.add(wrap);
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

        Span sticker = new Span("yo'lga chiqdikmi?");
        sticker.addClassName("home-cta-sticker");

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

        panel.add(sticker, content, actions);
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