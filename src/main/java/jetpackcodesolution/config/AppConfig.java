package jetpackcodesolution.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId bookingZone(AppProperties properties) {
        return ZoneId.of(properties.booking().zone());
    }
}
