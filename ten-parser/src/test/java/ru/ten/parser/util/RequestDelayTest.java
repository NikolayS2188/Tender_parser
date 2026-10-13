package ru.ten.parser.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestDelayTest {

    private final RequestDelay delay = new RequestDelay();

    @Test
    void shouldSleepAtLeastMinMillis() {
        long start = System.nanoTime();
        delay.sleep(100, 150);
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsed).isGreaterThanOrEqualTo(100);
    }

    @Test
    void shouldSleepAtMostMaxMillisPlusTolerance() {
        long start = System.nanoTime();
        delay.sleep(100, 150);
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsed).isLessThan(150 + 200);
    }

    @Test
    void shouldSleepExactValueWhenMinEqualsMax() {
        long start = System.nanoTime();
        delay.sleep(100, 100);
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsed).isGreaterThanOrEqualTo(100);
    }

    @Test
    void shouldThrowWhenMaxLessThanMin() {
        assertThatThrownBy(() -> delay.sleep(200, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxMillis");
    }
}
