import config.ConfigProvider;

import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LinkService {

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = ConfigProvider.CONFIG.codeLength();
    private static final int LIFETIME_HOURS = ConfigProvider.CONFIG.linkTtlHours();
    private final Map<String, ShortLink> links = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public ShortLink createLink(String originalUrl,
                                UUID userId,
                                int maxClicks) {

        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Некорректный URL!");
        }

        if (maxClicks <= 0) {
            throw new IllegalArgumentException(
                    "Лимит переходов должен быть больше нуля!"
            );
        }

        String shortCode;

        do {
            shortCode = generateShortCode();
        } while (links.containsKey(shortCode));

        ShortLink link = new ShortLink(
                originalUrl,
                shortCode,
                userId,
                maxClicks,
                LIFETIME_HOURS
        );

        links.put(shortCode, link);

        return link;
    }

    private String generateShortCode() {

        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {

            int index = random.nextInt(CHARACTERS.length());

            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }

    private boolean isValidUrl(String url) {

        try {
            URI uri = URI.create(url);

            return ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getShortUrl(ShortLink link) {
        return AppConstants.BASE_URL + link.getShortCode();
    }

    public ShortLink useLink(String shortCode) {

        ShortLink link = links.get(shortCode);

        if (link == null) {
            System.out.println("Ссылка не найдена!");
            return null;
        }

        if (link.isExpired()) {

            link.setStatus(LinkStatus.EXPIRED);

            System.out.println("Срок жизни ссылки истек!");

            links.remove(shortCode);

            return null;
        }

        if (link.getStatus() == LinkStatus.LIMIT_REACHED
                || link.getClickCount() >= link.getMaxClicks()) {

            link.setStatus(LinkStatus.LIMIT_REACHED);

            System.out.println("Лимит переходов исчерпан!");

            return null;
        }

        link.incrementClicks();

        System.out.println("Переход разрешен!");
        System.out.println("Осталось переходов: "
                + (link.getMaxClicks() - link.getClickCount()));

        if (link.getStatus() == LinkStatus.LIMIT_REACHED) {
            System.out.println("Это был последний разрешенный переход.");
        }

        return link;
    }

    public List<ShortLink> getUserLinks(UUID userId) {

        List<ShortLink> userLinks = new ArrayList<>();

        for (ShortLink link : links.values()) {

            if (link.getOwnerId().equals(userId)) {
                userLinks.add(link);
            }
        }

        return userLinks;
    }

    public boolean deleteLink(String shortCode, UUID userId) {

        ShortLink link = links.get(shortCode);

        if (link == null) {
            System.out.println("Ссылка не найдена!");
            return false;
        }

        if (!link.getOwnerId().equals(userId)) {

            System.out.println(
                    "Ошибка! Вы не являетесь владельцем этой ссылки."
            );

            return false;
        }

        links.remove(shortCode);

        System.out.println("Ссылка успешно удалена!");

        return true;
    }

    public boolean updateLinkSettings(String shortCode,
                                      UUID userId,
                                      int maxClicks,
                                      int lifetimeHours) {

        ShortLink link = links.get(shortCode);

        if (link == null) {
            System.out.println("Ссылка не найдена!");
            return false;
        }

        if (!link.getOwnerId().equals(userId)) {
            System.out.println(
                    "Ошибка! Вы не являетесь владельцем этой ссылки."
            );
            return false;
        }

        if (maxClicks <= 0) {
            throw new IllegalArgumentException(
                    "Лимит переходов должен быть больше нуля!"
            );
        }

        if (maxClicks < link.getClickCount()) {
            throw new IllegalArgumentException(
                    "Новый лимит не может быть меньше числа переходов!"
            );
        }

        if (lifetimeHours <= 0) {
            throw new IllegalArgumentException(
                    "Срок действия должен быть больше нуля!"
            );
        }

        link.updateSettings(maxClicks, lifetimeHours);
        System.out.println("Параметры ссылки успешно обновлены!");
        return true;
    }

    public void cleanupExpiredLinks() {

        Iterator<Map.Entry<String, ShortLink>> iterator =
                links.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, ShortLink> entry = iterator.next();

            ShortLink link = entry.getValue();

            if (link.isExpired()) {

                link.setStatus(LinkStatus.EXPIRED);

                System.out.println(
                        "\nУведомление владельцу " + link.getOwnerId()
                                + ": срок действия ссылки "
                                + getShortUrl(link) + " истек."
                );

                iterator.remove();
            }
        }
    }
}
