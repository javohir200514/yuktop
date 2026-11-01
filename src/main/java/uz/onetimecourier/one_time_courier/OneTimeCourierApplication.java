package uz.onetimecourier.one_time_courier;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Push
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
@EnableScheduling
public class OneTimeCourierApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(OneTimeCourierApplication.class, args);
    }
}