package ru.ten.parser.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ten.parser.client.RtsTenderClient;
import ru.ten.parser.client.SberbankAstClient;
import ru.ten.parser.model.Purchase;
import ru.ten.parser.model.SearchResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenderServiceTest {

    @Mock
    private SberbankAstClient sberbankClient;

    @Mock
    private RtsTenderClient rtsClient;

    private TenderService service;

    @BeforeEach
    void setUp() {
        service = new TenderService(sberbankClient, rtsClient);
    }

    private SearchResponse responseWith(Purchase... purchases) {
        List<SearchResponse.PurchaseItem> items = List.of(purchases).stream()
                .map(SearchResponse.PurchaseItem::new)
                .toList();
        return new SearchResponse(purchases.length, items);
    }

    @Test
    void shouldUseSberbankClientForSberbankPlatform() throws Exception {
        when(sberbankClient.search(anyString(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint("sberbank",
                List.of("kaspersky"),
                List.of("В процессе проведения"),
                1);

        verify(sberbankClient, times(1)).search("kaspersky", "В процессе проведения", 0);
        verify(rtsClient, never()).search(anyString(), anyString(), anyInt());
    }

    @Test
    void shouldUseRtsClientForRtsPlatform() throws Exception {
        when(rtsClient.search(anyString(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint("rts",
                List.of("kaspersky"),
                List.of("Активные"),  // ← реальный статус RTS
                1);

        verify(rtsClient, times(1)).search("kaspersky", "Активные", 0);
        verify(sberbankClient, never()).search(anyString(), anyString(), anyInt());
    }

    @Test
    void shouldThrowForUnknownPlatform() {
        assertThatThrownBy(() -> service.searchAndPrint(
                "unknown", List.of("kaspersky"), List.of("status"), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Неизвестная платформа");
    }

    @Test
    void shouldStopWhenPageIsEmpty() throws Exception {
        when(sberbankClient.search(anyString(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")))
                .thenReturn(new SearchResponse(1, List.of()));

        service.searchAndPrint("sberbank",
                List.of("kaspersky"),
                List.of("В процессе проведения"),
                2);

        verify(sberbankClient, times(2)).search(anyString(), anyString(), anyInt());
    }

    @Test
    void shouldIterateOverAllStatuses() throws Exception {
        when(sberbankClient.search(anyString(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint("sberbank",
                List.of("kaspersky"),
                List.of("В процессе проведения", "Подача заявок"),
                1);

        verify(sberbankClient, times(1)).search("kaspersky", "В процессе проведения", 0);
        verify(sberbankClient, times(1)).search("kaspersky", "Подача заявок", 0);
    }

    @Test
    void shouldUseMultiSearchForMultipleKeywords() throws Exception {
        when(sberbankClient.searchMulti(anyList(), anyString(), anyInt()))
                .thenReturn(responseWith(new Purchase("CODE-1", "Name 1")));

        service.searchAndPrint("sberbank",
                List.of("kaspersky", "usb"),
                List.of("В процессе проведения"),
                1);

        verify(sberbankClient, times(1)).searchMulti(
                List.of("kaspersky", "usb"), "В процессе проведения", 0);
        verify(sberbankClient, never()).search(anyString(), anyString(), anyInt());
    }
}
