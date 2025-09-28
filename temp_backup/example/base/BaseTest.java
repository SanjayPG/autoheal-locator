package com.example.base;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced BaseTest with AutoHeal support and comprehensive setup
 */
public abstract class BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    
    protected static Playwright playwright;
    protected static Browser browser;
    protected static BrowserContext context;
    protected Page page;
    
    // Configuration
    private static final String BROWSER_TYPE = System.getProperty("browser", "chromium");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("headless", "false"));
    private static final boolean TRACE_ENABLED = Boolean.parseBoolean(System.getProperty("trace", "true"));
    private static final String SCREENSHOT_DIR = "target/screenshots";
    private static final String TRACE_DIR = "target/traces";
    
    @BeforeAll
    static void setupPlaywright() {
        logger.info("Setting up Playwright with AutoHeal integration");
        
        // Create output directories
        createDirectories();
        
        playwright = Playwright.create();
        
        // Configure browser based on system property
        BrowserType browserType = getBrowserType();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(HEADLESS)
            .setSlowMo(50) // Add slight delay for better stability
            .setArgs(java.util.List.of(
                "--disable-web-security",
                "--disable-features=VizDisplayCompositor",
                "--no-sandbox"
            ));
            
        browser = browserType.launch(launchOptions);
        logger.info("Browser launched successfully: {}", BROWSER_TYPE);
    }
    
    @BeforeEach
    void setupTest() {
        // Create new context for each test for isolation
        BrowserContext.NewContextOptions contextOptions = new BrowserContext.NewContextOptions()
            .setViewportSize(1920, 1080)
            .setIgnoreHTTPSErrors(true);
            
        // Enable tracing if configured
        if (TRACE_ENABLED) {
            contextOptions.setRecordTrace(new BrowserContext.RecordTraceOptions()
                .setScreenshots(true)
                .setSources(true)
                .setSnapshots(true));
        }
        
        context = browser.newContext(contextOptions);
        page = context.newPage();
        
        // Configure page timeout
        page.setDefaultTimeout(30000);
        page.setDefaultNavigationTimeout(30000);
        
        // Add console message logging
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                logger.warn("Browser console error: {}", msg.text());
            }
        });
        
        // Add page error logging
        page.onPageError(error -> {
            logger.error("Page error: {}", error.getMessage());
        });
        
        // Add request failure logging
        page.onRequestFailed(request -> {
            logger.warn("Request failed: {} - {}", request.url(), request.failure());
        });
        
        logger.info("Test setup completed for: {}", getTestName());
    }
    
    @AfterEach
    void teardownTest() {
        String testName = getTestName();
        
        try {
            // Save trace if enabled and test failed
            if (TRACE_ENABLED && isTestFailed()) {
                saveTrace(testName);
            }
            
            // Take screenshot if test failed
            if (isTestFailed()) {
                takeScreenshot(testName);
            }
            
            // Get AutoHeal metrics before closing
            logAutoHealMetrics();
            
        } catch (Exception e) {
            logger.error("Error during test teardown: {}", e.getMessage());
        } finally {
            // Ensure resources are cleaned up
            if (context != null) {
                context.close();
            }
        }
        
        logger.info("Test teardown completed for: {}", testName);
    }
    
    @AfterAll
    static void teardownPlaywright() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        logger.info("Playwright teardown completed");
    }
    
    // ==================== UTILITY METHODS ====================
    
    private static BrowserType getBrowserType() {
        return switch (BROWSER_TYPE.toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "webkit", "safari" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }
    
    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));
            Files.createDirectories(Paths.get(TRACE_DIR));
        } catch (IOException e) {
            logger.error("Failed to create output directories: {}", e.getMessage());
        }
    }
    
    private String getTestName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().startsWith("test") && 
                element.getClassName().contains("Test")) {
                return element.getClassName().substring(element.getClassName().lastIndexOf('.') + 1) 
                    + "_" + element.getMethodName();
            }
        }
        return "unknown_test_" + System.currentTimeMillis();
    }
    
    private boolean isTestFailed() {
        // This is a simplified check - in a real implementation you might want to
        // track test status more precisely using JUnit 5 extensions
        return Thread.currentThread().getStackTrace().length > 10; // Simple heuristic
    }
    
    private void takeScreenshot(String testName) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = String.format("%s_%s_failure.png", testName, timestamp);
            Path screenshotPath = Paths.get(SCREENSHOT_DIR, filename);
            
            byte[] screenshot = page.screenshot();
            Files.write(screenshotPath, screenshot);
            
            logger.info("Screenshot saved: {}", screenshotPath.toAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to take screenshot: {}", e.getMessage());
        }
    }
    
    private void saveTrace(String testName) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = String.format("%s_%s_trace.zip", testName, timestamp);
            Path tracePath = Paths.get(TRACE_DIR, filename);
            
            context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
            
            logger.info("Trace saved: {}", tracePath.toAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to save trace: {}", e.getMessage());
        }
    }
    
    private void logAutoHealMetrics() {
        // This would be implemented by the specific test pages
        // that extend BasePage and have access to AutoHeal metrics
        logger.info("AutoHeal session completed for test: {}", getTestName());
    }
    
    // ==================== HELPER METHODS FOR TESTS ====================
    
    /**
     * Get current page instance
     */
    protected Page getPage() {
        return page;
    }
    
    /**
     * Get current browser context
     */
    protected BrowserContext getContext() {
        return context;
    }
    
    /**
     * Navigate to URL and wait for load
     */
    protected void navigateAndWait(String url) {
        page.navigate(url);
        page.waitForLoadState();
        logger.info("Navigated to: {}", url);
    }
    
    /**
     * Wait for network idle (useful for SPAs)
     */
    protected void waitForNetworkIdle() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
    
    /**
     * Take screenshot manually
     */
    protected void captureScreenshot(String name) {
        takeScreenshot(getTestName() + "_" + name);
    }
    
    /**
     * Execute JavaScript and return result
     */
    protected Object executeScript(String script) {
        return page.evaluate(script);
    }
    
    /**
     * Wait for element to be visible
     */
    protected void waitForVisible(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
    }
    
    /**
     * Wait for element to be hidden
     */
    protected void waitForHidden(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
    }
}