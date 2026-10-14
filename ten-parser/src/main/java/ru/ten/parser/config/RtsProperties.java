package ru.ten.parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rts")
public record RtsProperties(
        String baseUrl,
        String searchPath,
        String ajaxPath,        //URL для POST-запроса (HttpClient отправляет POST на /ajaxwithfullmodel)
        String userAgent,
        long delayMinMillis,
        long delayMaxMillis,
        boolean headless
) {
}
