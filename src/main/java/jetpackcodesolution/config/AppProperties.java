package jetpackcodesolution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security) {

    public record Security(String jwtSecret, Duration tokenTtl, String adminUser, String adminPassword) {
    }
}
