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

    // ==================== PLAYWRIGHT AUTO-HEAL API ====================

    /**
     * Find element with Playwright auto-healing
     * This method returns a native Playwright Locator with self-healing capabilities
     *
     * @param page Playwright Page object
     * @param originalLocator Original Playwright locator string (e.g., "getByLabel('Username')")
     * @param description Human-readable description for AI healing
     * @return Native Playwright Locator with auto-healing
     */
    public com.microsoft.playwright.Locator find(com.microsoft.playwright.Page page,
                                                   String originalLocator,
                                                   String description) {
        // When user provides JS-style string directly, both formats are the same
        return findInternal(page, originalLocator, originalLocator, description);
    }

    /**
     * Internal find method that separates display format from processing format
     *
     * @param page Playwright Page object
     * @param displayLocator Locator string for display in reports (Java syntax)
     * @param processingLocator Locator string for internal processing (JS syntax)
     * @param description Human-readable description for AI healing
     * @return Native Playwright Locator with auto-healing
     */
    private com.microsoft.playwright.Locator findInternal(com.microsoft.playwright.Page page,
                                                           String displayLocator,
                                                           String processingLocator,
                                                           String description) {
        if (!(adapter instanceof com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter)) {
            throw new IllegalStateException(
                    "find() method requires PlaywrightWebAutomationAdapter. Current adapter: " +
                            adapter.getClass().getSimpleName());
        }

        com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter pwAdapter =
                (com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter) adapter;

        long startTime = System.currentTimeMillis();
        SelectorStrategy strategy = SelectorStrategy.FAILED;
        boolean success = false;
        String actualSelector = displayLocator;
        String reasoning = null;
        long tokensUsed = 0;

        try {
            // Step 1: Try original locator first (use processingLocator for internal logic)
            com.autoheal.model.PlaywrightLocator parsedOriginal =
                    com.autoheal.util.PlaywrightLocatorParser.parse(processingLocator);
            com.microsoft.playwright.Locator originalLoc = pwAdapter.executePlaywrightLocator(parsedOriginal);

            int count = pwAdapter.countElements(originalLoc);
            if (count == 1) {
                // Playwright strict mode requires exactly 1 element
                logger.debug("Original Playwright locator worked: {}", processingLocator);

                // SUCCESS with original selector
                long duration = System.currentTimeMillis() - startTime;
                strategy = SelectorStrategy.ORIGINAL_SELECTOR;
                success = true;
                reasoning = "Original selector worked";

                // Convert to Java syntax for reporting
                actualSelector = parsedOriginal.toSelectorString();

                // Log to report (use displayLocator for "original" field)
                if (reporter != null) {
                    reporter.recordSelectorUsage(displayLocator, description, strategy,
                        duration, success, actualSelector, "playwright-locator", reasoning, 0);
                }

                // Cache the successful locator (use processingLocator for cache key)
                cachePlaywrightLocator(processingLocator, description, parsedOriginal);
                return originalLoc;
            } else if (count > 1) {
                logger.debug("Original Playwright locator is ambiguous (found {} elements), needs AI disambiguation: {}",
                        count, processingLocator);
            } else {
                logger.debug("Original Playwright locator found no elements: {}", processingLocator);
            }

            logger.debug("Original Playwright locator failed, trying cache and healing: {}", processingLocator);

            // Step 2: Check cache (use processingLocator for cache key)
            String cacheKey = com.autoheal.util.PlaywrightLocatorParser.generateCacheKey(
                    processingLocator, description);
            Optional<com.autoheal.model.CachedSelector> cached = selectorCache.get(cacheKey);

            if (cached.isPresent()) {
                String cachedLocatorStr = cached.get().getSelector();
                logger.debug("Found cached Playwright locator: {}", cachedLocatorStr);

                com.autoheal.model.PlaywrightLocator cachedLocator =
                        com.autoheal.util.PlaywrightLocatorParser.parse(cachedLocatorStr);
                com.microsoft.playwright.Locator cachedLoc = pwAdapter.executePlaywrightLocator(cachedLocator);

                int cachedCount = pwAdapter.countElements(cachedLoc);
                if (cachedCount == 1) {
                    selectorCache.updateSuccess(cacheKey, true);
                    logger.info("Cache hit for Playwright locator: {} -> {}", processingLocator, cachedLocatorStr);

                    // SUCCESS with cached selector
                    long duration = System.currentTimeMillis() - startTime;
                    strategy = SelectorStrategy.CACHED;
                    success = true;
                    // Convert to Java syntax for reporting
                    actualSelector = cachedLocator.toSelectorString();
                    reasoning = "Retrieved from cache";

                    // Log to report (use displayLocator for "original" field)
                    if (reporter != null) {
                        reporter.recordSelectorUsage(displayLocator, description, strategy,
                            duration, success, actualSelector, "playwright-locator", reasoning, 0);
                    }

                    return cachedLoc;
                } else {
                    selectorCache.updateSuccess(cacheKey, false);
                    logger.warn("Cached Playwright locator no longer works (found {} elements): {}", cachedCount, cachedLocatorStr);
                }
            }

            // Step 3: AI Healing (use processingLocator for AI)
            logger.info("Performing AI healing for Playwright locator: {}", processingLocator);
            PlaywrightHealingResult healingResult = performPlaywrightHealingWithStrategy(
                    page, processingLocator, description);

            com.microsoft.playwright.Locator healedLoc = pwAdapter.executePlaywrightLocator(healingResult.locator);

            int healedCount = pwAdapter.countElements(healedLoc);
            if (healedCount == 1) {
                logger.info("Successfully healed Playwright locator: {} -> {}",
                        processingLocator, healingResult.locator.toSelectorString());

                // SUCCESS with AI healing
                long duration = System.currentTimeMillis() - startTime;
                strategy = healingResult.strategyUsed;  // Actual strategy used (DOM or VISUAL)
                success = true;
                actualSelector = healingResult.locator.toSelectorString();
                reasoning = healingResult.reasoning;
                tokensUsed = healingResult.tokensUsed;

                // Log to report (use displayLocator for "original" field)
                if (reporter != null) {
                    reporter.recordSelectorUsage(displayLocator, description, strategy,
                        duration, success, actualSelector, "playwright-locator", reasoning, tokensUsed);
                }

                // Cache the healed locator (use processingLocator for cache key)
                cachePlaywrightLocator(processingLocator, description, healingResult.locator);
                return healedLoc;
            } else if (healedCount > 1) {
                logger.warn("Healed locator is still ambiguous (found {} elements): {}",
                        healedCount, healingResult.locator.toSelectorString());
            }

            throw new AutoHealException(ErrorCode.ELEMENT_NOT_FOUND,
                    "Could not find element even after healing: " + description);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Playwright find() failed after {}ms: {}", duration, e.getMessage());

            // FAILURE - Log to report (use displayLocator for "original" field)
            strategy = SelectorStrategy.FAILED;
            reasoning = "Failed: " + e.getMessage();

            if (reporter != null) {
                reporter.recordSelectorUsage(displayLocator, description, strategy,
                    duration, false, null, null, reasoning, 0);
            }

            throw new AutoHealException(ErrorCode.ELEMENT_NOT_FOUND,
                    "Failed to find Playwright element: " + description, e);
        }
    }

    /**
     * Find element with Playwright auto-healing using native Locator object (ZERO-REWRITE MIGRATION)
     *
     * This overloaded method enables seamless migration for existing Playwright projects by accepting
     * native Playwright Locator objects and automatically converting them to JavaScript-style strings
     * for AI healing.
     *
     * <p><strong>Zero-Rewrite Migration Example:</strong></p>
     * <pre>
     * // Keep your existing Playwright code (NO CHANGES):
     * Locator loginButton = page.getByRole(AriaRole.BUTTON,
     *     new Page.GetByRoleOptions().setName("Login"));
     *
     * // Wrap with AutoHeal for self-healing (ONE LINE):
     * Locator healedButton = autoHeal.find(page, loginButton, "Login button");
     *
     * // Use normally (NO CHANGES):
     * healedButton.click();  // Now has AI self-healing!
     * </pre>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>Works with most common Playwright locators: getByRole, getByPlaceholder, getByText, etc.</li>
     *   <li>Complex chained locators may fail extraction - use JavaScript string format for those</li>
     *   <li>Automatically converts to JavaScript-style format internally for AI healing</li>
     * </ul>
     *
     * @param page Playwright Page object (must not be null)
     * @param nativeLocator Native Playwright Locator object (e.g., page.getByRole(...))
     * @param description Human-readable description for AI healing (must not be null or empty)
     * @return Native Playwright Locator with auto-healing capabilities
     * @throws IllegalArgumentException if page, nativeLocator, or description is null/empty
     * @throws com.autoheal.exception.PlaywrightLocatorExtractionException if locator string cannot be extracted
     * @throws AutoHealException if element cannot be found even after healing
     * @since 1.0.0
     */
    public com.microsoft.playwright.Locator find(com.microsoft.playwright.Page page,
                                                   com.microsoft.playwright.Locator nativeLocator,
                                                   String description) {
        // Validation
        if (page == null) {
            throw new IllegalArgumentException("Page cannot be null");
        }
        if (nativeLocator == null) {
            throw new IllegalArgumentException(
                "Native Locator cannot be null. Please provide a valid Playwright Locator object.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Description cannot be null or empty. Provide a meaningful description for AI healing.");
        }

        try {
            // Extract JavaScript-style locator string from native Locator (for internal processing)
            String jsStyleLocator = com.autoheal.util.PlaywrightLocatorConverter.extractLocatorString(nativeLocator);

            logger.debug("Zero-rewrite migration: Extracted JS-style locator: {}", jsStyleLocator);

            // Convert to Java syntax for the report (what user wants to see)
            com.autoheal.model.PlaywrightLocator parsedLocator =
                com.autoheal.util.PlaywrightLocatorParser.parse(jsStyleLocator);
            String javaStyleLocator = parsedLocator.toSelectorString();

            logger.debug("Zero-rewrite migration: Java-style for report: {}", javaStyleLocator);

            // Now call the internal find method with BOTH:
            // - javaStyleLocator for "originalLocator" (shown in report)
            // - jsStyleLocator for internal processing
            return findInternal(page, javaStyleLocator, jsStyleLocator, description);

        } catch (com.autoheal.exception.PlaywrightLocatorExtractionException e) {
            // Re-throw with helpful context
            logger.error("Failed to extract locator from native Playwright Locator: {}", e.getMessage());
            throw new AutoHealException(
                ErrorCode.INVALID_LOCATOR,
                "Cannot use native Playwright Locator object: " + e.getMessage() + " " +
                "For complex locators, use JavaScript-style string format instead: " +
                "autoHeal.find(page, \"getByRole('button', { name: 'Text' })\", \"description\")",
                e
            );
        }
    }

    private void cachePlaywrightLocator(String originalLocator, String description,
                                        com.autoheal.model.PlaywrightLocator locator) {
        String cacheKey = com.autoheal.util.PlaywrightLocatorParser.generateCacheKey(
                originalLocator, description);

        // Store the JS-style locator for cache (not Java syntax)
        // Because when we retrieve from cache, we need to parse it, and parser expects JS format
        String jsStyleCachedLocator = convertToJSStyle(locator);

        com.autoheal.model.CachedSelector cached = new com.autoheal.model.CachedSelector(
                jsStyleCachedLocator,
                null  // ElementFingerprint not needed for Playwright caching
        );
        selectorCache.put(cacheKey, cached);
    }

    /**
     * Convert PlaywrightLocator to JavaScript-style string for cache storage
     */
    private String convertToJSStyle(com.autoheal.model.PlaywrightLocator locator) {
        return switch (locator.getType()) {
            case GET_BY_ROLE -> {
                String name = (String) locator.getOption("name");
                yield name != null
                    ? String.format("getByRole('%s', { name: '%s' })", locator.getValue(), name.replace("'", "\\'"))
                    : String.format("getByRole('%s')", locator.getValue());
            }
            case GET_BY_LABEL -> String.format("getByLabel('%s')", locator.getValue().replace("'", "\\'"));
            case GET_BY_PLACEHOLDER -> String.format("getByPlaceholder('%s')", locator.getValue().replace("'", "\\'"));
            case GET_BY_TEXT -> String.format("getByText('%s')", locator.getValue().replace("'", "\\'"));
            case GET_BY_ALT_TEXT -> String.format("getByAltText('%s')", locator.getValue().replace("'", "\\'"));
            case GET_BY_TITLE -> String.format("getByTitle('%s')", locator.getValue().replace("'", "\\'"));
            case GET_BY_TEST_ID -> String.format("getByTestId('%s')", locator.getValue().replace("'", "\\'"));
            case CSS_SELECTOR -> locator.getValue();
            case XPATH -> locator.getValue();
        };
    }

    /**
     * Wrapper class to track strategy used during Playwright healing
     */
    private static class PlaywrightHealingResult {
        final com.autoheal.model.PlaywrightLocator locator;
        final SelectorStrategy strategyUsed;
        final String reasoning;
        final long tokensUsed;

        PlaywrightHealingResult(com.autoheal.model.PlaywrightLocator locator,
                                SelectorStrategy strategyUsed,
                                String reasoning,
                                long tokensUsed) {
            this.locator = locator;
            this.strategyUsed = strategyUsed;
            this.reasoning = reasoning;
            this.tokensUsed = tokensUsed;
        }
    }

    private PlaywrightHealingResult performPlaywrightHealingWithStrategy(
            com.microsoft.playwright.Page page, String originalLocator, String description) {

        try {
            com.autoheal.model.ExecutionStrategy strategy = configuration.getPerformanceConfig().getExecutionStrategy();
            logger.debug("Playwright healing using {} strategy for: {}", strategy, description);

            return switch (strategy) {
                case DOM_ONLY -> performPlaywrightDOMHealing(page, originalLocator, description);
                case SMART_SEQUENTIAL -> performPlaywrightSmartSequential(page, originalLocator, description);
                case PARALLEL -> performPlaywrightParallel(page, originalLocator, description);
                case VISUAL_FIRST -> performPlaywrightVisualFirst(page, originalLocator, description);
                case SEQUENTIAL -> performPlaywrightSequential(page, originalLocator, description);
            };

        } catch (Exception e) {
            logger.error("Playwright healing failed: {}", e.getMessage());
            throw new RuntimeException("Playwright healing failed", e);
        }
    }

    /**
     * DOM-only healing strategy for Playwright (lowest cost)
     */
    private PlaywrightHealingResult performPlaywrightDOMHealing(
            com.microsoft.playwright.Page page, String originalLocator, String description) throws Exception {

        logger.debug("Playwright DOM-only strategy: Using only DOM analysis");
        String html = page.content();

        com.autoheal.model.AIAnalysisResult result = aiService.analyzeDOM(
                html,
                description,
                originalLocator,
                com.autoheal.model.AutomationFramework.PLAYWRIGHT
        ).get();

        com.autoheal.model.PlaywrightLocator locator = extractPlaywrightLocatorFromResult(result, description);
        return new PlaywrightHealingResult(
                locator,
                SelectorStrategy.DOM_ANALYSIS,
                "AI healed selector using DOM analysis",
                1500  // Estimate for DOM tokens
        );
    }

    /**
     * Smart Sequential: Try DOM first (cheaper), then Visual if DOM fails
     */
    private PlaywrightHealingResult performPlaywrightSmartSequential(
            com.microsoft.playwright.Page page, String originalLocator, String description) throws Exception {

        logger.debug("Playwright Smart Sequential: Trying DOM first (cost-effective)");

        try {
            // Try DOM first (cheaper)
            PlaywrightHealingResult domResult = performPlaywrightDOMHealing(page, originalLocator, description);

            // Validate DOM result works
            com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter pwAdapter =
                (com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter) adapter;
            com.microsoft.playwright.Locator testLoc = pwAdapter.executePlaywrightLocator(domResult.locator);

            if (pwAdapter.countElements(testLoc) == 1) {
                logger.info("Smart Sequential: DOM succeeded, skipping visual (cost saved!)");
                return domResult;
            } else {
                logger.debug("Smart Sequential: DOM failed, trying visual analysis");
                return performPlaywrightVisualHealing(page, originalLocator, description);
            }
        } catch (Exception e) {
            logger.debug("Smart Sequential: DOM failed with exception, trying visual analysis");
            return performPlaywrightVisualHealing(page, originalLocator, description);
        }
    }

    /**
     * Parallel strategy: Run DOM + Visual in parallel, select best result
     */
    private PlaywrightHealingResult performPlaywrightParallel(
            com.microsoft.playwright.Page page, String originalLocator, String description) throws Exception {

        logger.debug("Playwright Parallel: Running DOM + Visual in parallel");

        CompletableFuture<PlaywrightHealingResult> domFuture =
            CompletableFuture.supplyAsync(() -> {
                try {
                    return performPlaywrightDOMHealing(page, originalLocator, description);
                } catch (Exception e) {
                    logger.debug("Parallel DOM failed: {}", e.getMessage());
                    return null;
                }
            });

        CompletableFuture<PlaywrightHealingResult> visualFuture =
            CompletableFuture.supplyAsync(() -> {
                try {
                    return performPlaywrightVisualHealing(page, originalLocator, description);
                } catch (Exception e) {
                    logger.debug("Parallel Visual failed: {}", e.getMessage());
                    return null;
                }
            });

        // Wait for both to complete
        CompletableFuture.allOf(domFuture, visualFuture).get();

        PlaywrightHealingResult domResult = domFuture.get();
        PlaywrightHealingResult visualResult = visualFuture.get();

        com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter pwAdapter =
            (com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter) adapter;

        // Test both results and pick the best one
        if (domResult != null) {
            com.microsoft.playwright.Locator domLoc = pwAdapter.executePlaywrightLocator(domResult.locator);
            if (pwAdapter.countElements(domLoc) == 1) {
                logger.info("Parallel: DOM result succeeded");
                return domResult;
            }
        }

        if (visualResult != null) {
            com.microsoft.playwright.Locator visualLoc = pwAdapter.executePlaywrightLocator(visualResult.locator);
            if (pwAdapter.countElements(visualLoc) == 1) {
                logger.info("Parallel: Visual result succeeded");
                return visualResult;
            }
        }

        throw new RuntimeException("Parallel strategy: Both DOM and Visual failed");
    }

    /**
     * Visual-first strategy: Try Visual first, then DOM if Visual fails
     */
    private PlaywrightHealingResult performPlaywrightVisualFirst(
            com.microsoft.playwright.Page page, String originalLocator, String description) throws Exception {

        logger.debug("Playwright Visual-first: Trying visual analysis first");

        try {
            PlaywrightHealingResult visualResult = performPlaywrightVisualHealing(page, originalLocator, description);

            // Validate Visual result works
            com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter pwAdapter =
                (com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter) adapter;
            com.microsoft.playwright.Locator testLoc = pwAdapter.executePlaywrightLocator(visualResult.locator);

            if (pwAdapter.countElements(testLoc) == 1) {
                logger.info("Visual-first: Visual succeeded");
                return visualResult;
            } else {
                logger.debug("Visual-first: Visual failed, trying DOM analysis");
                return performPlaywrightDOMHealing(page, originalLocator, description);
            }
        } catch (Exception e) {
            logger.debug("Visual-first: Visual failed with exception, trying DOM analysis");
            return performPlaywrightDOMHealing(page, originalLocator, description);
        }
    }

    /**
     * Sequential strategy: Try DOM, then Visual
     */
    private PlaywrightHealingResult performPlaywrightSequential(
            com.microsoft.playwright.Page page, String originalLocator, String description) throws Exception {

        logger.debug("Playwright Sequential: Trying DOM first, then Visual");

        try {
            return performPlaywrightDOMHealing(page, originalLocator, description);
        } catch (Exception e) {
            logger.debug("Sequential: DOM failed, trying Visual");
            return performPlaywrightVisualHealing(page, originalLocator, description);
        }
    }

    /**
     * Visual healing strategy for Playwright (higher cost, uses screenshot)
     */
    private PlaywrightHealingResult performPlaywrightVisualHealing(
            com.microsoft.playwright.Page page, String originalLocator, String description) throws Exception {

        logger.debug("Playwright Visual analysis: Taking screenshot and analyzing");
        byte[] screenshot = page.screenshot();

        com.autoheal.model.AIAnalysisResult result = aiService.analyzeVisual(
                screenshot,
                description
        ).get();

        com.autoheal.model.PlaywrightLocator locator = extractPlaywrightLocatorFromResult(result, description);
        return new PlaywrightHealingResult(
                locator,
                SelectorStrategy.VISUAL_ANALYSIS,
                "AI healed selector using visual/screenshot analysis",
                45000  // Estimate for visual analysis tokens (much higher)
        );
    }

    /**
     * Extract PlaywrightLocator from AI result with fallbacks
     */
    private com.autoheal.model.PlaywrightLocator extractPlaywrightLocatorFromResult(
            com.autoheal.model.AIAnalysisResult result, String description) {

        // First choice: AI returned Playwright locator directly
        if (result.getPlaywrightLocator() != null) {
            logger.info("AI returned Playwright locator: {}", result.getPlaywrightLocator().toSelectorString());
            return result.getPlaywrightLocator();
        }

        // Second choice: AI returned CSS selector, wrap it
        if (result.getRecommendedSelector() != null && !result.getRecommendedSelector().isEmpty()) {
            logger.info("AI returned CSS selector, wrapping as Playwright CSS locator: {}", result.getRecommendedSelector());
            return com.autoheal.model.PlaywrightLocator.builder()
                    .byCss(result.getRecommendedSelector())
                    .build();
        }

        // Final fallback
        logger.warn("AI healing did not return valid locator, using fallback CSS selector");
        return com.autoheal.model.PlaywrightLocator.builder()
                .byCss(generateFallbackCssSelector(description))
                .build();
    }

    private String buildPlaywrightPrompt(String html, String description, String previousLocator) {
        // This will be integrated with ResilientAIService later
        return String.format(
                "Find Playwright locator for '%s'. Previous locator '%s' failed. HTML: %s",
                description, previousLocator, html.substring(0, Math.min(1000, html.length())));
    }

    private String generateFallbackCssSelector(String description) {
        // Simple fallback - in reality this would use AI
        return "*[data-testid*='" + description.replaceAll("\\s+", "-").toLowerCase() + "']";
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