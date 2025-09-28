package com.autoheal.config;

import java.time.Duration;

/**
 * Configuration for resilience patterns (circuit breaker, retry, etc.)
 */
public class ResilienceConfig {
    private final int circuitBreakerFailureThreshold;
    private final Duration circuitBreakerTimeout;
    private final int retryMaxAttempts;
    private final Duration retryDelay;

    public static Builder builder() {
        return new Builder();
    }

    public static ResilienceConfig defaultConfig() {
        return builder().build();
    }

    public static class Builder {
        private int circuitBreakerFailureThreshold = 5;
        private Duration circuitBreakerTimeout = Duration.ofMinutes(5);
        private int retryMaxAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(1);

        public Builder circuitBreakerFailureThreshold(int threshold) {
            this.circuitBreakerFailureThreshold = threshold;
            return this;
        }

        public Builder circuitBreakerTimeout(Duration timeout) {
            this.circuitBreakerTimeout = timeout;
            return this;
        }

        public Builder retryMaxAttempts(int maxAttempts) {
            this.retryMaxAttempts = maxAttempts;
            return this;
        }

        public Builder retryDelay(Duration delay) {
            this.retryDelay = delay;
            return this;
        }

        public ResilienceConfig build() {
            return new ResilienceConfig(circuitBreakerFailureThreshold, circuitBreakerTimeout,
                    retryMaxAttempts, retryDelay);
        }
    }

    private ResilienceConfig(int circuitBreakerFailureThreshold, Duration circuitBreakerTimeout,
                             int retryMaxAttempts, Duration retryDelay) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        this.circuitBreakerTimeout = circuitBreakerTimeout;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryDelay = retryDelay;
    }

    // Getters
    public int getCircuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
    public Duration getCircuitBreakerTimeout() { return circuitBreakerTimeout; }
    public int getRetryMaxAttempts() { return retryMaxAttempts; }
    public Duration getRetryDelay() { return retryDelay; }
}