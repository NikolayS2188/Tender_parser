package ru.ten.parser.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Человечная задержка между запросами.
 * Случайное значение в диапазоне [minMillis, maxMillis].
 */
@Component
public class RequestDelay {

    private static final Logger log = LoggerFactory.getLogger(RequestDelay.class);

    /**
     * Спит случайное количество миллисекунд в диапазоне [min, max].
     *
     * @param minMillis минимальная задержка
     * @param maxMillis максимальная задержка
     */
    public void sleep(long minMillis, long maxMillis) {
        if (maxMillis < minMillis) {
            throw new IllegalArgumentException(
                    "maxMillis (" + maxMillis + ") < minMillis (" + minMillis + ")");
        }

        long delay = (minMillis == maxMillis)
                ? minMillis
                : ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1);

        log.debug("Пауза {} мс перед следующим запросом", delay);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Задержка прервана", e);
        }
    }
}
