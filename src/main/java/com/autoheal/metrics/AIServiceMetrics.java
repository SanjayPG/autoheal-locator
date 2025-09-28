package com.autoheal.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for AI service performance and reliability
 */
public class AIServiceMetrics {
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successfulRequests = new AtomicLong();
    private final AtomicLong failedRequests = new AtomicLong();
    private final AtomicLong totalResponseTime = new AtomicLong();
    private final AtomicLong circuitBreakerOpenCount = new AtomicLong();

    /**
     * Record an AI service request
     *
     * @param success true if request was successful
     * @param responseTimeMs response time in milliseconds
     */
    public void recordRequest(boolean success, long responseTimeMs) {
        totalRequests.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);

        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
    }

    /**
     * Record circuit breaker opening
     */
    public void recordCircuitBreakerOpen() {
        circuitBreakerOpenCount.incrementAndGet();
    }

    /**
     * Calculate current success rate
     *
     * @return Success rate between 0.0 and 1.0
     */
    public double getSuccessRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) successfulRequests.get() / total : 0.0;
    }

    /**
     * Calculate average response time
     *
     * @return Average response time in milliseconds
     */
    public double getAverageResponseTime() {
        long total = totalRequests.get();
        return total > 0 ? (double) totalResponseTime.get() / total : 0.0;
    }

    // Getters for raw values
    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public long getFailedRequests() { return failedRequests.get(); }
    public long getTotalResponseTime() { return totalResponseTime.get(); }
    public long getCircuitBreakerOpenCount() { return circuitBreakerOpenCount.get(); }
}