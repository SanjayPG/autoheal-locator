package com.example.base;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.LocatorOptions;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.impl.adapter.PlaywrightElementWrapper;
import com.autoheal.model.LocatorStrategy;
import com.example.config.AutoHealStrategyConfig;
import com.example.config.AutoHealStrategyConfig.ElementPriority;
import com.example.config.AutoHealStrategyConfig.HealingStrategy;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Strategy-aware BasePage with multiple AutoHeal approaches
 */
public abstract class StrategyAwareBasePage {
    private static final Logger logger = LoggerFactory.getLogger(StrategyAwareBasePage.class);
    
    protected final Page page;
    private final AutoHealLocator autoHeal;
    private final AutoHealStrategyConfig defaultStrategy;
    private static final int QUICK_TIMEOUT = 2000;
    
    public StrategyAwareBasePage(Page page) {
        this(page, AutoHealStrategyConfig.defaultConfig());
    }
    
    public StrategyAwareBasePage(Page page, AutoHealStrategyConfig strategyConfig) {
        this.page = page;
        this.defaultStrategy = strategyConfig;
        
        // Initialize AutoHeal with strategy configuration
        PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .enableCaching(true)
            .enableRetries(true)
            .maxRetries(strategyConfig.getMaxRetries())
            .elementTimeout(java.time.Duration.ofMillis(strategyConfig.getTimeoutMs()))
            .build();
            
        this.autoHeal = AutoHealLocator.builder()
            .withWebAdapter(adapter)
            .withConfiguration(config)
            .build();
            
        logger.info("Initialized StrategyAwareBasePage with: {}", strategyConfig);
    }
    
    // ==================== STRATEGY-BASED ELEMENT INTERACTIONS ====================
    
    /**
     * Click with default strategy
     */
    protected void click(String selector, String description) {
        click(selector, description, defaultStrategy);
    }
    
    /**
     * Click with specific strategy
     */
    protected void click(String selector, String description, AutoHealStrategyConfig strategy) {
        // STEP 1: Always try original locator first
        if (tryOriginalClick(selector)) {
            return;
        }
        
        // STEP 2: Apply AutoHeal strategy
        executeAutoHealStrategy(
            () -> performHealingClick(selector, description, strategy),
            selector, description, strategy, "click"
        );
    }
    
    /**
     * Click with element priority (automatic strategy selection)
     */
    protected void click(String selector, String description, ElementPriority priority) {
        click(selector, description, AutoHealStrategyConfig.forPriority(priority));
    }
    
    /**
     * Type with strategy support
     */
    protected void type(String selector, String text, String description, AutoHealStrategyConfig strategy) {
        // STEP 1: Try original locator first
        if (tryOriginalType(selector, text)) {
            return;
        }
        
        // STEP 2: Apply AutoHeal strategy
        executeAutoHealStrategy(
            () -> performHealingType(selector, text, description, strategy),
            selector, description, strategy, "type"
        );
    }
    
    protected void type(String selector, String text, String description) {
        type(selector, text, description, defaultStrategy);
    }
    
    protected void type(String selector, String text, String description, ElementPriority priority) {
        type(selector, text, description, AutoHealStrategyConfig.forPriority(priority));
    }
    
    // ==================== STRATEGY EXECUTION ENGINE ====================
    
    private void executeAutoHealStrategy(Runnable healingAction, String selector, String description, 
                                       AutoHealStrategyConfig strategy, String action) {
        logger.info("🔄 AutoHeal {} activated: {} | Strategy: {} | Selector: {}", 
            action, description, strategy.getHealingStrategy(), selector);
        
        long startTime = System.currentTimeMillis();
        
        try {
            switch (strategy.getHealingStrategy()) {
                case DOM_ONLY -> executeDomOnlyStrategy(healingAction, selector, description);
                case VISUAL_ONLY -> executeVisualOnlyStrategy(healingAction, selector, description);
                case HYBRID_SEQUENTIAL -> executeHybridSequentialStrategy(healingAction, selector, description);
                case HYBRID_PARALLEL -> executeHybridParallelStrategy(healingAction, selector, description);
                case COST_OPTIMIZED -> executeCostOptimizedStrategy(healingAction, selector, description);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ AutoHeal {} successful: {} | Duration: {}ms | Strategy: {}", 
                action, description, duration, strategy.getHealingStrategy());
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("❌ AutoHeal {} failed: {} | Duration: {}ms | Strategy: {} | Error: {}", 
                action, description, duration, strategy.getHealingStrategy(), e.getMessage());
            throw new RuntimeException(
                String.format("AutoHeal %s failed for '%s' using strategy %s: %s", 
                    action, description, strategy.getHealingStrategy(), e.getMessage()));
        }
    }
    
