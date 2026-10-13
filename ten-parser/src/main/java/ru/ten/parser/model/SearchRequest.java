package ru.ten.parser.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Тело POST-запроса к /api/Processing/main.
 */
public record SearchRequest(
        @JsonProperty("windowCode") String windowCode,
        @JsonProperty("actionType") String actionType,
        @JsonProperty("actionCode") String actionCode,
        @JsonProperty("documentBody") String documentBody,
        @JsonProperty("parm") String parm,
        @JsonProperty("filterBody") String filterBody,
        @JsonProperty("options") Object options
) {
}
