package com.autoheal.impl.adapter;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ElementState;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wrapper to make Playwright ElementHandle compatible with AutoHeal WebElement interface
 */
public class PlaywrightElementWrapper {
    private static final Logger logger = LoggerFactory.getLogger(PlaywrightElementWrapper.class);
    
    public WrappedElement wrapElement(ElementHandle elementHandle) {
        return new WrappedElement(elementHandle);
    }
    
    /**
     * Wrapper class that makes Playwright ElementHandle behave like WebElement
     */
    public static class WrappedElement implements WebElement {
        private final ElementHandle elementHandle;
        
        public WrappedElement(ElementHandle elementHandle) {
            this.elementHandle = elementHandle;
        }
        
        public ElementHandle getElementHandle() {
            return elementHandle;
        }
        
        public void click() {
            try {
                elementHandle.click();
            } catch (PlaywrightException e) {
                logger.error("Failed to click element: {}", e.getMessage());
                throw new RuntimeException("Click failed", e);
            }
        }
        
        @Override
        public void sendKeys(CharSequence... keysToSend) {
            try {
                String text = String.join("", keysToSend);
                elementHandle.fill(text);
            } catch (PlaywrightException e) {
                logger.error("Failed to send keys to element: {}", e.getMessage());
                throw new RuntimeException("SendKeys failed", e);
            }
        }
        
        @Override
        public void clear() {
            try {
                elementHandle.fill("");
            } catch (PlaywrightException e) {
                logger.error("Failed to clear element: {}", e.getMessage());
                throw new RuntimeException("Clear failed", e);
            }
        }

        @Override
        public String getText() {
            try {
                return elementHandle.textContent();
            } catch (PlaywrightException e) {
                logger.error("Failed to get text from element: {}", e.getMessage());
                return "";
            }
        }

        @Override
        public String getAttribute(String name) {
            try {
                return elementHandle.getAttribute(name);
            } catch (PlaywrightException e) {
                logger.error("Failed to get attribute '{}' from element: {}", name, e.getMessage());
                return null;
            }
        }

        @Override
        public boolean isDisplayed() {
            try {
                return elementHandle.isVisible();
            } catch (PlaywrightException e) {
                logger.error("Failed to check if element is displayed: {}", e.getMessage());
                return false;
            }
        }

        @Override
        public boolean isEnabled() {
            try {
                return elementHandle.isEnabled();
            } catch (PlaywrightException e) {
                logger.error("Failed to check if element is enabled: {}", e.getMessage());
                return false;
            }
        }

        @Override
        public String getTagName() {
            try {
                return (String) elementHandle.evaluate("el => el.tagName.toLowerCase()");
            } catch (PlaywrightException e) {
                logger.error("Failed to get tag name from element: {}", e.getMessage());
                return "";
            }
        }

        @Override
        public boolean isSelected() {
            try {
                return (Boolean) elementHandle.evaluate("el => el.checked || el.selected");
            } catch (PlaywrightException e) {
                logger.error("Failed to check if element is selected: {}", e.getMessage());
                return false;
            }
        }

        @Override
        public void submit() {
            try {
                elementHandle.evaluate("el => el.form.submit()");
            } catch (PlaywrightException e) {
                logger.error("Failed to submit form: {}", e.getMessage());
                throw new RuntimeException("Submit failed", e);
            }
        }

        @Override
        public List<WebElement> findElements(By by) {
            throw new UnsupportedOperationException("findElements not supported on wrapped Playwright elements");
        }

        @Override
        public WebElement findElement(By by) {
            throw new UnsupportedOperationException("findElement not supported on wrapped Playwright elements");
        }

        @Override
        public Point getLocation() {
            try {
                BoundingBox box = elementHandle.boundingBox();
                return box != null ? new Point((int) box.x, (int) box.y) : new Point(0, 0);
            } catch (PlaywrightException e) {
                logger.error("Failed to get element location: {}", e.getMessage());
                return new Point(0, 0);
            }
        }

        @Override
        public Dimension getSize() {
            try {
                BoundingBox box = elementHandle.boundingBox();
                return box != null ? new Dimension((int) box.width, (int) box.height) : new Dimension(0, 0);
            } catch (PlaywrightException e) {
                logger.error("Failed to get element size: {}", e.getMessage());
                return new Dimension(0, 0);
            }
        }

        @Override
        public Rectangle getRect() {
            try {
                BoundingBox box = elementHandle.boundingBox();
                if (box != null) {
                    return new Rectangle((int) box.x, (int) box.y, (int) box.height, (int) box.width);
                }
                return new Rectangle(0, 0, 0, 0);
            } catch (PlaywrightException e) {
                logger.error("Failed to get element rectangle: {}", e.getMessage());
                return new Rectangle(0, 0, 0, 0);
            }
        }

        @Override
        public String getCssValue(String propertyName) {
            try {
                return (String) elementHandle.evaluate(
                    "el => window.getComputedStyle(el).getPropertyValue(arguments[0])",
                    propertyName
                );
            } catch (PlaywrightException e) {
                logger.error("Failed to get CSS value '{}': {}", propertyName, e.getMessage());
                return null;
            }
        }

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
            try {
                byte[] screenshot = elementHandle.screenshot();
                return target.convertFromPngBytes(screenshot);
            } catch (PlaywrightException e) {
                logger.error("Failed to take element screenshot: {}", e.getMessage());
                throw new WebDriverException("Screenshot failed", e);
            }
        }
        
        public BoundingBox getBoundingBox() {
            try {
                return elementHandle.boundingBox();
            } catch (PlaywrightException e) {
                logger.error("Failed to get bounding box from element: {}", e.getMessage());
                return null;
            }
        }
        
        public void waitFor() {
            try {
                // Use Playwright's ElementState enum
                elementHandle.waitForElementState(ElementState.VISIBLE);
            } catch (PlaywrightException e) {
                logger.error("Failed to wait for element: {}", e.getMessage());
                throw new RuntimeException("Wait failed", e);
            }
        }
        
        public void hover() {
            try {
                elementHandle.hover();
            } catch (PlaywrightException e) {
                logger.error("Failed to hover over element: {}", e.getMessage());
                throw new RuntimeException("Hover failed", e);
            }
        }
        
        @Override
        public String toString() {
            try {
                String tagName = getTagName();
                String id = getAttribute("id");
                String className = getAttribute("class");
                return String.format("WrappedElement{tagName='%s', id='%s', class='%s'}", 
                    tagName, id, className);
            } catch (Exception e) {
                return "WrappedElement{unknown}";
            }
        }
    }
}