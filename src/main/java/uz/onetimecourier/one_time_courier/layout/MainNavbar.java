package uz.onetimecourier.one_time_courier.layout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import uz.onetimecourier.one_time_courier.config.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Yuqori navbar (kompyuter) va telefon uchun pastki panel (ilova uslubida, 5 ta tugma).
 * Telefonda (≤820px) yuqorida logotip (ichki sahifalarda "orqaga" tugmasi ham), pastda tab panel turadi.
 * Ilova rejimida (PWA, TWA, WebView, ?app=1) <html> ga "app-mode" klassi qo'yiladi.
 */
@CssImport("./styles/navbar-user-menu.css")
@CssImport("./styles/navbar-links.css")
@CssImport("./styles/navbar-mobile-tabs.css")
public class MainNavbar extends Div {

    /** Aloqa havolasi. O'zingizning Telegram manzilingizga almashtiring. */
    private static final String TELEGRAM_URL = "https://t.me/yukbor";

    /** Shartlar va maxfiylik sahifalari yo'llari. Sahifalar yo'q bo'lsa, null qiling. */
    private static final String TERMS_ROUTE = "/terms";
    private static final String PRIVACY_ROUTE = "/privacy";

    /** Asosiy (tab) sahifalar: ularda "orqaga" tugmasi ko'rinmaydi. */
    private static final Set<String> ROOT_PATHS = Set.of(
            "", "deliveries", "couriers", "trips", "reyting/kuryerlar", "about",
            "deliveries/mine", "courier/deliveries", "login", "register");

    private static final String DROPDOWN_JS = """
            const w = this;
            if (w.__nuInit) { return; }
            w.__nuInit = true;
            const chip = w.querySelector('.navbar-user');
            const dropdown = w.querySelector('.nu-dropdown');
            if (!chip || !dropdown) { return; }
            const setOpen = open => {
              w.classList.toggle('nu-open', open);
              chip.setAttribute('aria-expanded', String(open));
            };
            chip.addEventListener('click', () => setOpen(!w.classList.contains('nu-open')));
            chip.addEventListener('keydown', e => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                setOpen(!w.classList.contains('nu-open'));
              }
            });
            const onDoc = e => {
              if (!w.isConnected) {
                document.removeEventListener('click', onDoc);
                document.removeEventListener('keydown', onKey);
                return;
              }
              if (!w.contains(e.target)) { setOpen(false); }
            };
            const onKey = e => {
              if (e.key === 'Escape' && w.classList.contains('nu-open')) {
                setOpen(false);
                chip.focus();
              }
            };
            document.addEventListener('click', onDoc);
            document.addEventListener('keydown', onKey);
            dropdown.addEventListener('click', () => setOpen(false));
            """;

    /** Ilova rejimini aniqlaydi (PWA, TWA, WebView, ?app=1). ?app=0 oddiy rejimga qaytaradi. */
    private static final String APP_MODE_JS = """
            let app = false;
            try {
              const q = new URLSearchParams(window.location.search);
              if (q.get('app') === '1') { localStorage.setItem('yb-app', '1'); }
              if (q.get('app') === '0') { localStorage.removeItem('yb-app'); }
              app = localStorage.getItem('yb-app') === '1';
            } catch (e) {}
            const ua = navigator.userAgent || '';
            app = app
              || window.matchMedia('(display-mode: standalone)').matches
              || window.matchMedia('(display-mode: fullscreen)').matches
              || window.matchMedia('(display-mode: minimal-ui)').matches
              || window.navigator.standalone === true
              || (document.referrer || '').startsWith('android-app://')
              || /; wv\\)/.test(ua);
            document.documentElement.classList.toggle('app-mode', app);
            """;

    /** Sahifa almashganda eng tepaga qaytaradi. */
    private static final String SCROLL_TOP_JS = """
            window.scrollTo({top: 0, left: 0, behavior: 'instant'});
            document.documentElement.scrollTop = 0;
            document.body.scrollTop = 0;
            """;

