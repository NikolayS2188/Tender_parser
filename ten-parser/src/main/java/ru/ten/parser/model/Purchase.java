package ru.ten.parser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Одна закупка из массива PurchaseList.
 * Нас интересуют только purchCode и purchName, остальное игнорируем.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Purchase(
        @JsonProperty("purchCode") String purchCode,
        @JsonProperty("purchName") String purchName
) {
}
