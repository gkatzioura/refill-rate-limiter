package io.github.resilience4j.ratelimiter;

import java.time.Duration;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.then;

public class RefillRateLimiterConfigTest {

    private static final int LIMIT = 50;
    private static final int PERMIT_CAPACITY = 60;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String REFRESH_PERIOD_MUST_NOT_BE_NULL = "RefreshPeriod must not be null";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void builderPositive() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
                                                                .timeoutDuration(TIMEOUT)
                                                                .limitRefreshPeriod(REFRESH_PERIOD)
                                                                .limitForPeriod(LIMIT)
                                                                .build();

        then(config.rateLimiterConfig().getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.rateLimiterConfig().getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        then(config.rateLimiterConfig().getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getPermitCapacity()).isEqualTo(LIMIT);
    }

    @Test
    public void builderLimitCapacityAdjusted() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
                                                                .timeoutDuration(TIMEOUT)
                                                                .limitRefreshPeriod(REFRESH_PERIOD)
                                                                .limitForPeriod(LIMIT)
                                                                .permitCapacity(PERMIT_CAPACITY)
                                                                .build();

        Duration adjustedPeriod = REFRESH_PERIOD.dividedBy(LIMIT).multipliedBy(PERMIT_CAPACITY);

        then(config.rateLimiterConfig().getLimitForPeriod()).isEqualTo(PERMIT_CAPACITY);
        then(config.rateLimiterConfig().getLimitRefreshPeriod()).isEqualTo(adjustedPeriod);
        then(config.rateLimiterConfig().getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getPermitCapacity()).isEqualTo(PERMIT_CAPACITY);
    }

    @Test
    public void testDefaultBurst() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
                                                                .timeoutDuration(TIMEOUT)
                                                                .limitRefreshPeriod(REFRESH_PERIOD)
                                                                .limitForPeriod(LIMIT)
                                                                .build();

        then(config.rateLimiterConfig().getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.rateLimiterConfig().getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        then(config.rateLimiterConfig().getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getPermitCapacity()).isEqualTo(LIMIT);
    }

    @Test
    public void testDefaultInitialPermits() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
                                                                .timeoutDuration(TIMEOUT)
                                                                .limitRefreshPeriod(REFRESH_PERIOD)
                                                                .limitForPeriod(LIMIT)
                                                                .build();

        then(config.rateLimiterConfig().getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.rateLimiterConfig().getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        then(config.rateLimiterConfig().getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getInitialPermits()).isEqualTo(LIMIT);
    }

    @Test
    public void builderTimeoutIsNull() throws Exception {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() ->RefillRateLimiterConfig.custom()
                               .timeoutDuration(null));
    }

    @Test
    public void builderRefreshPeriodIsNull() throws Exception {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> RefillRateLimiterConfig.custom()
                                                         .limitRefreshPeriod(null));
    }

    @Test
    public void builderRefreshPeriodTooShort() throws Exception {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->RefillRateLimiterConfig.custom()
                               .timeoutDuration(TIMEOUT)
                               .limitRefreshPeriod(Duration.ZERO)
                               .limitForPeriod(LIMIT)
                               .build());
    }

    @Test
    public void builderLimitIsLessThanOne() throws Exception {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> RefillRateLimiterConfig.custom()
                               .limitForPeriod(0));
    }

}
