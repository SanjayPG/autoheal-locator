package com.autoheal.util;

import com.microsoft.playwright.Locator;
import com.autoheal.exception.PlaywrightLocatorExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Internal utility to extract JavaScript-style locator strings from native Playwright Locator objects.
 *
 * This enables zero-rewrite migration for existing Playwright projects by allowing
 * AutoHeal to accept native Locator objects and automatically converting them to
 * the JavaScript-style strings that the AI healing system expects.
 *
 * <p><strong>INTERNAL USE ONLY</strong> - This class is not part of the public API
 * and may change without notice. Users should use {@link com.autoheal.AutoHealLocator#find}
 * methods instead.</p>
 *
 * <p>Supported locator types:</p>
 * <ul>
 *   <li>getByRole('role', { name: 'text' })</li>
 *   <li>getByPlaceholder('text')</li>
 *   <li>getByText('text')</li>
 *   <li>getByLabel('text')</li>
 *   <li>getByTestId('id')</li>
 *   <li>getByAltText('text')</li>
 *   <li>getByTitle('text')</li>
 *   <li>CSS selectors</li>
 *   <li>XPath expressions</li>
 * </ul>
 *
 * @author AutoHeal Framework
 * @version 1.0.0
 * @since 1.0.0
 */
public class PlaywrightLocatorConverter {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightLocatorConverter.class);

    // Private constructor to prevent instantiation
    private PlaywrightLocatorConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Extracts JavaScript-style locator string from a Playwright Locator object.
     *
     * This method attempts to extract the internal selector representation from
     * the Playwright Locator and converts it to the JavaScript-style format that
     * AutoHeal's AI healing system expects.
     *
     * @param locator Native Playwright Locator object (must not be null)
     * @return JavaScript-style locator string (e.g., "getByRole('button', { name: 'Login' })")
     * @throws IllegalArgumentException if locator is null
     * @throws PlaywrightLocatorExtractionException if extraction fails
     */
    public static String extractLocatorString(Locator locator) {
        if (locator == null) {
            throw new IllegalArgumentException(
                "Locator cannot be null. Please provide a valid Playwright Locator object.");
        }

        try {
            logger.debug("Attempting to extract locator string from: {}", locator.getClass().getName());

            // Method 1: Try toString() - Playwright often includes the selector
            String toString = locator.toString();
            if (toString != null && toString.contains("Locator@")) {
                String extracted = extractFromToString(toString);
                if (extracted != null) {
                    String normalized = normalizePlaywrightSelector(extracted);
                    logger.debug("Successfully extracted via toString(): {}", normalized);
                    return normalized;
                }
            }

            // Method 2: Try reflection to access internal fields
            String selector = extractViaReflection(locator);
            if (selector != null && !selector.isEmpty()) {
                String normalized = normalizePlaywrightSelector(selector);
                logger.debug("Successfully extracted via reflection: {}", normalized);
                return normalized;
            }

            // Method 3: Failed to extract
            throw new PlaywrightLocatorExtractionException(
                "Unable to extract locator string from Playwright Locator. " +
                "This may occur with complex chained locators or unsupported locator types. " +
                "Recommendation: Use JavaScript-style string format instead: " +
                "autoHeal.find(page, \"getByRole('button', { name: 'Text' })\", \"description\"). " +
                "Locator type: " + locator.getClass().getName()
            );

        } catch (PlaywrightLocatorExtractionException e) {
            throw e; // Re-throw our custom exception
        } catch (Exception e) {
            logger.error("Unexpected error extracting locator: {}", e.getMessage(), e);
            throw new PlaywrightLocatorExtractionException(
                "Failed to extract locator from Playwright Locator object. " +
                "Locator type: " + locator.getClass().getName() + ". " +
                "Consider using JavaScript-style string format instead. " +
                "Error: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Attempts to extract selector from toString() representation
     */
    private static String extractFromToString(String toString) {
        // Extract: "Locator@abc[selector='...']"
        int start = toString.indexOf("selector='");
        if (start != -1) {
            start += 10; // Length of "selector='"
            int end = toString.indexOf("'", start);
            if (end != -1) {
                return toString.substring(start, end);
            }
        }

        // Try with double quotes: selector="..."
        start = toString.indexOf("selector=\"");
        if (start != -1) {
            start += 10; // Length of "selector=\""
            int end = toString.indexOf("\"", start);
            if (end != -1) {
                return toString.substring(start, end);
            }
        }

        return null;
    }

    /**
     * Attempts to extract selector using reflection on the Locator's internal fields
     */
    private static String extractViaReflection(Locator locator) {
        try {
            Class<?> locatorClass = locator.getClass();

            // Try common field names
            String[] fieldNames = {"selector", "_selector", "expression", "script", "selectorString"};

            for (String fieldName : fieldNames) {
                Field field = findField(locatorClass, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(locator);
                    if (value instanceof String) {
                        return (String) value;
                    }
                }
            }

            // Try getter methods
            String[] methodNames = {"getSelector", "selector", "getExpression", "expression", "toString"};

            for (String methodName : methodNames) {
                try {
                    Method method = locatorClass.getMethod(methodName);
                    method.setAccessible(true);
                    Object value = method.invoke(locator);
                    if (value instanceof String) {
                        String strValue = (String) value;
                        // Avoid infinite loop with toString
                        if (!strValue.contains("Locator@") || methodName.equals("toString")) {
                            return strValue;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next method
                }
            }

            return null;

        } catch (Exception e) {
            logger.warn("Reflection-based extraction failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Recursively search for a field in class hierarchy
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Normalize Playwright selector format to AutoHeal's expected JavaScript-style format
     *
     * Converts Playwright internal format to JavaScript format:
     * - role=button[name="Login"] → getByRole('button', { name: 'Login' })
     * - text=Submit → getByText('Submit')
     * - [placeholder="Username"] → getByPlaceholder('Username')
     */
    private static String normalizePlaywrightSelector(String selector) {
        if (selector == null || selector.isEmpty()) {
            throw new PlaywrightLocatorExtractionException("Extracted selector is empty");
        }

        // Strip "internal:" prefix if present (Playwright's internal format)
        if (selector.startsWith("internal:")) {
            selector = selector.substring(9); // Remove "internal:"
        }

        // Unwrap locator("...") wrapper if present
        // Sometimes Playwright wraps selectors like: locator("getByRole('button', { name: 'X' })")
        if (selector.startsWith("locator(\"") && selector.endsWith("\")")) {
            selector = selector.substring(9, selector.length() - 2); // Extract content between locator(" and ")
        } else if (selector.startsWith("locator('") && selector.endsWith("')")) {
            selector = selector.substring(9, selector.length() - 2); // Extract content between locator(' and ')
        }

        // Unwrap getByRole/getByText/etc wrappers that contain internal chained selectors
        // Example: getByRole('listitem >> internal:has-text="Product 1"i >> internal:role=button')
        // This happens when toString() returns Playwright's internal representation wrapped in a getBy* call
        if (selector.startsWith("getBy")) {
            // Check if it contains >> which indicates it's actually an internal chained selector
            // wrapped incorrectly by toString()
            String unwrapped = unwrapGetByWrapper(selector);
            if (unwrapped != null && unwrapped.contains(" >> ")) {
                // It's a chained selector that was incorrectly wrapped
                selector = unwrapped;
            } else if (unwrapped == null) {
                // It's a proper JavaScript format getBy call, return as-is
                return selector;
            } else {
                // Single selector that was wrapped, use the unwrapped version
                selector = unwrapped;
            }
        }

        // Check for >> operator (indicates filters or chained locators)
        // Example: role=listitem >> internal:has-text="Product 2"i
        if (selector.contains(" >> ")) {
            return convertChainedLocator(selector);
        }

        // Already in JavaScript format (and not wrapped)
        if (selector.startsWith("getBy")) {
            return selector;
        }

        // Convert internal Playwright format to JavaScript format

        // role=button[name="Login"] → getByRole('button', { name: 'Login' })
        if (selector.startsWith("role=")) {
            return convertRoleSelector(selector);
        }

        // text=Submit → getByText('Submit')
        // text="Welcome"s → getByText('Welcome', { exact: true })
        // text=/pattern/i → getByText(/pattern/i)
        if (selector.startsWith("text=")) {
            return convertTextSelector(selector);
        }

        // [placeholder="Username"] → getByPlaceholder('Username')
        if (selector.contains("placeholder=")) {
            String placeholder = extractAttribute(selector, "placeholder");
            if (placeholder != null) {
                return String.format("getByPlaceholder('%s')", escapeQuotes(placeholder));
            }
        }

        // [aria-label="Email"] → getByLabel('Email')
        if (selector.contains("aria-label=")) {
            String label = extractAttribute(selector, "aria-label");
            if (label != null) {
                return String.format("getByLabel('%s')", escapeQuotes(label));
            }
        }

        // [data-testid="submit"] → getByTestId('submit')
        if (selector.contains("data-testid=")) {
            String testId = extractAttribute(selector, "data-testid");
            if (testId != null) {
                return String.format("getByTestId('%s')", escapeQuotes(testId));
            }
        }

        // [alt="Logo"] → getByAltText('Logo')
        if (selector.contains("alt=")) {
            String alt = extractAttribute(selector, "alt");
            if (alt != null) {
                return String.format("getByAltText('%s')", escapeQuotes(alt));
            }
        }

        // [title="Close"] → getByTitle('Close')
        if (selector.contains("title=")) {
            String title = extractAttribute(selector, "title");
            if (title != null) {
                return String.format("getByTitle('%s')", escapeQuotes(title));
            }
        }

        // XPath
        if (selector.startsWith("xpath=") || selector.startsWith("//")) {
            return selector;
        }

        // CSS selector (default) - return as-is
        return selector;
    }

    /**
     * Convert role=button[name="Login"] to getByRole('button', { name: 'Login' })
     * Also handles regex patterns: role=button[name=/submit/i] -> getByRole('button', { name: /submit/i })
     */
    private static String convertRoleSelector(String selector) {
        // Extract: role=button[name="Login"] or role=button[name=/submit/i]
        String roleAndOptions = selector.substring(5); // Remove "role="

        int bracketIndex = roleAndOptions.indexOf("[");
        if (bracketIndex == -1) {
            // Simple role without options: role=button
            return String.format("getByRole('%s')", roleAndOptions.trim());
        }

        String role = roleAndOptions.substring(0, bracketIndex).trim();
        String options = roleAndOptions.substring(bracketIndex);

        // Check if it's a regex pattern (name=/pattern/flags)
        String namePattern = extractRegexPattern(options, "name");
        if (namePattern != null) {
            return String.format("getByRole('%s', { name: %s })",
                escapeQuotes(role), namePattern);
        }

        // Extract regular name from [name="Login"]
        String name = extractAttribute(options, "name");

        if (name != null) {
            return String.format("getByRole('%s', { name: '%s' })",
                escapeQuotes(role), escapeQuotes(name));
        }

        return String.format("getByRole('%s')", escapeQuotes(role));
    }

    /**
     * Convert text="Welcome"i to getByText('Welcome')
     * Also handles exact option: text="Welcome"s -> getByText('Welcome', { exact: true })
     * Also handles regex patterns: text=/pattern/i -> getByText(/pattern/i)
     */
    private static String convertTextSelector(String selector) {
        // Extract: text="Welcome"i or text="Welcome"s or text=/pattern/i
        String textAndOptions = selector.substring(5); // Remove "text="

        // Check if it's a regex pattern (text=/pattern/flags)
        if (textAndOptions.startsWith("/")) {
            // Extract regex pattern with flags
            int lastSlash = textAndOptions.lastIndexOf('/');
            if (lastSlash > 0) {
                String pattern = textAndOptions.substring(1, lastSlash);
                String flags = textAndOptions.substring(lastSlash + 1);
                return String.format("getByText(/%s/%s)", pattern, flags);
            }
        }

        // Extract quoted text: text="Welcome"s or text="Welcome"i
        char quoteChar = textAndOptions.charAt(0);
        if (quoteChar == '"' || quoteChar == '\'') {
            int endQuote = textAndOptions.lastIndexOf(quoteChar);
            if (endQuote > 0) {
                String text = textAndOptions.substring(1, endQuote);
                String flags = textAndOptions.substring(endQuote + 1);

                // Check for 's' flag (exact match)
                if (flags.contains("s")) {
                    return String.format("getByText('%s', { exact: true })", escapeQuotes(text));
                } else {
                    // Default or 'i' flag (case-insensitive substring match)
                    return String.format("getByText('%s')", escapeQuotes(text));
                }
            }
        }

        // Fallback: treat as plain text
        return String.format("getByText('%s')", escapeQuotes(textAndOptions));
    }

    /**
     * Extract regex pattern: [name=/submit/i] → "/submit/i"
     * Returns null if not a regex pattern
     */
    private static String extractRegexPattern(String selector, String attributeName) {
        // Look for: name=/pattern/flags
        String pattern = attributeName + "=/";
        int start = selector.indexOf(pattern);

        if (start == -1) {
            return null; // Not a regex pattern
        }

        start += attributeName.length() + 1; // Move to start of pattern (after "name=/")

        // Find the closing / and any flags after it
        int end = selector.indexOf("/", start);
        if (end == -1) {
            return null; // Invalid regex format
        }

        // Extract pattern
        String regexPattern = selector.substring(start, end);

        // Extract flags (i, g, m, etc.) - they appear after the closing /
        StringBuilder flags = new StringBuilder();
        int flagPos = end + 1;
        while (flagPos < selector.length()) {
            char ch = selector.charAt(flagPos);
            if (ch == ']' || ch == ' ') {
                break; // End of flags
            }
            flags.append(ch);
            flagPos++;
        }

        // Return as JavaScript regex literal: /pattern/flags
        return "/" + regexPattern + "/" + flags.toString();
    }

    /**
     * Extract attribute value: [placeholder="Username"] → "Username"
     * Also handles case-insensitive flags: [name="Submit"i] → "Submit"
     */
    private static String extractAttribute(String selector, String attributeName) {
        // Try double quotes
        String pattern = attributeName + "=\"";
        int start = selector.indexOf(pattern);
        char quoteChar = '"';

        // Try single quotes if not found
        if (start == -1) {
            pattern = attributeName + "='";
            start = selector.indexOf(pattern);
            quoteChar = '\'';
        }

        if (start != -1) {
            start += pattern.length();
            int end = selector.indexOf(quoteChar, start);
            if (end != -1) {
                String value = selector.substring(start, end);
                // Strip case-insensitive flag if present (e.g., "Submit"i -> "Submit")
                // The flag appears after the closing quote, so we don't need to handle it here
                return value;
            }
        }

        return null;
    }

    /**
     * Convert chained locator with filters
     * Example: role=listitem >> internal:has-text="Product 2"i >> internal:role=button[name="Add to cart"i]
     * Output: getByRole('listitem').filter({ hasText: 'Product 2' }).getByRole('button', { name: 'Add to cart' })
     */
    private static String convertChainedLocator(String selector) {
        String[] segments = selector.split(" >> ");

        // First segment is the base locator
        String baseLocator = normalizeSegment(segments[0]);

        // Remaining segments are filters or child locators
        StringBuilder result = new StringBuilder(baseLocator);

        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i].trim();

            // Remove "internal:" prefix if present
            if (segment.startsWith("internal:")) {
                segment = segment.substring(9);
            }

            // Check if it's a filter
            if (segment.startsWith("has-text=")) {
                result.append(convertHasTextFilter(segment));
            } else if (segment.startsWith("has-not-text=")) {
                result.append(convertHasNotTextFilter(segment));
            } else if (segment.startsWith("has=")) {
                // Phase 2: nested locator filter - for now, just note it
                logger.warn("Nested 'has' filter detected but not yet fully supported: {}", segment);
                result.append(".filter({ has: '").append(segment.substring(4)).append("' })");
            } else if (segment.startsWith("has-not=")) {
                // Phase 2: nested locator filter - for now, just note it
                logger.warn("Nested 'hasNot' filter detected but not yet fully supported: {}", segment);
                result.append(".filter({ hasNot: '").append(segment.substring(8)).append("' })");
            } else {
                // It's a child locator (like getByRole after filter)
                String childLocator = normalizeSegment(segment);
                // Extract just the method call (remove "page." prefix if present)
                if (childLocator.startsWith("page.")) {
                    childLocator = childLocator.substring(5); // Remove "page."
                }
                result.append(".").append(childLocator);
            }
        }

        return result.toString();
    }

    /**
     * Normalize a single segment (could be a base locator or child locator)
     * Reuses the existing conversion logic by recursively calling normalizePlaywrightSelector
     * but with the >> check disabled (since we've already split by >>)
     */
    private static String normalizeSegment(String segment) {
        segment = segment.trim();

        // Remove "internal:" prefix if present
        if (segment.startsWith("internal:")) {
            segment = segment.substring(9);
        }

        // Use existing conversion logic for different locator types
        if (segment.startsWith("role=")) {
            return convertRoleSelector(segment);
        } else if (segment.startsWith("text=")) {
            return convertTextSelector(segment);
        } else if (segment.contains("placeholder=")) {
            String placeholder = extractAttribute(segment, "placeholder");
            if (placeholder != null) {
                return String.format("getByPlaceholder('%s')", escapeQuotes(placeholder));
            }
        } else if (segment.contains("aria-label=")) {
            String label = extractAttribute(segment, "aria-label");
            if (label != null) {
                return String.format("getByLabel('%s')", escapeQuotes(label));
            }
        } else if (segment.contains("data-testid=")) {
            String testId = extractAttribute(segment, "data-testid");
            if (testId != null) {
                return String.format("getByTestId('%s')", escapeQuotes(testId));
            }
        } else if (segment.contains("alt=")) {
            String alt = extractAttribute(segment, "alt");
            if (alt != null) {
                return String.format("getByAltText('%s')", escapeQuotes(alt));
            }
        } else if (segment.contains("title=")) {
            String title = extractAttribute(segment, "title");
            if (title != null) {
                return String.format("getByTitle('%s')", escapeQuotes(title));
            }
        } else if (segment.startsWith("xpath=") || segment.startsWith("//")) {
            return segment;
        }

        // CSS selector (default) - return as-is
        return segment;
    }

    /**
     * Convert has-text filter
     * Examples:
     *   has-text="Product 2"i → .filter({ hasText: 'Product 2' })
     *   has-text=/product 2/i → .filter({ hasText: /product 2/i })
     */
    private static String convertHasTextFilter(String filter) {
        // Remove "has-text=" prefix
        String textPart = filter.substring(9);

        // Check if it's a regex pattern (starts with /)
        if (textPart.startsWith("/")) {
            int lastSlash = textPart.lastIndexOf('/');
            if (lastSlash > 0) {
                String pattern = textPart.substring(1, lastSlash);
                String flags = textPart.substring(lastSlash + 1);
                return String.format(".filter({ hasText: /%s/%s })", pattern, flags);
            }
        }

        // Extract quoted text: "Product 2"i or 'Product 2'i
        char quoteChar = textPart.charAt(0);
        if (quoteChar == '"' || quoteChar == '\'') {
            int endQuote = textPart.lastIndexOf(quoteChar);
            if (endQuote > 0) {
                String text = textPart.substring(1, endQuote);
                // Ignore flags after closing quote (they're for Playwright's internal use)
                return String.format(".filter({ hasText: '%s' })", escapeQuotes(text));
            }
        }

        // Fallback
        return String.format(".filter({ hasText: '%s' })", escapeQuotes(textPart));
    }

    /**
     * Convert has-not-text filter
     * Examples:
     *   has-not-text="Out of stock"i → .filter({ hasNotText: 'Out of stock' })
     *   has-not-text=/out of stock/i → .filter({ hasNotText: /out of stock/i })
     */
    private static String convertHasNotTextFilter(String filter) {
        // Remove "has-not-text=" prefix
        String textPart = filter.substring(13);

        // Check if it's a regex pattern (starts with /)
        if (textPart.startsWith("/")) {
            int lastSlash = textPart.lastIndexOf('/');
            if (lastSlash > 0) {
                String pattern = textPart.substring(1, lastSlash);
                String flags = textPart.substring(lastSlash + 1);
                return String.format(".filter({ hasNotText: /%s/%s })", pattern, flags);
            }
        }

        // Extract quoted text: "Out of stock"i or 'Out of stock'i
        char quoteChar = textPart.charAt(0);
        if (quoteChar == '"' || quoteChar == '\'') {
            int endQuote = textPart.lastIndexOf(quoteChar);
            if (endQuote > 0) {
                String text = textPart.substring(1, endQuote);
                // Ignore flags after closing quote (they're for Playwright's internal use)
                return String.format(".filter({ hasNotText: '%s' })", escapeQuotes(text));
            }
        }

        // Fallback
        return String.format(".filter({ hasNotText: '%s' })", escapeQuotes(textPart));
    }

    /**
     * Unwrap getBy* wrapper to extract the actual selector
     * Example: getByRole('listitem >> internal:has-text="Product 1"i') → listitem >> internal:has-text="Product 1"i
     * Returns null if it's a proper JavaScript getBy call (no unwrapping needed)
     */
    private static String unwrapGetByWrapper(String selector) {
        // Patterns to match:
        // getByRole('...')
        // getByRole("...")
        // getByText('...')
        // etc.

        String[] patterns = {
            "getByRole\\('", "getByRole\\(\"",
            "getByText\\('", "getByText\\(\"",
            "getByLabel\\('", "getByLabel\\(\"",
            "getByPlaceholder\\('", "getByPlaceholder\\(\"",
            "getByAltText\\('", "getByAltText\\(\"",
            "getByTitle\\('", "getByTitle\\(\"",
            "getByTestId\\('", "getByTestId\\(\""
        };

        for (String pattern : patterns) {
            if (selector.matches(pattern + ".*")) {
                // Extract the content between the quotes
                char quoteChar = pattern.endsWith("\\'") ? '\'' : '"';
                int startQuote = selector.indexOf(quoteChar);
                if (startQuote == -1) continue;

                // Find the matching closing quote
                // Need to handle escaped quotes properly
                int endQuote = findClosingQuote(selector, startQuote + 1, quoteChar);
                if (endQuote == -1) continue;

                String content = selector.substring(startQuote + 1, endQuote);

                // Check if this looks like Playwright's internal format (contains >> or role= etc.)
                // If it's a proper JavaScript format, it won't have these
                if (content.contains(" >> ") ||
                    content.startsWith("role=") ||
                    content.startsWith("text=") ||
                    content.startsWith("internal:")) {
                    return content; // Return unwrapped content
                }

                // It's a proper JavaScript getBy call, return null to indicate no unwrapping needed
                return null;
            }
        }

        // Didn't match any getBy pattern
        return null;
    }

    /**
     * Find the closing quote, handling escaped quotes
     */
    private static int findClosingQuote(String str, int startPos, char quoteChar) {
        for (int i = startPos; i < str.length(); i++) {
            if (str.charAt(i) == quoteChar) {
                // Check if it's escaped
                if (i > 0 && str.charAt(i - 1) == '\\') {
                    continue; // Skip escaped quote
                }
                return i;
            }
        }
        return -1; // Not found
    }

    /**
     * Escape single quotes for JavaScript string literals
     */
    private static String escapeQuotes(String text) {
        if (text == null) return "";
        return text.replace("'", "\\'");
    }

    /**
     * Check if extraction is supported for the given locator
     *
     * @param locator Playwright Locator to test
     * @return true if extraction is likely to succeed, false otherwise
     */
    public static boolean canExtract(Locator locator) {
        if (locator == null) {
            return false;
        }

        try {
            extractLocatorString(locator);
            return true;
        } catch (Exception e) {
            logger.debug("Extraction check failed for locator: {}", e.getMessage());
            return false;
        }
    }
}
