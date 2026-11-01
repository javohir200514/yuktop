package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;
import uz.onetimecourier.one_time_courier.layout.PageLayout;

/**
 * Kirgan, lekin roli mos kelmagan foydalanuvchiga tushunarli xabar ko'rsatadi.
 * Loyihada AccessDeniedException uchun boshqa xato sahifa bo'lsa, shuni o'chirib,
 * o'sha sahifaga xabarni qo'shing (ikkita bo'lsa ilova ishga tushmaydi).
 */
@ParentLayout(PageLayout.class)
@CssImport("./styles/create-delivery.css")
public class AccessDeniedView extends Div implements HasErrorParameter<AccessDeniedException> {

    private final Paragraph message = new Paragraph();

    public AccessDeniedView() {
        addClassName("cd-denied");

        H1 title = new H1("Kirish mumkin emas");
        title.addClassName("cd-title");
        message.addClassName("cd-lead");

        NativeButton home = new NativeButton("Bosh sahifaga");
        home.addClassName("cd-submit");
        home.getElement().setAttribute("type", "button");
        home.addClickListener(e -> UI.getCurrent().navigate(""));

        add(title, message, home);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        String path = event.getLocation().getPath();
        if (path.startsWith("deliveries/new")) {
            message.setText("Yuk joylash uchun jo'natuvchi hisobi kerak.");
        } else {
            message.setText("Bu sahifaga kirish huquqingiz yo'q.");
        }
        return HttpServletResponse.SC_FORBIDDEN;
    }
}