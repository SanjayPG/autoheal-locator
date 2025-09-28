package com.autoheal.util;

import com.autoheal.model.LocatorType;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utility class to automatically detect locator types and convert them to appropriate Selenium By objects
 */
public class LocatorTypeDetector {
    private static final Logger logger = LoggerFactory.getLogger(LocatorTypeDetector.class);

    // Regex patterns for detection
    private static final Pattern XPATH_PATTERN = Pattern.compile("^(//|/|\\./|\\.\\./).*");
    private static final Pattern CSS_ID_PATTERN = Pattern.compile("^#[a-zA-Z][a-zA-Z0-9_-]*$");
    private static final Pattern CSS_CLASS_PATTERN = Pattern.compile("^\\.[a-zA-Z][a-zA-Z0-9_-]*$");
    private static final Pattern CSS_ATTRIBUTE_PATTERN = Pattern.compile(".*\\[.*=.*\\].*");
    private static final Pattern CSS_PSEUDO_PATTERN = Pattern.compile(".*:.*");
    private static final Pattern CSS_COMBINATOR_PATTERN = Pattern.compile(".*[>+~].*");
    private static final Pattern CSS_MULTIPLE_PATTERN = Pattern.compile(".*[#\\.].*");

    // Simple identifier patterns
    private static final Pattern SIMPLE_ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("^(a|abbr|address|area|article|aside|audio|b|base|bdi|bdo|big|blockquote|body|br|button|canvas|caption|cite|code|col|colgroup|data|datalist|dd|del|details|dfn|dialog|div|dl|dt|em|embed|fieldset|figcaption|figure|footer|form|h1|h2|h3|h4|h5|h6|head|header|hr|html|i|iframe|img|input|ins|kbd|label|legend|li|link|main|map|mark|meta|meter|nav|noscript|object|ol|optgroup|option|output|p|param|picture|pre|progress|q|rp|rt|ruby|s|samp|script|section|select|small|source|span|strong|style|sub|summary|sup|svg|table|tbody|td|textarea|tfoot|th|thead|time|title|tr|track|u|ul|var|video|wbr)$");

    /**
     * Detect the locator type based on the locator string
     *
     * @param locator The locator string to analyze
     * @return The detected LocatorType
     */
    public static LocatorType detectType(String locator) {
        if (locator == null || locator.trim().isEmpty()) {
            throw new IllegalArgumentException("Locator cannot be null or empty");
        }

        String trimmedLocator = locator.trim();
        logger.debug("Detecting locator type for: '{}'", trimmedLocator);

        // 1. Check for XPath (starts with // or / or ./ or ../)
        if (XPATH_PATTERN.matcher(trimmedLocator).matches()) {
            logger.debug("Detected as XPath: '{}'", trimmedLocator);
            return LocatorType.XPATH;
        }

        // 2. Check for CSS ID selector (#id)
        if (CSS_ID_PATTERN.matcher(trimmedLocator).matches()) {
            logger.debug("Detected as CSS ID selector: '{}'", trimmedLocator);
            return LocatorType.CSS_SELECTOR;
        }

        // 3. Check for CSS class selector (.class)
        if (CSS_CLASS_PATTERN.matcher(trimmedLocator).matches()) {
            logger.debug("Detected as CSS class selector: '{}'", trimmedLocator);
            return LocatorType.CSS_SELECTOR;
        }

        // 4. Check for complex CSS selectors
        if (CSS_ATTRIBUTE_PATTERN.matcher(trimmedLocator).matches() ||
            CSS_PSEUDO_PATTERN.matcher(trimmedLocator).matches() ||
            CSS_COMBINATOR_PATTERN.matcher(trimmedLocator).matches() ||
            CSS_MULTIPLE_PATTERN.matcher(trimmedLocator).matches()) {
            logger.debug("Detected as complex CSS selector: '{}'", trimmedLocator);
            return LocatorType.CSS_SELECTOR;
        }

        // 5. Check for HTML tag names
        if (TAG_NAME_PATTERN.matcher(trimmedLocator.toLowerCase()).matches()) {
            logger.debug("Detected as tag name: '{}'", trimmedLocator);
            return LocatorType.TAG_NAME;
        }

        // 6. Check if it looks like link text (contains spaces or common link words)
        if (isLikelyLinkText(trimmedLocator)) {
            logger.debug("Detected as link text: '{}'", trimmedLocator);
            return LocatorType.LINK_TEXT;
        }

        // 7. Default to ID for simple identifiers, NAME as fallback
        if (SIMPLE_ID_PATTERN.matcher(trimmedLocator).matches()) {
            logger.debug("Detected as simple identifier (ID): '{}'", trimmedLocator);
            return LocatorType.ID;
        }

        // 8. If nothing else matches, treat as partial link text or name
        logger.debug("Detected as name attribute (fallback): '{}'", trimmedLocator);
        return LocatorType.NAME;
    }

