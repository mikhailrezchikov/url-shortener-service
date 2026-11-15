import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LinkService - unit tests")
class LinkServiceTest {

    private static final String URL = "https://example.com/page";

    private LinkService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new LinkService();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("createLink создаёт активную ссылку с корректными данными")
    void createLinkReturnsActiveLink() {
        ShortLink link = service.createLink(URL, userId, 5);

        assertThat(link.getOriginalUrl()).isEqualTo(URL);
        assertThat(link.getOwnerId()).isEqualTo(userId);
        assertThat(link.getMaxClicks()).isEqualTo(5);
        assertThat(link.getStatus()).isEqualTo(LinkStatus.ACTIVE);
        assertThat(link.getShortCode()).hasSize(7);
    }

    @Test
    @DisplayName("createLink генерирует уникальные короткие коды")
    void createLinkGeneratesUniqueCodes() {
        ShortLink first = service.createLink(URL, userId, 5);
        ShortLink second = service.createLink(URL, userId, 5);

        assertThat(first.getShortCode()).isNotEqualTo(second.getShortCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-url", "ftp://example.com", "http://"})
    @DisplayName("createLink отклоняет некорректные URL")
    void createLinkRejectsInvalidUrl(String invalidUrl) {
        assertThatThrownBy(() -> service.createLink(invalidUrl, userId, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("createLink отклоняет неположительный лимит переходов")
    void createLinkRejectsNonPositiveMaxClicks(int maxClicks) {
        assertThatThrownBy(() -> service.createLink(URL, userId, maxClicks))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getShortUrl собирает короткую ссылку из базового URL и кода")
    void getShortUrlBuildsFullUrl() {
        ShortLink link = service.createLink(URL, userId, 5);

        assertThat(service.getShortUrl(link))
                .isEqualTo(AppConstants.BASE_URL + link.getShortCode());
    }

    @Test
    @DisplayName("useLink увеличивает счётчик переходов")
    void useLinkIncrementsClicks() {
        ShortLink link = service.createLink(URL, userId, 3);

        ShortLink result = service.useLink(link.getShortCode());

        assertThat(result).isSameAs(link);
        assertThat(link.getClickCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("useLink возвращает null для неизвестного кода")
    void useLinkUnknownCodeReturnsNull() {
        assertThat(service.useLink("unknown")).isNull();
    }

    @Test
    @DisplayName("useLink отклоняет переходы после исчерпания лимита")
    void useLinkRejectsAfterLimitReached() {
        ShortLink link = service.createLink(URL, userId, 2);

        service.useLink(link.getShortCode());
        service.useLink(link.getShortCode());

        assertThat(service.useLink(link.getShortCode())).isNull();
        assertThat(link.getStatus()).isEqualTo(LinkStatus.LIMIT_REACHED);
        assertThat(link.getClickCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getUserLinks возвращает только ссылки указанного владельца")
    void getUserLinksFiltersByOwner() {
        UUID otherUser = UUID.randomUUID();

        ShortLink mine = service.createLink(URL, userId, 5);
        service.createLink("https://other.com", otherUser, 5);

        List<ShortLink> result = service.getUserLinks(userId);

        assertThat(result).containsExactly(mine);
    }

    @Test
    @DisplayName("getUserLinks возвращает пустой список, если ссылок нет")
    void getUserLinksReturnsEmptyList() {
        assertThat(service.getUserLinks(userId)).isEmpty();
    }

    @Test
    @DisplayName("deleteLink удаляет ссылку владельцем")
    void deleteLinkByOwner() {
        ShortLink link = service.createLink(URL, userId, 5);

        boolean deleted = service.deleteLink(link.getShortCode(), userId);

        assertThat(deleted).isTrue();
        assertThat(service.getUserLinks(userId)).isEmpty();
    }

    @Test
    @DisplayName("deleteLink не удаляет чужую ссылку")
    void deleteLinkByNonOwnerFails() {
        ShortLink link = service.createLink(URL, userId, 5);

        boolean deleted = service.deleteLink(link.getShortCode(), UUID.randomUUID());

        assertThat(deleted).isFalse();
        assertThat(service.getUserLinks(userId)).containsExactly(link);
    }

    @Test
    @DisplayName("deleteLink возвращает false для неизвестного кода")
    void deleteLinkUnknownCodeFails() {
        assertThat(service.deleteLink("unknown", userId)).isFalse();
    }

    @Test
    @DisplayName("владелец может изменить лимит и срок действия ссылки")
    void ownerCanUpdateLinkSettings() {
        ShortLink link = service.createLink(URL, userId, 1);
        var originalExpiresAt = link.getExpiresAt();
        service.useLink(link.getShortCode());

        boolean updated = service.updateLinkSettings(
                link.getShortCode(), userId, 5, 48
        );

        assertThat(updated).isTrue();
        assertThat(link.getMaxClicks()).isEqualTo(5);
        assertThat(link.getExpiresAt()).isAfter(originalExpiresAt);
        assertThat(link.getStatus()).isEqualTo(LinkStatus.ACTIVE);
    }

    @Test
    @DisplayName("чужой пользователь не может изменить параметры ссылки")
    void nonOwnerCannotUpdateLinkSettings() {
        ShortLink link = service.createLink(URL, userId, 5);

        boolean updated = service.updateLinkSettings(
                link.getShortCode(), UUID.randomUUID(), 10, 24
        );

        assertThat(updated).isFalse();
        assertThat(link.getMaxClicks()).isEqualTo(5);
    }

    @Test
    @DisplayName("новый лимит не может быть меньше уже сделанных переходов")
    void updateLinkSettingsRejectsLimitBelowClickCount() {
        ShortLink link = service.createLink(URL, userId, 5);
        service.useLink(link.getShortCode());
        service.useLink(link.getShortCode());

        assertThatThrownBy(() -> service.updateLinkSettings(
                link.getShortCode(), userId, 1, 24
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
