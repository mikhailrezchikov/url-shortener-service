import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final LinkService linkService = new LinkService();

    private static User currentUser;

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("       URL SHORTENER SERVICE");
        System.out.println("==================================");

        while (true) {

            linkService.cleanupExpiredLinks();

            printMenu();

            int choice = readInt();

            switch (choice) {

                case 1 -> createShortLink();

                case 2 -> showMyLinks();

                case 3 -> openShortLink();

                case 4 -> deleteShortLink();

                case 5 -> switchUser();

                case 0 -> {
                    System.out.println("До свидания!");
                    scanner.close();
                    return;
                }

                default -> System.out.println(
                        "Некорректный пункт меню!"
                );
            }
        }
    }

    private static void printMenu() {

        System.out.println("\n========== МЕНЮ ==========");

        if (currentUser == null) {
            System.out.println("Текущий пользователь: не создан");
        } else {
            System.out.println(
                    "Текущий UUID: " + currentUser.getId()
            );
        }

        System.out.println("1. Создать короткую ссылку");
        System.out.println("2. Мои ссылки");
        System.out.println("3. Перейти по короткой ссылке");
        System.out.println("4. Удалить короткую ссылку");
        System.out.println("5. Сменить пользователя");
        System.out.println("0. Выход");

        System.out.print("Выберите действие: ");
    }

    private static void createShortLink() {

        if (currentUser == null) {

            currentUser = new User();

            System.out.println(
                    "Создан новый пользователь!"
            );

            System.out.println(
                    "Ваш UUID: " + currentUser.getId()
            );
        }

        System.out.print("Введите исходный URL: ");

        String url = scanner.nextLine().trim();

        System.out.print("Введите максимальное количество переходов: ");

        int maxClicks = readInt();

        try {

            ShortLink link = linkService.createLink(
                    url,
                    currentUser.getId(),
                    maxClicks
            );

            System.out.println("\nСсылка успешно создана!");

            System.out.println(
                    "Короткая ссылка: "
                            + linkService.getShortUrl(link)
            );

            System.out.println(
                    "Исходная ссылка: " + link.getOriginalUrl()
            );

            System.out.println(
                    "Лимит переходов: " + link.getMaxClicks()
            );

            System.out.println(
                    "Действует до: " + link.getExpiresAt()
            );

        } catch (IllegalArgumentException e) {

            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void showMyLinks() {

        if (currentUser == null) {
            System.out.println("Сначала создайте пользователя!");
            return;
        }

        List<ShortLink> userLinks =
                linkService.getUserLinks(currentUser.getId());

        if (userLinks.isEmpty()) {

            System.out.println("У вас пока нет ссылок.");

            return;
        }

        System.out.println("\n========== МОИ ССЫЛКИ ==========");

        for (ShortLink link : userLinks) {

            System.out.println("-----------------------------");

            System.out.println(
                    "Короткая ссылка: "
                            + linkService.getShortUrl(link)
            );

            System.out.println(
                    "Исходная ссылка: " + link.getOriginalUrl()
            );

            System.out.println(
                    "Переходы: "
                            + link.getClickCount()
                            + " / "
                            + link.getMaxClicks()
            );

            System.out.println(
                    "Статус: " + link.getStatus()
            );

            System.out.println(
                    "Истекает: " + link.getExpiresAt()
            );
        }
    }

    private static void openShortLink() {

        System.out.print(
                "Введите короткий код или короткую ссылку: "
        );

        String input = scanner.nextLine().trim();

        String shortCode = extractShortCode(input);

        if (shortCode == null) {
            System.out.println("Некорректная короткая ссылка!");
            System.out.println(AppConstants.DOMAIN);
            return;
        }

        ShortLink link = linkService.useLink(shortCode);

        if (link == null) {
            return;
        }

        try {

            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(
                    Desktop.Action.BROWSE
            )) {

                System.out.println(
                        "Открытие браузера не поддерживается."
                );

                return;
            }

            Desktop.getDesktop().browse(
                    new URI(link.getOriginalUrl())
            );

            System.out.println("Браузер открыт!");

        } catch (Exception e) {

            System.out.println(
                    "Не удалось открыть ссылку: " + e.getMessage()
            );
        }
    }

    private static String extractShortCode(String input) {
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            return input;
        }

        URI uri = URI.create(input);

        if (AppConstants.DOMAIN.equals(uri.getHost())) {
            return uri.getPath().substring(1);
        }

        return input;
    }

    private static void deleteShortLink() {

        if (currentUser == null) {
            System.out.println("Сначала создайте пользователя!");
            return;
        }

        System.out.print("Введите короткий код ссылки: ");

        String shortCode = scanner.nextLine().trim();

        linkService.deleteLink(
                shortCode,
                currentUser.getId()
        );
    }

    private static void switchUser() {

        currentUser = new User();

        System.out.println("Создан новый пользователь!");

        System.out.println(
                "Ваш UUID: " + currentUser.getId()
        );
    }

    private static int readInt() {

        while (true) {

            String input = scanner.nextLine().trim();

            try {

                return Integer.parseInt(input);

            } catch (NumberFormatException e) {

                System.out.print(
                        "Введите целое число: "
                );
            }
        }
    }
}
