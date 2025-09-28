package com.autoheal.reporting;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.core.WebAutomationAdapter;
import com.autoheal.reporting.AutoHealReporter.SelectorStrategy;
import org.openqa.selenium.WebElement;

import java.util.concurrent.CompletableFuture;

/**
 * AutoHealLocator wrapper that automatically reports all selector usage
 */
public class ReportingAutoHealLocator {

    private final AutoHealLocator autoHeal;
    private final AutoHealReporter reporter;

    public ReportingAutoHealLocator(WebAutomationAdapter adapter, AutoHealConfiguration config) {
        this.autoHeal = AutoHealLocator.builder()
                .withWebAdapter(adapter)
                .withConfiguration(config)
                .build();
        this.reporter = new AutoHealReporter();

        System.out.println("🔍 AutoHeal Reporting System ACTIVE");
        System.out.println("All selector usage will be tracked and reported!");
    }

    /**
     * Find element with full reporting
     */
    public WebElement findElement(String selector, String description) {
        long startTime = System.currentTimeMillis();
        SelectorStrategy strategy = SelectorStrategy.FAILED;
        String actualSelector = selector;
        String elementDetails = null;
        String reasoning = null;
        boolean success = false;

        try {
            WebElement element = autoHeal.findElement(selector, description);
            long duration = System.currentTimeMillis() - startTime;

            success = true;
            elementDetails = String.format("%s#%s.%s",
                element.getTagName(),
                element.getAttribute("id") != null ? element.getAttribute("id") : "null",
                element.getAttribute("class") != null ? element.getAttribute("class") : "null");

            // Infer strategy based on timing and selector matching
            if (duration < 200) {
                strategy = SelectorStrategy.ORIGINAL_SELECTOR;
                reasoning = "Original selector worked immediately";
            } else if (duration < 1500) {
                strategy = SelectorStrategy.CACHED;
                reasoning = "Retrieved from cache";
                actualSelector = inferActualSelector(element);
            } else if (duration > 3000) {
                strategy = SelectorStrategy.DOM_ANALYSIS;
                reasoning = "AI DOM analysis was used to heal broken selector";
                actualSelector = inferActualSelector(element);
            } else {
                strategy = SelectorStrategy.DOM_ANALYSIS;
                reasoning = "Selector healing was attempted";
                actualSelector = inferActualSelector(element);
            }

            reporter.recordSelectorUsage(selector, description, strategy, duration,
                success, actualSelector, elementDetails, reasoning);

            return element;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            strategy = SelectorStrategy.FAILED;
            reasoning = "Failed: " + e.getMessage();

            reporter.recordSelectorUsage(selector, description, strategy, duration,
                false, null, null, reasoning);

            throw e;
        }
    }

    /**
     * Find element async with reporting
     */
    public CompletableFuture<WebElement> findElementAsync(String selector, String description) {
        long startTime = System.currentTimeMillis();

        return autoHeal.findElementAsync(selector, description)
                .whenComplete((element, throwable) -> {
                    long duration = System.currentTimeMillis() - startTime;
                    SelectorStrategy strategy;
                    String actualSelector = selector;
                    String elementDetails = null;
                    String reasoning;
                    boolean success = throwable == null && element != null;

                    if (success) {
                        elementDetails = String.format("%s#%s.%s",
                            element.getTagName(),
                            element.getAttribute("id") != null ? element.getAttribute("id") : "null",
                            element.getAttribute("class") != null ? element.getAttribute("class") : "null");

                        if (duration < 200) {
                            strategy = SelectorStrategy.ORIGINAL_SELECTOR;
                            reasoning = "Original selector worked immediately";
                        } else if (duration < 1500) {
                            strategy = SelectorStrategy.CACHED;
                            reasoning = "Retrieved from cache";
                            actualSelector = inferActualSelector(element);
                        } else {
                            strategy = SelectorStrategy.DOM_ANALYSIS;
                            reasoning = "AI DOM analysis was used to heal broken selector";
                            actualSelector = inferActualSelector(element);
                        }
                    } else {
                        strategy = SelectorStrategy.FAILED;
                        reasoning = "Failed: " + (throwable != null ? throwable.getMessage() : "Unknown error");
                    }

                    reporter.recordSelectorUsage(selector, description, strategy, duration,
                        success, actualSelector, elementDetails, reasoning);
                });
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
     * Generate all report formats
     */
    public void generateReports() {
        System.out.println("\n📝 Generating AutoHeal reports...");
        reporter.generateHTMLReport();
        reporter.generateJSONReport();
        reporter.generateTextReport();
        reporter.printSummary();
    }

    /**
     * Generate specific report format
     */
    public void generateHTMLReport() {
        reporter.generateHTMLReport();
    }

    public void generateJSONReport() {
        reporter.generateJSONReport();
    }

    public void generateTextReport() {
        reporter.generateTextReport();
    }

    /**
     * Get the underlying AutoHeal instance for advanced operations
     */
    public AutoHealLocator getAutoHeal() {
        return autoHeal;
    }

    /**
     * Shutdown with reports
     */
    public void shutdown() {
        generateReports();
        autoHeal.shutdown();
    }
}