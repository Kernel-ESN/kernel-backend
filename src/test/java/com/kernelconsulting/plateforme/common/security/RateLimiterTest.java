package com.kernelconsulting.plateforme.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final class MovableClock extends Clock {
        long millis;

        @Override
        public long millis() {
            return millis;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void bloqueAuDelaDeLaLimitePuisRepartLaMinuteSuivante() {
        MovableClock clock = new MovableClock();
        RateLimiter limiter = new RateLimiter(2, clock);
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isFalse();
        clock.millis = 60_000;
        assertThat(limiter.tryAcquire("ip")).isTrue();
    }

    @Test
    void compteChaqueAdresseAPart() {
        RateLimiter limiter = new RateLimiter(1, new MovableClock());
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("b")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
    }
}
