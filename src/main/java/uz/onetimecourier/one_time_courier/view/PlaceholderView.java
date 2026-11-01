package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import uz.onetimecourier.one_time_courier.layout.AuthLayout;

@Route(value = "p/:section", layout = AuthLayout.class)
@PageTitle("One-Time Courier")
@AnonymousAllowed
public class PlaceholderView extends Div implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String title = switch (event.getRouteParameters().get("section").orElse("")) {
            case "deliveries" -> "Yetkazmalar";
            case "safety" -> "Xavfsizlik";
            case "about" -> "Biz haqimizda";
            default -> "Sahifa";
        };
        add(new H2(title), new Paragraph("Bu bo'lim tez orada tayyor bo'ladi."));
    }
}