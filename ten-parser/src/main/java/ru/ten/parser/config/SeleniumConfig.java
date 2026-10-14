package ru.ten.parser.config;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
public class SeleniumConfig {

    private static final Logger log = LoggerFactory.getLogger(SeleniumConfig.class);

    private final RtsProperties props;

    public SeleniumConfig(RtsProperties props) {
        this.props = props;
    }

    @Bean(destroyMethod = "quit")
    @Lazy
    public WebDriver rtsWebDriver() {
        log.info("Инициализация Chrome для РТС-Тендер. Headless: {}", props.headless());

        ChromeOptions options = new ChromeOptions();

        // Маскировка автоматизации
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // User-Agent
        options.addArguments("--user-agent=" + props.userAgent());

        // Headless
        if (props.headless()) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }

        // Дополнительные аргументы для стабильности в Docker
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);

        // Убираем navigator.webdriver
        ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        return driver;
    }
}
