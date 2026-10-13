package ru.ten.parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки поиска, которые используются при сборке filterBody.
 */
@ConfigurationProperties(prefix = "sberbank")
public record SearchProperties(
        String baseUrl,
        String apiPath,
        String referer,
        String pageUrl,
        String windowCode,
        String userAgent,
        int pageSize,
        String startPublicDate,
        String endPublicDate,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        long delayMinMillis,
        long delayMaxMillis
) {
}
