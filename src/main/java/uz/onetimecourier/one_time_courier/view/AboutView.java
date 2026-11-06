package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import uz.onetimecourier.one_time_courier.layout.PageLayout;

@Route(value = "about", layout = PageLayout.class)
@PageTitle("Biz haqimizda | YukBor")
@AnonymousAllowed
@CssImport("./styles/about-page.css")
public class AboutView extends Div {

    /** Hero fonidagi yo'l chizig'i (faqat kompyuterda ko'rinadi). Ranglar brend ranglari. */
    private static final String ROUTE_SVG = """
        <svg class="ab-route-svg" viewBox="0 0 600 560" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
          <path d="M-20 520 C 120 420, 200 520, 320 360 S 470 170, 540 120"
                fill="none" stroke="#B7CDF6" stroke-width="4" stroke-linecap="round" stroke-dasharray="2 14"/>
          <circle cx="40" cy="498" r="10" fill="#fff" stroke="#1764ED" stroke-width="4"/>
          <g transform="translate(540 92)">
            <path d="M0-44C-23-44-38-29-38-10C-38 16 0 44 0 44C0 44 38 16 38-10C38-29 23-44 0-44Z" fill="#1764ED"/>
            <circle cx="0" cy="-12" r="12" fill="#fff"/>
          </g>
        </svg>""";

    public AboutView() {
        addClassName("ab-page");
        add(hero(), strip(), how(), roles(), cta(), new SiteFooter());
    }

    // ---------- Hero ----------
    private Component hero() {
        Span kicker = new Span("Biz haqimizda");
        kicker.addClassName("ab-kicker");

        Span mark = new Span("Odamlar ham.");
        mark.addClassName("ab-amber");
        H1 title = new H1();
        title.add(new Text("Yuk bir joydan boshqasiga boradi. "), mark);

        Paragraph lead = new Paragraph(
                "YukBor yuk jo‘natmoqchi bo‘lganlarni shu yo‘nalishda allaqachon ketayotgan "
                        + "odamlar bilan uchrashtiradi. Shahar ichida ham, viloyatlar orasida ham.");
        lead.addClassName("ab-lead");

        Div actions = new Div(
                button("/register", "Ro‘yxatdan o‘tish", "ab-btn-primary"),
                link("#how", "Qanday ishlashini ko‘rish ↓"));
        actions.addClassName("ab-actions");

        Div text = new Div(kicker, title, lead, actions);
        text.addClassName("ab-hero-text");

        // Rasmni o'zingiznikiga almashtiring: images/yukbor-kuryer-mashina.png
        Div frame = new Div(photo("/images/yukbor-kuryer-mashina.png", "Kuryer yukni mashinadan olmoqda"));
        frame.addClassName("ab-frame");

        Div outer = section("ab-hero", text, frame);
        outer.getElement().insertChild(0, new Html(ROUTE_SVG).getElement());
        return outer;
    }

    // ---------- Namuna yo'nalishlar ----------
    private Component strip() {
        Span label = new Span("Misol uchun yo‘nalishlar:");
        label.addClassName("ab-strip-label");
        Div row = new Div(label,
                new Span("Toshkent → Samarqand"),
                new Span("Andijon → Toshkent"),
                new Span("Chilonzor → Yunusobod"),
                new Span("Buxoro → Navoiy"));
        row.addClassName("ab-strip-row");
        return section("ab-strip", row);
    }

    // ---------- Qanday ishlaydi (bekatlar) ----------
    private Component how() {
        Div route = new Div(
                stop("1", "Yukni yozasiz",
                        "Qayerdan qayerga ketishi, nima ekani va buning uchun qancha to‘lashingizni kiritasiz."),
                stop("2", "Yo‘ldagi odam oladi",
                        "Shu yo‘nalishda ketayotgan kuryer buyurtmani qabul qiladi va yukni siz aytgan joydan oladi."),
                stop("3", "Yo‘lini kuzatasiz",
                        "Yuk qayerdaligini xaritada ko‘rasiz. Olinganda bir martalik kod so‘raladi, shuning uchun yuk begonaga ketmaydi."),
                stop("4", "Kod bilan topshiriladi",
                        "Qabul qiluvchi kodni aytadi, yuk topshirildi deb belgilanadi va pul kuryerga o‘tadi."));
        route.addClassName("ab-route");

        Div outer = section("ab-how", sectionHead("Jarayon", "Qanday ishlaydi"), route);
        outer.setId("how");
        return outer;
    }

