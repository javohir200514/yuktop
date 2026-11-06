package uz.onetimecourier.one_time_courier;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Push
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
@EnableScheduling
@PWA(
        name = "YukBor",
        shortName = "YukBor",
        description = "Shahar ichida va viloyatlararo yetkazish platformasi",
        backgroundColor = "#121212",
        themeColor = "#121212",
        display = "standalone",
        startPath = "deliveries",
        iconPath = "icons/icon.png"
)
@Viewport("width=device-width, initial-scale=1, viewport-fit=cover")
@Meta(name = "theme-color", content = "#121212")
@Meta(name = "mobile-web-app-capable", content = "yes")
@Meta(name = "apple-mobile-web-app-capable", content = "yes")
@Meta(name = "apple-mobile-web-app-status-bar-style", content = "black-translucent")
public class OneTimeCourierApplication implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(OneTimeCourierApplication.class, args);
    }
}