package ru.ten.parser.client;

import ru.ten.parser.model.SearchResponse;

import java.util.List;

/**
 * Общий интерфейс для клиентов тендерных площадок.
 */
public interface TenderClient {

    /**
     * Поиск по одному ключевому слову.
     */
    SearchResponse search(String keyword, String status, int pageNum) throws Exception;

    /**
     * Поиск по нескольким ключевым словам.
     */
    SearchResponse searchMulti(List<String> keywords, String status, int pageNum) throws Exception;

    /**
     * Название площадки.
     */
    String getPlatformName();
}
