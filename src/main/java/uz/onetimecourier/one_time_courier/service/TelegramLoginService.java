package uz.onetimecourier.one_time_courier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uz.onetimecourier.one_time_courier.config.TelegramLoginSessionStore;
import uz.onetimecourier.one_time_courier.entity.UserEntity;


@Service
@RequiredArgsConstructor
public class TelegramLoginService {

    public record Started(String token, String link) {}

    private final TelegramLoginSessionStore store;
    private final OAuthUserService oauthUserService;

    @Value("${telegram.bot.username:}")
    private String botUsername;

    @Value("${telegram.bot.token:}")
    private String botToken;

    public boolean isEnabled() {
        return !botUsername.isBlank() && !botToken.isBlank();
    }

    public Started start() {
        String token = store.create();
        return new Started(token, "https://t.me/" + botUsername + "?start=" + token);
    }

    public TelegramLoginSessionStore.PollResult poll(String token) {
        return store.poll(token);
    }

    public UserEntity resolveUser(TelegramLoginSessionStore.TelegramProfile p) {
        return oauthUserService.findOrCreateTelegramUser(p.id(), p.firstName(), p.lastName(), p.username());
    }
    public TelegramLoginSessionStore.PollResult verify(String token, String code) {
        return store.verify(token, code);
    }
}