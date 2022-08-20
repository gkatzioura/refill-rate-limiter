package io.github.resilience4j.ratelimiter;


import java.io.Serializable;
import java.time.Duration;
import java.util.function.Predicate;

import io.vavr.control.Either;
import static java.util.Objects.requireNonNull;

/**
 * {@link RefillRateLimiter} is a permission rate based Rate Limiter.
 * Instead of resetting permits based on a permission period the permission release is based on a rate.
 * Therefore {@link RefillRateLimiterConfig#nanosPerPermit} is used which is a product of the division
 * of {@link RateLimiterConfig#limitRefreshPeriod} to {@link RateLimiterConfig#limitForPeriod}.
 */
public class RefillRateLimiterConfig implements Serializable {

    private static final long serialVersionUID = 3095810082683985263L;

    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL = "LimitRefreshPeriod must not be null";
    private static final Duration ACCEPTABLE_REFRESH_PERIOD = Duration.ofNanos(1L);
    private static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;
    private static final String ZERO_NANOS_PER_PERMISSION_STATE = "Current settings lead to zero nanos per permission, adjust period and limit";
    private static final String ZERO_NANOS_PER_PERMISSION_ARGUMENT = "At least 1 nanos per permission should be provided";

    private final RateLimiterConfig rateLimiterConfig;

    private final int permitCapacity;
    private final long nanosPerFullCapacity;
    private final int initialPermits;
    private final long nanosPerPermit;

    private static Predicate<Either<? extends Throwable, ?>> drainPermissionsOnResult = any -> false;

    private RefillRateLimiterConfig(Duration timeoutDuration, int permitCapacity, long nanosPerPermit,
                                    long nanosPerFullCapacity,
                                    int initialPermits, boolean writableStackTraceEnabled) {
        this.rateLimiterConfig = RateLimiterConfig.custom()
                                                  .timeoutDuration(timeoutDuration)
                                                  .limitRefreshPeriod(Duration.ofNanos(nanosPerPermit * permitCapacity))
                                                  .limitForPeriod(permitCapacity)
                                                  .drainPermissionsOnResult(drainPermissionsOnResult)
                                                  .writableStackTraceEnabled(writableStackTraceEnabled)
                                                  .build();

        noZeroNanosOnPermissionArgument(nanosPerPermit);
        this.permitCapacity = permitCapacity;
        this.nanosPerFullCapacity = nanosPerFullCapacity;
        this.initialPermits = initialPermits;
        this.nanosPerPermit = nanosPerPermit;
    }

    /**
     * Returns a builder to create a custom RefillRateLimiterConfig.
     *
     * @return a {@link RefillRateLimiterConfig.Builder}
     */
    public static RefillRateLimiterConfig.Builder custom() {
        return new RefillRateLimiterConfig.Builder();
    }

    /**
     * Returns a builder to create a custom RefillRateLimiterConfig using specified config as prototype
     *
     * @param prototype A {@link RefillRateLimiterConfig} prototype.
     * @return a {@link RefillRateLimiterConfig.Builder}
     */
    public static RefillRateLimiterConfig.Builder from(RefillRateLimiterConfig prototype) {
        return new RefillRateLimiterConfig.Builder(prototype);
    }

    /**
     * Creates a default RefillRateLimiter configuration.
     *
     * @return a default RefillRateLimiter configuration.
     */
    public static RefillRateLimiterConfig ofDefaults() {
        return new RefillRateLimiterConfig.Builder().build();
    }

    private static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    private static Duration checkLimitRefreshPeriod(Duration limitRefreshPeriod) {
        requireNonNull(limitRefreshPeriod, LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL);
        boolean refreshPeriodIsTooShort =
                limitRefreshPeriod.compareTo(ACCEPTABLE_REFRESH_PERIOD) < 0;
        if (refreshPeriodIsTooShort) {
            throw new IllegalArgumentException("LimitRefreshPeriod is too short");
        }
        return limitRefreshPeriod;
    }

    private static int checkLimitForPeriod(final int limitForPeriod) {
        if (limitForPeriod < 1) {
            throw new IllegalArgumentException("LimitForPeriod should be greater than 0");
        }
        return limitForPeriod;
    }

    /**
     * Return the underlying {@link RateLimiterConfig}
     */
    public RateLimiterConfig rateLimiterConfig() {
        return rateLimiterConfig;
    }

    /**
     * Get the permit capacity the RefillRateLimiter should have.
     *
     * @return
     */
    public int getPermitCapacity() {
        return permitCapacity;
    }

    /**
     * Get the permits the RefillRateLimiter is configured to start with.
     *
     * @return
     */
    public int getInitialPermits() {
        return initialPermits;
    }

    /**
     * Get the nanos needed to replenish one permit.
     *
     * @return
     */
    public long getNanosPerPermit() {
        return nanosPerPermit;
    }

    /**
     * Get the nanos needed to reach full capacity
     * @return
     */
    public long getNanosPerFullCapacity() {
        return nanosPerFullCapacity;
    }

    public Duration getLimitRefreshPeriod() {
        return rateLimiterConfig.getLimitRefreshPeriod();
    }