    // ==================== INDIVIDUAL STRATEGY IMPLEMENTATIONS ====================
    
    private void executeDomOnlyStrategy(Runnable healingAction, String selector, String description) {
        logger.debug("🔍 Executing DOM-only strategy for: {}", description);
        // Configure for DOM analysis only
        healingAction.run();
    }
    
    private void executeVisualOnlyStrategy(Runnable healingAction, String selector, String description) {
        logger.debug("👁️ Executing Visual-only strategy for: {}", description);
        // Configure for visual analysis only
        healingAction.run();
    }
    
    private void executeHybridSequentialStrategy(Runnable healingAction, String selector, String description) {
        logger.debug("🔄 Executing Hybrid Sequential strategy for: {}", description);
        
        // Try DOM first
        try {
            logger.debug("  → Step 1: Trying DOM analysis");
            executeDomOnlyStrategy(healingAction, selector, description);
            logger.debug("  → DOM analysis successful");
            return;
        } catch (Exception e) {
            logger.debug("  → DOM analysis failed: {}", e.getMessage());
        }
        
        // Fallback to Visual
        try {
            logger.debug("  → Step 2: Trying Visual analysis");
            executeVisualOnlyStrategy(healingAction, selector, description);
            logger.debug("  → Visual analysis successful");
        } catch (Exception e) {
            logger.debug("  → Visual analysis also failed: {}", e.getMessage());
            throw e;
        }
    }
    
    private void executeHybridParallelStrategy(Runnable healingAction, String selector, String description) {
        logger.debug("⚡ Executing Hybrid Parallel strategy for: {}", description);
        
        // Execute DOM and Visual analysis in parallel
        CompletableFuture<Void> domFuture = CompletableFuture.runAsync(() -> {
            try {
                logger.debug("  → Parallel: Starting DOM analysis");
                executeDomOnlyStrategy(healingAction, selector, description);
                logger.debug("  → Parallel: DOM analysis completed");
            } catch (Exception e) {
                logger.debug("  → Parallel: DOM analysis failed: {}", e.getMessage());
                throw new RuntimeException("DOM analysis failed", e);
            }
        });
        
        CompletableFuture<Void> visualFuture = CompletableFuture.runAsync(() -> {
            try {
                logger.debug("  → Parallel: Starting Visual analysis");
                executeVisualOnlyStrategy(healingAction, selector, description);
                logger.debug("  → Parallel: Visual analysis completed");
            } catch (Exception e) {
                logger.debug("  → Parallel: Visual analysis failed: {}", e.getMessage());
                throw new RuntimeException("Visual analysis failed", e);
            }
        });
        
        // Wait for first successful completion or both to fail
        try {
            CompletableFuture.anyOf(domFuture, visualFuture)
                .get(defaultStrategy.getTimeoutMs(), TimeUnit.MILLISECONDS);
            logger.debug("  → Parallel: First strategy completed successfully");
        } catch (Exception e) {
            logger.debug("  → Parallel: All strategies failed: {}", e.getMessage());
            throw new RuntimeException("All parallel strategies failed", e);
        }
    }
    
