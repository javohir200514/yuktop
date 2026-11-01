package uz.onetimecourier.one_time_courier.layout;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

@AnonymousAllowed
public class AuthLayout extends Div implements RouterLayout {


    public static final String LOGO = """
        <span class="logo-mark">
            <svg viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0116 0z"/>
                <path d="M9.5 9.5L12 8l2.5 1.5v3L12 14l-2.5-1.5z"/>
            </svg>
        </span>
        """;

    private static final String MARK = """
        <span class="brand-mark">
            <svg viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 8v8l-9 5-9-5V8l9-5z"/>
                <path d="M3.3 7L12 12l8.7-5M12 22V12"/>
            </svg>
        </span>
        """;

    private final Div content = new Div();

    public AuthLayout(AuthenticationContext authContext) {
        addClassName("yb-shell");
        content.addClassName("yb-content");
        add(new MainNavbar(authContext), content);
    }

    public static Div brand() {
        Div brand = new Div();
        brand.addClassName("brand");
        brand.add(new Html(MARK));

        Div name = new Div();
        name.addClassName("brand-name");
        name.getElement().setProperty("innerHTML", "Yuk<span>Bor</span>");

        brand.add(name);
        return brand;
    }

    @Override
    public void showRouterLayoutContent(HasElement routerContent) {
        content.getElement().removeAllChildren();
        if (routerContent != null) {
            content.getElement().appendChild(routerContent.getElement());
        }
    }
}