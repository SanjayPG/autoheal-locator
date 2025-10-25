package com.autoheal.impl.adapter;

import com.autoheal.core.WebAutomationAdapter;
import com.autoheal.model.AutomationFramework;
import com.autoheal.model.ElementContext;
import com.autoheal.model.ElementFingerprint;
import com.autoheal.model.Position;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Playwright implementation of WebAutomationAdapter for AutoHeal
 */
public class PlaywrightWebAutomationAdapter implements WebAutomationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PlaywrightWebAutomationAdapter.class);

    private final Page page;
    private final PlaywrightElementWrapper elementWrapper;

    public PlaywrightWebAutomationAdapter(Page page) {
        this.page = page;
        this.elementWrapper = new PlaywrightElementWrapper();
    }

    @Override
    public AutomationFramework getFrameworkType() {
        return AutomationFramework.PLAYWRIGHT;
    }
    
    @Override
    public CompletableFuture<List<WebElement>> findElements(String selector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ElementHandle> elements = page.querySelectorAll(selector);
                return elements.stream()
                    .map(elementWrapper::wrapElement)
                    .map(wrapped -> (WebElement) wrapped)
                    .collect(Collectors.toList());

            } catch (PlaywrightException e) {
                logger.debug("Failed to find elements with selector: {} - {}", selector, e.getMessage());
                return List.of();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<WebElement>> findElements(By by) {
        // Convert Selenium By to CSS selector for Playwright
        String selector = convertByToSelector(by);
        return findElements(selector);
    }

    private String convertByToSelector(By by) {
        String byString = by.toString();
        if (byString.startsWith("By.id: ")) {
            return "#" + byString.substring("By.id: ".length());
        } else if (byString.startsWith("By.className: ")) {
            return "." + byString.substring("By.className: ".length());
        } else if (byString.startsWith("By.cssSelector: ")) {
            return byString.substring("By.cssSelector: ".length());
        } else if (byString.startsWith("By.xpath: ")) {
            // Playwright supports XPath
            return byString.substring("By.xpath: ".length());
        } else if (byString.startsWith("By.tagName: ")) {
            return byString.substring("By.tagName: ".length());
        } else {
            // Default to treating as CSS selector
            return byString;
        }
    }
    
    @Override
    public CompletableFuture<String> getPageSource() {
        return CompletableFuture.supplyAsync(() -> page.content());
    }
    
    
    @Override
    public CompletableFuture<byte[]> takeScreenshot() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return page.screenshot();
            } catch (PlaywrightException e) {
                logger.error("Failed to take screenshot: {}", e.getMessage());
                throw new RuntimeException("Screenshot failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<ElementContext> getElementContext(WebElement element) {
        return CompletableFuture.supplyAsync(() -> {
            if (!(element instanceof PlaywrightElementWrapper.WrappedElement)) {
                throw new IllegalArgumentException("Element must be a Playwright wrapped element");
            }

            PlaywrightElementWrapper.WrappedElement wrappedElement =
                (PlaywrightElementWrapper.WrappedElement) element;
            ElementHandle elementHandle = wrappedElement.getElementHandle();

            try {
                // Get element properties
                String tagName = (String) elementHandle.evaluate("el => el.tagName.toLowerCase()");
                String id = (String) elementHandle.getAttribute("id");
                String className = (String) elementHandle.getAttribute("class");
                String text = elementHandle.textContent();

                // Get position
                BoundingBox boundingBox = elementHandle.boundingBox();
                Position position = null;
                if (boundingBox != null) {
                    position = new Position(
                        (int) boundingBox.x,
                        (int) boundingBox.y,
                        (int) boundingBox.width,
                        (int) boundingBox.height
                    );
                }

                // Create fingerprint
                ElementFingerprint fingerprint = ElementFingerprint.builder()
                    .tagName(tagName)
                    .id(id)
                    .className(className)
                    .text(text)
                    .position(position)
                    .build();

                return ElementContext.builder()
                    .element(element)
                    .fingerprint(fingerprint)
                    .pageUrl(page.url())
                    .build();

            } catch (PlaywrightException e) {
                logger.error("Failed to get element context: {}", e.getMessage());
                throw new RuntimeException("Failed to analyze element", e);
            }
        });
    }
    

    // Playwright-specific methods
    public Page getPage() {
        return page;
    }

    public PlaywrightElementWrapper getElementWrapper() {
        return elementWrapper;
    }

    /**
     * Execute a PlaywrightLocator and return native Playwright Locator object
     *
     * @param playwrightLocator The PlaywrightLocator to execute
     * @return Native Playwright Locator
     */
    public com.microsoft.playwright.Locator executePlaywrightLocator(com.autoheal.model.PlaywrightLocator playwrightLocator) {
        return switch (playwrightLocator.getType()) {
            case GET_BY_ROLE -> {
                String roleName = playwrightLocator.getValue();
                Object nameOption = playwrightLocator.getOption("name");

                // Parse role enum
                com.microsoft.playwright.options.AriaRole role = parseAriaRole(roleName);

                if (nameOption != null) {
                    yield page.getByRole(role,
                            new com.microsoft.playwright.Page.GetByRoleOptions()
                                    .setName(nameOption.toString()));
                } else {
                    yield page.getByRole(role);
                }
            }
            case GET_BY_LABEL -> page.getByLabel(playwrightLocator.getValue());
            case GET_BY_PLACEHOLDER -> page.getByPlaceholder(playwrightLocator.getValue());
            case GET_BY_TEXT -> page.getByText(playwrightLocator.getValue());
            case GET_BY_ALT_TEXT -> page.getByAltText(playwrightLocator.getValue());
            case GET_BY_TITLE -> page.getByTitle(playwrightLocator.getValue());
            case GET_BY_TEST_ID -> page.getByTestId(playwrightLocator.getValue());
            case CSS_SELECTOR, XPATH -> page.locator(playwrightLocator.getValue());
        };
    }

    /**
     * Parse ARIA role string to AriaRole enum
     */
    private com.microsoft.playwright.options.AriaRole parseAriaRole(String roleName) {
        try {
            // Convert to uppercase and replace spaces with underscores
            String enumName = roleName.toUpperCase().replace(" ", "_");
            return com.microsoft.playwright.options.AriaRole.valueOf(enumName);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown ARIA role: {}, falling back to BUTTON", roleName);
            return com.microsoft.playwright.options.AriaRole.BUTTON;
        }
    }

    /**
     * Try executing a locator and return count of matching elements
     *
     * @param locator The Playwright locator to test
     * @return Number of matching elements, or 0 if failed
     */
    public int countElements(com.microsoft.playwright.Locator locator) {
        try {
            return locator.count();
        } catch (PlaywrightException e) {
            logger.debug("Failed to count elements: {}", e.getMessage());
            return 0;
        }
    }
}