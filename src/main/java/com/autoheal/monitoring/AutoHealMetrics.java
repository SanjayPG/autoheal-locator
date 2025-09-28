package com.autoheal.monitoring;

import com.autoheal.metrics.AIServiceMetrics;
import com.autoheal.metrics.CacheMetrics;
import com.autoheal.metrics.LocatorMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive metrics container for AutoHeal system
 */
public class AutoHealMetrics {
    private final LocatorMetrics locatorMetrics;
    private final CacheMetrics cacheMetrics;
    private final AIServiceMetrics aiServiceMetrics;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocatorMetrics locatorMetrics;
        private CacheMetrics cacheMetrics;
        private AIServiceMetrics aiServiceMetrics;

        public Builder locatorMetrics(LocatorMetrics metrics) {
            this.locatorMetrics = metrics;
            return this;
        }

        public Builder cacheMetrics(CacheMetrics metrics) {
            this.cacheMetrics = metrics;
            return this;
        }

        public Builder aiServiceMetrics(AIServiceMetrics metrics) {
            this.aiServiceMetrics = metrics;
            return this;
        }

        public AutoHealMetrics build() {
            return new AutoHealMetrics(locatorMetrics, cacheMetrics, aiServiceMetrics);
        }
    }

    private AutoHealMetrics(LocatorMetrics locatorMetrics, CacheMetrics cacheMetrics, AIServiceMetrics aiServiceMetrics) {
        this.locatorMetrics = locatorMetrics;
        this.cacheMetrics = cacheMetrics;
        this.aiServiceMetrics = aiServiceMetrics;
    }

    // Getters
    public LocatorMetrics getLocatorMetrics() { return locatorMetrics; }
    public CacheMetrics getCacheMetrics() { return cacheMetrics; }
    public AIServiceMetrics getAiServiceMetrics() { return aiServiceMetrics; }

    /**
     * Convert metrics to a flat map for monitoring systems
     *
     * @return Map of metric names to values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> metrics = new HashMap<>();

        // Locator metrics
        if (locatorMetrics != null) {
            metrics.put("locator.total_requests", locatorMetrics.getTotalRequests());
            metrics.put("locator.success_rate", locatorMetrics.getSuccessRate());
            metrics.put("locator.cache_hit_rate", locatorMetrics.getCacheHitRate());
            metrics.put("locator.avg_execution_time", locatorMetrics.getAverageExecutionTime());
        }

        // Cache metrics
        if (cacheMetrics != null) {
            metrics.put("cache.hits", cacheMetrics.getHits());
            metrics.put("cache.misses", cacheMetrics.getMisses());
            metrics.put("cache.hit_rate", cacheMetrics.getHitRate());
            metrics.put("cache.evictions", cacheMetrics.getEvictions());
            metrics.put("cache.avg_load_time", cacheMetrics.getAverageLoadTime());
        }

        // AI service metrics
        if (aiServiceMetrics != null) {
            metrics.put("ai.total_requests", aiServiceMetrics.getTotalRequests());
            metrics.put("ai.success_rate", aiServiceMetrics.getSuccessRate());
            metrics.put("ai.avg_response_time", aiServiceMetrics.getAverageResponseTime());
            metrics.put("ai.circuit_breaker_opens", aiServiceMetrics.getCircuitBreakerOpenCount());
        }

        return metrics;
    }

    @Override
    public String toString() {
        return String.format("AutoHealMetrics{locator=%s, cache=%s, ai=%s}",
                locatorMetrics != null ? "present" : "null",
                cacheMetrics != null ? "present" : "null",
                aiServiceMetrics != null ? "present" : "null");
    }
}