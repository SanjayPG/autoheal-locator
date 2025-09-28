package com.autoheal;

import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.LocatorOptions;
import com.autoheal.core.AIService;
import com.autoheal.core.ElementLocator;
import com.autoheal.core.SelectorCache;
import com.autoheal.core.WebAutomationAdapter;
import com.autoheal.exception.AutoHealException;
import com.autoheal.exception.ErrorCode;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import com.autoheal.impl.ai.ResilientAIService;
import com.autoheal.impl.cache.CaffeineBasedSelectorCache;
import com.autoheal.impl.cache.RedisBasedSelectorCache;
import com.autoheal.impl.cache.PersistentFileSelectorCache;
import com.autoheal.impl.locator.CostOptimizedHybridElementLocator;
import com.autoheal.impl.locator.DOMElementLocator;
import com.autoheal.impl.locator.HybridElementLocator;
import com.autoheal.impl.locator.VisualElementLocator;
import com.autoheal.metrics.CacheMetrics;
import com.autoheal.metrics.LocatorMetrics;
import com.autoheal.model.CachedSelector;
import com.autoheal.model.ElementContext;
import com.autoheal.model.LocatorRequest;
import com.autoheal.model.LocatorResult;
import com.autoheal.model.LocatorStrategy;
import com.autoheal.model.LocatorType;
import com.autoheal.monitoring.AutoHealMetrics;
import com.autoheal.monitoring.HealthStatus;
import com.autoheal.reporting.AutoHealReporter;
import com.autoheal.reporting.AutoHealReporter.SelectorStrategy;
import com.autoheal.util.LocatorTypeDetector;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main AutoHeal Locator facade providing enterprise-grade element location
 */
public class AutoHealLocator {
    private static final Logger logger = LoggerFactory.getLogger(AutoHealLocator.class);

    private final SelectorCache selectorCache;
    private final ElementLocator elementLocator;
    private final WebAutomationAdapter adapter;
    private final AutoHealConfiguration configuration;
    private final ExecutorService executorService;
    private final LocatorMetrics metrics;
    private final AutoHealReporter reporter;
    private final AIService aiService;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private WebAutomationAdapter adapter;
        private AutoHealConfiguration configuration = null; // Lazy initialization
        private SelectorCache customCache;
        private AIService customAIService;

