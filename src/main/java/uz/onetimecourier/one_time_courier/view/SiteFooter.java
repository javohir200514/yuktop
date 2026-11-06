package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

/**
 * Saytning umumiy pastki qismi (footer). Istalgan sahifaga qo'shish mumkin:
 * {@code add(new SiteFooter());}
 */
@CssImport("./styles/footer.css")
public class SiteFooter extends Footer {

    private static final String SUPPORT_TELEGRAM_URL = "https://t.me/Javohir_05_08";

    private static final String ICON_TELEGRAM = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <circle cx="24" cy="24" r="22" fill="#ffffff"/>
              <path d="M9.5 23.5l25-10c1.2-.5 2.3.3 1.9 2.1l-4.3 20.2c-.3 1.4-1.2 1.7-2.4 1.1l-6.6-4.9-3.2 3.1c-.4.4-.7.7-1.4.7l.5-6.7 12.2-11c.5-.5-.1-.7-.8-.3l-15 9.5-6.5-2c-1.4-.4-1.4-1.4.6-2.2z"
                    fill="#1d5fd1"/>
            </svg>
            """;

    public SiteFooter() {

        addClassNames("home-footer", "home-footer-bleed");

        // ---- Yuqori chiziq: sariq-qora "ogohlantirish" lentasi ----
        Div stripe = new Div();
        stripe.addClassName("home-footer-stripe");
        stripe.getElement().setAttribute("aria-hidden", "true");

        // ---- Asosiy qism ----
        Div inner = new Div();
        inner.addClassName("home-footer-inner");

        Div grid = new Div(
                brandColumn(),
                footerColumn("Platforma",
                        footerLink("Yuklar", "/deliveries"),
                        footerLink("Kuryerlar", "/couriers"),
                        footerLink("Biz haqimizda", "/about"),
                        footerLink("Yuk joylash", "/deliveries/new")),
                footerColumn("Kuryerlar uchun",
                        footerLink("Kuryer bo'lish", "/register"),
                        footerLink("Safar e'lon qilish", "/trips/new")),
                footerColumn("Hisob",
                        footerLink("Kirish", "/login"),
                        footerLink("Ro'yxatdan o'tish", "/register"))
        );
        grid.addClassName("home-footer-grid");
        inner.add(grid);

        // ---- Pastki chiziq ----
        Div bottom = new Div();
        bottom.addClassName("home-footer-bottom");

        Div bottomInner = new Div();
        bottomInner.addClassName("home-footer-bottom-inner");

        Span copy = new Span("© 2026 YukBor. Barcha huquqlar himoyalangan.");
        copy.addClassName("home-footer-copy");

        Span note = new Span("Yo'lingiz. Kimningdir yetkazmasi.");
        note.addClassName("home-footer-note");

        bottomInner.add(copy, note);
        bottom.add(bottomInner);

        add(stripe, inner, bottom);
    }

    // ---------------------------------------------------------------- ustunlar

    private Div brandColumn() {

        Div col = new Div();
        col.addClassName("home-footer-brand");

        // Logotip: navbar bilan bir xil sariq belgi
        Div logoRow = new Div();
        logoRow.addClassName("home-footer-logo-row");

        Span plate = new Span("YB");
        plate.addClassName("home-footer-plate");
        plate.getElement().setAttribute("aria-hidden", "true");

        Span accent = new Span("BOR");
        accent.addClassName("home-footer-brand-accent");

        Span brandName = new Span();
        brandName.addClassName("home-footer-brand-name");
        brandName.add(new Span("YUK"), accent);

        logoRow.add(plate, brandName);

        Paragraph text = new Paragraph(
                "Shahar ichida va viloyatlararo yetkazish uchun ishonchli platforma. "
                        + "Jo'natuvchi va kuryerni bir joyga yig'amiz."
        );
        text.addClassName("home-footer-text");

        Anchor telegram = new Anchor(SUPPORT_TELEGRAM_URL);
        telegram.addClassName("home-footer-social");
        telegram.setTarget("_blank");
        telegram.getElement().setAttribute("rel", "noopener noreferrer");
        telegram.add(
                svgBox(ICON_TELEGRAM, "home-footer-social-icon"),
                new Span("Telegram'da yozing")
        );

        col.add(logoRow, text, telegram);
        return col;
    }

    private Div footerColumn(String title, Anchor... links) {

        Div col = new Div();
        col.addClassName("home-footer-col");

        Span heading = new Span(title);
        heading.addClassName("home-footer-title");

        Div list = new Div(links);
        list.addClassName("home-footer-links");

        col.add(heading, list);
        return col;
    }

    private Anchor footerLink(String text, String href) {
        Anchor a = new Anchor(href, text);
        a.addClassName("home-footer-link");
        return a;
    }

    /** Inline SVG ni konteynerga joylaydi. SVG matni faqat kod ichidagi konstantadan keladi. */
    private static Div svgBox(String svg, String cssClass) {
        Div box = new Div();
        box.addClassName(cssClass);
        box.getElement().setProperty("innerHTML", svg);
        box.getElement().setAttribute("aria-hidden", "true");
        return box;
    }
}