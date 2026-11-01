package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import uz.onetimecourier.one_time_courier.dto.VehicleInfo;
import uz.onetimecourier.one_time_courier.enums.VehicleType;
import uz.onetimecourier.one_time_courier.service.VehicleVerificationService;
import uz.onetimecourier.one_time_courier.service.VehicleVerificationService.VerificationResult;

import java.util.function.Consumer;

/** Ro'yxatdan o'tgandan keyin transport ma'lumotlarini so'raydi va tekshiradi. */
public class VehicleVerificationDialog extends Dialog {

    public VehicleVerificationDialog(VehicleVerificationService verifier,
                                     Consumer<VehicleInfo> onVerified) {
        setHeaderTitle("Transport ma'lumotlari");
        setCloseOnOutsideClick(false);

        Select<VehicleType> type = new Select<>();
        type.setLabel("Transport turi");
        type.setItems(VehicleType.values());
        type.setItemLabelGenerator(VehicleType::getLabel);

        TextField brand = new TextField("Marka");
        TextField model = new TextField("Model");
        TextField color = new TextField("Rang");
        TextField plate = new TextField("Davlat raqami");
        plate.setPlaceholder("01 A 123 BC");

        Paragraph error = new Paragraph();
        error.getStyle().set("color", "var(--lumo-error-text-color)");
        error.setVisible(false);

        Button cancel = new Button("Bekor qilish", e -> close());

        Button verify = new Button("Tasdiqlash", e -> {
            VerificationResult result = verifier.verify(new VehicleInfo(
                    type.getValue(), brand.getValue(), model.getValue(),
                    color.getValue(), plate.getValue()));
            if (!result.ok()) {
                error.setText(result.error());
                error.setVisible(true);
                return;
            }
            close();
            onVerified.accept(result.verified());
        });
        verify.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new FormLayout(type, brand, model, color, plate), error);
        getFooter().add(cancel, verify);
    }
}