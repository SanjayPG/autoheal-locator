package com.autoheal.resilience;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit Breaker implementation for AI service resilience
 */
public class CircuitBreaker {

    /**
     * Circuit breaker states
     */
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Failing, blocking requests
        HALF_OPEN  // Testing if service has recovered
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final int failureThreshold;
    private final long timeoutMs;

    /**
     * Create a new circuit breaker
     *
     * @param failureThreshold number of failures before opening circuit
     * @param timeout duration to wait before attempting half-open
     */
    public CircuitBreaker(int failureThreshold, Duration timeout) {
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeout.toMillis();
    }

    /**
     * Check if requests can be executed
     *
     * @return true if request can proceed, false if circuit is open
     */
    public boolean canExecute() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;
            case OPEN:
                if (System.currentTimeMillis() - lastFailureTime.get() > timeoutMs) {
                    state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    return true;
                }
                return false;
            case HALF_OPEN:
                return true;
            default:
                return false;
        }
    }

    /**
     * Record a successful operation
     */
    public void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    /**
     * Record a failed operation
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        if (failures >= failureThreshold) {
            state.set(State.OPEN);
        }
    }

    /**
     * Check if circuit breaker is open
     *
     * @return true if circuit is open (blocking requests)
     */
    public boolean isOpen() {
        return state.get() == State.OPEN;
    }

    /**
     * Get current state
     *
     * @return current circuit breaker state
     */
    public State getState() {
        return state.get();
    }

    /**
     * Get current failure count
     *
     * @return number of recent failures
     */
    public int getFailureCount() {
        return failureCount.get();
    }
}