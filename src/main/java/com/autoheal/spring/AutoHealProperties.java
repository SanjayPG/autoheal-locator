package com.autoheal.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for AutoHeal
 */
@ConfigurationProperties("autoheal")
public class AutoHealProperties {
    private Cache cache = new Cache();
    private Ai ai = new Ai();
    private Performance performance = new Performance();
    private Resilience resilience = new Resilience();

    public static class Cache {
        private int maximumSize = 10000;
        private String expireAfterWrite = "24h";
        private String expireAfterAccess = "2h";
        private boolean recordStats = true;

        // Getters and setters
        public int getMaximumSize() { return maximumSize; }
        public void setMaximumSize(int maximumSize) { this.maximumSize = maximumSize; }
        public String getExpireAfterWrite() { return expireAfterWrite; }
        public void setExpireAfterWrite(String expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
        public String getExpireAfterAccess() { return expireAfterAccess; }
        public void setExpireAfterAccess(String expireAfterAccess) { this.expireAfterAccess = expireAfterAccess; }
        public boolean isRecordStats() { return recordStats; }
        public void setRecordStats(boolean recordStats) { this.recordStats = recordStats; }
    }

    public static class Ai {
        private String provider = "openai";
        private String apiKey = System.getenv("OPENAI_API_KEY");
        private String timeout = "30s";
        private int maxRetries = 3;
        private boolean visualAnalysisEnabled = false;

        // Getters and setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getTimeout() { return timeout; }
        public void setTimeout(String timeout) { this.timeout = timeout; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public boolean isVisualAnalysisEnabled() { return visualAnalysisEnabled; }
        public void setVisualAnalysisEnabled(boolean visualAnalysisEnabled) { this.visualAnalysisEnabled = visualAnalysisEnabled; }
    }

    public static class Performance {
        private int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        private String elementTimeout = "10s";
        private boolean enableMetrics = true;
        private int maxConcurrentRequests = 50;

        // Getters and setters
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        public String getElementTimeout() { return elementTimeout; }
        public void setElementTimeout(String elementTimeout) { this.elementTimeout = elementTimeout; }
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
        public void setMaxConcurrentRequests(int maxConcurrentRequests) { this.maxConcurrentRequests = maxConcurrentRequests; }
    }

    public static class Resilience {
        private int circuitBreakerFailureThreshold = 5;
        private String circuitBreakerTimeout = "5m";
        private int retryMaxAttempts = 3;
        private String retryDelay = "1s";

        // Getters and setters
        public int getCircuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
        public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) { this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold; }
        public String getCircuitBreakerTimeout() { return circuitBreakerTimeout; }
        public void setCircuitBreakerTimeout(String circuitBreakerTimeout) { this.circuitBreakerTimeout = circuitBreakerTimeout; }
        public int getRetryMaxAttempts() { return retryMaxAttempts; }
        public void setRetryMaxAttempts(int retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }
        public String getRetryDelay() { return retryDelay; }
        public void setRetryDelay(String retryDelay) { this.retryDelay = retryDelay; }
    }

    // Getters and setters for main properties
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }
    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }
    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }
    public Resilience getResilience() { return resilience; }
    public void setResilience(Resilience resilience) { this.resilience = resilience; }
}