    /**
     * Telefonda (≤820px): scroll qiladigan element(lar)ni topib, pastiga pastki panel balandligicha
     * bo'shliq beradi. Scroll ichki elementda bo'lmasa, bo'shliq body ga beriladi.
     * Kompyuterda hamma narsa tozalanadi.
     */
    private static final String BOTTOM_PAD_JS = """
            (function () {
              if (window.__ybPad) { window.__ybPad(); return; }
              const PAD = 'calc(40px + env(safe-area-inset-bottom, 0px) + 28px)';
              const apply = () => {
                const mobile = window.matchMedia('(max-width: 820px)').matches;
                const vh = window.innerHeight;
                let innerScroller = false;
                document.querySelectorAll('body *').forEach(el => {
                  if (el.closest('.main-navbar') || el.closest('vaadin-dialog-overlay')
                      || el.closest('vaadin-select-overlay') || el.closest('vaadin-notification-card')
                      || el.closest('vaadin-date-picker-overlay') || el.closest('vaadin-time-picker-overlay')) {
                    return;
                  }
                  const cs = getComputedStyle(el);
                  const scrolls = /(auto|scroll)/.test(cs.overflowY)
                      && el.clientHeight > vh * 0.4 && el.scrollHeight > el.clientHeight;
                  if (scrolls && mobile) {
                    innerScroller = true;
                    el.style.setProperty('padding-bottom', PAD, 'important');
                    el.dataset.ybPad = '1';
                  } else if (el.dataset.ybPad === '1' && !(mobile && scrolls)) {
                    el.style.removeProperty('padding-bottom');
                    delete el.dataset.ybPad;
                  }
                });
                if (mobile && !innerScroller) {
                  document.body.style.setProperty('padding-bottom', PAD, 'important');
                } else {
                  document.body.style.removeProperty('padding-bottom');
                }
              };
              window.__ybPad = apply;
              window.addEventListener('resize', apply);
              new MutationObserver(() => {
                clearTimeout(window.__ybPadT);
                window.__ybPadT = setTimeout(apply, 120);
              }).observe(document.body, {childList: true, subtree: true});
              apply();
            })();
            """;

    private record NavItem(Anchor anchor, List<String> paths, String activeClass) {
    }

    private final List<NavItem> navItems = new ArrayList<>();
    private Registration navRegistration;

    private final Div tabs = new Div();
    private final NativeButton profileTab = new NativeButton();
    private final NativeButton backButton = new NativeButton();

    public MainNavbar(AuthenticationContext authContext) {

        addClassName("main-navbar");

        Div inner = new Div();
        inner.addClassName("navbar-inner");

        // Orqaga tugmasi (faqat telefonda, ichki sahifalarda)
        Icon backIcon = VaadinIcon.ANGLE_LEFT.create();
        backIcon.getElement().setAttribute("aria-hidden", "true");
        backButton.add(backIcon);
        backButton.addClassName("navbar-back");
        backButton.getElement().setAttribute("type", "button");
        backButton.getElement().setAttribute("aria-label", "Orqaga");
        backButton.addClickListener(e -> UI.getCurrent().getPage().executeJs("window.history.back();"));

        Anchor logo = new Anchor("/");
        logo.addClassName("navbar-logo");
        logo.getElement().setAttribute("aria-label", "YukBor — bosh sahifa");

        Span accent = new Span("Bor");
        accent.addClassName("navbar-logo-accent");

        Span word = new Span();
        word.addClassName("navbar-logo-word");
        word.add(new Span("Yuk"), accent);

        logo.add(word);

        // Menyu (kompyuter)
        Div menu = new Div(
                link("Yuklar", "/deliveries", VaadinIcon.PACKAGE),
                link("Safarlar", "/couriers", VaadinIcon.TRUCK, "/trips"),
                link("Ishonchli kuryerlar", "/reyting/kuryerlar", VaadinIcon.TROPHY),
                link("Biz haqimizda", "/about", VaadinIcon.INFO_CIRCLE)
        );
        if (isLoggedIn() && isSender()) {
            Anchor send = link("Yuk joylash", "/deliveries/new", VaadinIcon.PLUS);
            send.addClassName("navbar-link--cta");
            menu.add(send);
        }
        if (isLoggedIn() && isCourier()) {
            menu.add(link("Mening yetkazishlarim", "/courier/deliveries", VaadinIcon.CLIPBOARD_CHECK));
        }
        menu.addClassName("navbar-menu");

        // O'ng tomon (kompyuter)
        Div actions = new Div();
        actions.addClassName("navbar-actions");

        if (isLoggedIn()) {
            actions.add(userMenu(authContext));
        } else {
            Anchor login = link("Kirish", "/login", null);
            login.addClassName("navbar-login");

            Anchor register = link("Ro'yxatdan o'tish", "/register", null);
            register.addClassName("navbar-register");

            actions.add(login, register);
        }

        Div panel = new Div(menu, actions);
        panel.addClassName("navbar-panel");

        inner.add(backButton, logo, panel);
        add(inner);

        buildTabs(authContext);
        add(tabs);
    }

