package com.example.pages;

import com.example.base.StrategyAwareBasePage;
import com.example.config.AutoHealStrategyConfig;
import com.example.config.AutoHealStrategyConfig.ElementPriority;
import com.example.config.AutoHealStrategyConfig.HealingStrategy;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginPage demonstrating different AutoHeal strategies
 */
public class StrategyAwareLoginPage extends StrategyAwareBasePage {
    private static final Logger logger = LoggerFactory.getLogger(StrategyAwareLoginPage.class);

    // Selectors
    private static final String USERNAME_FIELD = "#user-name";
    private static final String PASSWORD_FIELD = "#password";
    private static final String LOGIN_BUTTON = "#login-button";
    private static final String ERROR_MESSAGE = "[data-test='error']";
    private static final String LOGO = ".login_logo";

    // Descriptions
    private static final String USERNAME_DESCRIPTION = "Username input field for login";
    private static final String PASSWORD_DESCRIPTION = "Password input field for login";
    private static final String LOGIN_BUTTON_DESCRIPTION = "Login submit button";
    private static final String ERROR_MESSAGE_DESCRIPTION = "Login error message container";
    private static final String LOGO_DESCRIPTION = "SauceDemo application logo";

    public StrategyAwareLoginPage(Page page) {
        // Use default strategy (HYBRID_SEQUENTIAL with NORMAL priority)
        super(page);
        logger.info("StrategyAwareLoginPage initialized with default strategy");
    }
    
    public StrategyAwareLoginPage(Page page, AutoHealStrategyConfig strategy) {
        super(page, strategy);
        logger.info("StrategyAwareLoginPage initialized with custom strategy: {}", strategy);
    }
    
    // ==================== DIFFERENT STRATEGY EXAMPLES ====================
    
    /**
     * Navigate to login page
     */
    public StrategyAwareLoginPage goToLoginPage(String baseUrl) {
        page.navigate(baseUrl);
        page.waitForLoadState();
        logger.info("Navigated to login page: {}", baseUrl);
        return this;
    }
    
    /**
     * FAST STRATEGY: Username entry (DOM only - fastest)
     * Use for simple, stable elements like input fields
     */
    public StrategyAwareLoginPage enterUsername(String username) {
        logger.info("Entering username with FAST strategy (DOM only)");
        // Use LOW priority = DOM_ONLY strategy for speed
        type(USERNAME_FIELD, username, USERNAME_DESCRIPTION, ElementPriority.LOW);
        return this;
    }
    
    /**
     * NORMAL STRATEGY: Password entry (Hybrid Sequential)
     * Use for moderately important elements
     */
    public StrategyAwareLoginPage enterPassword(String password) {
        logger.info("Entering password with NORMAL strategy (Hybrid Sequential)");
        // Use NORMAL priority = DOM first, then Visual if needed
        type(PASSWORD_FIELD, password, PASSWORD_DESCRIPTION, ElementPriority.NORMAL);
        return this;
    }
    
    /**
     * CRITICAL STRATEGY: Login button (Parallel Hybrid - maximum reliability)
     * Use for crucial elements where failure is not acceptable
     */
    public void clickLoginButton() {
        logger.info("Clicking login button with CRITICAL strategy (Parallel Hybrid)");
        // Use CRITICAL priority = Parallel DOM + Visual for maximum reliability
        click(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION, ElementPriority.CRITICAL);
    }
    
    /**
     * VISUAL STRATEGY: Complex elements that might need image recognition
     */
    public void clickLoginButtonVisualOnly() {
        logger.info("Clicking login button with VISUAL_ONLY strategy");
        AutoHealStrategyConfig visualStrategy = new AutoHealStrategyConfig(
            HealingStrategy.VISUAL_ONLY,
            com.autoheal.model.ExecutionStrategy.SEQUENTIAL,
            true, false, false, 3, 20000
        );
        click(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION, visualStrategy);
    }
    
    /**
     * COST-OPTIMIZED STRATEGY: Intelligent strategy selection
     */
    public String getErrorMessageCostOptimized() {
        logger.info("Getting error message with COST_OPTIMIZED strategy");
        AutoHealStrategyConfig costStrategy = new AutoHealStrategyConfig(
            HealingStrategy.COST_OPTIMIZED,
            com.autoheal.model.ExecutionStrategy.COST_OPTIMIZED,
            true, true, false, 2, 15000
        );
        
        try {
            // This will automatically choose the best strategy based on element characteristics
            Object element = autoHeal.findElement(ERROR_MESSAGE, ERROR_MESSAGE_DESCRIPTION);
            if (element instanceof com.autoheal.impl.adapter.PlaywrightElementWrapper.WrappedElement) {
                return ((com.autoheal.impl.adapter.PlaywrightElementWrapper.WrappedElement) element).getText();
            }
        } catch (Exception e) {
            logger.error("Cost-optimized strategy failed: {}", e.getMessage());
        }
        
        // Fallback to normal Playwright
        return page.textContent(ERROR_MESSAGE);
    }
    
