package config;

import org.aeonbits.owner.ConfigFactory;

public final class ConfigProvider {
    public static final AppConfig CONFIG = ConfigFactory.create(AppConfig.class);
}