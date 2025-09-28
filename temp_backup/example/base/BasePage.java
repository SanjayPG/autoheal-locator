package com.example.base;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.impl.adapter.PlaywrightElementWrapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.options.WaitForSelectorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced BasePage with AutoHeal integration for Playwright
 */
public abstract class BasePage {
    private static final Logger logger = LoggerFactory.getLogger(BasePage.class);
    
    protected final Page page;
    protected final AutoHealLocator autoHeal;
    protected final PlaywrightWebAutomationAdapter adapter;
    private static final int DEFAULT_TIMEOUT = 10000;
    
    public BasePage(Page page) {
        this.page = page;
        this.adapter = new PlaywrightWebAutomationAdapter(page);
        
        // Initialize AutoHeal with optimized configuration
        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .enableCaching(true)
            .enableRetries(true)
            .maxRetries(3)
            .elementTimeout(Duration.ofSeconds(10))
            .build();
            
        this.autoHeal = AutoHealLocator.builder()
            .withWebAdapter(adapter)
            .withConfiguration(config)
            .build();
            
        logger.info("BasePage initialized with AutoHeal for page: {}", page.url());
    }
    
    // ==================== ENHANCED ELEMENT INTERACTION METHODS ====================
    
    /**
     * Find element with AutoHeal capabilities
     */
    protected PlaywrightElementWrapper.WrappedElement findElementWithHealing(String selector, String description) {
        try {
            Object element = autoHeal.findElement(selector, description);
            if (element instanceof PlaywrightElementWrapper.WrappedElement) {
                return (PlaywrightElementWrapper.WrappedElement) element;
            }
            throw new RuntimeException("Unexpected element type returned from AutoHeal");
        } catch (Exception e) {
            logger.error("AutoHeal failed to find element '{}' with selector '{}': {}", 
                description, selector, e.getMessage());
            // Fallback to standard Playwright
            return findElementFallback(selector);
        }
    }
    
    /**
     * Fallback method using standard Playwright
     */
    private PlaywrightElementWrapper.WrappedElement findElementFallback(String selector) {
        try {
            ElementHandle element = page.waitForSelector(selector, 
                new WaitForSelectorOptions().setTimeout(DEFAULT_TIMEOUT));
            if (element != null) {
                return adapter.getElementWrapper().wrapElement(element);
            }
        } catch (Exception e) {
            logger.error("Fallback also failed for selector '{}': {}", selector, e.getMessage());
        }
        throw new RuntimeException("Element not found with selector: " + selector);
    }
    
    /**
     * Click element with AutoHeal
     */
    protected void click(String selector, String description) {
        PlaywrightElementWrapper.WrappedElement element = findElementWithHealing(selector, description);
        element.click();
        logger.debug("Clicked on element: {}", description);
    }
    
    /**
     * Click element using standard selector (for backward compatibility)
     */
    protected void click(String selector) {
        click(selector, "Clickable element with selector: " + selector);
    }
    
    /**
     * Type text with AutoHeal
     */
    protected void type(String selector, String text, String description) {
        PlaywrightElementWrapper.WrappedElement element = findElementWithHealing(selector, description);
        element.clear();
        element.sendKeys(text);
        logger.debug("Typed '{}' into element: {}", text, description);
    }
    
    /**
     * Type text using standard selector (for backward compatibility)
     */
    protected void type(String selector, String text) {
        type(selector, text, "Input field with selector: " + selector);
    }
    
    /**
     * Get text with AutoHeal
     */
    protected String getText(String selector, String description) {
        PlaywrightElementWrapper.WrappedElement element = findElementWithHealing(selector, description);
        String text = element.getText();
        logger.debug("Retrieved text '{}' from element: {}", text, description);
        return text;
    }
    
    /**
     * Get text using standard selector (for backward compatibility)
     */
    protected String getText(String selector) {
        return getText(selector, "Text element with selector: " + selector);
    }
    
