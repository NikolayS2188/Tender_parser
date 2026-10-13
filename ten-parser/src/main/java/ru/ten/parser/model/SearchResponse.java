package ru.ten.parser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Ответ API Сбербанк-АСТ.
 * Нас интересует только массив PurchaseList и поле Total.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponse(
        @JsonProperty("Total") int total,
        @JsonProperty("PurchaseList") List<PurchaseItem> purchaseList
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PurchaseItem(
            @JsonProperty("_source") Purchase source
    ) {
    }
}
