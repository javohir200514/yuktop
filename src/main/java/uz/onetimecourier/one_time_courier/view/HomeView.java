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
    //  IKONKALAR
    //  Uslub: ingichka (2.4px) kontur + bitta aksent chiziq + yengil dog'.
    //  Ranglar CSS dan keladi:
    //    kontur  -> currentColor
    //    class a -> var(--accent)   (aksent chiziq)
    //    class f -> yengil to'ldirma
    //    class w -> g'ildirak to'ldirmasi
    // =====================================================================

    private static final String ICON_PACKAGE = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M7 14.5 24 6l17 8.5v19L24 42 7 33.5z"/>
              <g fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M7 14.5 24 6l17 8.5v19L24 42 7 33.5z"/>
                <path d="M7 14.5 24 23l17-8.5"/>
                <path d="M24 23v19"/>
                <path class="a" d="M15.5 10.2l17 8.7"/>
              </g>
            </svg>
            """;

    private static final String ICON_TRUCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M3 12h26v23H3z"/>
              <g fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 12h26v23H3z"/>
                <path d="M29 19h8.5l7 8.5V35H29"/>
                <path class="a" d="M33 22.5h3.7l3.8 4.5H33z"/>
                <path d="M9 19h12"/>
                <circle class="w" cx="13" cy="36" r="4.8"/>
                <circle class="w" cx="36" cy="36" r="4.8"/>
              </g>
            </svg>
            """;

    private static final String ICON_SHIELD = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M24 5 40 11v11c0 9.5-6.5 16.5-16 20C14.5 38.5 8 31.5 8 22V11z"/>
              <g fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M24 5 40 11v11c0 9.5-6.5 16.5-16 20C14.5 38.5 8 31.5 8 22V11z"/>
                <path class="a" stroke-width="3.2" d="M16.5 24l5.500 5.500L32 19"/>
              </g>
            </svg>
            """;

    private static final String ICON_PIN = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M24 5c-7.2 0-13 5.4-13 12.3C11 27.5 24 43 24 43s13-15.5 13-25.7C37 10.4 31.200 5 24 5z"/>
              <g fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M24 5c-7.2 0-13 5.4-13 12.3C11 27.5 24 43 24 43s13-15.5 13-25.7C37 10.4 31.200 5 24 5z"/>
                <circle class="a" cx="24" cy="17.500" r="5"/>
              </g>
            </svg>
            """;

    private static final String ICON_CLOCK = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <circle class="f" cx="24" cy="24" r="18"/>
              <g fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="24" cy="24" r="18"/>
                <path class="a" stroke-width="3" d="M24 13v11l7.500 4.500"/>
                <path d="M24 3.500V6M44.500 24H42M24 44.500V42M3.500 24H6"/>
              </g>
            </svg>
            """;

    private static final String ICON_BOLT = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M27 4 10 27h12l-2 17 18-25H26z"/>
              <g fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M27 4 10 27h12l-2 17 18-25H26z"/>
                <path class="a" d="M37 9l3-3M40.500 15H45"/>
              </g>
            </svg>
            """;

    private static final String ICON_TELEGRAM = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path class="f" d="M43 6 6 21l12 5 5 13z"/>
              <g fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M43 6 6 21l12 5 5 13z"/>
                <path class="a" d="M18 26 43 6"/>
                <path d="M23 39l5-8"/>
              </g>
            </svg>
            """;

    // Tugma va accordion ikonkalari (24x24, currentColor)
    private static final String ICON_SEARCH = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6"
                 stroke-linecap="round" xmlns="http://www.w3.org/2000/svg">
              <circle cx="11" cy="11" r="7"/>
              <path d="M20 20l-4.200-4.200"/>
            </svg>
            """;

    private static final String ICON_SEND = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6"
                 stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 19V5M5 12l7-7 7 7"/>
            </svg>
            """;

    private static final String ICON_ARROW = """
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6"
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

    // =====================================================================
    //  HERO RASMI: yo'l xaritasi (Buxoro -> Samarqand -> Toshkent)
    // =====================================================================

    private static final String HERO_ART = """
            <svg viewBox="0 0 520 440" xmlns="http://www.w3.org/2000/svg" role="img"
                 aria-label="Shaharlar orasidagi yo'l xaritasi">
              <rect x="6" y="6" width="508" height="428" rx="30" fill="#dfece3" stroke="#111512" stroke-width="2.5"/>

              <!-- xarita to'ri -->
              <g stroke="#fffefb" stroke-width="2" opacity=".8">
                <path d="M6 110H514M6 220H514M6 330H514"/>
                <path d="M130 6V434M260 6V434M390 6V434"/>
              </g>

              <!-- daryo -->
              <path d="M-10 250 C 90 210, 150 300, 250 270 S 420 200, 540 240"
                    fill="none" stroke="#c4ddcd" stroke-width="22" stroke-linecap="round"/>

              <!-- yo'l -->
              <path id="ybRoute" d="M78 354 C 128 262, 236 336, 272 236 S 400 150, 446 84"
                    fill="none" stroke="#111512" stroke-width="3" stroke-linecap="round"
                    stroke-dasharray="1 11"/>
              <path d="M78 354 C 128 262, 236 336, 272 236 S 400 150, 446 84"
                    fill="none" stroke="#ff5a1f" stroke-width="5" stroke-linecap="round"
                    stroke-dasharray="26 14" opacity=".9"/>

              <!-- bekatlar -->
              <g stroke="#111512" stroke-width="4">
                <circle cx="78" cy="354" r="12" fill="#fffefb"/>
                <circle cx="272" cy="236" r="12" fill="#fffefb"/>
                <circle cx="446" cy="84" r="15" fill="#ff5a1f"/>
              </g>
              <g font-family="'DM Mono', ui-monospace, monospace" font-size="15" font-weight="500"
                 fill="#111512" letter-spacing="1">
                <text x="98" y="392">BUXORO</text>
                <text x="292" y="274">SAMARQAND</text>
                <text x="426" y="124" text-anchor="end">TOSHKENT</text>
              </g>

              <!-- harakatlanuvchi yuk -->
              <g class="home-art-dot">
                <circle r="9" fill="#111512" stroke="#fffefb" stroke-width="3">
                  <animateMotion dur="7s" repeatCount="indefinite" rotate="auto">
                    <mpath href="#ybRoute"/>
                  </animateMotion>
                </circle>
              </g>

              <!-- yopishtirilgan izoh -->
              <g transform="translate(46 70) rotate(-5)">
                <rect width="172" height="74" rx="10" fill="#fffefb" stroke="#111512" stroke-width="2.5"/>
                <rect x="62" y="-9" width="48" height="17" rx="3" fill="#ff5a1f" stroke="#111512" stroke-width="2"/>
                <text x="86" y="34" text-anchor="middle" font-family="Caveat, 'Segoe Print', cursive"
                      font-size="27" font-weight="700" fill="#111512">yo'lingizdagi yuk</text>
                <text x="86" y="60" text-anchor="middle" font-family="Caveat, 'Segoe Print', cursive"
                      font-size="27" font-weight="700" fill="#ff5a1f">= daromad</text>
              </g>
            </svg>
            """;

    // Lenta uchun shaharlar
    private static final String[] MARQUEE_CITIES = {
            "Toshkent", "Samarqand", "Buxoro", "Andijon",
            "Farg'ona", "Namangan", "Qarshi", "Nukus", "Termiz"
    };

    public HomeView() {
        addClassName("home-page");

        add(
                hero(),
                marquee(),
                steps(),
                features(),
                faq(),
                ctaBand(),
                new SiteFooter(),
                mobileBar()
        );
    }

    /**
     * Telefonda: hero tugmalari ekrandan chiqib ketgach, pastda doimiy
     * "Yuklar / Yuk joylash" paneli paydo bo'ladi.
     */
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

        // ---- matn ----
        Div copy = new Div();
        copy.addClassName("home-hero-copy");

        Span chip = new Span();
        chip.addClassName("home-chip");
        Span dot = new Span();
        dot.addClassName("home-chip-dot");
        chip.add(dot, new Span("yo'ldosh yuk · yo'ldosh kuryer"));

        H1 title = new H1();
        title.addClassName("home-title");

        Span line1 = new Span("Yo'lingizda yuk olib keting.");
        line1.addClassName("home-title-line");

        Span line2 = new Span();
        line2.addClassName("home-title-line");
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
                new Span("Yuklarni ko'rish"),
                svgBox(ICON_ARROW, "home-btn-icon")
        );

        Anchor sendButton = new Anchor("/deliveries/new");
        sendButton.addClassNames("home-btn", "home-btn-secondary");
        sendButton.add(
                svgBox(ICON_SEND, "home-btn-icon"),
                new Span("Yuk joylashtirish")
        );

        Div actions = new Div(findButton, sendButton);
        actions.addClassName("home-actions");

        Div stats = new Div(
                stat(null, String.valueOf(Region.values().length), "hudud bo'yicha"),
                stat(null, "2×", "tomonlama tasdiqlash"),
                stat(ICON_TELEGRAM, null, "Telegram orqali tasdiq")
        );
        stats.addClassName("home-stats");

        copy.add(chip, title, description, actions, stats);

        // ---- xarita ----
        Div art = svgBox(HERO_ART, "home-art");
        art.getElement().removeAttribute("aria-hidden");

        grid.add(copy, art);
        container.add(grid);
        hero.add(container);
        return hero;
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

    // ================= SHAHARLAR LENTASI =================

    private Div marquee() {

        Div wrap = new Div();
        wrap.addClassName("home-marquee");

        Div track = new Div();
        track.addClassName("home-marquee-track");

        // Bir xil ikki nusxa: CSS animatsiyasi uzluksiz aylanishi uchun
        track.add(marqueeGroup(false), marqueeGroup(true));

        wrap.add(track);
        return wrap;
    }

    private Div marqueeGroup(boolean duplicate) {

        Div group = new Div();
        group.addClassName("home-marquee-group");
        if (duplicate) {
            group.getElement().setAttribute("aria-hidden", "true");
        }

        for (String city : MARQUEE_CITIES) {
            Span name = new Span(city);
            name.addClassName("home-marquee-city");

            Span dot = new Span();
            dot.addClassName("home-marquee-dot");

            group.add(name, dot);
        }
        return group;
    }

    // ================= QADAMLAR =================

    private Div steps() {

        Div section = new Div();
        section.addClassName("home-section");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(
                sectionHeader(
                        "Qanday ishlaydi",
                        "Yetkazish endi ancha oson",
                        "Jo'natuvchilar va kuryerlar uchun yagona qulay platforma. Uch qadam, ortiqcha gap yo'q."
                )
        );

        Div grid = new Div();
        grid.addClassName("home-steps");

        grid.add(
                step(
                        "01",
                        ICON_PACKAGE,
                        "Yuk joylashtiring",
                        "Yukingiz haqida ma'lumot kiriting va o'zingizga mos kuryerni toping."
                ),
                step(
                        "02",
                        ICON_TRUCK,
                        "Kuryer toping",
                        "O'z yo'nalishingizga mos keladigan yuklarni tanlang."
                ),
                step(
                        "03",
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

        H3 heading = new H3(title);
        Paragraph text = new Paragraph(description);

        step.add(num, svgBox(iconSvg, "home-step-icon"), heading, text);
        return step;
    }

    // ================= AFZALLIKLAR =================

    private Div features() {

        Div section = new Div();
        section.addClassNames("home-section", "home-section-dark");

        Div container = new Div();
        container.addClassName("home-container");

        container.add(
                sectionHeader(
                        "Bizning ustunligimiz",
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
        card.addClassName("home-feature");

        card.add(svgBox(iconSvg, "home-feature-icon"), new H3(title), new Paragraph(description));
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

        wrap.add(
                sectionHeader(
                        "Savollar",
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
                                + "esa raqami Kuryerlar sahifasida ko'rinadi.",
                        true
                ),
                faqItem(
                        "Yuk yo'qolsa yoki shikastlansa nima bo'ladi?",
                        "Yetkazish jarayonida nizo chiqsa, buyurtmani \"Nizoli\" holatiga o'tkazish mumkin. "
                                + "Olib ketish va yetkazish vaqtlari tizimda qayd etiladi, shu asosida holat "
                                + "tekshiriladi. Yuk qiymati bo'yicha kafolat va kompensatsiya tartibi "
                                + "alohida shartlarda belgilanadi.",
                        false
                ),
                faqItem(
                        "Kuryer bo'lish uchun nima kerak?",
                        "Ro'yxatdan o'tish, Telegram orqali telefon raqamingizni tasdiqlash va "
                                + "transport vositangiz ma'lumotlarini (turi, markasi, rangi va davlat raqami) "
                                + "kiritish kifoya. Tasdiqlangandan so'ng yuklarni qabul qilish va safar "
                                + "e'lon qilish mumkin bo'ladi.",
                        false
                )
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
        toggle.getElement().setAttribute("aria-expanded", String.valueOf(openByDefault));

        Span text = new Span(question);
        Div chevron = svgBox(ICON_CHEVRON, "home-faq-chevron");

        toggle.add(text, chevron);

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