    /**
     * Check if element is visible with AutoHeal
     */
    protected boolean isVisible(String selector, String description) {
        try {
            PlaywrightElementWrapper.WrappedElement element = findElementWithHealing(selector, description);
            boolean isVisible = element.isDisplayed();
            logger.debug("Element '{}' visibility: {}", description, isVisible);
            return isVisible;
        } catch (Exception e) {
            logger.debug("Element '{}' not found or not visible: {}", description, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if element is visible using standard selector (for backward compatibility)
     */
    protected boolean isVisible(String selector) {
        return isVisible(selector, "Element with selector: " + selector);
    }
    
    /**
     * Wait for element with AutoHeal
     */
    protected PlaywrightElementWrapper.WrappedElement waitForElement(String selector, String description) {
        PlaywrightElementWrapper.WrappedElement element = findElementWithHealing(selector, description);
        element.waitFor();
        logger.debug("Successfully waited for element: {}", description);
        return element;
    }
    
    /**
     * Wait for element using standard selector (for backward compatibility)
     */
    protected PlaywrightElementWrapper.WrappedElement waitForElement(String selector) {
        return waitForElement(selector, "Element with selector: " + selector);
    }
    
    /**
     * Get attribute value with AutoHeal
     */
    protected String getAttribute(String selector, String attributeName, String description) {
        PlaywrightElementWrapper.WrappedElement element = findElementWithHealing(selector, description);
        String value = element.getAttribute(attributeName);
        logger.debug("Retrieved attribute '{}' = '{}' from element: {}", attributeName, value, description);
        return value;
    }
    
    /**
     * Check if element is enabled with AutoHeal
     */
    protected boolean isEnabled(String selector, String description) {
        try {
            PlaywrightElementWrapper.WrappedElement element = findElementWithHealing(selector, description);
            boolean isEnabled = element.isEnabled();
            logger.debug("Element '{}' enabled: {}", description, isEnabled);
            return isEnabled;
        } catch (Exception e) {
            logger.debug("Element '{}' not found or not enabled: {}", description, e.getMessage());
            return false;
        }
    }
    
    // ==================== NAVIGATION METHODS ====================
    
    /**
     * Navigate to URL
     */
    protected void navigateTo(String url) {
        page.navigate(url);
        logger.info("Navigated to: {}", url);
    }
    
    /**
     * Get current URL
     */
    protected String getCurrentUrl() {
        return page.url();
    }
    
    /**
     * Get page title
     */
    protected String getTitle() {
        return page.title();
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Take screenshot for debugging
     */
    protected byte[] takeScreenshot() {
        try {
            return page.screenshot();
        } catch (Exception e) {
            logger.error("Failed to take screenshot: {}", e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Wait for page load
     */
    protected void waitForPageLoad() {
        page.waitForLoadState();
        logger.debug("Page load completed for: {}", page.url());
    }
    
    /**
     * Execute JavaScript
     */
    protected Object executeScript(String script) {
        return page.evaluate(script);
    }
    
    /**
     * Refresh page
     */
    protected void refresh() {
        page.reload();
        logger.debug("Page refreshed");
    }
    
    // ==================== AUTOHEAL SPECIFIC METHODS ====================
    
    /**
     * Get AutoHeal metrics for monitoring
     */
    public String getAutoHealMetrics() {
        try {
            var metrics = autoHeal.getMetrics();
            return String.format("AutoHeal Metrics - Success Rate: %.2f%%, Cache Hit Rate: %.2f%%",
                metrics.getLocatorMetrics().getSuccessRate() * 100,
                metrics.getCacheMetrics().getHitRate() * 100);
        } catch (Exception e) {
            logger.error("Failed to get AutoHeal metrics: {}", e.getMessage());
            return "AutoHeal metrics unavailable";
        }
    }
    
    /**
     * Clear AutoHeal cache (useful for testing)
     */
    public void clearAutoHealCache() {
        autoHeal.clearCache();
        logger.info("AutoHeal cache cleared");
    }
    
    /**
     * Check AutoHeal health status
     */
    public boolean isAutoHealHealthy() {
        try {
            return autoHeal.getHealthStatus().isOverall();
        } catch (Exception e) {
            logger.error("Failed to check AutoHeal health: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Shutdown AutoHeal resources
     */
    public void shutdown() {
        if (autoHeal != null) {
            autoHeal.shutdown();
            logger.info("AutoHeal resources shutdown completed");
        }
    }
}