    // ---------- Ikki rol ----------
    private Component roles() {
        Div sender = panel("ab-panel-sender", "Yuk jo‘natuvchi uchun",
                new String[]{
                        "Buyurtma berish uchun hisob kerak: yuk kimniki ekani aniq bo‘lishi uchun.",
                        "Yuk qayerdaligini xaritada ko‘rib turasiz.",
                        "To‘lov yuk topshirilgunicha ushlab turiladi."},
                "/register", "Yuk jo‘natish");

        Div courier = panel("ab-panel-courier", "Kuryer uchun",
                new String[]{
                        "Hech kim sizni majburlamaydi: yo‘lingizga mos yukni o‘zingiz tanlaysiz.",
                        "Yukni qabul qilgach, uni topshirish sizning majburiyatingiz. Ishonch ballingiz shunga bog‘liq.",
                        "Yuk olishdan oldin hujjatingiz tekshiriladi."},
                "/register", "Kuryer bo‘lish");

        Div grid = new Div(sender, courier);
        grid.addClassName("ab-panels");

        Paragraph note = new Paragraph(
                "Yukni qabul qiluvchida hisob bo‘lishi shart emas: unga topshirish uchun kod yuboriladi.");
        note.addClassName("ab-note");

        return section("ab-roles", sectionHead("Rollar", "Kim nima qiladi"), grid, note);
    }

    // ---------- Pastki chaqiriq ----------
    private Component cta() {
        H2 title = new H2("Yo‘lingiz bor, yuk ham bor. Qolganini biz hal qilamiz.");
        Div actions = new Div(
                button("/register", "Yuk jo‘natish", "ab-btn-white"),
                button("/register", "Kuryer bo‘lish", "ab-btn-outline-light"));
        actions.addClassName("ab-actions");
        Div box = new Div(title, actions);
        box.addClassName("ab-cta-box");
        return section("ab-cta", box);
    }

    // ---------- Yordamchi metodlar ----------

    /** To'liq kenglikdagi fon + ichida markazlashgan konteyner. */
    private static Div section(String cssClass, Component... children) {
        Div container = new Div(children);
        container.addClassName("ab-container");
        Div outer = new Div(container);
        outer.addClassNames("ab-section", cssClass);
        return outer;
    }

    /** Bo'lim sarlavhasi: yuqorida kichik ko'k yorliq, ostida katta sarlavha. */
    private static Div sectionHead(String kicker, String title) {
        Span label = new Span(kicker);
        label.addClassName("ab-section-kicker");
        H2 h = new H2(title);
        h.addClassName("ab-section-title");
        Div head = new Div(label, h);
        head.addClassName("ab-section-head");
        return head;
    }

    private static Div stop(String number, String title, String text) {
        Span dot = new Span(number);
        dot.addClassName("ab-dot");
        Div stop = new Div(dot, new H3(title), new Paragraph(text));
        stop.addClassName("ab-stop");
        return stop;
    }

    private static Div panel(String cssClass, String title, String[] items, String href, String buttonText) {
        UnorderedList list = new UnorderedList();
        for (String item : items) {
            list.add(new ListItem(item));
        }
        Div panel = new Div(new H3(title), list,
                button(href, buttonText, cssClass.endsWith("courier") ? "ab-btn-white" : "ab-btn-primary"));
        panel.addClassNames("ab-panel", cssClass);
        return panel;
    }

    private static Anchor button(String href, String text, String variant) {
        Anchor a = new Anchor(href, text);
        a.addClassNames("ab-btn", variant);
        return a;
    }

    private static Anchor link(String href, String text) {
        Anchor a = new Anchor(href, text);
        a.addClassName("ab-link");
        return a;
    }

    /** Rasm fon sifatida beriladi: fayl bo'lmasa oddiy rangli blok o'z joyida qoladi. */
    private static Div photo(String url, String alt) {
        Div photo = new Div();
        photo.addClassName("ab-photo");
        photo.getStyle().set("--photo", "url('" + url + "')");
        photo.getElement().setAttribute("role", "img");
        photo.getElement().setAttribute("aria-label", alt);
        return photo;
    }
}