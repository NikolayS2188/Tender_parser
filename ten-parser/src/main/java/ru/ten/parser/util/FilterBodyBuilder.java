package ru.ten.parser.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Собирает XML-строку filterBody для Сбербанк-АСТ.
 */
public final class FilterBodyBuilder {

    private FilterBodyBuilder() {
    }

    /**
     * Одно ключевое слово.
     */
    public static String buildOneWord(String keyword,
                                      String status,
                                      int pageNum,
                                      int pageSize,
                                      String startPublicDate,
                                      String endPublicDate) {
        return buildInternal(
                escapeXml(keyword),
                "oneWord",
                status,
                pageNum,
                pageSize,
                startPublicDate,
                endPublicDate
        );
    }

    /**
     * Несколько ключевых слов (режим anyWord — любое из слов).
     */
    public static String buildAnyWord(List<String> keywords,
                                      String status,
                                      int pageNum,
                                      int pageSize,
                                      String startPublicDate,
                                      String endPublicDate) {
        String joined = keywords.stream()
                .map(FilterBodyBuilder::escapeXml)
                .collect(Collectors.joining(" "));

        return buildInternal(
                joined,
                "anyWord",
                status,
                pageNum,
                pageSize,
                startPublicDate,
                endPublicDate
        );
    }

    private static String buildInternal(String mainInput,
                                        String searchBarType,
                                        String status,
                                        int pageNum,
                                        int pageSize,
                                        String startPublicDate,
                                        String endPublicDate) {
        String statusTag = (status == null || status.isBlank())
                ? ""
                : "<purchStateNameTerm>" + escapeXml(status) + "</purchStateNameTerm>";

        return "<query>"
                + "<targetPageCode>OTUnitedPurchaseList</targetPageCode>"
                + "<searchbyorg>1</searchbyorg>"
                + "<pagesize>" + pageSize + "</pagesize>"
                + "<pagenum>" + pageNum + "</pagenum>"
                + "<sortOrder>desc</sortOrder>"
                + "<searchBarType>" + searchBarType + "</searchBarType>"
                + "<mainInput>" + mainInput + "</mainInput>"
                + "<publicDate>"
                + "<startPublicDate>" + escapeXml(startPublicDate) + "</startPublicDate>"
                + "<endPublicDate>" + escapeXml(endPublicDate) + "</endPublicDate>"
                + "</publicDate>"
                + statusTag
                + "<fields><field>hasSt14</field></fields>"
                + "<sortType></sortType>"
                + "<requestEndDate>"
                + "<startRequestDate></startRequestDate>"
                + "<endRequestDate></endRequestDate>"
                + "</requestEndDate>"
                + "<IsSMPTerm></IsSMPTerm>"
                + "</query>";
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
