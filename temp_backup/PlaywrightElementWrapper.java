package com.autoheal.impl.adapter;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static class WrappedElement {
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
        
        public void sendKeys(String text) {
            try {
                elementHandle.fill(text);
            } catch (PlaywrightException e) {
                logger.error("Failed to send keys to element: {}", e.getMessage());
                throw new RuntimeException("SendKeys failed", e);
            }
        }
        
        public void clear() {
            try {
                elementHandle.fill("");
            } catch (PlaywrightException e) {
                logger.error("Failed to clear element: {}", e.getMessage());
                throw new RuntimeException("Clear failed", e);
            }
        }
        
        public String getText() {
            try {
                return elementHandle.textContent();
            } catch (PlaywrightException e) {
                logger.error("Failed to get text from element: {}", e.getMessage());
                return "";
            }
        }
        
        public String getAttribute(String name) {
            try {
                return elementHandle.getAttribute(name);
            } catch (PlaywrightException e) {
                logger.error("Failed to get attribute '{}' from element: {}", name, e.getMessage());
                return null;
            }
        }
        
        public boolean isDisplayed() {
            try {
                return elementHandle.isVisible();
            } catch (PlaywrightException e) {
                logger.error("Failed to check if element is displayed: {}", e.getMessage());
                return false;
            }
        }
        
        public boolean isEnabled() {
            try {
                return elementHandle.isEnabled();
            } catch (PlaywrightException e) {
                logger.error("Failed to check if element is enabled: {}", e.getMessage());
                return false;
            }
        }
        
        public String getTagName() {
            try {
                return (String) elementHandle.evaluate("el => el.tagName.toLowerCase()");
            } catch (PlaywrightException e) {
                logger.error("Failed to get tag name from element: {}", e.getMessage());
                return "";
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
                elementHandle.waitForElementState(ElementHandle.ElementState.VISIBLE);
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