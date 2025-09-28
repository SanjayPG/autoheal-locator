package com.autoheal.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for element locator performance and usage
 */
public class LocatorMetrics {
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successfulRequests = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong totalExecutionTime = new AtomicLong();

    /**
     * Record a locator request with its outcome
     *
     * @param success true if request was successful
     * @param executionTimeMs execution time in milliseconds
     * @param fromCache true if result came from cache
     */
    public void recordRequest(boolean success, long executionTimeMs, boolean fromCache) {
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
        }
        totalExecutionTime.addAndGet(executionTimeMs);

        if (fromCache) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
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
     * Calculate cache hit rate
     *
     * @return Cache hit rate between 0.0 and 1.0
     */
    public double getCacheHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }

    /**
     * Calculate average execution time
     *
     * @return Average execution time in milliseconds
     */
    public double getAverageExecutionTime() {
        long total = totalRequests.get();
        return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
    }

    // Getters for raw values
    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public long getCacheHits() { return cacheHits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }
    public long getTotalExecutionTime() { return totalExecutionTime.get(); }
}