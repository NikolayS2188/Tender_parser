package ru.ten.parser.client;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.ten.parser.config.RtsProperties;

import java.net.HttpCookie;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис, который через Selenium проходит Anti-DDoS и извлекает cookies + fotoken
 */
@Component
public class RtsSessionProvider {

    private static final Logger log = LoggerFactory.getLogger(RtsSessionProvider.class);

    private final WebDriver driver;
    private final RtsProperties props;

    public RtsSessionProvider(WebDriver driver, RtsProperties props) {
        this.driver = driver;
        this.props = props;
    }

    /**
     * Открывает страницу поиска, проходит Anti-DDoS и возвращает сессию.
     */
    public RtsSession obtainSession() {
        log.info("РТС-Тендер: открываем страницу поиска для получения сессии...");

        String url = props.baseUrl() + props.searchPath();
        driver.get(url);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        // 1. Ждём прохождения Anti-DDoS — появления поля поиска
        wait.until(ExpectedConditions.presenceOfElementLocated(
                org.openqa.selenium.By.cssSelector("input[type='text']")
        ));
        log.info("Anti-DDoS пройден, поле поиска найдено");

        // 2. Ждём появления куки XSRF-TOKEN (признак, что fotoken получен)
        wait.until(d -> {
            Set<Cookie> cookies = d.manage().getCookies();
            return cookies.stream().anyMatch(c -> c.getName().equals("XSRF-TOKEN"));
        });
        log.info("XSRF-TOKEN получен");

        // 3. Забираем все куки
        Set<Cookie> seleniumCookies = driver.manage().getCookies();
        List<HttpCookie> httpCookies = seleniumCookies.stream()
                .map(c -> {
                    HttpCookie httpCookie = new HttpCookie(c.getName(), c.getValue());
                    httpCookie.setDomain(c.getDomain());
                    httpCookie.setPath(c.getPath());
                    return httpCookie;
                })
                .collect(Collectors.toList());

        // 4. Извлекаем XSRF-TOKEN и fotoken
        String xsrfToken = seleniumCookies.stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN"))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("XSRF-TOKEN не найден"));

        // После появления XSRF-TOKEN — пауза для получения fotoken
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String fotoken = seleniumCookies.stream()
                .filter(c -> c.getName().equals("fotoken"))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null); // fotoken может отсутствовать — попробуем извлечь из localStorage

        if (fotoken == null) {
            // Пробуем извлечь из localStorage через JavaScript
            try {
                Object token = ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("return localStorage.getItem('fotoken');");
                if (token != null) {
                    fotoken = token.toString();
                }
            } catch (Exception e) {
                log.warn("Не удалось извлечь fotoken из localStorage", e);
            }
        }

        log.info("Сессия получена: cookies={}, xsrfToken={}, fotoken={}",
                httpCookies.size(),
                xsrfToken != null ? "present" : "null",
                fotoken != null ? "present" : "null");

        return new RtsSession(httpCookies, xsrfToken, fotoken);
    }
}
