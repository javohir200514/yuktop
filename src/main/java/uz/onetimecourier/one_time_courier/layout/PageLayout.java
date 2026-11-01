package uz.onetimecourier.one_time_courier.layout;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

@AnonymousAllowed
public class PageLayout extends Div implements RouterLayout {

    private final Div content = new Div();

    public PageLayout(AuthenticationContext authContext) {
        addClassName("yb-page");
        content.addClassName("yb-page-content");
        add(new MainNavbar(authContext), content);
    }

    @Override
    public void showRouterLayoutContent(HasElement routerContent) {
        content.getElement().removeAllChildren();
        if (routerContent != null) {
            content.getElement().appendChild(routerContent.getElement());
        }
    }
}