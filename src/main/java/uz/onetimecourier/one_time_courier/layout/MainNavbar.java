package uz.onetimecourier.one_time_courier.layout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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

@CssImport("./styles/navbar-user-menu.css")
@CssImport("./styles/navbar-links.css")
public class MainNavbar extends Div {

    /** Ochiladigan menyu: bosilganda ochiladi/yopiladi, tashqariga bosilsa yoki Esc bosilsa yopiladi. */
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

    /** Navbar havolasi va unga mos yo'llar (alias'lar bilan birga). */
    private record NavItem(Anchor anchor, List<String> paths) {
    }

    private final Button toggle = new Button();
    private final List<NavItem> navItems = new ArrayList<>();
    private Registration navRegistration;

    public MainNavbar(AuthenticationContext authContext) {

        addClassName("main-navbar");

        // Asosiy navbar konteyneri
        Div inner = new Div();
        inner.addClassName("navbar-inner");

        // LOGO
        Anchor logo = new Anchor("/", "YukBor");
        logo.addClassName("navbar-logo");
        logo.getElement().setAttribute("aria-label", "YukBor — bosh sahifa");

        // MENYU
        Div menu = new Div(
                link("Yuklar", "/deliveries", VaadinIcon.PACKAGE),
                link("Kuryerlar", "/couriers", VaadinIcon.TRUCK, "/trips"),
                link("Biz haqimizda", "/about", VaadinIcon.INFO_CIRCLE)
        );
        if (isLoggedIn() && isSender()) {
            menu.add(link("Yuk joylash", "/deliveries/new", VaadinIcon.PLUS));
        }
        if (isLoggedIn() && isCourier()) {
            menu.add(link("Mening yetkazishlarim", "/courier/deliveries", VaadinIcon.CLIPBOARD_CHECK));
        }
        menu.addClassName("navbar-menu");

        // O'NG TOMON
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

        // MENYU VA ACTIONS PANELI
        Div panel = new Div(menu, actions);
        panel.addClassName("navbar-panel");

        // MOBIL MENYU TUGMASI
        toggle.setIcon(VaadinIcon.MENU.create());
        toggle.addClassName("navbar-toggle");
        toggle.setAriaLabel("Menyuni ochish");

        toggle.addClickListener(event -> {
            boolean open = !getClassNames().contains("open");
            setMenuOpen(open);
        });

        // ELEMENTLARNI JOYLASHTIRISH
        inner.add(logo, panel, toggle);
        add(inner);
    }

