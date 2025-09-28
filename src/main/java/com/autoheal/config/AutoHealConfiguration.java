package com.autoheal.config;

/**
 * Main configuration class for AutoHeal locator system
 */
public class AutoHealConfiguration {
    private final CacheConfig cacheConfig;
    private final AIConfig aiConfig;
    private final PerformanceConfig performanceConfig;
    private final ResilienceConfig resilienceConfig;
    private final ReportingConfig reportingConfig;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CacheConfig cacheConfig = CacheConfig.defaultConfig();
        private AIConfig aiConfig = null; // Lazy initialization - only load from properties if not explicitly set
        private PerformanceConfig performanceConfig = PerformanceConfig.defaultConfig();
        private ResilienceConfig resilienceConfig = ResilienceConfig.defaultConfig();
        private ReportingConfig reportingConfig = ReportingConfig.defaultConfig();

        public Builder cache(CacheConfig cacheConfig) {
            this.cacheConfig = cacheConfig;
            return this;
        }

        public Builder ai(AIConfig aiConfig) {
            this.aiConfig = aiConfig;
            return this;
        }

        public Builder performance(PerformanceConfig performanceConfig) {
            this.performanceConfig = performanceConfig;
            return this;
        }

        public Builder resilience(ResilienceConfig resilienceConfig) {
            this.resilienceConfig = resilienceConfig;
            return this;
        }

        public Builder reporting(ReportingConfig reportingConfig) {
            this.reportingConfig = reportingConfig;
            return this;
        }

        public AutoHealConfiguration build() {
            // Lazy initialization of AI config - only load from properties if not explicitly set
            AIConfig finalAiConfig = aiConfig;
            if (finalAiConfig == null) {
                finalAiConfig = AIConfig.fromProperties();
            }
            return new AutoHealConfiguration(cacheConfig, finalAiConfig, performanceConfig, resilienceConfig, reportingConfig);
        }
    }

    private AutoHealConfiguration(CacheConfig cacheConfig, AIConfig aiConfig,
                                  PerformanceConfig performanceConfig, ResilienceConfig resilienceConfig,
                                  ReportingConfig reportingConfig) {
        this.cacheConfig = cacheConfig;
        this.aiConfig = aiConfig;
        this.performanceConfig = performanceConfig;
        this.resilienceConfig = resilienceConfig;
        this.reportingConfig = reportingConfig;
    }

    // Getters
    public CacheConfig getCacheConfig() { return cacheConfig; }
    public AIConfig getAiConfig() { return aiConfig; }
    public PerformanceConfig getPerformanceConfig() { return performanceConfig; }
    public ResilienceConfig getResilienceConfig() { return resilienceConfig; }
    public ReportingConfig getReportingConfig() { return reportingConfig; }
}