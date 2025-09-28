package com.autoheal.exception;

import java.time.Duration;

/**
 * Exception thrown when circuit breaker is open and blocking requests
 */
public class CircuitBreakerOpenException extends AutoHealException {
    private final Duration retryAfter;

    public CircuitBreakerOpenException(String message) {
        this(message, Duration.ofMinutes(5));
    }

    public CircuitBreakerOpenException(String message, Duration retryAfter) {
        super(ErrorCode.CIRCUIT_BREAKER_OPEN, message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}