    @Override
    public String toString() {
        return "RefillRateLimiterConfig{" +
               "timeoutDuration=" + rateLimiterConfig.getTimeoutDuration() +
               ", permitCapacity=" + permitCapacity +
               ", nanosPerPermission=" + nanosPerPermit +
               ", writableStackTraceEnabled=" + rateLimiterConfig.isWritableStackTraceEnabled() +
               '}';
    }

    public static class Builder {
        private Duration timeoutDuration = Duration.ofSeconds(5);
        private Duration limitRefreshPeriod = Duration.ofNanos(500);
        private int limitForPeriod = 50;
        private int permitCapacity = 0;
        private int initialPermits = 0;
        private boolean initialPermitsSet;
        private Predicate<Either<? extends Throwable, ?>> drainPermissionsOnResult = any -> false;
        private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

        public Builder() {
        }

        public Builder(RefillRateLimiterConfig prototype) {
            this.timeoutDuration = prototype.rateLimiterConfig().getTimeoutDuration();
            this.limitRefreshPeriod = Duration.ofNanos(prototype.nanosPerPermit);
            this.limitForPeriod = 1;
            this.permitCapacity = prototype.permitCapacity;
            this.writableStackTraceEnabled = prototype.rateLimiterConfig().isWritableStackTraceEnabled();
        }

        public RefillRateLimiterConfig build() {
            if (permitCapacity < limitForPeriod) {
                permitCapacity = limitForPeriod;
            }

            if (!initialPermitsSet) {
                initialPermits = limitForPeriod;
            }

            final long nanosPerPermission = calculateNanosPerPermit(limitRefreshPeriod, limitForPeriod);
            noZeroNanosOnPermissionState(nanosPerPermission);

            final long nanosPerFullCapacity = calculateNanosPerFullCapacity(nanosPerPermission, permitCapacity);

            return new RefillRateLimiterConfig(timeoutDuration, permitCapacity, nanosPerPermission, nanosPerFullCapacity,
                                               initialPermits, writableStackTraceEnabled);
        }


        public Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        public Builder drainPermissionsOnResult(
                Predicate<Either<? extends Throwable, ?>> drainPermissionsOnResult) {
            this.drainPermissionsOnResult = drainPermissionsOnResult;
            return this;
        }

        public Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures the period needed for the permit number specified. After each period
         * permissions up to {@link RefillRateLimiterConfig.Builder#limitForPeriod} should be released.
         * Default value is 500 nanoseconds.
         *
         * @param limitRefreshPeriod the period of limit refresh
         * @return the RefillRateLimiterConfig.Builder
         */
        public Builder limitRefreshPeriod(final Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = checkLimitRefreshPeriod(limitRefreshPeriod);
            return this;
        }

        /**
         * Configures the permits to release through a refresh period. Count of permissions released
         * during one rate limiter period specified by {@link RefillRateLimiterConfig.Builder#limitRefreshPeriod}
         * value. Default value is 50.
         *
         * @param limitForPeriod the permissions limit for refresh period
         * @return the RefillRateLimiterConfig.Builder
         */
        public Builder limitForPeriod(final int limitForPeriod) {
            this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
            return this;
        }

        /**
         * Configures the permissions capacity. Count of max permissions available
         * If no value specified the default value is the one
         * specified for {@link RefillRateLimiterConfig.Builder#limitForPeriod}.
         *
         * @param permitCapacity the capacity of permissions
         * @return the RateLimiterConfig.Builder
         */
        public Builder permitCapacity(final int permitCapacity) {
            this.permitCapacity = permitCapacity;
            return this;
        }

        /**
         * Configures the initial permit available.
         * If no value specified the default value is the one
         * specified for {@link RefillRateLimiterConfig.Builder#limitForPeriod}.
         *
         * @param initialPermits the initial permits
         * @return the RateLimiterConfig.Builder
         */
        public Builder initialPermits(final int initialPermits) {
            this.initialPermits = initialPermits;
            this.initialPermitsSet = true;
            return this;
        }

        /**
         * Calculate the nanos needed for one permit
         *
         * @param limitRefreshPeriod
         * @param limitForPeriod
         * @return
         */
        private long calculateNanosPerPermit(Duration limitRefreshPeriod, int limitForPeriod) {
            long permissionsPeriodInNanos = limitRefreshPeriod.toNanos();
            return permissionsPeriodInNanos / limitForPeriod;
        }

        private long calculateNanosPerFullCapacity(long nanosPerPermission, long permitCapacity) {
            return nanosPerPermission * permitCapacity;
        }

    }

    private static void noZeroNanosOnPermissionArgument(long nanosPerPermit ) {
        if(nanosPerPermit<=0) {
            throw new IllegalArgumentException(ZERO_NANOS_PER_PERMISSION_ARGUMENT);
        }
    }

    private static void noZeroNanosOnPermissionState(long nanosPerPermit ) {
        if(nanosPerPermit<=0) {
            throw new IllegalStateException(ZERO_NANOS_PER_PERMISSION_STATE);
        }
    }

}