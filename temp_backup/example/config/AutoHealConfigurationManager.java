package com.example.config;

import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import com.autoheal.config.CacheConfig;
import com.autoheal.config.PerformanceConfig;
import com.autoheal.config.ResilienceConfig;
import com.autoheal.model.AIProvider;
import com.autoheal.model.ExecutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Configuration manager for AutoHeal with Playwright integration
 */
public class AutoHealConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(AutoHealConfigurationManager.class);
    
    private static final String DEFAULT_CONFIG_FILE = "autoheal-playwright.properties";
    private static final String FALLBACK_CONFIG_FILE = "autoheal-default.properties";
    
    private static AutoHealConfiguration cachedConfiguration;
    private static Properties properties;
    
    /**
     * Get AutoHeal configuration for Playwright
     */
    public static AutoHealConfiguration getConfiguration() {
        if (cachedConfiguration == null) {
            synchronized (AutoHealConfigurationManager.class) {
                if (cachedConfiguration == null) {
                    cachedConfiguration = loadConfiguration();
                }
            }
        }
        return cachedConfiguration;
    }
    
    /**
     * Load configuration from properties files
     */
    private static AutoHealConfiguration loadConfiguration() {
        properties = loadProperties();
        
        return AutoHealConfiguration.builder()
            .cacheConfig(buildCacheConfig())
            .aiConfig(buildAIConfig())
            .performanceConfig(buildPerformanceConfig())
            .resilienceConfig(buildResilienceConfig())
            .enableCaching(getBooleanProperty("autoheal.features.enable-caching", true))
            .enableRetries(getBooleanProperty("autoheal.resilience.retry-max-attempts", 3) > 0)
            .maxRetries(getIntProperty("autoheal.resilience.retry-max-attempts", 3))
            .elementTimeout(Duration.ofSeconds(getIntProperty("autoheal.performance.element-timeout-seconds", 15)))
            .build();
    }
    
    /**
     * Load properties from configuration files
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        
        // Load default configuration first
        loadPropertiesFromFile(props, FALLBACK_CONFIG_FILE);
        
        // Override with Playwright-specific configuration
        loadPropertiesFromFile(props, DEFAULT_CONFIG_FILE);
        
        // Override with system properties
        props.putAll(System.getProperties());
        
        logger.info("Loaded AutoHeal configuration from {} properties", props.size());
        return props;
    }
    
    /**
     * Load properties from a specific file
     */
    private static void loadPropertiesFromFile(Properties props, String filename) {
        try (InputStream input = AutoHealConfigurationManager.class
                .getClassLoader().getResourceAsStream(filename)) {
            
            if (input != null) {
                props.load(input);
                logger.debug("Loaded properties from: {}", filename);
            } else {
                logger.warn("Configuration file not found: {}", filename);
            }
        } catch (IOException e) {
            logger.error("Failed to load properties from {}: {}", filename, e.getMessage());
        }
    }
    
    /**
     * Build cache configuration
     */
    private static CacheConfig buildCacheConfig() {
        return CacheConfig.builder()
            .maximumSize(getIntProperty("autoheal.cache.maximum-size", 15000))
            .expireAfterWrite(parseDuration("autoheal.cache.expire-after-write", "24h"))
            .expireAfterAccess(parseDuration("autoheal.cache.expire-after-access", "4h"))
            .recordStats(getBooleanProperty("autoheal.cache.record-stats", true))
            .build();
    }
    
    /**
     * Build AI service configuration
     */
    private static AIConfig buildAIConfig() {
        String providerString = getStringProperty("autoheal.ai.provider", "openai");
        AIProvider provider = AIProvider.valueOf(providerString.toUpperCase());
        
        return AIConfig.builder()
            .provider(provider)
            .apiKey(getStringProperty("autoheal.ai.api-key", ""))
            .timeout(parseDuration("autoheal.ai.timeout", "30s"))
            .maxRetries(getIntProperty("autoheal.ai.max-retries", 3))
            .visualAnalysisEnabled(getBooleanProperty("autoheal.ai.visual-analysis-enabled", true))
            .model(getStringProperty("autoheal.ai.model", "gpt-4"))
            .build();
    }
    
    /**
     * Build performance configuration
     */
    private static PerformanceConfig buildPerformanceConfig() {
        return PerformanceConfig.builder()
            .threadPoolSize(getIntProperty("autoheal.performance.thread-pool-size", 12))
            .elementTimeout(parseDuration("autoheal.performance.element-timeout", "15s"))
            .enableMetrics(getBooleanProperty("autoheal.performance.enable-metrics", true))
            .maxConcurrentRequests(getIntProperty("autoheal.performance.max-concurrent-requests", 75))
            .executionStrategy(ExecutionStrategy.COST_OPTIMIZED)
            .build();
    }
    
    /**
     * Build resilience configuration
     */
    private static ResilienceConfig buildResilienceConfig() {
        return ResilienceConfig.builder()
            .circuitBreakerFailureThreshold(getIntProperty("autoheal.resilience.circuit-breaker-failure-threshold", 5))
            .circuitBreakerTimeout(parseDuration("autoheal.resilience.circuit-breaker-timeout", "3m"))
            .retryMaxAttempts(getIntProperty("autoheal.resilience.retry-max-attempts", 3))
            .retryDelay(parseDuration("autoheal.resilience.retry-delay", "1s"))
            .build();
    }
    
    // ==================== PROPERTY HELPER METHODS ====================
    
    private static String getStringProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    private static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for property {}: {}. Using default: {}", 
                    key, value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    private static Duration parseDuration(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        try {
            return Duration.parse("PT" + value.toUpperCase());
        } catch (Exception e) {
            logger.warn("Invalid duration format for property {}: {}. Using default: {}", 
                key, value, defaultValue);
            try {
                return Duration.parse("PT" + defaultValue.toUpperCase());
            } catch (Exception ex) {
                return Duration.ofSeconds(30); // Fallback to 30 seconds
            }
        }
    }
    
    // ==================== CONFIGURATION ACCESS METHODS ====================
    
    /**
     * Get test environment
     */
    public static String getTestEnvironment() {
        return getStringProperty("autoheal.test.environment", "local");
    }
    
    /**
     * Get base URL for tests
     */
    public static String getBaseUrl() {
        return getStringProperty("autoheal.test.base-url", "https://www.saucedemo.com");
    }
    
    /**
     * Check if headless mode is enabled
     */
    public static boolean isHeadless() {
        return getBooleanProperty("autoheal.test.headless", false);
    }
    
    /**
     * Get browser type
     */
    public static String getBrowser() {
        return getStringProperty("autoheal.test.browser", "chromium");
    }
    
    /**
     * Check if tracing is enabled
     */
    public static boolean isTracingEnabled() {
        return getBooleanProperty("autoheal.playwright.enable-tracing", true);
    }
    
    /**
     * Check if screenshot on failure is enabled
     */
    public static boolean isScreenshotOnFailureEnabled() {
        return getBooleanProperty("autoheal.playwright.screenshot-on-failure", true);
    }
    
    /**
     * Get Playwright wait timeout
     */
    public static int getWaitTimeout() {
        return getIntProperty("autoheal.playwright.wait-timeout", 10000);
    }
    
    /**
     * Get navigation timeout
     */
    public static int getNavigationTimeout() {
        return getIntProperty("autoheal.playwright.navigation-timeout", 30000);
    }
    
    /**
     * Get slow motion setting
     */
    public static int getSlowMotion() {
        return getIntProperty("autoheal.playwright.slow-motion", 0);
    }
    
    /**
     * Check if AI healing is enabled
     */
    public static boolean isAIHealingEnabled() {
        return getBooleanProperty("autoheal.features.enable-ai-healing", true);
    }
    
    /**
     * Check if visual healing is enabled
     */
    public static boolean isVisualHealingEnabled() {
        return getBooleanProperty("autoheal.features.enable-visual-healing", false);
    }
    
    /**
     * Reload configuration (useful for testing)
     */
    public static synchronized void reloadConfiguration() {
        cachedConfiguration = null;
        properties = null;
        logger.info("AutoHeal configuration reloaded");
    }
    
    /**
     * Print current configuration for debugging
     */
    public static void printConfiguration() {
        if (properties != null) {
            logger.info("AutoHeal Configuration:");
            properties.entrySet().stream()
                .filter(entry -> entry.getKey().toString().startsWith("autoheal."))
                .sorted((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
                .forEach(entry -> logger.info("  {} = {}", entry.getKey(), entry.getValue()));
        }
    }
}