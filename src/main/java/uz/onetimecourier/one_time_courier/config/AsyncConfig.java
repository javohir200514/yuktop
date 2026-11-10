package uz.onetimecourier.one_time_courier.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Telegram bildirishnomalari so'rov oqimidan tashqarida yuboriladi: yuk yaratgan foydalanuvchi
 * Telegram API javobini kutib qolmaydi va Vaadin session qulfi band bo'lmaydi.
 * Ikkita oqim: Telegram chat boshiga limitlar (sekundiga ~30 xabar) oshib ketmasligi uchun.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    public static final String TELEGRAM_EXECUTOR = "telegramExecutor";

    @Bean(name = TELEGRAM_EXECUTOR)
    public Executor telegramExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("telegram-notify-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5000);
        // Navbat to'lsa, so'rov oqimini to'smaymiz: xabarni tashlab, logga yozamiz
        executor.setRejectedExecutionHandler((r, ex) ->
                log.warn("Telegram bildirishnoma navbati to'ldi, xabar tashlab yuborildi"));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}