    /**
     * Convert locator string to appropriate Selenium By object
     *
     * @param locator The locator string
     * @param type The detected or specified locator type
     * @return Selenium By object
     */
    public static By createBy(String locator, LocatorType type) {
        if (locator == null || locator.trim().isEmpty()) {
            throw new IllegalArgumentException("Locator cannot be null or empty");
        }

        String trimmedLocator = locator.trim();
        logger.debug("Creating By object for '{}' as {}", trimmedLocator, type);

        switch (type) {
            case CSS_SELECTOR:
                return By.cssSelector(trimmedLocator);
            case XPATH:
                return By.xpath(trimmedLocator);
            case ID:
                return By.id(trimmedLocator);
            case NAME:
                return By.name(trimmedLocator);
            case CLASS_NAME:
                return By.className(trimmedLocator);
            case TAG_NAME:
                return By.tagName(trimmedLocator);
            case LINK_TEXT:
                return By.linkText(trimmedLocator);
            case PARTIAL_LINK_TEXT:
                return By.partialLinkText(trimmedLocator);
            default:
                logger.warn("Unknown locator type {}, defaulting to CSS selector", type);
                return By.cssSelector(trimmedLocator);
        }
    }

    /**
     * Auto-detect and create Selenium By object
     *
     * @param locator The locator string
     * @return Selenium By object
     */
    public static By autoCreateBy(String locator) {
        LocatorType detectedType = detectType(locator);
        return createBy(locator, detectedType);
    }

    /**
     * Check if the locator string looks like link text
     *
     * @param locator The locator string to check
     * @return true if it looks like link text
     */
    private static boolean isLikelyLinkText(String locator) {
        // Contains spaces (likely readable text)
        if (locator.contains(" ")) {
            return true;
        }

        // Common link text patterns
        String lowerLocator = locator.toLowerCase();
        return lowerLocator.contains("click") ||
               lowerLocator.contains("here") ||
               lowerLocator.contains("more") ||
               lowerLocator.contains("read") ||
               lowerLocator.contains("view") ||
               lowerLocator.contains("login") ||
               lowerLocator.contains("logout") ||
               lowerLocator.contains("sign") ||
               lowerLocator.contains("register") ||
               lowerLocator.contains("home") ||
               lowerLocator.contains("about") ||
               lowerLocator.contains("contact") ||
               lowerLocator.contains("help") ||
               lowerLocator.contains("support") ||
               (lowerLocator.length() > 15 && !lowerLocator.matches(".*[#.\\[\\]@/].*")); // Long text without CSS/XPath chars
    }

    /**
     * Get a human-readable description of what was detected
     *
     * @param locator The original locator
     * @param detectedType The detected type
     * @return Human-readable description
     */
    public static String getDetectionDescription(String locator, LocatorType detectedType) {
        return String.format("Auto-detected '%s' as %s locator", locator, detectedType.getDisplayName());
    }

    /**
     * Check if a locator needs healing context (for AI prompts)
     *
     * @param type The locator type
     * @return true if this type benefits from healing context
     */
    public static boolean needsHealingContext(LocatorType type) {
        return type == LocatorType.CSS_SELECTOR ||
               type == LocatorType.XPATH ||
               type == LocatorType.ID ||
               type == LocatorType.NAME;
    }
}