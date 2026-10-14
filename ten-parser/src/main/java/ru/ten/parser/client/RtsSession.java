package ru.ten.parser.client;

import java.net.HttpCookie;
import java.util.List;

/**
 * Сессия РТС-Тендер: для хранения cookies + fotoken, полученные через Selenium, после прохождения Anti-DDoS.
 */
public record RtsSession(
        List<HttpCookie> cookies,
        String xsrfToken,
        String fotoken
) {
}
