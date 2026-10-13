package ru.ten.parser.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.ten.parser.config.SearchProperties;
import ru.ten.parser.model.SearchRequest;
import ru.ten.parser.model.SearchResponse;
import ru.ten.parser.util.FilterBodyBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class SberbankAstClient {

    private static final Logger log = LoggerFactory.getLogger(SberbankAstClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SearchProperties props;

    public SberbankAstClient(HttpClient httpClient,
                             ObjectMapper objectMapper,
                             SearchProperties props) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /**
     * Поиск по одному ключевому слову.
     */
    public SearchResponse search(String keyword,
                                 String status,
                                 int pageNum) throws Exception {
        String filterBody = FilterBodyBuilder.buildOneWord(
                keyword, status, pageNum, props.pageSize(),
                props.startPublicDate(), props.endPublicDate()
        );
        return doSearch(filterBody);
    }

    /**
     * Поиск по нескольким ключевым словам (anyWord).
     */
    public SearchResponse searchMulti(List<String> keywords,
                                      String status,
                                      int pageNum) throws Exception {
        String filterBody = FilterBodyBuilder.buildAnyWord(
                keywords, status, pageNum, props.pageSize(),
                props.startPublicDate(), props.endPublicDate()
        );
        return doSearch(filterBody);
    }

    private SearchResponse doSearch(String filterBody) throws Exception {
        SearchRequest request = new SearchRequest(
                props.windowCode(),
                "MONITOR",
                "default",
                "",
                "es",
                filterBody,
                Map.of()
        );

        String jsonBody = objectMapper.writeValueAsString(request);
        log.debug("POST {} body: {}", props.apiPath(), jsonBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(props.baseUrl() + props.apiPath()))
                .timeout(Duration.ofSeconds(props.readTimeoutSeconds()))  // ← таймаут на чтение
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", props.userAgent())
                .header("Origin", props.baseUrl())
                .header("Referer", props.referer())
                .header("x-ast-actioncode", "default")
                .header("x-ast-actiontype", "MONITOR")
                .header("x-ast-pageurl", props.pageUrl())
                .header("x-ast-windowcode", props.windowCode())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        log.debug("Response status: {}, body size: {}",
                response.statusCode(), response.body().length());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Сбербанк-АСТ вернул HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        return objectMapper.readValue(response.body(), SearchResponse.class);
    }
}