    // ---------------------------------------------------------------- pastki panel

    private void buildTabs(AuthenticationContext authContext) {

        tabs.addClassName("mtb");

        Div bar = new Div();
        bar.addClassName("mtb-bar");
        bar.getElement().setAttribute("role", "navigation");
        bar.getElement().setAttribute("aria-label", "Asosiy menyu");

        bar.add(tab("Bosh sahifa", "/", VaadinIcon.HOME, false));

        if (!isLoggedIn()) {
            bar.add(tab("Yuklar", "/deliveries", VaadinIcon.PACKAGE, false));
            bar.add(tab("Safarlar", "/couriers", VaadinIcon.TRUCK, false, "/trips"));
            bar.add(tab("Kuryerlar", "/reyting/kuryerlar", VaadinIcon.TROPHY, false));
            bar.add(tab("Kirish", "/login", VaadinIcon.SIGN_IN, false));
            tabs.add(bar);
            return;
        }

        bar.add(tab("Safarlar", "/couriers", VaadinIcon.TRUCK, false, "/trips"));

        if (isSender()) {
            bar.add(tab("Yuk joylash", "/deliveries/new", VaadinIcon.PLUS, true, "/deliveries/edit"));
        } else if (isCourier()) {
            bar.add(tab("Yetkazishlar", "/courier/deliveries", VaadinIcon.CLIPBOARD_CHECK, true));
        } else {
            bar.add(tab("Kuryerlar", "/reyting/kuryerlar", VaadinIcon.TROPHY, false));
        }

        bar.add(tab("Yuklar", "/deliveries", VaadinIcon.PACKAGE, false));

        profileTab.addClassName("mtb-item");
        profileTab.getElement().setAttribute("type", "button");
        profileTab.getElement().setAttribute("aria-haspopup", "menu");
        profileTab.getElement().setAttribute("aria-expanded", "false");
        profileTab.getElement().setAttribute("aria-label", "Profil menyusi");
        Icon profileIcon = VaadinIcon.USER.create();
        profileIcon.addClassName("mtb-icon");
        profileIcon.getElement().setAttribute("aria-hidden", "true");
        Span profileLabel = new Span("Profil");
        profileLabel.addClassName("mtb-label");
        profileTab.add(profileIcon, profileLabel);
        profileTab.addClickListener(e -> setSheetOpen(!tabs.getClassNames().contains("mtb-open")));
        bar.add(profileTab);

        Div backdrop = new Div();
        backdrop.addClassName("mtb-backdrop");
        backdrop.getElement().setAttribute("aria-hidden", "true");
        backdrop.addClickListener(e -> setSheetOpen(false));

        tabs.add(backdrop, profileSheet(authContext), bar);
    }

