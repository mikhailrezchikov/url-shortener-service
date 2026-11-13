import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LinkService - integration tests")
class LinkServiceIntegrationTest {

    private LinkService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new LinkService();
        user = new User();
    }

    @Test
    @DisplayName("полный жизненный цикл: создание -> переходы -> лимит -> удаление")
    void fullLinkLifecycle() {
        ShortLink link = service.createLink("https://example.com", user.getId(), 3);

        String shortUrl = service.getShortUrl(link);
        assertThat(shortUrl).startsWith(AppConstants.BASE_URL);

        assertThat(service.useLink(link.getShortCode())).isNotNull();
        assertThat(service.useLink(link.getShortCode())).isNotNull();
        assertThat(service.useLink(link.getShortCode())).isNotNull();
        assertThat(link.getStatus()).isEqualTo(LinkStatus.LIMIT_REACHED);

        assertThat(service.useLink(link.getShortCode())).isNull();

        assertThat(service.deleteLink(link.getShortCode(), user.getId())).isTrue();
        assertThat(service.useLink(link.getShortCode())).isNull();
    }

    @Test
    @DisplayName("пользователи изолированы: не видят и не удаляют чужие ссылки")
    void usersAreIsolated() {
        User other = new User();

        ShortLink mine = service.createLink("https://mine.com", user.getId(), 5);
        ShortLink theirs = service.createLink("https://theirs.com", other.getId(), 5);

        assertThat(service.getUserLinks(user.getId())).containsExactly(mine);
        assertThat(service.getUserLinks(other.getId())).containsExactly(theirs);

        assertThat(service.deleteLink(theirs.getShortCode(), user.getId())).isFalse();
        assertThat(service.getUserLinks(other.getId())).containsExactly(theirs);
    }

    @Test
    @DisplayName("useLink помечает истёкшую ссылку как EXPIRED и удаляет её")
    void useLinkExpiresOutdatedLink() throws Exception {
        ShortLink link = service.createLink("https://example.com", user.getId(), 5);
        forceExpire(link);

        assertThat(service.useLink(link.getShortCode())).isNull();
        assertThat(link.getStatus()).isEqualTo(LinkStatus.EXPIRED);

        assertThat(service.useLink(link.getShortCode())).isNull();
    }

    @Test
    @DisplayName("cleanupExpiredLinks удаляет только истёкшие ссылки")
    void cleanupRemovesOnlyExpired() throws Exception {
        ShortLink active = service.createLink("https://active.com", user.getId(), 5);
        ShortLink expired = service.createLink("https://expired.com", user.getId(), 5);
        forceExpire(expired);

        service.cleanupExpiredLinks();

        List<ShortLink> remaining = service.getUserLinks(user.getId());
        assertThat(remaining).containsExactly(active);
        assertThat(remaining).doesNotContain(expired);
    }

    private static void forceExpire(ShortLink link) throws Exception {
        Field expiresAt = ShortLink.class.getDeclaredField("expiresAt");
        expiresAt.setAccessible(true);
        expiresAt.set(link, LocalDateTime.now().minusHours(1));
    }
}
