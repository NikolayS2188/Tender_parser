package ru.ten.parser.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ten.parser.client.SberbankAstClient;
import ru.ten.parser.config.SearchProperties;
import ru.ten.parser.model.Purchase;
import ru.ten.parser.model.SearchResponse;
import ru.ten.parser.util.RequestDelay;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenderServiceTest {

    @Mock
    private SberbankAstClient client;

    @Mock
    private RequestDelay delay;

    private SearchProperties props;

    private TenderService service;

    @BeforeEach
    void setUp() {
        // SearchProperties — record, можно создать вручную
        props = new SearchProperties(
                "https://www.sberbank-ast.ru",
                "/api/Processing/main",
                "https://www.sberbank-ast.ru/UnitedPurchaseList.html",
                "https://www.sberbank-ast.ru/UnitedPurchaseList.html",
                "/EsOpenUnitedPurchaseList",
                "Mozilla/5.0 ...",
                20,
                "01.01.2024",
                "09.10.2026 23:59:59",
                30,
                60,
                1500,
                3000
        );

        service = new TenderService(client, props, delay);
    }

    private SearchResponse responseWith(Purchase... purchases) {
        List<SearchResponse.PurchaseItem> items = List.of(purchases).stream()
                .map(SearchResponse.PurchaseItem::new)
                .toList();
        return new SearchResponse(purchases.length, items);
    }

    @Test
    void shouldStopWhenPageIsEmpty() throws Exception {
        when(client.search(eq("kaspersky"), eq("В процессе проведения"), eq(0)))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));
        when(client.search(eq("kaspersky"), eq("В процессе проведения"), eq(1)))
                .thenReturn(new SearchResponse(1, List.of()));

        service.searchAndPrint(List.of("kaspersky"), List.of("В процессе проведения"), 2);

        verify(client, times(1)).search("kaspersky", "В процессе проведения", 0);
        verify(client, times(1)).search("kaspersky", "В процессе проведения", 1);
    }

    @Test
    void shouldIterateOverAllStatuses() throws Exception {
        when(client.search(anyString(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint(
                List.of("kaspersky"),
                List.of("В процессе проведения", "Подача заявок"),
                1
        );

        verify(client, times(1)).search("kaspersky", "В процессе проведения", 0);
        verify(client, times(1)).search("kaspersky", "Подача заявок", 0);
    }

    @Test
    void shouldUseMultiSearchForMultipleKeywords() throws Exception {
        when(client.searchMulti(anyList(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint(
                List.of("kaspersky", "usb"),
                List.of("В процессе проведения"),
                1
        );

        verify(client, times(1)).searchMulti(
                List.of("kaspersky", "usb"), "В процессе проведения", 0);
        verify(client, never()).search(anyString(), anyString(), anyInt());
    }

    @Test
    void shouldNotSleepBeforeFirstRequest() throws Exception {
        when(client.search(anyString(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint(
                List.of("kaspersky"),
                List.of("В процессе проведения"),
                1
        );

        // Один запрос — задержки быть не должно
        verify(delay, never()).sleep(anyLong(), anyLong());
    }

    @Test
    void shouldSleepBetweenRequests() throws Exception {
        when(client.search(anyString(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint(
                List.of("kaspersky"),
                List.of("В процессе проведения"),
                2
        );

        // Два запроса — ровно одна задержка (перед вторым)
        verify(delay, times(1)).sleep(props.delayMinMillis(), props.delayMaxMillis());
    }
}
