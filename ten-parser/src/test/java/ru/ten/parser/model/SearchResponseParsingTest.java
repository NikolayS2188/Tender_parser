package ru.ten.parser.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchResponseParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseSearchResponse() throws Exception {
        String json = """
                {
                  "Total": 6,
                  "PurchaseList": [
                    {
                      "_source": {
                        "purchCode": "SBR003-260105603700089.1",
                        "purchName": "Оказание услуг по продлению лицензии..."
                      }
                    },
                    {
                      "_source": {
                        "purchCode": "SBR063-260559703100039.1",
                        "purchName": "Поставка сертифицированного ПО..."
                      }
                    }
                  ]
                }
                """;

        SearchResponse response = objectMapper.readValue(json, SearchResponse.class);

        assertThat(response.total()).isEqualTo(6);
        assertThat(response.purchaseList()).hasSize(2);
        assertThat(response.purchaseList().get(0).source().purchCode())
                .isEqualTo("SBR003-260105603700089.1");
        assertThat(response.purchaseList().get(0).source().purchName())
                .isEqualTo("Оказание услуг по продлению лицензии...");
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        String json = """
                {
                  "Total": 1,
                  "UnknownField": "value",
                  "PurchaseList": [
                    {
                      "_source": {
                        "purchCode": "CODE-1",
                        "purchName": "Name",
                        "unknown": "ignored"
                      }
                    }
                  ]
                }
                """;

        SearchResponse response = objectMapper.readValue(json, SearchResponse.class);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.purchaseList()).hasSize(1);
    }

    @Test
    void shouldHandleEmptyPurchaseList() throws Exception {
        String json = """
                {
                  "Total": 0,
                  "PurchaseList": []
                }
                """;

        SearchResponse response = objectMapper.readValue(json, SearchResponse.class);

        assertThat(response.total()).isZero();
        assertThat(response.purchaseList()).isEmpty();
    }
}
