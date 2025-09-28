package com.autoheal.monitoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Health status information for AutoHeal system
 */
public class HealthStatus {
    private final boolean overall;
    private final boolean aiServiceHealthy;
    private final double successRate;
    private final double cacheHitRate;
    private final Map<String, Object> details;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean overall;
        private boolean aiServiceHealthy = true;
        private double successRate = 0.0;
        private double cacheHitRate = 0.0;
        private Map<String, Object> details = new HashMap<>();

        public Builder overall(boolean overall) {
            this.overall = overall;
            return this;
        }

        public Builder aiServiceHealthy(boolean healthy) {
            this.aiServiceHealthy = healthy;
            return this;
        }

        public Builder successRate(double rate) {
            this.successRate = rate;
            return this;
        }

        public Builder cacheHitRate(double rate) {
            this.cacheHitRate = rate;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = new HashMap<>(details);
            return this;
        }

        public Builder addDetail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public HealthStatus build() {
            return new HealthStatus(overall, aiServiceHealthy, successRate, cacheHitRate, details);
        }
    }

    private HealthStatus(boolean overall, boolean aiServiceHealthy, double successRate,
                         double cacheHitRate, Map<String, Object> details) {
        this.overall = overall;
        this.aiServiceHealthy = aiServiceHealthy;
        this.successRate = successRate;
        this.cacheHitRate = cacheHitRate;
        this.details = Collections.unmodifiableMap(new HashMap<>(details));
    }

    // Getters
    public boolean isOverall() {
        return overall;
    }

    public boolean isAiServiceHealthy() {
        return aiServiceHealthy;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public double getCacheHitRate() {
        return cacheHitRate;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * Check if the system is healthy (alias for isOverall for testing compatibility)
     *
     * @return true if system is healthy, false otherwise
     */
    public boolean isHealthy() {
        return overall;
    }

    /**
     * Get health status as a severity level
     *
     * @return severity level (HEALTHY, DEGRADED, CRITICAL)
     */
    public Severity getSeverity() {
        if (!overall) {
            return Severity.CRITICAL;
        } else if (successRate < 0.9 || cacheHitRate < 0.5 || !aiServiceHealthy) {
            return Severity.DEGRADED;
        } else {
            return Severity.HEALTHY;
        }
    }

    /**
     * Health severity levels
     */
    public enum Severity {
        HEALTHY,
        DEGRADED,
        CRITICAL
    }

    @Override
    public String toString() {
        return String.format("HealthStatus{overall=%s, aiHealthy=%s, successRate=%.2f, cacheHitRate=%.2f, severity=%s}",
                overall, aiServiceHealthy, successRate, cacheHitRate, getSeverity());
    }
}