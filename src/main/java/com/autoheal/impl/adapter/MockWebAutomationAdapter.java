package com.autoheal.impl.adapter;

import com.autoheal.core.WebAutomationAdapter;
import com.autoheal.model.ElementContext;
import com.autoheal.model.Position;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation for testing purposes
 */
public class MockWebAutomationAdapter implements WebAutomationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MockWebAutomationAdapter.class);

    private final Map<String, List<WebElement>> mockElements = new ConcurrentHashMap<>();
    private final Map<String, String> mockPageSources = new ConcurrentHashMap<>();
    private final Map<String, byte[]> mockScreenshots = new ConcurrentHashMap<>();

    public MockWebAutomationAdapter() {
        logger.debug("MockWebAutomationAdapter initialized");
    }

    public void addMockElement(String selector, WebElement element) {
        mockElements.computeIfAbsent(selector, k -> new ArrayList<>()).add(element);
        logger.debug("Added mock element for selector: {}", selector);
    }

    public void addMockPageSource(String pageSource) {
        mockPageSources.put("default", pageSource);
        logger.debug("Added mock page source (length: {} characters)", pageSource.length());
    }

    public void addMockScreenshot(byte[] screenshot) {
        mockScreenshots.put("default", screenshot);
        logger.debug("Added mock screenshot (size: {} bytes)", screenshot.length);
    }

    @Override
    public CompletableFuture<List<WebElement>> findElements(String selector) {
        List<WebElement> elements = mockElements.getOrDefault(selector, new ArrayList<>());
        logger.debug("Mock found {} elements for selector: {}", elements.size(), selector);
        return CompletableFuture.completedFuture(elements);
    }

    @Override
    public CompletableFuture<List<WebElement>> findElements(By by) {
        // For mock, just use the By string representation as the key
        String byKey = by.toString();
        List<WebElement> elements = mockElements.getOrDefault(byKey, new ArrayList<>());
        logger.debug("Mock found {} elements for By: {}", elements.size(), byKey);
        return CompletableFuture.completedFuture(elements);
    }

    @Override
    public CompletableFuture<String> getPageSource() {
        String pageSource = mockPageSources.getOrDefault("default", "<html><body>Mock page</body></html>");
        logger.debug("Mock returning page source (length: {} characters)", pageSource.length());
        return CompletableFuture.completedFuture(pageSource);
    }

    @Override
    public CompletableFuture<byte[]> takeScreenshot() {
        byte[] screenshot = mockScreenshots.getOrDefault("default", new byte[]{});
        logger.debug("Mock returning screenshot (size: {} bytes)", screenshot.length);
        return CompletableFuture.completedFuture(screenshot);
    }

    @Override
    public CompletableFuture<ElementContext> getElementContext(WebElement element) {
        ElementContext context = ElementContext.builder()
                .parentContainer("mock-parent")
                .relativePosition(new Position(100, 100, 50, 20))
                .textContent("Mock Element")
                .build();
        logger.debug("Mock returning element context for element");
        return CompletableFuture.completedFuture(context);
    }
}