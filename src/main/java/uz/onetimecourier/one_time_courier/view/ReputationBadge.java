package uz.onetimecourier.one_time_courier.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import uz.onetimecourier.one_time_courier.dto.ReputationDto;

import java.util.Locale;

/** Foydalanuvchi reytingi: o'rtacha yulduz, baholar soni va bajarilgan yuklar foizi. */
public class ReputationBadge extends Div {

    private static final String TEXT = "#1e293b";
    private static final String MUTED = "#64748b";

    public ReputationBadge(ReputationDto r) {
        addClassName("rep-badge");
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px");

        if (r.ratingCount() == 0) {
            Span none = new Span("Hozircha baholar yo'q");
            none.getStyle().set("color", MUTED).set("font-size", "0.9rem");
            add(none);
        } else {
            Span stars = new Span(String.format(Locale.US, "★ %.1f", r.avgStars()));
            stars.getStyle().set("color", "#d97706").set("font-weight", "700").set("font-size", "1rem");

            Span count = new Span("(" + r.ratingCount() + " ta baho)");
            count.getStyle().set("color", MUTED).set("font-size", "0.85rem");

            Div top = new Div(stars, count);
            top.getStyle().set("display", "flex").set("align-items", "center").set("gap", "8px");
            add(top);
        }

        if (r.total() == 0) {
            Span fresh = new Span("Hali yuklar yo'q");
            fresh.getStyle().set("color", MUTED).set("font-size", "0.85rem");
            add(fresh);
            return;
        }

        double pct = Math.max(0, Math.min(100, r.completionPercent()));
        String color = pct >= 80 ? "#16a34a" : pct >= 50 ? "#f59e0b" : "#dc2626";

        Span label = new Span(String.format(Locale.US,
                "Bajarilgan: %d%%  ·  %d ta yuk", Math.round(pct), r.total()));
        label.getStyle().set("color", TEXT).set("font-size", "0.85rem");

        Div bar = new Div();
        bar.getStyle()
                .set("height", "6px")
                .set("width", pct + "%")
                .set("background", color)
                .set("border-radius", "999px");

        Div track = new Div(bar);
        track.getStyle()
                .set("width", "100%")
                .set("height", "6px")
                .set("background", "#e5e7eb")
                .set("border-radius", "999px")
                .set("overflow", "hidden");

        add(label, track);
    }
}