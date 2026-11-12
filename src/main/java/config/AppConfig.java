package config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

@Sources("classpath:application.properties")
public interface AppConfig extends Config {

    @Key("link.ttl.hours")
    int linkTtlHours();

    @Key("link.base.url")
    String baseUrl();

    @Key("link.code.length")
    int codeLength();
}