    private Anchor tab(String text, String route, VaadinIcon icon, boolean center, String... aliases) {

        Anchor anchor = new Anchor(route);
        anchor.addClassName("mtb-item");
        if (center) {
            anchor.addClassName("mtb-item--center");
        }

        Icon iconComponent = icon.create();
        iconComponent.addClassName("mtb-icon");
        iconComponent.getElement().setAttribute("aria-hidden", "true");

        Span label = new Span(text);
        label.addClassName("mtb-label");

        anchor.add(iconComponent, label);

        List<String> paths = new ArrayList<>();
        paths.add(normalize(route));
        for (String alias : aliases) {
            paths.add(normalize(alias));
        }
        navItems.add(new NavItem(anchor, List.copyOf(paths), "mtb-item--active"));

        anchor.getElement().addEventListener("click", event -> setSheetOpen(false));
        return anchor;
    }

    private Div profileSheet(AuthenticationContext authContext) {

        Div sheet = new Div();
        sheet.addClassName("mtb-sheet");
        sheet.getElement().setAttribute("role", "menu");

        String[] info = accountInfo();
        if (info[0] != null || info[1] != null) {
            Div head = new Div();
            head.addClassName("mtb-sheet-head");
            if (info[0] != null) {
                Span n = new Span(info[0]);
                n.addClassName("mtb-sheet-name");
                head.add(n);
            }
            if (info[1] != null) {
                Span c = new Span(info[1]);
                c.addClassName("mtb-sheet-sub");
                head.add(c);
            }
            String role = roleLabel();
            if (role != null) {
                Span badge = new Span(role);
                badge.addClassNames("navbar-role", isCourier() ? "navbar-role--courier" : "navbar-role--sender");
                head.add(badge);
            }
            sheet.add(head);
        }

        if (isSender()) {
            sheet.add(sheetLink("Yuklarimning holati", "/deliveries/mine", VaadinIcon.CLIPBOARD_TEXT));
        }
        if (isCourier()) {
            sheet.add(sheetLink("Mening yetkazishlarim", "/courier/deliveries", VaadinIcon.CLIPBOARD_CHECK));
            sheet.add(sheetLink("Bildirishnomalar", "/kuryer/bildirishnoma", VaadinIcon.BELL));
        }
        sheet.add(sheetLink("Ishonchli kuryerlar", "/reyting/kuryerlar", VaadinIcon.TROPHY));
        sheet.add(sheetLink("Biz haqimizda", "/about", VaadinIcon.INFO_CIRCLE));

        Anchor contact = sheetLink("Telegram'da yozing", TELEGRAM_URL, VaadinIcon.CHAT);
        contact.setTarget("_blank");
        contact.getElement().setAttribute("rel", "noopener noreferrer");
        sheet.add(contact);

        if (TERMS_ROUTE != null) {
            sheet.add(sheetLink("Foydalanish shartlari", TERMS_ROUTE, VaadinIcon.FILE_TEXT));
        }
        if (PRIVACY_ROUTE != null) {
            sheet.add(sheetLink("Maxfiylik siyosati", PRIVACY_ROUTE, VaadinIcon.LOCK));
        }

        NativeButton logout = new NativeButton("Chiqish");
        logout.addClassNames("mtb-sheet-item", "mtb-sheet-item--danger");
        logout.getElement().setAttribute("type", "button");
        logout.getElement().setAttribute("role", "menuitem");
        logout.addClickListener(e -> authContext.logout());
        sheet.add(logout);

        return sheet;
    }

    private Anchor sheetLink(String text, String route, VaadinIcon icon) {
        Anchor a = new Anchor(route);
        a.addClassName("mtb-sheet-item");
        a.getElement().setAttribute("role", "menuitem");

        Icon i = icon.create();
        i.addClassName("mtb-sheet-icon");
        i.getElement().setAttribute("aria-hidden", "true");
        a.add(i, new Span(text));

        a.getElement().addEventListener("click", event -> setSheetOpen(false));
        return a;
    }

