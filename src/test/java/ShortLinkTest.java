import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShortLink - unit tests")
class ShortLinkTest {

    private static final String URL = "https://example.com/page";
    private static final String CODE = "abc1234";
    private static final UUID OWNER = UUID.randomUUID();

    private ShortLink newLink(int maxClicks, int lifetimeHours) {
        return new ShortLink(URL, CODE, OWNER, maxClicks, lifetimeHours);
    }

    @Test
    @DisplayName("новый линк активен и счётчик переходов равен нулю")
    void newLinkIsActiveWithZeroClicks() {
        ShortLink link = newLink(5, 24);

        assertThat(link.getOriginalUrl()).isEqualTo(URL);
        assertThat(link.getShortCode()).isEqualTo(CODE);
        assertThat(link.getOwnerId()).isEqualTo(OWNER);
        assertThat(link.getMaxClicks()).isEqualTo(5);
        assertThat(link.getClickCount()).isZero();
        assertThat(link.getStatus()).isEqualTo(LinkStatus.ACTIVE);
    }

    @Test
    @DisplayName("incrementClicks увеличивает счётчик, но не меняет статус до лимита")
    void incrementClicksBelowLimitKeepsActive() {
        ShortLink link = newLink(3, 24);

        link.incrementClicks();

        assertThat(link.getClickCount()).isEqualTo(1);
        assertThat(link.getStatus()).isEqualTo(LinkStatus.ACTIVE);
    }

    @Test
    @DisplayName("достижение лимита переводит линк в LIMIT_REACHED")
    void reachingLimitSetsLimitReached() {
        ShortLink link = newLink(2, 24);

        link.incrementClicks();
        link.incrementClicks();

        assertThat(link.getClickCount()).isEqualTo(2);
        assertThat(link.getStatus()).isEqualTo(LinkStatus.LIMIT_REACHED);
    }

    @Test
    @DisplayName("isExpired = false пока срок жизни не вышел")
    void notExpiredWithinLifetime() {
        ShortLink link = newLink(5, 24);

        assertThat(link.isExpired()).isFalse();
    }

    @Test
    @DisplayName("isExpired = true при нулевом сроке жизни")
    void expiredWithZeroLifetime() {
        ShortLink link = newLink(5, 0);

        assertThat(link.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isExpired = true при истёкшем сроке жизни")
    void expiredWithNegativeLifetime() {
        ShortLink link = newLink(5, -1);

        assertThat(link.isExpired()).isTrue();
    }
}
