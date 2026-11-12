import config.ConfigProvider;

import java.net.URI;

public final class AppConstants {

    public static final String BASE_URL = ConfigProvider.CONFIG.baseUrl();
    public static final String DOMAIN = URI.create(BASE_URL).getHost();
}
