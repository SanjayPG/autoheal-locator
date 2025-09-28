package com.autoheal.impl.adapter;

import com.autoheal.core.WebAutomationAdapter;
import com.autoheal.model.ElementContext;
import com.autoheal.model.ElementFingerprint;
import com.autoheal.model.Position;
import com.autoheal.util.LocatorTypeDetector;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Selenium WebDriver adapter implementation
 */
public class SeleniumWebAutomationAdapter implements WebAutomationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SeleniumWebAutomationAdapter.class);

    private final WebDriver driver;
    private final ExecutorService executorService;

    public SeleniumWebAutomationAdapter(WebDriver driver) {
        this.driver = driver;
        this.executorService = Executors.newFixedThreadPool(4);
        logger.info("SeleniumWebAutomationAdapter initialized");
    }

    @Override
    public CompletableFuture<List<WebElement>> findElements(String selector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                By locator = LocatorTypeDetector.autoCreateBy(selector);
                List<WebElement> elements = driver.findElements(locator);
                logger.debug("Found {} elements for selector: {}", elements.size(), selector);
                return elements;
            } catch (Exception e) {
                logger.error("Failed to find elements with selector: {}", selector, e);
                throw new RuntimeException("Failed to find elements with selector: " + selector, e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<List<WebElement>> findElements(By by) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WebElement> elements = driver.findElements(by);
                logger.debug("Found {} elements for By locator: {}", elements.size(), by.toString());
                return elements;
            } catch (Exception e) {
                logger.error("Failed to find elements with By locator: {}", by.toString(), e);
                throw new RuntimeException("Failed to find elements with By locator: " + by.toString(), e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<String> getPageSource() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String pageSource = driver.getPageSource();
                logger.debug("Retrieved page source (length: {} characters)", pageSource.length());
                return pageSource;
            } catch (Exception e) {
                logger.error("Failed to get page source", e);
                throw new RuntimeException("Failed to get page source", e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<byte[]> takeScreenshot() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (driver instanceof TakesScreenshot) {
                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    logger.debug("Screenshot taken (size: {} bytes)", screenshot.length);
                    return screenshot;
                } else {
                    throw new UnsupportedOperationException("Driver does not support screenshots");
                }
            } catch (Exception e) {
                logger.error("Failed to take screenshot", e);
                throw new RuntimeException("Failed to take screenshot", e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<ElementContext> getElementContext(WebElement element) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Extract element context information
                String parentContainer = extractParentContainer(element);
                Position position = extractPosition(element);
                List<String> siblings = extractSiblingElements(element);
                Map<String, String> attributes = extractAttributes(element);
                String textContent = element.getText();

                ElementFingerprint fingerprint = new ElementFingerprint(
                        parentContainer,
                        position,
                        extractComputedStyles(element),
                        textContent,
                        siblings,
                        generateVisualHash(element)
                );

                ElementContext context = ElementContext.builder()
                        .parentContainer(parentContainer)
                        .relativePosition(position)
                        .siblingElements(siblings)
                        .attributes(attributes)
                        .textContent(textContent)
                        .fingerprint(fingerprint)
                        .build();

                logger.debug("Extracted context for element: {}", element.getTagName());
                return context;

            } catch (Exception e) {
                logger.error("Failed to extract element context", e);
                throw new RuntimeException("Failed to extract element context", e);
            }
        }, executorService);
    }


    private String extractParentContainer(WebElement element) {
        try {
            WebElement parent = element.findElement(By.xpath(".."));
            return parent.getTagName() +
                    (parent.getAttribute("class") != null ? "." + parent.getAttribute("class").split(" ")[0] : "") +
                    (parent.getAttribute("id") != null ? "#" + parent.getAttribute("id") : "");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Position extractPosition(WebElement element) {
        try {
            org.openqa.selenium.Rectangle rect = element.getRect();
            return new Position(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        } catch (Exception e) {
            return new Position(0, 0, 0, 0);
        }
    }

    private List<String> extractSiblingElements(WebElement element) {
        try {
            List<WebElement> siblings = element.findElements(By.xpath("..//*"));
            return siblings.stream()
                    .limit(5) // Limit to avoid too much data
                    .map(WebElement::getTagName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, String> extractAttributes(WebElement element) {
        Map<String, String> attributes = new HashMap<>();
        try {
            // Common attributes to extract
            String[] attrNames = {"id", "class", "name", "type", "value", "href", "src", "data-testid"};

            for (String attrName : attrNames) {
                String value = element.getAttribute(attrName);
                if (value != null && !value.isEmpty()) {
                    attributes.put(attrName, value);
                }
            }
        } catch (Exception e) {
            // Ignore extraction errors
        }
        return attributes;
    }

    private Map<String, String> extractComputedStyles(WebElement element) {
        Map<String, String> styles = new HashMap<>();
        try {
            if (driver instanceof JavascriptExecutor) {
                JavascriptExecutor js = (JavascriptExecutor) driver;

                // Extract key CSS properties
                String[] properties = {"display", "visibility", "position", "z-index", "background-color", "color"};

                for (String property : properties) {
                    String value = (String) js.executeScript(
                            "return window.getComputedStyle(arguments[0]).getPropertyValue(arguments[1]);",
                            element, property
                    );
                    if (value != null && !value.isEmpty()) {
                        styles.put(property, value);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore style extraction errors
        }
        return styles;
    }

    private String generateVisualHash(WebElement element) {
        try {
            // Generate a simple hash based on element properties
            StringBuilder hashBuilder = new StringBuilder();
            hashBuilder.append(element.getTagName());
            hashBuilder.append(element.getText());
            hashBuilder.append(element.getSize().toString());
            hashBuilder.append(element.getLocation().toString());

            return Integer.toHexString(hashBuilder.toString().hashCode());
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Shutdown the adapter and cleanup resources
     */
    public void shutdown() {
        executorService.shutdown();
        logger.info("SeleniumWebAutomationAdapter shutdown completed");
    }
}