    // ==================== COMBINED STRATEGIES FOR COMPLEX WORKFLOWS ====================
    
    /**
     * Speed-optimized login (DOM only for all elements)
     */
    public void loginFast(String username, String password) {
        logger.info("=== SPEED-OPTIMIZED LOGIN (DOM only) ===");
        AutoHealStrategyConfig fastStrategy = AutoHealStrategyConfig.fastConfig();
        
        type(USERNAME_FIELD, username, USERNAME_DESCRIPTION, fastStrategy);
        type(PASSWORD_FIELD, password, PASSWORD_DESCRIPTION, fastStrategy);
        click(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION, fastStrategy);
    }
    
    /**
     * Reliability-optimized login (Parallel hybrid for all elements)
     */
    public void loginReliable(String username, String password) {
        logger.info("=== RELIABILITY-OPTIMIZED LOGIN (Parallel hybrid) ===");
        AutoHealStrategyConfig reliableStrategy = AutoHealStrategyConfig.reliableConfig();
        
        type(USERNAME_FIELD, username, USERNAME_DESCRIPTION, reliableStrategy);
        type(PASSWORD_FIELD, password, PASSWORD_DESCRIPTION, reliableStrategy);
        click(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION, reliableStrategy);
    }
    
    /**
     * Balanced login (Different strategies per element priority)
     */
    public void loginBalanced(String username, String password) {
        logger.info("=== BALANCED LOGIN (Mixed strategies) ===");
        
        // Fast for input fields (stable elements)
        type(USERNAME_FIELD, username, USERNAME_DESCRIPTION, ElementPriority.LOW);
        type(PASSWORD_FIELD, password, PASSWORD_DESCRIPTION, ElementPriority.LOW);
        
        // Reliable for critical action button
        click(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION, ElementPriority.CRITICAL);
    }
    
    /**
     * Progressive strategy (escalate if failures occur)
     */
    public void loginProgressive(String username, String password) {
        logger.info("=== PROGRESSIVE LOGIN (Escalating strategies) ===");
        
        try {
            // Try fast first
            loginFast(username, password);
            logger.info("Fast login successful");
        } catch (Exception e) {
            logger.warn("Fast login failed, trying reliable: {}", e.getMessage());
            try {
                // Clear any partial input and retry with reliable strategy
                page.reload();
                page.waitForLoadState();
                loginReliable(username, password);
                logger.info("Reliable login successful");
            } catch (Exception e2) {
                logger.error("All login strategies failed: {}", e2.getMessage());
                throw new RuntimeException("All login strategies exhausted", e2);
            }
        }
    }
    
    // ==================== STRATEGY TESTING AND COMPARISON ====================
    
    /**
     * Test all strategies and compare performance
     */
    public void compareStrategies(String username, String password) {
        logger.info("=== STRATEGY PERFORMANCE COMPARISON ===");
        
        // Test DOM only
        long startTime = System.currentTimeMillis();
        try {
            page.reload();
            page.waitForLoadState();
            loginFast(username, password);
            long domTime = System.currentTimeMillis() - startTime;
            logger.info("DOM-only strategy completed in {}ms", domTime);
        } catch (Exception e) {
            logger.warn("DOM-only strategy failed: {}", e.getMessage());
        }
        
        // Test Hybrid Sequential
        startTime = System.currentTimeMillis();
        try {
            page.reload();
            page.waitForLoadState();
            login(username, password); // Default strategy
            long hybridTime = System.currentTimeMillis() - startTime;
            logger.info("Hybrid sequential strategy completed in {}ms", hybridTime);
        } catch (Exception e) {
            logger.warn("Hybrid sequential strategy failed: {}", e.getMessage());
        }
        
        // Test Parallel Hybrid
        startTime = System.currentTimeMillis();
        try {
            page.reload();
            page.waitForLoadState();
            loginReliable(username, password);
            long parallelTime = System.currentTimeMillis() - startTime;
            logger.info("Parallel hybrid strategy completed in {}ms", parallelTime);
        } catch (Exception e) {
            logger.warn("Parallel hybrid strategy failed: {}", e.getMessage());
        }
        
        logger.info("=== STRATEGY COMPARISON COMPLETED ===");
    }
    
    // ==================== STANDARD LOGIN METHODS ====================
    
    /**
     * Standard login using default strategy
     */
    public void login(String username, String password) {
        logger.info("Performing standard login with default strategy");
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
    }
    
    /**
     * Check login result
     */
    public boolean isErrorMessageDisplayed() {
        try {
            return page.isVisible(ERROR_MESSAGE, new Page.IsVisibleOptions().setTimeout(2000));
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getErrorMessage() {
        if (isErrorMessageDisplayed()) {
            return page.textContent(ERROR_MESSAGE);
        }
        return "";
    }
    
    public String getLogoText() {
        return page.textContent(LOGO);
    }
}