    private void executeCostOptimizedStrategy(Runnable healingAction, String selector, String description) {
        logger.debug("💰 Executing Cost-Optimized strategy for: {}", description);
        
        // Intelligent strategy selection based on element type and context
        if (isFastElement(selector, description)) {
            logger.debug("  → Cost optimization: Using DOM-only for fast element");
            executeDomOnlyStrategy(healingAction, selector, description);
        } else if (isComplexElement(selector, description)) {
            logger.debug("  → Cost optimization: Using Visual analysis for complex element");
            executeVisualOnlyStrategy(healingAction, selector, description);
        } else {
            logger.debug("  → Cost optimization: Using Sequential hybrid for standard element");
            executeHybridSequentialStrategy(healingAction, selector, description);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private boolean tryOriginalClick(String selector) {
        try {
            page.click(selector, new Page.ClickOptions().setTimeout(QUICK_TIMEOUT));
            logger.debug("✅ Original click worked: {}", selector);
            return true;
        } catch (TimeoutError | PlaywrightException e) {
            logger.debug("❌ Original click failed: {} - {}", selector, e.getMessage());
            return false;
        }
    }
    
    private boolean tryOriginalType(String selector, String text) {
        try {
            page.fill(selector, text, new Page.FillOptions().setTimeout(QUICK_TIMEOUT));
            logger.debug("✅ Original type worked: {}", selector);
            return true;
        } catch (TimeoutError | PlaywrightException e) {
            logger.debug("❌ Original type failed: {} - {}", selector, e.getMessage());
            return false;
        }
    }
    
    private void performHealingClick(String selector, String description, AutoHealStrategyConfig strategy) {
        LocatorOptions options = createLocatorOptions(strategy);
        Object element = autoHeal.findElement(selector, description);
        if (element instanceof PlaywrightElementWrapper.WrappedElement) {
            ((PlaywrightElementWrapper.WrappedElement) element).click();
        }
    }
    
    private void performHealingType(String selector, String text, String description, AutoHealStrategyConfig strategy) {
        LocatorOptions options = createLocatorOptions(strategy);
        Object element = autoHeal.findElement(selector, description);
        if (element instanceof PlaywrightElementWrapper.WrappedElement) {
            PlaywrightElementWrapper.WrappedElement wrappedElement = 
                (PlaywrightElementWrapper.WrappedElement) element;
            wrappedElement.clear();
            wrappedElement.sendKeys(text);
        }
    }
    
    private LocatorOptions createLocatorOptions(AutoHealStrategyConfig strategy) {
        return LocatorOptions.builder()
            .enableCaching(true)
            .enableRetries(strategy.getMaxRetries() > 1)
            .maxRetries(strategy.getMaxRetries())
            .timeout(java.time.Duration.ofMillis(strategy.getTimeoutMs()))
            .preferredStrategies(strategy.getPreferredStrategies())
            .enableParallelExecution(strategy.isParallelExecutionEnabled())
            .build();
    }
    
    private boolean isFastElement(String selector, String description) {
        // Simple heuristics for fast elements
        return selector.startsWith("#") || // ID selectors are usually fast
               description.toLowerCase().contains("button") ||
               description.toLowerCase().contains("link");
    }
    
    private boolean isComplexElement(String selector, String description) {
        // Heuristics for complex elements that might need visual analysis
        return description.toLowerCase().contains("dynamic") ||
               description.toLowerCase().contains("generated") ||
               description.toLowerCase().contains("image") ||
               selector.contains(":nth-child") ||
               selector.length() > 50; // Complex selector
    }
    
    // ==================== CONVENIENCE METHODS ====================
    
    /**
     * Critical element - use maximum reliability
     */
    protected void clickCritical(String selector, String description) {
        click(selector, description, ElementPriority.CRITICAL);
    }
    
    /**
     * Fast element - DOM only for speed
     */
    protected void clickFast(String selector, String description) {
        click(selector, description, ElementPriority.LOW);
    }
    
    /**
     * Backward compatibility
     */
    protected void click(String selector) {
        click(selector, "Element with selector: " + selector);
    }
    
    protected void type(String selector, String text) {
        type(selector, text, "Input field with selector: " + selector);
    }
    
    // ==================== CONFIGURATION AND UTILITIES ====================
    
    public void setDefaultStrategy(AutoHealStrategyConfig strategy) {
        logger.info("Updated default AutoHeal strategy: {}", strategy);
    }
    
    public AutoHealStrategyConfig getDefaultStrategy() {
        return defaultStrategy;
    }
    
    public String getAutoHealMetrics() {
        try {
            var metrics = autoHeal.getMetrics();
            return String.format("AutoHeal Metrics - Success: %.1f%%, Cache: %.1f%%, Strategy: %s",
                metrics.getLocatorMetrics().getSuccessRate() * 100,
                metrics.getCacheMetrics().getHitRate() * 100,
                defaultStrategy.getHealingStrategy());
        } catch (Exception e) {
            return "AutoHeal metrics unavailable";
        }
    }
}