        public Builder withWebAdapter(WebAutomationAdapter adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder withConfiguration(AutoHealConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withCache(SelectorCache cache) {
            this.customCache = cache;
            return this;
        }

        public Builder withAIService(AIService aiService) {
            this.customAIService = aiService;
            return this;
        }

        public AutoHealLocator build() {
            if (adapter == null) {
                throw new IllegalArgumentException("WebAutomationAdapter is required");
            }

            // Lazy initialization of configuration if not explicitly set
            AutoHealConfiguration finalConfiguration = configuration;
            if (finalConfiguration == null) {
                finalConfiguration = AutoHealConfiguration.builder().build();
            }

            SelectorCache cache = customCache != null ?
                    customCache : createCacheBasedOnConfig(finalConfiguration.getCacheConfig());

            AIService aiService = customAIService != null ?
                    customAIService : new ResilientAIService(finalConfiguration.getAiConfig(),
                    finalConfiguration.getResilienceConfig());

            return new AutoHealLocator(adapter, finalConfiguration, cache, aiService);
        }
    }

    private AutoHealLocator(WebAutomationAdapter adapter, AutoHealConfiguration configuration,
                            SelectorCache selectorCache, AIService aiService) {
        this.adapter = adapter;
        this.configuration = configuration;
        this.selectorCache = selectorCache;
        this.aiService = aiService;
        this.metrics = new LocatorMetrics();

        this.executorService = Executors.newFixedThreadPool(
                configuration.getPerformanceConfig().getThreadPoolSize()
        );

        // Initialize reporter if reporting is enabled
        this.reporter = configuration.getReportingConfig().isEnabled() ?
                        new AutoHealReporter(configuration.getAiConfig()) : null;
        if (this.reporter != null) {
            logger.info("AutoHeal Reporting System ACTIVE - All selector usage will be tracked");
        }

        // Initialize element locators with cost optimization
        List<ElementLocator> locators = Arrays.asList(
                new DOMElementLocator(aiService),
                new VisualElementLocator(aiService)
        );
        this.elementLocator = new CostOptimizedHybridElementLocator(
                locators, 
                configuration.getPerformanceConfig().getExecutionStrategy()
        );

        logger.info("AutoHealLocator initialized successfully");
    }

    // ==================== PUBLIC API METHODS ====================

    /**
     * Find element with auto-healing capabilities
     */
    public CompletableFuture<WebElement> findElementAsync(String selector, String description) {
        return findElementAsync(selector, description, LocatorOptions.defaultOptions());
    }

    /**
     * Find element with custom options
     */
    public CompletableFuture<WebElement> findElementAsync(String selector, String description, LocatorOptions options) {
        // Auto-detect locator type and create Selenium By object
        LocatorType detectedType = LocatorTypeDetector.detectType(selector);
        org.openqa.selenium.By seleniumBy = LocatorTypeDetector.createBy(selector, detectedType);

        logger.debug("Auto-detected '{}' as {} locator", selector, detectedType.getDisplayName());

        LocatorRequest request = LocatorRequest.builder()
                .selector(selector)
                .description(description)
                .options(options)
                .adapter(adapter)
                .locatorType(detectedType)
                .seleniumBy(seleniumBy)
                .build();

        return locateElementWithHealing(request)
                .thenApply(LocatorResult::getElement);
    }

    /**
     * Synchronous version for backward compatibility
     */
    public WebElement findElement(String selector, String description) {
        if (reporter != null) {
            return findElementWithReporting(selector, description);
        }

        try {
            return findElementAsync(selector, description)
                    .get(configuration.getPerformanceConfig().getElementTimeout().toMillis(),
                            TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new AutoHealException(ErrorCode.TIMEOUT_EXCEEDED,
                    "Element location timed out", e);
        }
    }

    /**
     * Synchronous element finding with integrated reporting
     */
    private WebElement findElementWithReporting(String selector, String description) {
        long startTime = System.currentTimeMillis();
        SelectorStrategy strategy = SelectorStrategy.FAILED;
        String actualSelector = selector;
        String elementDetails = null;
        String reasoning = null;
        boolean success = false;
        long tokensUsed = 0;

        try {
            // Create request like in findElementAsync
            LocatorType detectedType = LocatorTypeDetector.detectType(selector);
            org.openqa.selenium.By seleniumBy = LocatorTypeDetector.createBy(selector, detectedType);

            LocatorRequest request = LocatorRequest.builder()
                    .selector(selector)
                    .description(description)
                    .options(LocatorOptions.defaultOptions())
                    .adapter(adapter)
                    .locatorType(detectedType)
                    .seleniumBy(seleniumBy)
                    .build();

            // Get the full LocatorResult instead of just the element
            LocatorResult result = locateElementWithHealing(request)
                    .get(configuration.getPerformanceConfig().getElementTimeout().toMillis(),
                            TimeUnit.MILLISECONDS);

            WebElement element = result.getElement();
            long duration = System.currentTimeMillis() - startTime;

            success = true;
            elementDetails = String.format("%s#%s.%s",
                element.getTagName(),
                element.getAttribute("id") != null ? element.getAttribute("id") : "null",
                element.getAttribute("class") != null ? element.getAttribute("class") : "null");

            // Use ACTUAL strategy from LocatorResult instead of timing-based inference
            actualSelector = result.getActualSelector();
            reasoning = result.getReasoning();

            // Convert LocatorStrategy to SelectorStrategy
            switch (result.getStrategy()) {
                case ORIGINAL_SELECTOR:
                    strategy = SelectorStrategy.ORIGINAL_SELECTOR;
                    tokensUsed = 0;
                    break;
                case CACHED:
                    strategy = SelectorStrategy.CACHED;
                    tokensUsed = 0;
                    break;
                case DOM_ANALYSIS:
                    strategy = SelectorStrategy.DOM_ANALYSIS;
                    tokensUsed = 1500; // Estimate
                    break;
                case VISUAL_ANALYSIS:
                    strategy = SelectorStrategy.VISUAL_ANALYSIS;
                    tokensUsed = 45000; // Estimate
                    break;
                default:
                    strategy = SelectorStrategy.DOM_ANALYSIS;
                    tokensUsed = 1500;
                    break;
            }

            reporter.recordSelectorUsage(selector, description, strategy, duration,
                success, actualSelector, elementDetails, reasoning, tokensUsed);

            return element;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            strategy = SelectorStrategy.FAILED;
            reasoning = "Failed: " + e.getMessage();

            reporter.recordSelectorUsage(selector, description, strategy, duration,
                false, null, null, reasoning, 0);

            throw new AutoHealException(ErrorCode.TIMEOUT_EXCEEDED,
                    "Element location timed out", e);
        }
    }

    private String inferActualSelector(WebElement element) {
        // Try to infer the actual selector that was used
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) {
            return "#" + id;
        }

        String name = element.getAttribute("name");
        if (name != null && !name.isEmpty()) {
            return "[name='" + name + "']";
        }

        String className = element.getAttribute("class");
        if (className != null && !className.isEmpty()) {
            String firstClass = className.split(" ")[0];
            return "." + firstClass;
        }

        return element.getTagName();
    }

    /**
     * Find multiple elements with healing
     */
    public CompletableFuture<List<WebElement>> findElementsAsync(String selector, String description) {
        return findElementAsync(selector, description)
                .thenCompose(firstElement -> {
                    // If we found one element, find all elements with the same successful selector
                    return adapter.findElements(getLastSuccessfulSelector(selector, description))
                            .exceptionally(throwable -> Arrays.asList(firstElement));
                });
    }

    /**
     * Synchronous version for finding multiple elements
     */
    public List<WebElement> findElements(String selector, String description) {
        try {
            return findElementsAsync(selector, description)
                    .get(configuration.getPerformanceConfig().getElementTimeout().toMillis(),
                            TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new AutoHealException(ErrorCode.TIMEOUT_EXCEEDED,
                    "Multiple elements location timed out", e);
        }
    }

    /**
     * Check if element is present without throwing exceptions
     */
    public CompletableFuture<Boolean> isElementPresentAsync(String selector, String description) {
        return findElementAsync(selector, description)
                .thenApply(element -> true)
                .exceptionally(throwable -> false);
    }

    // ==================== CORE HEALING LOGIC ====================

    private CompletableFuture<LocatorResult> locateElementWithHealing(LocatorRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: ALWAYS try original selector first (might be correct!)
        return trySelectorWithBy(request.getSeleniumBy(), request, startTime)
                .thenCompose(originalResult -> {
                    if (originalResult != null) {
                        // Original selector worked! Cache it and return immediately
                        cacheSuccessfulSelector(request, request.getOriginalSelector(), originalResult);
                        return CompletableFuture.completedFuture(
                                LocatorResult.builder()
                                        .element(originalResult)
                                        .actualSelector(request.getOriginalSelector())
                                        .strategy(LocatorStrategy.ORIGINAL_SELECTOR)
                                        .executionTime(Duration.ofMillis(System.currentTimeMillis() - startTime))
                                        .fromCache(false)
                                        .confidence(1.0)
                                        .reasoning("Original selector worked")
                                        .build()
                        );
                    } else {
                        // Step 2: Original selector failed, now check cache
                        return tryCache(request, startTime);
                    }
                });
    }

    private CompletableFuture<LocatorResult> tryCache(LocatorRequest request, long startTime) {
        if (!request.getOptions().isEnableCaching()) {
            return performHealing(request, startTime);
        }

        String cacheKey = generateCacheKey(request);
        Optional<CachedSelector> cached = selectorCache.get(cacheKey);


        if (cached.isPresent() && cached.get().getCurrentSuccessRate() > 0.7) {
            // Found valid cache entry - try using it
            org.openqa.selenium.By cachedBy = LocatorTypeDetector.autoCreateBy(cached.get().getSelector());

            // Use a timeout to ensure cache doesn't hang
            return trySelectorWithBy(cachedBy, request, startTime)
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenCompose(result -> {
                        if (result != null) {
                            // Cache hit successful!
                            selectorCache.updateSuccess(cacheKey, true);
                            return CompletableFuture.completedFuture(LocatorResult.builder()
                                    .element(result)
                                    .actualSelector(cached.get().getSelector())
                                    .strategy(LocatorStrategy.CACHED)
                                    .executionTime(Duration.ofMillis(System.currentTimeMillis() - startTime))
                                    .fromCache(true)
                                    .confidence(cached.get().getCurrentSuccessRate())
                                    .reasoning("Retrieved from cache")
                                    .build());
                        } else {
                            // Cache miss - element not found with cached selector
                            selectorCache.updateSuccess(cacheKey, false);
                            return performHealing(request, startTime);
                        }
                    })
                    .exceptionally(throwable -> {
                        // Cache failed due to timeout or error - try healing
                        logger.warn("Cache lookup failed for {}: {}", request.getOriginalSelector(), throwable.getMessage());
                        selectorCache.updateSuccess(cacheKey, false);
                        // Don't use join() - return the future directly
                        try {
                            return performHealing(request, startTime).get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        // No cache entry or low success rate - perform healing
        return performHealing(request, startTime);
    }

    private CompletableFuture<LocatorResult> performHealing(LocatorRequest request, long startTime) {
        return elementLocator.locate(request)
                .thenApply(result -> {
                    // Cache the successful result
                    if (result.getElement() != null) {
                        cacheSuccessfulSelector(request, result.getActualSelector(), result.getElement());
                    }
                    return result;
                })
                .exceptionally(throwable -> {
                    throw new AutoHealException(ErrorCode.ELEMENT_NOT_FOUND,
                            "All healing strategies failed for selector: " + request.getOriginalSelector(),
                            throwable);
                });
    }

    private CompletableFuture<WebElement> trySelector(String selector, LocatorRequest request, long startTime) {
        return adapter.findElements(selector)
                .thenApply(elements -> {
                    if (!elements.isEmpty()) {
                        return disambiguateElements(elements, request);
                    }
                    return null;
                })
                .exceptionally(throwable -> null);
    }

    private CompletableFuture<WebElement> trySelectorWithBy(org.openqa.selenium.By by, LocatorRequest request, long startTime) {
        return adapter.findElements(by)
                .thenApply(elements -> {
                    if (!elements.isEmpty()) {
                        return disambiguateElements(elements, request);
                    }
                    return null;
                })
                .exceptionally(throwable -> null);
    }

    private WebElement disambiguateElements(List<WebElement> elements, LocatorRequest request) {
        if (elements.size() == 1) {
            return elements.get(0);
        }

        // Multiple elements found - use AI for disambiguation
        try {
            logger.debug("Multiple elements found ({}), using AI for disambiguation with description: {}",
                        elements.size(), request.getDescription());

            return aiService.selectBestMatchingElement(elements, request.getDescription())
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("AI disambiguation failed, falling back to first element: {}", e.getMessage());
            return elements.get(0);
        }
    }

    private void cacheSuccessfulSelector(LocatorRequest request, String successfulSelector, WebElement element) {
        if (!request.getOptions().isEnableCaching()) {
            return;
        }

        try {
            String cacheKey = generateCacheKey(request);
            // Get element context synchronously to ensure cache is populated immediately
            ElementContext context = adapter.getElementContext(element).join();

            CachedSelector cachedSelector = new CachedSelector(successfulSelector, context.getFingerprint());
            selectorCache.put(cacheKey, cachedSelector);

        } catch (Exception e) {
            logger.error("Failed to cache selector: {}", e.getMessage());
        }
    }

    private String generateCacheKey(LocatorRequest request) {
        if (selectorCache instanceof CaffeineBasedSelectorCache) {
            return ((CaffeineBasedSelectorCache) selectorCache)
                    .generateContextualKey(request.getOriginalSelector(),
                            request.getDescription(),
                            request.getContext());
        } else {
            return request.getOriginalSelector() + "|" + request.getDescription();
        }
    }

    private String getLastSuccessfulSelector(String originalSelector, String description) {
        String cacheKey = originalSelector + "|" + description;
        Optional<CachedSelector> cached = selectorCache.get(cacheKey);
        return cached.map(CachedSelector::getSelector).orElse(originalSelector);
    }

    // ==================== CACHE MANAGEMENT METHODS ====================

    /**
     * Clear all cached selectors
     * Useful when page structure changes or you want to force fresh AI healing
     */
    public void clearCache() {
        selectorCache.clearAll();
    }

    /**
     * Remove a specific cached selector
     *
     * @param originalSelector The original selector to remove from cache
     * @param description The description used when the selector was cached
     * @return true if the entry was removed, false if it didn't exist
     */
    public boolean removeCachedSelector(String originalSelector, String description) {
        LocatorRequest tempRequest = LocatorRequest.builder()
                .selector(originalSelector)
                .description(description)
                .adapter(adapter)
                .build();
        String cacheKey = generateCacheKey(tempRequest);
        return selectorCache.remove(cacheKey);
    }

    /**
     * Get the current number of cached selectors
     *
     * @return Number of entries in the cache
     */
    public long getCacheSize() {
        return selectorCache.size();
    }

    /**
     * Get cache performance metrics
     *
     * @return Current cache metrics including hit rate, miss rate, etc.
     */
    public CacheMetrics getCacheMetrics() {
        return selectorCache.getMetrics();
    }

    /**
     * Manually clean up expired cache entries
     * This is automatically done periodically, but can be called manually
     */
    public void cleanupExpiredCache() {
        selectorCache.evictExpired();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get comprehensive metrics for monitoring
     */
    public AutoHealMetrics getMetrics() {
        return AutoHealMetrics.builder()
                .locatorMetrics(metrics)
                .cacheMetrics(selectorCache.getMetrics())
                .build();
    }

    /**
     * Health check for monitoring systems
     */
    public HealthStatus getHealthStatus() {
        double successRate = metrics.getSuccessRate();
        double cacheHitRate = selectorCache.getMetrics().getHitRate();

        return HealthStatus.builder()
                .overall(successRate > 0.8)
                .successRate(successRate)
                .cacheHitRate(cacheHitRate)
                .build();
    }


    /**
     * Generate reports if reporting is enabled
     */
    public void generateReports() {
        if (reporter != null) {
            logger.info("Generating AutoHeal reports...");
            if (configuration.getReportingConfig().isGenerateHTML()) {
                reporter.generateHTMLReport();
            }
            if (configuration.getReportingConfig().isGenerateJSON()) {
                reporter.generateJSONReport();
            }
            if (configuration.getReportingConfig().isGenerateText()) {
                reporter.generateTextReport();
            }
            if (configuration.getReportingConfig().isConsoleLogging()) {
                reporter.printSummary();
            }
        }
    }

    /**
     * Get reporter instance for advanced reporting operations
     */
    public AutoHealReporter getReporter() {
        return reporter;
    }

    /**
     * Graceful shutdown with optional report generation
     */
    public void shutdown() {
        if (reporter != null) {
            generateReports();
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("AutoHealLocator shutdown completed");
    }


    /**
     * Map LocatorStrategy to SelectorStrategy for reporting
     */
    private SelectorStrategy mapLocatorStrategyToSelectorStrategy(LocatorStrategy locatorStrategy) {
        if (locatorStrategy == null) {
            return SelectorStrategy.FAILED;
        }

        switch (locatorStrategy) {
            case ORIGINAL_SELECTOR:
                return SelectorStrategy.ORIGINAL_SELECTOR;
            case CACHED:
                return SelectorStrategy.CACHED;
            case DOM_ANALYSIS:
                return SelectorStrategy.DOM_ANALYSIS;
            case VISUAL_ANALYSIS:
                return SelectorStrategy.VISUAL_ANALYSIS;
            default:
                return SelectorStrategy.FAILED;
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Create cache instance based on configuration
     */
    private static SelectorCache createCacheBasedOnConfig(com.autoheal.config.CacheConfig cacheConfig) {
        switch (cacheConfig.getCacheType()) {
            case PERSISTENT_FILE:
                try {
                    return new PersistentFileSelectorCache(cacheConfig);
                } catch (Exception e) {
                    System.err.println("[FILE-CACHE-ERROR] Failed to initialize file cache: " + e.getMessage());
                    System.err.println("[FILE-CACHE-ERROR] Falling back to Caffeine cache");
                    return new CaffeineBasedSelectorCache(cacheConfig);
                }
            case REDIS:
                try {
                    return new RedisBasedSelectorCache(
                            cacheConfig,
                            cacheConfig.getRedisHost(),
                            cacheConfig.getRedisPort(),
                            cacheConfig.getRedisPassword()
                    );
                } catch (Exception e) {
                    System.err.println("[REDIS-ERROR] Failed to initialize Redis cache: " + e.getMessage());
                    System.err.println("[REDIS-ERROR] Falling back to Caffeine cache");
                    return new CaffeineBasedSelectorCache(cacheConfig);
                }
            case HYBRID:
                // TODO: Implement hybrid cache (Caffeine L1 + Redis L2)
                System.out.println("[CACHE-INFO] Hybrid cache not yet implemented, using Caffeine");
                return new CaffeineBasedSelectorCache(cacheConfig);
            case CAFFEINE:
            default:
                return new CaffeineBasedSelectorCache(cacheConfig);
        }
    }

    // ==================== BACKWARD COMPATIBILITY ====================

    /**
     * Legacy constructor for backward compatibility
     */
    public AutoHealLocator(WebDriver driver) {
        this(new SeleniumWebAutomationAdapter(driver),
                AutoHealConfiguration.builder().build(),
                new CaffeineBasedSelectorCache(com.autoheal.config.CacheConfig.defaultConfig()),
                new ResilientAIService(com.autoheal.config.AIConfig.fromProperties(),
                        com.autoheal.config.ResilienceConfig.defaultConfig()));
    }
}