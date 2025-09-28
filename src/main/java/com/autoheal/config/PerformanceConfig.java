package com.autoheal.config;

import com.autoheal.model.ExecutionStrategy;
import java.time.Duration;

/**
 * Configuration for performance tuning
 */
public class PerformanceConfig {
    private final int threadPoolSize;
    private final Duration elementTimeout;
    private final boolean enableMetrics;
    private final int maxConcurrentRequests;
    private final ExecutionStrategy executionStrategy;

    public static Builder builder() {
        return new Builder();
    }

    public static PerformanceConfig defaultConfig() {
        return builder().build();
    }

    public static class Builder {
        private int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        private Duration elementTimeout = Duration.ofSeconds(10);
        private boolean enableMetrics = true;
        private int maxConcurrentRequests = 50;
        private ExecutionStrategy executionStrategy = ExecutionStrategy.SEQUENTIAL;

        public Builder threadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public Builder elementTimeout(Duration elementTimeout) {
            this.elementTimeout = elementTimeout;
            return this;
        }

        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }

        public Builder maxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
            return this;
        }

        public Builder executionStrategy(ExecutionStrategy executionStrategy) {
            this.executionStrategy = executionStrategy;
            return this;
        }

        public PerformanceConfig build() {
            return new PerformanceConfig(threadPoolSize, elementTimeout, enableMetrics, maxConcurrentRequests, executionStrategy);
        }
    }

    private PerformanceConfig(int threadPoolSize, Duration elementTimeout,
                              boolean enableMetrics, int maxConcurrentRequests, ExecutionStrategy executionStrategy) {
        this.threadPoolSize = threadPoolSize;
        this.elementTimeout = elementTimeout;
        this.enableMetrics = enableMetrics;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.executionStrategy = executionStrategy;
    }

    // Getters
    public int getThreadPoolSize() { return threadPoolSize; }
    public Duration getElementTimeout() { return elementTimeout; }
    public boolean isEnableMetrics() { return enableMetrics; }
    public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
    public ExecutionStrategy getExecutionStrategy() { return executionStrategy; }
}