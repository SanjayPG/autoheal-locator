package com.autoheal.core;

import com.autoheal.model.AutomationFramework;
import com.autoheal.model.ElementContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter interface for web automation frameworks
 */
public interface WebAutomationAdapter {

    /**
     * Get the automation framework type of this adapter
     *
     * @return The automation framework type (SELENIUM or PLAYWRIGHT)
     */
    AutomationFramework getFrameworkType();

    /**
     * Find elements using the given selector
     *
     * @param selector CSS selector, XPath, or other selector format
     * @return CompletableFuture containing list of matching elements
     */
    CompletableFuture<List<WebElement>> findElements(String selector);

    /**
     * Find elements using the given Selenium By object
     *
     * @param by Selenium By locator object
     * @return CompletableFuture containing list of matching elements
     */
    CompletableFuture<List<WebElement>> findElements(By by);

    /**
     * Get the current page source
     *
     * @return CompletableFuture containing HTML page source
     */
    CompletableFuture<String> getPageSource();

    /**
     * Take a screenshot of the current page
     *
     * @return CompletableFuture containing screenshot as byte array
     */
    CompletableFuture<byte[]> takeScreenshot();

    /**
     * Extract contextual information about an element
     *
     * @param element The element to analyze
     * @return CompletableFuture containing element context information
     */
    CompletableFuture<ElementContext> getElementContext(WebElement element);
}