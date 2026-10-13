package ru.ten.parser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ten.parser.client.SberbankAstClient;
import ru.ten.parser.config.SearchProperties;
import ru.ten.parser.model.Purchase;
import ru.ten.parser.model.SearchResponse;
import ru.ten.parser.util.RequestDelay;

import java.util.List;

@Service
public class TenderService {

    private static final Logger log = LoggerFactory.getLogger(TenderService.class);

    private final SberbankAstClient client;
    private final SearchProperties props;
    private final RequestDelay delay;

    public TenderService(SberbankAstClient client,
                         SearchProperties props,
                         RequestDelay delay) {
        this.client = client;
        this.props = props;
        this.delay = delay;
    }

    /**
     * Ищет закупки по каждому статусу и печатает номер + наименование.
     * Между запросами — случайная задержка.
     */
    public void searchAndPrint(List<String> keywords,
                               List<String> statuses,
                               int pages) throws Exception {

        boolean firstRequest = true;

        for (String status : statuses) {
            log.info("########## Статус: {} ##########", status);

            for (int page = 0; page < pages; page++) {
                log.info("=== Статус '{}', страница {} ===", status, page + 1);

                // Задержка перед каждым запросом, кроме первого
                if (!firstRequest) {
                    delay.sleep(props.delayMinMillis(), props.delayMaxMillis());
                }
                firstRequest = false;

                SearchResponse response = (keywords.size() == 1)
                        ? client.search(keywords.get(0), status, page)
                        : client.searchMulti(keywords, status, page);

                int count = response.purchaseList() == null ? 0 : response.purchaseList().size();
                log.info("Total: {}, на странице: {}", response.total(), count);

                if (count == 0) {
                    log.info("Страница {} пуста, завершаем статус '{}'.", page + 1, status);
                    break;
                }

                for (SearchResponse.PurchaseItem item : response.purchaseList()) {
                    Purchase p = item.source();
                    if (p == null) continue;

                    System.out.println("Номер: " + p.purchCode());
                    System.out.println("Наименование: " + p.purchName());
                    System.out.println("---");
                }
            }
        }
    }
}
