package com.autoheal.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for cache performance and efficiency
 */
public class CacheMetrics {
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong loadCount = new AtomicLong();
    private final AtomicLong totalLoadTime = new AtomicLong();

    /**
     * Record cache hit
     */
    public void recordHit() {
        hits.incrementAndGet();
    }

    /**
     * Record cache miss
     */
    public void recordMiss() {
        misses.incrementAndGet();
    }

    /**
     * Record cache eviction
     */
    public void recordEviction() {
        evictions.incrementAndGet();
    }

    /**
     * Record cache load operation
     *
     * @param loadTimeMs time taken to load in milliseconds
     */
    public void recordLoad(long loadTimeMs) {
        loadCount.incrementAndGet();
        totalLoadTime.addAndGet(loadTimeMs);
    }

    /**
     * Calculate cache hit rate
     *
     * @return Hit rate between 0.0 and 1.0
     */
    public double getHitRate() {
        long total = hits.get() + misses.get();
        return total > 0 ? (double) hits.get() / total : 0.0;
    }

    /**
     * Calculate average load time
     *
     * @return Average load time in milliseconds
     */
    public double getAverageLoadTime() {
        long loads = loadCount.get();
        return loads > 0 ? (double) totalLoadTime.get() / loads : 0.0;
    }

    // Getters for raw values
    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }
    public long getEvictions() { return evictions.get(); }
    public long getLoadCount() { return loadCount.get(); }
}