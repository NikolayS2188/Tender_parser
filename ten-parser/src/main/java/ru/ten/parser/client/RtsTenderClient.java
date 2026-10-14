package ru.ten.parser.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.ten.parser.config.RtsProperties;
import ru.ten.parser.model.Purchase;
import ru.ten.parser.model.SearchResponse;
import ru.ten.parser.util.RequestDelay;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Использует RtsSessionProvider для получения сессии, HttpClient для POST и Jsoup для парсинга HTML
 */
@Component
public class RtsTenderClient implements TenderClient {

    private static final Logger log = LoggerFactory.getLogger(RtsTenderClient.class);

    private final RtsSessionProvider sessionProvider;
    private final RtsProperties props;
    private final RequestDelay delay;
    private final ObjectMapper objectMapper;

    private RtsSession session;
    private HttpClient httpClient;

    public RtsTenderClient(RtsSessionProvider sessionProvider,
                           RtsProperties props,
                           RequestDelay delay,
                           ObjectMapper objectMapper) {
        this.sessionProvider = sessionProvider;
        this.props = props;
        this.delay = delay;
        this.objectMapper = objectMapper;
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
        ensureSession();

        // Задержка между POST-запросами (кроме первого)
        if (pageNum > 0) {
            delay.sleep(props.delayMinMillis(), props.delayMaxMillis());
        }

        String query = String.join(" ", keywords);
        log.info("РТС-Тендер поиск: query='{}', pageNum={}", query, pageNum);

        // 1. Формируем JSON-тело запроса (как в HAR)
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("contractStatuses", List.of());
        filter.put("notificationStatuses", mapStatusToNotificationStatuses(status));
        filter.put("isSearch223", true);
        filter.put("isSearch44", true);
        filter.put("isSearch615", true);
        filter.put("isSearchCOM", true);
        filter.put("isSearchZMO", true);
        filter.put("isSearchSP", false);
        filter.put("isSearchRFP", true);
        filter.put("customerIds", List.of());
        filter.put("deliveryRegionIds", List.of());
        filter.put("etpCodes", List.of());
        filter.put("pwsIds", List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25));
        filter.put("regionIds", List.of());
        filter.put("isSmp", null);
        filter.put("nmcFrom", 0);
        filter.put("nmcTo", null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("filter", filter);
        body.put("isAscendingSorting", false);
        body.put("searchQuery", query);
        body.put("skip", pageNum * 10);
        body.put("sort", "PublishDate");
        body.put("top", 10);
        body.put("type", 1);
        body.put("RegionAlias", "");
        body.put("CityName", "");

        String jsonBody = objectMapper.writeValueAsString(body);
        log.debug("POST {} body: {}", props.ajaxPath(), jsonBody);

        // 2. Отправляем POST-запрос
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(props.baseUrl() + props.ajaxPath()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", props.userAgent())
                .header("Origin", props.baseUrl())
                .header("Referer", props.baseUrl() + props.searchPath())
                .header("X-Requested-With", "XMLHttpRequest")
                .header("X-XSRF-TOKEN", session.xsrfToken());

        if (session.fotoken() != null) {
            builder.header("fotoken", session.fotoken());
        }

        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("Response status: {}, body size: {}", response.statusCode(), response.body().length());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "РТС-Тендер вернул HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        // 3. Парсим HTML-ответ через Jsoup
        return parseHtmlResponse(response.body(), query);
    }

    /**
     * Маппинг текстового статуса на числовой код РТС-Тендер.
     * Коды:
     * 1 — Активные
     * 2 — Определение победителя
     * 3 — Идут торги
     * 4 — Подписание контракта
     * 5 — Архив
     */
    private List<Integer> mapStatusToNotificationStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of(1); // по умолчанию — «Активные»
        }

        return switch (status.toLowerCase().trim()) {
            case "в процессе проведения", "активные" -> List.of(1);
            case "определение победителя" -> List.of(2);
            case "идут торги" -> List.of(3);
            case "подписание контракта" -> List.of(4);
            case "архив" -> List.of(5);
            default -> {
                log.warn("Неизвестный статус '{}', используем «Активные»", status);
                yield List.of(1);
            }
        };
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

                // Фильтр: только те, где есть ключевое слово
                if (!name.toLowerCase().contains(query.toLowerCase())) {
                    continue;
                }

                // Номер закупки — из ссылки "Закупка №..."
                String number = "—";
                Element numberEl = card.selectFirst("a:contains(Закупка №)");
                if (numberEl != null) {
                    number = numberEl.text()
                            .replace("Закупка №", "")
                            .replace(" в ЕИС", "")
                            .trim();
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

    private void ensureSession() {
        if (session == null) {
            session = sessionProvider.obtainSession();

            // Создаём HttpClient с cookies
            CookieManager cookieManager = new CookieManager();
            for (var cookie : session.cookies()) {
                cookieManager.getCookieStore().add(null, cookie);
            }

            this.httpClient = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
        }
    }
}
