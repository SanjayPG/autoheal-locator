package com.autoheal.util;

import org.openqa.selenium.By;

import java.util.regex.Pattern;

/**
 * Utilities for selector parsing and manipulation
 */
public class SelectorUtils {

    private static final Pattern XPATH_PATTERN = Pattern.compile("^(//|\\().*");
    private static final Pattern ID_PATTERN = Pattern.compile("^#[a-zA-Z][a-zA-Z0-9_-]*$");
    private static final Pattern CLASS_PATTERN = Pattern.compile("^\\.[a-zA-Z][a-zA-Z0-9_-]*$");

    /**
     * Parse a selector string into a Selenium By locator
     *
     * @param selector the selector string
     * @return appropriate By locator
     */
    public static By parseSelector(String selector) {
        if (selector == null || selector.trim().isEmpty()) {
            throw new IllegalArgumentException("Selector cannot be null or empty");
        }

        selector = selector.trim();

        if (isXPath(selector)) {
            return By.xpath(selector);
        } else if (isId(selector)) {
            return By.id(selector.substring(1));
        } else if (isClass(selector)) {
            return By.className(selector.substring(1));
        } else {
            // Default to CSS selector
            return By.cssSelector(selector);
        }
    }

    /**
     * Check if selector is XPath
     *
     * @param selector the selector to check
     * @return true if XPath, false otherwise
     */
    public static boolean isXPath(String selector) {
        return XPATH_PATTERN.matcher(selector).matches();
    }

    /**
     * Check if selector is ID selector
     *
     * @param selector the selector to check
     * @return true if ID selector, false otherwise
     */
    public static boolean isId(String selector) {
        return ID_PATTERN.matcher(selector).matches();
    }

    /**
     * Check if selector is class selector
     *
     * @param selector the selector to check
     * @return true if class selector, false otherwise
     */
    public static boolean isClass(String selector) {
        return CLASS_PATTERN.matcher(selector).matches();
    }

    /**
     * Normalize selector for consistent caching
     *
     * @param selector the selector to normalize
     * @return normalized selector
     */
    public static String normalizeSelector(String selector) {
        if (selector == null) {
            return null;
        }

        return selector.trim()
                .replaceAll("\\s+", " ")  // Multiple spaces to single space
                .replaceAll("\\s*>\\s*", ">")  // Remove spaces around child combinator
                .replaceAll("\\s*\\+\\s*", "+")  // Remove spaces around adjacent sibling
                .replaceAll("\\s*~\\s*", "~");   // Remove spaces around general sibling
    }

    /**
     * Extract element type from selector for logging/metrics
     *
     * @param selector the selector to analyze
     * @return element type or "unknown"
     */
    public static String extractElementType(String selector) {
        if (selector == null || selector.trim().isEmpty()) {
            return "unknown";
        }

        selector = selector.trim().toLowerCase();

        // Common element types
        if (selector.contains("button")) return "button";
        if (selector.contains("input")) return "input";
        if (selector.contains("select")) return "select";
        if (selector.contains("textarea")) return "textarea";
        if (selector.contains("a") || selector.contains("link")) return "link";
        if (selector.contains("img")) return "image";
        if (selector.contains("div")) return "div";
        if (selector.contains("span")) return "span";
        if (selector.contains("form")) return "form";

        return "element";
    }

    /**
     * Generate fallback selectors for an element
     *
     * @param originalSelector the original selector that failed
     * @return array of fallback selectors to try
     */
    public static String[] generateFallbackSelectors(String originalSelector) {
        if (originalSelector == null || originalSelector.trim().isEmpty()) {
            return new String[0];
        }

        // This is a simplified implementation
        // In practice, this would be more sophisticated
        return new String[]{
                originalSelector.replace("#", "[id='") + "']",
                originalSelector.replace(".", "[class*='") + "']",
                "//*[contains(@class, '" + originalSelector.replace(".", "") + "')]"
        };
    }
}