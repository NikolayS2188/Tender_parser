package ru.ten.parser.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.ten.parser.config.RtsProperties;
import ru.ten.parser.model.Purchase;
import ru.ten.parser.model.SearchResponse;
import ru.ten.parser.util.RequestDelay;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class RtsTenderClient implements TenderClient {

    private static final Logger log = LoggerFactory.getLogger(RtsTenderClient.class);

    private final WebDriver driver;
    private final RtsProperties props;
    private final RequestDelay delay;

    public RtsTenderClient(WebDriver driver, RtsProperties props, RequestDelay delay) {
        this.driver = driver;
        this.props = props;
        this.delay = delay;
    }

    @Override
    public SearchResponse search(String keyword, String status, int pageNum) throws Exception {
        return doSearch(List.of(keyword), status, pageNum);
    }

    @Override
    public SearchResponse searchMulti(List<String> keywords, String status, int pageNum) throws Exception {
        return doSearch(keywords, status, pageNum);
    }

    @Override
    public String getPlatformName() {
        return "RTS-Tender";
    }

    private SearchResponse doSearch(List<String> keywords, String status, int pageNum) throws Exception {
        String query = String.join(" ", keywords);
        log.info("РТС-Тендер поиск: query='{}', status='{}', pageNum={}", query, status, pageNum);

        String url = props.baseUrl() + props.searchPath();
        driver.get(url);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        // 1. Ждём поле поиска
        WebElement searchBox = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.search__text"))
        );

        // 2. Вводим запрос
        searchBox.clear();
        searchBox.sendKeys(query);

        // 3. Применяем фильтр статуса (если нужен) и делаем первый поиск
        boolean needStatusFilter = status != null
                && !status.isBlank()
                && !status.equalsIgnoreCase("Активные")
                && !status.equalsIgnoreCase("В процессе проведения");

        if (needStatusFilter) {
            applyStatusFilterAndSearch(status, wait);
        } else {
            clickMainSearchButton(wait);
        }

        // 4. Ждём карточки
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".card-item")));

        // 5. Пауза после загрузки
        delay.sleep(props.delayMinMillis(), props.delayMaxMillis());

        // 6. Если нужна страница > 0 — переходим вперёд через клик по ">"
        for (int i = 0; i < pageNum; i++) {
            delay.sleep(props.delayMinMillis(), props.delayMaxMillis());
            goToNextPage(wait);
        }

        // 7. HTML
        String html = driver.getPageSource();

        // 8. Парсим
        return parseHtmlResponse(html, query);
    }

    /**
     * Клик по основной кнопке "Найти" (mainButtonSearch).
     * Используется, когда фильтр статуса не нужен — работает "Активные" по умолчанию.
     */
    private void clickMainSearchButton(WebDriverWait wait) {
        try {
            // Дожидаемся исчезновения лоадера
            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector("div.loader")
                ));
            } catch (Exception e) {
                log.debug("Лоадер не найден или не исчез");
            }

            WebElement searchButton = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("button.search__btn.mainButtonSearch")
                    )
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", searchButton);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchButton);
            log.info("Основной поиск запущен (статус по умолчанию — Активные)");

        } catch (Exception e) {
            log.error("Не удалось кликнуть по основной кнопке 'Найти'", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Открывает модалку фильтров, снимает "Активные", отмечает нужный статус
     * и кликает "Найти" внутри модалки (bottomFilterSearch) — это применяет
     * фильтры и сразу делает поиск.
     */
    private void applyStatusFilterAndSearch(String status, WebDriverWait wait) {
        try {
            log.info("Применяем фильтр статуса '{}' через модалку", status);

            // 1. Открываем модалку — клик по "Настройка поиска" (кнопка "7")
            WebElement filterButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("div.main-search__settings")
                    )
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filterButton);

            // 2. Ждём появления модалки
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("#ftfbn-portal.modal-settings-active")
            ));
            log.info("Модалка фильтров открыта");

            // 3. Снимаем "Активные", если отмечен (по умолчанию он активен)
            try {
                WebElement activeCheckbox = driver.findElement(
                        By.xpath("//div[@id='ftfbn-portal']//label[normalize-space(text())='Активные']/preceding-sibling::input[@type='checkbox']")
                );
                if (activeCheckbox.isSelected()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", activeCheckbox);
                    log.info("Чекбокс 'Активные' снят");
                }
            } catch (Exception e) {
                log.debug("Чекбокс 'Активные' не найден — возможно, уже снят");
            }

            // 4. Отмечаем нужный статус (не "Определение победителя" — его не трогаем)
            WebElement checkbox = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[@id='ftfbn-portal']//label[normalize-space(text())='" + status + "']/preceding-sibling::input[@type='checkbox']")
                    )
            );
            if (!checkbox.isSelected()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);
                log.info("Чекбокс '{}' отмечен", status);
            } else {
                log.info("Чекбокс '{}' уже отмечен", status);
            }

            // 5. Кликаем "Найти" внутри модалки — применяет фильтры и делает поиск
            WebElement applyButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("button.search__btn.bottomFilterSearch")
                    )
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", applyButton);
            log.info("Фильтр статуса '{}' применён, поиск запущен через модалку", status);

            // 6. Ждём закрытия модалки (она закрывается автоматически после "Найти")
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("#ftfbn-portal.modal-settings-active")
            ));

        } catch (Exception e) {
            log.warn("Не удалось применить фильтр статуса '{}'", status, e);
            // Пытаемся закрыть модалку, если она осталась открытой
            try {
                WebElement closeBtn = driver.findElement(By.cssSelector(".modal-close"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeBtn);
            } catch (Exception ignored) {
            }
        }
    }

    private SearchResponse parseHtmlResponse(String html, String query) {
        Document doc = Jsoup.parse(html);
        Elements cards = doc.select(".card-item");

        List<Purchase> purchases = new ArrayList<>();

        for (Element card : cards) {
            try {
                Element titleEl = card.selectFirst(".card-item__title");
                if (titleEl == null) continue;

                String name = titleEl.text().trim();

                if (!name.toLowerCase().contains(query.toLowerCase())) {
                    continue;
                }

                // Номер закупки — ищем через поток
                String number = "—";
                for (Element a : card.select("a")) {
                    String text = a.text();
                    if (text.contains("Закупка №")) {
                        number = text.replace("Закупка №", "").replace(" в ЕИС", "").trim();
                        break;
                    }
                }

                // Fallback — из data-purchase-info
                if (number.equals("—")) {
                    Element detailsBtn = card.selectFirst("a.button-red");
                    if (detailsBtn != null) {
                        String info = detailsBtn.attr("data-purchase-info");
                        if (info != null && info.contains("RegistrationNumber")) {
                            int start = info.indexOf("RegistrationNumber") + 21;
                            int end = info.indexOf("\"", start);
                            number = info.substring(start, end);
                        }
                    }
                }

                if (!name.isEmpty() && !number.equals("—")) {
                    purchases.add(new Purchase(number, name));
                }
            } catch (Exception e) {
                log.warn("Не удалось извлечь данные из карточки", e);
            }
        }

        log.info("РТС-Тендер: найдено {} записей", purchases.size());

        return new SearchResponse(purchases.size(),
                purchases.stream().map(SearchResponse.PurchaseItem::new).toList());
    }

    private void goToNextPage(WebDriverWait wait) {
        log.info("Переходим на следующую страницу...");

        // Запоминаем первый номер закупки до клика
        String beforeFirstNumber = getFirstPurchaseNumber();
        log.debug("Первый номер до клика: {}", beforeFirstNumber);

        // Кликаем ">"
        WebElement nextButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("a.page-link.next"))
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);

        // Ждём, пока первый номер изменится
        wait.until(d -> {
            String afterFirstNumber = getFirstPurchaseNumber();
            return afterFirstNumber != null && !afterFirstNumber.equals(beforeFirstNumber);
        });
        log.info("Страница обновлена");
    }

    private String getFirstPurchaseNumber() {
        try {
            WebElement firstCard = driver.findElement(By.cssSelector(".card-item"));
            for (WebElement a : firstCard.findElements(By.tagName("a"))) {
                String text = a.getText();
                if (text.contains("Закупка №")) {
                    return text.replace("Закупка №", "").replace(" в ЕИС", "").trim();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
