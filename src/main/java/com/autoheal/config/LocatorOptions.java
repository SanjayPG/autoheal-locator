package com.autoheal.config;

import java.time.Duration;

/**
 * Configuration options for element location operations
 */
public class LocatorOptions {
    private final Duration timeout;
    private final boolean enableVisualAnalysis;
    private final boolean enableCaching;
    private final double confidenceThreshold;
    private final int maxCandidates;

    public static Builder builder() {
        return new Builder();
    }

    public static LocatorOptions defaultOptions() {
        return builder().build();
    }

    public static class Builder {
        private Duration timeout = Duration.ofSeconds(10);
        private boolean enableVisualAnalysis = true;
        private boolean enableCaching = true;
        private double confidenceThreshold = 0.7;
        private int maxCandidates = 5;

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder enableVisualAnalysis(boolean enable) {
            this.enableVisualAnalysis = enable;
            return this;
        }

        public Builder enableCaching(boolean enable) {
            this.enableCaching = enable;
            return this;
        }

        public Builder confidenceThreshold(double threshold) {
            this.confidenceThreshold = threshold;
            return this;
        }

        public Builder maxCandidates(int maxCandidates) {
            this.maxCandidates = maxCandidates;
            return this;
        }

        public LocatorOptions build() {
            return new LocatorOptions(timeout, enableVisualAnalysis, enableCaching,
                    confidenceThreshold, maxCandidates);
        }
    }

    private LocatorOptions(Duration timeout, boolean enableVisualAnalysis, boolean enableCaching,
                           double confidenceThreshold, int maxCandidates) {
        this.timeout = timeout;
        this.enableVisualAnalysis = enableVisualAnalysis;
        this.enableCaching = enableCaching;
        this.confidenceThreshold = confidenceThreshold;
        this.maxCandidates = maxCandidates;
    }

    // Getters
    public Duration getTimeout() { return timeout; }
    public boolean isEnableVisualAnalysis() { return enableVisualAnalysis; }
    public boolean isEnableCaching() { return enableCaching; }
    public double getConfidenceThreshold() { return confidenceThreshold; }
    public int getMaxCandidates() { return maxCandidates; }
}