    // ---------------------------------------------------------------- joriy sahifa

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (navRegistration == null) {
            UI ui = attachEvent.getUI();
            // Har navigatsiyadan keyin joriy havola belgilanadi
            navRegistration = ui.addAfterNavigationListener(
                    event -> markActive(event.getLocation().getPath()));
            // Navbar birinchi marta chizilganda joriy sahifa uchun ham
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

    /** Joriy yo'lga mos havolani belgilaydi: rangi, tagidagi chiziq va aria-current. */
    private void markActive(String path) {
        String current = normalize(path == null ? "" : path);
        for (NavItem item : navItems) {
            boolean active = item.paths().contains(current);
            item.anchor().getClassNames().set("navbar-link--active", active);
            if (active) {
                item.anchor().getElement().setAttribute("aria-current", "page");
            } else {
                item.anchor().getElement().removeAttribute("aria-current");
            }
        }
    }

    // ---------------------------------------------------------------- profil menyusi

    /**
     * Profil chipi va uning tagida ochiladigan menyu:
     * Profil, Yuklarimning holati (SENDER) yoki Mening yetkazishlarim (COURIER), Chiqish (qizil).
     */
    private Div userMenu(AuthenticationContext authContext) {

        String firstName = displayName();

        Image photo = new Image(
                "/images/profile.png",
                "Profil rasmi"
        );

        photo.addClassName("navbar-profile-img");

        Div avatar = new Div(photo);
        avatar.addClassName("navbar-avatar");

        Span name = new Span(firstName);
        name.addClassName("navbar-user-name");

        Span arrow = new Span();
        arrow.add(VaadinIcon.ANGLE_DOWN.create());
        arrow.addClassName("navbar-user-arrow");

        Div chip = new Div(avatar, name, arrow);
        chip.addClassName("navbar-user");

        chip.getElement().setAttribute("role", "button");
        chip.getElement().setAttribute("tabindex", "0");
        chip.getElement().setAttribute("aria-haspopup", "menu");
        chip.getElement().setAttribute("aria-expanded", "false");
        chip.getElement().setAttribute("aria-label", "Profil menyusini ochish");

        // Ochiladigan menyu
        Div dropdown = new Div();
        dropdown.addClassName("nu-dropdown");
        dropdown.getElement().setAttribute("role", "menu");

        Div head = accountHeader();
        if (head != null) {
            dropdown.add(head);
        }

        /*Anchor profileItem = new Anchor("/profile", "Profil");
        profileItem.addClassName("nu-item");
        profileItem.getElement().setAttribute("role", "menuitem");
        dropdown.add(profileItem);*/

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

    /** Menyu tepasidagi sarlavha: to'liq ism va kontakt. Faqat setText ishlatiladi. */
    private Div accountHeader() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        Object principal = auth == null
                ? null
                : auth.getPrincipal();

        String fullName = null;
        String contact = null;

        if (principal instanceof CustomUserDetails user) {
            fullName = user.getFullName();
            contact = user.getContactLabel();
        } else if (principal instanceof OAuth2User oauthUser) {
            fullName = oauthUser.getAttribute("name");
            contact = oauthUser.getAttribute("email");
        }

        if ((fullName == null || fullName.isBlank()) && (contact == null || contact.isBlank())) {
            return null;
        }

        Div head = new Div();
        head.addClassName("nu-head");

        if (fullName != null && !fullName.isBlank()) {
            Span n = new Span(fullName.trim());
            n.addClassName("nu-head-name");
            head.add(n);
        }
        if (contact != null && !contact.isBlank()) {
            Span c = new Span(contact.trim());
            c.addClassName("nu-head-sub");
            head.add(c);
        }
        return head;
    }

    // ---------------------------------------------------------------- havolalar

    /**
     * Navbar havolasi. icon null bo'lsa, ikonkasiz. aliases: shu havola ham faol bo'ladigan qo'shimcha yo'llar.
     */
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
        navItems.add(new NavItem(anchor, List.copyOf(paths)));

        anchor.getElement().addEventListener("click", event -> {
            setMenuOpen(false);
        });

        return anchor;
    }

    /** "/deliveries" → "deliveries". */
    private static String normalize(String route) {
        return route == null ? "" : route.replaceFirst("^/+", "");
    }

    /**
     * Mobil menyuni ochish yoki yopish.
     */
    private void setMenuOpen(boolean open) {

        getClassNames().set("open", open);

        toggle.setIcon(
                (open ? VaadinIcon.CLOSE : VaadinIcon.MENU).create()
        );

        toggle.setAriaLabel(
                open ? "Menyuni yopish" : "Menyuni ochish"
        );
    }

    /**
     * Foydalanuvchi tizimga kirganligini tekshirish.
     */
    private boolean isLoggedIn() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** Faqat menyuni ko'rsatish uchun. Haqiqiy ruxsat tekshiruvi view va servisda. */
    private boolean isSender() {
        return hasRole("SENDER");
    }

    /** Faqat menyuni ko'rsatish uchun. Haqiqiy ruxsat tekshiruvi view va servisda. */
    private boolean isCourier() {
        return hasRole("COURIER");
    }

    private boolean hasRole(String role) {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        Object principal = auth == null
                ? null
                : auth.getPrincipal();

        return principal instanceof CustomUserDetails user
                && role.equals(String.valueOf(user.getRole()));
    }

    /**
     * Foydalanuvchi ismini olish.
     */
    private String displayName() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        Object principal = auth == null
                ? null
                : auth.getPrincipal();

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