    private void setSheetOpen(boolean open) {
        tabs.getClassNames().set("mtb-open", open);
        profileTab.getElement().setAttribute("aria-expanded", String.valueOf(open));
        profileTab.getClassNames().set("mtb-item--active", open);
    }

    // ---------------------------------------------------------------- joriy sahifa

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (navRegistration == null) {
            UI ui = attachEvent.getUI();
            ui.getPage().executeJs(APP_MODE_JS);
            ui.getPage().executeJs(BOTTOM_PAD_JS);
            navRegistration = ui.addAfterNavigationListener(event -> {
                markActive(event.getLocation().getPath());
                ui.getPage().executeJs(SCROLL_TOP_JS);
                ui.getPage().executeJs(BOTTOM_PAD_JS);
            });
            var location = ui.getInternals().getActiveViewLocation();
            if (location != null) {
                markActive(location.getPath());
            }
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (navRegistration != null) {
            navRegistration.remove();
            navRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    private void markActive(String path) {
        String current = normalize(path == null ? "" : path);
        for (NavItem item : navItems) {
            boolean active = item.paths().stream()
                    .anyMatch(p -> p.equals(current) || (current.startsWith(p + "/") && p.endsWith("edit")));
            item.anchor().getClassNames().set(item.activeClass(), active);
            if (active) {
                item.anchor().getElement().setAttribute("aria-current", "page");
            } else {
                item.anchor().getElement().removeAttribute("aria-current");
            }
        }
        getClassNames().set("navbar--back", !ROOT_PATHS.contains(current));
        setSheetOpen(false);
    }

    // ---------------------------------------------------------------- profil menyusi (kompyuter)

    private Div userMenu(AuthenticationContext authContext) {

        String firstName = displayName();

        Image photo = new Image("/images/profile.png", "Profil rasmi");
        photo.addClassName("navbar-profile-img");

        Div avatar = new Div(photo);
        avatar.addClassName("navbar-avatar");

        Span name = new Span(firstName);
        name.addClassName("navbar-user-name");

        Span arrow = new Span();
        arrow.add(VaadinIcon.ANGLE_DOWN.create());
        arrow.addClassName("navbar-user-arrow");

        Div chip = new Div(avatar, name);
        String role = roleLabel();
        if (role != null) {
            Span badge = new Span(role);
            badge.addClassNames("navbar-role", isCourier() ? "navbar-role--courier" : "navbar-role--sender");
            chip.add(badge);
        }
        chip.add(arrow);
        chip.addClassName("navbar-user");

        chip.getElement().setAttribute("role", "button");
        chip.getElement().setAttribute("tabindex", "0");
        chip.getElement().setAttribute("aria-haspopup", "menu");
        chip.getElement().setAttribute("aria-expanded", "false");
        chip.getElement().setAttribute("aria-label", "Profil menyusini ochish");

        Div dropdown = new Div();
        dropdown.addClassName("nu-dropdown");
        dropdown.getElement().setAttribute("role", "menu");

        Div head = accountHeader();
        if (head != null) {
            dropdown.add(head);
        }

        if (isSender()) {
            Anchor deliveriesItem = new Anchor("/deliveries/mine", "Yuklarimning holati");
            deliveriesItem.addClassName("nu-item");
            deliveriesItem.getElement().setAttribute("role", "menuitem");
            dropdown.add(deliveriesItem);
        }
        if (isCourier()) {
            Anchor courierItem = new Anchor("/courier/deliveries", "Mening yetkazishlarim");
            courierItem.addClassName("nu-item");
            courierItem.getElement().setAttribute("role", "menuitem");
            dropdown.add(courierItem);

            Anchor notificationsItem = new Anchor("/kuryer/bildirishnoma", "Bildirishnomalar");
            notificationsItem.addClassName("nu-item");
            notificationsItem.getElement().setAttribute("role", "menuitem");
            dropdown.add(notificationsItem);
        }

        Div separator = new Div();
        separator.addClassName("nu-sep");
        separator.getElement().setAttribute("role", "separator");
        dropdown.add(separator);

        NativeButton logoutItem = new NativeButton("Chiqish");
        logoutItem.addClassNames("nu-item", "nu-item--danger");
        logoutItem.getElement().setAttribute("type", "button");
        logoutItem.getElement().setAttribute("role", "menuitem");
        logoutItem.addClickListener(event -> authContext.logout());
        dropdown.add(logoutItem);

        Div wrapper = new Div(chip, dropdown);
        wrapper.addClassName("nu-wrap");
        wrapper.addAttachListener(event -> wrapper.getElement().executeJs(DROPDOWN_JS));
        return wrapper;
    }

    private String[] accountInfo() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth == null ? null : auth.getPrincipal();

        String fullName = null;
        String contact = null;

        if (principal instanceof CustomUserDetails user) {
            fullName = user.getFullName();
            contact = user.getContactLabel();
        } else if (principal instanceof OAuth2User oauthUser) {
            fullName = oauthUser.getAttribute("name");
            contact = oauthUser.getAttribute("email");
        }

        return new String[]{
                fullName == null || fullName.isBlank() ? null : fullName.trim(),
                contact == null || contact.isBlank() ? null : contact.trim()
        };
    }

    private Div accountHeader() {

        String[] info = accountInfo();
        if (info[0] == null && info[1] == null) {
            return null;
        }

        Div head = new Div();
        head.addClassName("nu-head");

        if (info[0] != null) {
            Span n = new Span(info[0]);
            n.addClassName("nu-head-name");
            head.add(n);
        }
        if (info[1] != null) {
            Span c = new Span(info[1]);
            c.addClassName("nu-head-sub");
            head.add(c);
        }
        return head;
    }

    // ---------------------------------------------------------------- havolalar

    private Anchor link(String text, String route, VaadinIcon icon, String... aliases) {

        Anchor anchor = new Anchor(route, text);
        anchor.addClassName("navbar-link");

        if (icon != null) {
            Icon iconComponent = icon.create();
            iconComponent.addClassName("navbar-link-icon");
            iconComponent.getElement().setAttribute("aria-hidden", "true");
            anchor.addComponentAsFirst(iconComponent);
        }

        List<String> paths = new ArrayList<>();
        paths.add(normalize(route));
        for (String alias : aliases) {
            paths.add(normalize(alias));
        }
        navItems.add(new NavItem(anchor, List.copyOf(paths), "navbar-link--active"));

        return anchor;
    }

    private static String normalize(String route) {
        return route == null ? "" : route.replaceFirst("^/+", "");
    }

    private boolean isLoggedIn() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    private boolean isSender() {
        return hasRole("SENDER");
    }

    private boolean isCourier() {
        return hasRole("COURIER");
    }

    private boolean hasRole(String role) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth == null ? null : auth.getPrincipal();

        return principal instanceof CustomUserDetails user
                && role.equals(String.valueOf(user.getRole()));
    }

    private String roleLabel() {
        if (isCourier()) {
            return "Kuryer";
        }
        if (isSender()) {
            return "Jo'natuvchi";
        }
        return null;
    }

    private String displayName() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth == null ? null : auth.getPrincipal();

        String name = null;

        if (principal instanceof CustomUserDetails user) {
            name = user.getFullName();
        } else if (principal instanceof OAuth2User oauthUser) {
            String givenName = oauthUser.getAttribute("given_name");
            String fullName = oauthUser.getAttribute("name");

            name = (givenName != null && !givenName.isBlank())
                    ? givenName
                    : fullName;
        }

        if (name == null || name.isBlank()) {
            return "Profil";
        }

        return name.trim().split("\\s+")[0];
    }
}