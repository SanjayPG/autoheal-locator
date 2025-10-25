package com.autoheal.util;

import com.autoheal.model.LocatorFilter;
import com.autoheal.model.PlaywrightLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse Playwright locator strings into PlaywrightLocator objects
 */
public class PlaywrightLocatorParser {
    private static final Logger logger = LoggerFactory.getLogger(PlaywrightLocatorParser.class);

    // Patterns for parsing different locator types
    // Handles both string names and regex patterns:
    // getByRole('button', { name: 'Login' }) or getByRole('button', { name: /submit/i })
    private static final Pattern ROLE_PATTERN = Pattern.compile(
            "getByRole\\(['\"]([^'\"]+)['\"](?:,\\s*\\{\\s*name:\\s*(?:['\"]([^'\"]+)['\"]|(/[^/]+/\\w*))\\s*\\})?\\)"
    );
    private static final Pattern LABEL_PATTERN = Pattern.compile("getByLabel\\(['\"]([^'\"]+)['\"]\\)");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("getByPlaceholder\\(['\"]([^'\"]+)['\"]\\)");
    // Handles both string text, regex patterns, and exact option:
    // getByText('Welcome') or getByText(/pattern/i) or getByText('Welcome', { exact: true })
    private static final Pattern TEXT_PATTERN = Pattern.compile(
            "getByText\\((?:(?:['\"]([^'\"]+)['\"])|(/[^/]+/\\w*))(?:,\\s*\\{\\s*exact:\\s*(true|false)\\s*\\})?\\)"
    );
    private static final Pattern ALT_TEXT_PATTERN = Pattern.compile("getByAltText\\(['\"]([^'\"]+)['\"]\\)");
    private static final Pattern TITLE_PATTERN = Pattern.compile("getByTitle\\(['\"]([^'\"]+)['\"]\\)");
    private static final Pattern TEST_ID_PATTERN = Pattern.compile("getByTestId\\(['\"]([^'\"]+)['\"]\\)");

    // Pattern for parsing filters: .filter({ hasText: 'text' }) or .filter({ hasText: /pattern/i })
    private static final Pattern FILTER_PATTERN = Pattern.compile(
            "\\.filter\\(\\{\\s*(hasText|hasNotText|has|hasNot):\\s*(?:(?:['\"]([^'\"]+)['\"])|(/[^/]+/\\w*))\\s*\\}\\)"
    );

    /**
     * Parse a locator string into a PlaywrightLocator object
     *
     * @param locatorString The string representation of the locator
     * @return PlaywrightLocator object
     * @throws IllegalArgumentException if the locator string cannot be parsed
     */
    public static PlaywrightLocator parse(String locatorString) {
        if (locatorString == null || locatorString.trim().isEmpty()) {
            throw new IllegalArgumentException("Locator string cannot be null or empty");
        }

        String trimmed = locatorString.trim();

        // Unwrap locator("...") wrapper if present
        // Sometimes extracted selectors are wrapped like: locator("getByRole('button', { name: 'X' })")
        if (trimmed.startsWith("locator(\"") && trimmed.endsWith("\")")) {
            String unwrapped = trimmed.substring(9, trimmed.length() - 2);
            logger.debug("Unwrapped locator() wrapper: {} -> {}", trimmed, unwrapped);
            trimmed = unwrapped;
        } else if (trimmed.startsWith("locator('") && trimmed.endsWith("')")) {
            String unwrapped = trimmed.substring(9, trimmed.length() - 2);
            logger.debug("Unwrapped locator() wrapper: {} -> {}", trimmed, unwrapped);
            trimmed = unwrapped;
        }

        // Extract filters before parsing base locator
        // Example: getByRole('listitem').filter({ hasText: 'Product 2' }).filter({ hasNotText: 'Out' })
        List<LocatorFilter> filters = new ArrayList<>();
        Matcher filterMatcher = FILTER_PATTERN.matcher(trimmed);

        while (filterMatcher.find()) {
            String filterType = filterMatcher.group(1);  // hasText, hasNotText, has, hasNot
            String textValue = filterMatcher.group(2);   // String value like 'Product 2'
            String regexValue = filterMatcher.group(3);  // Regex pattern like /product 2/i

            LocatorFilter.FilterType type = switch (filterType) {
                case "hasText" -> LocatorFilter.FilterType.HAS_TEXT;
                case "hasNotText" -> LocatorFilter.FilterType.HAS_NOT_TEXT;
                case "has" -> LocatorFilter.FilterType.HAS;
                case "hasNot" -> LocatorFilter.FilterType.HAS_NOT;
                default -> null;
            };

            if (type != null) {
                String value = textValue != null ? textValue : regexValue;
                boolean isRegex = regexValue != null;
                filters.add(new LocatorFilter(type, value, isRegex));
                logger.debug("Extracted filter: type={}, value={}, isRegex={}", type, value, isRegex);
            }
        }

        // Remove filters from locator string before parsing base locator
        String baseLocatorString = trimmed.replaceAll("\\.filter\\(\\{[^}]+\\}\\)", "");
        logger.debug("Base locator after removing filters: {}", baseLocatorString);
        trimmed = baseLocatorString;

        // Try getByRole
        Matcher roleMatcher = ROLE_PATTERN.matcher(trimmed);
        if (roleMatcher.matches()) {
            String role = roleMatcher.group(1).trim();
            String name = roleMatcher.group(2);        // String name like 'Login'
            String regexName = roleMatcher.group(3);   // Regex pattern like /submit/i

            PlaywrightLocator.Builder builder = PlaywrightLocator.builder()
                    .type(PlaywrightLocator.Type.GET_BY_ROLE)
                    .value(role);

            if (name != null) {
                builder.option("name", name);
                builder.option("isRegex", "false");
            } else if (regexName != null) {
                builder.option("name", regexName);
                builder.option("isRegex", "true");
            }

            // Add filters if present
            builder.filters(filters);

            return builder.build();
        }

        // Try getByLabel
        Matcher labelMatcher = LABEL_PATTERN.matcher(trimmed);
        if (labelMatcher.matches()) {
            return PlaywrightLocator.builder()
                    .byLabel(labelMatcher.group(1))
                    .filters(filters)
                    .build();
        }

        // Try getByPlaceholder
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(trimmed);
        if (placeholderMatcher.matches()) {
            return PlaywrightLocator.builder()
                    .byPlaceholder(placeholderMatcher.group(1))
                    .filters(filters)
                    .build();
        }

        // Try getByText
        Matcher textMatcher = TEXT_PATTERN.matcher(trimmed);
        if (textMatcher.matches()) {
            String text = textMatcher.group(1);         // String text like 'Welcome'
            String regexText = textMatcher.group(2);    // Regex pattern like /welcome/i
            String exact = textMatcher.group(3);        // 'true' or 'false' for exact option

            PlaywrightLocator.Builder builder = PlaywrightLocator.builder()
                    .type(PlaywrightLocator.Type.GET_BY_TEXT);

            if (text != null) {
                builder.value(text);
                builder.option("isRegex", "false");
                if ("true".equals(exact)) {
                    builder.option("exact", "true");
                }
            } else if (regexText != null) {
                builder.value(regexText);
                builder.option("isRegex", "true");
            }

            // Add filters if present
            builder.filters(filters);

            return builder.build();
        }

        // Try getByAltText
        Matcher altTextMatcher = ALT_TEXT_PATTERN.matcher(trimmed);
        if (altTextMatcher.matches()) {
            return PlaywrightLocator.builder()
                    .type(PlaywrightLocator.Type.GET_BY_ALT_TEXT)
                    .value(altTextMatcher.group(1))
                    .filters(filters)
                    .build();
        }

        // Try getByTitle
        Matcher titleMatcher = TITLE_PATTERN.matcher(trimmed);
        if (titleMatcher.matches()) {
            return PlaywrightLocator.builder()
                    .type(PlaywrightLocator.Type.GET_BY_TITLE)
                    .value(titleMatcher.group(1))
                    .filters(filters)
                    .build();
        }

        // Try getByTestId
        Matcher testIdMatcher = TEST_ID_PATTERN.matcher(trimmed);
        if (testIdMatcher.matches()) {
            return PlaywrightLocator.builder()
                    .byTestId(testIdMatcher.group(1))
                    .filters(filters)
                    .build();
        }

        // Check if it's an XPath
        if (trimmed.startsWith("xpath=") || trimmed.startsWith("//") || trimmed.startsWith("(//")) {
            return PlaywrightLocator.builder()
                    .type(PlaywrightLocator.Type.XPATH)
                    .value(trimmed)
                    .filters(filters)
                    .build();
        }

        // Check if it's explicitly marked as CSS
        if (trimmed.startsWith("css:")) {
            return PlaywrightLocator.builder()
                    .byCss(trimmed.substring(4).trim())
                    .filters(filters)
                    .build();
        }

        // Default: treat as CSS selector
        logger.debug("Treating locator as CSS selector: {}", trimmed);
        return PlaywrightLocator.builder()
                .byCss(trimmed)
                .filters(filters)
                .build();
    }

    /**
     * Check if a string looks like a Playwright user-facing locator
     *
     * @param locatorString The locator string to check
     * @return true if it's a Playwright-style locator
     */
    public static boolean isPlaywrightStyleLocator(String locatorString) {
        if (locatorString == null || locatorString.trim().isEmpty()) {
            return false;
        }

        String trimmed = locatorString.trim();
        return trimmed.startsWith("getByRole(") ||
                trimmed.startsWith("getByLabel(") ||
                trimmed.startsWith("getByPlaceholder(") ||
                trimmed.startsWith("getByText(") ||
                trimmed.startsWith("getByAltText(") ||
                trimmed.startsWith("getByTitle(") ||
                trimmed.startsWith("getByTestId(");
    }

    /**
     * Extract the locator type from a string
     *
     * @param locatorString The locator string
     * @return The locator type, or CSS_SELECTOR if unclear
     */
    public static PlaywrightLocator.Type extractType(String locatorString) {
        if (locatorString == null || locatorString.trim().isEmpty()) {
            return PlaywrightLocator.Type.CSS_SELECTOR;
        }

        String trimmed = locatorString.trim();

        if (trimmed.startsWith("getByRole(")) return PlaywrightLocator.Type.GET_BY_ROLE;
        if (trimmed.startsWith("getByLabel(")) return PlaywrightLocator.Type.GET_BY_LABEL;
        if (trimmed.startsWith("getByPlaceholder(")) return PlaywrightLocator.Type.GET_BY_PLACEHOLDER;
        if (trimmed.startsWith("getByText(")) return PlaywrightLocator.Type.GET_BY_TEXT;
        if (trimmed.startsWith("getByAltText(")) return PlaywrightLocator.Type.GET_BY_ALT_TEXT;
        if (trimmed.startsWith("getByTitle(")) return PlaywrightLocator.Type.GET_BY_TITLE;
        if (trimmed.startsWith("getByTestId(")) return PlaywrightLocator.Type.GET_BY_TEST_ID;
        if (trimmed.startsWith("xpath=") || trimmed.startsWith("//")) return PlaywrightLocator.Type.XPATH;

        return PlaywrightLocator.Type.CSS_SELECTOR;
    }

    /**
     * Generate a cache key from a locator string and description
     *
     * @param locatorString The locator string
     * @param description The description
     * @return Cache key string
     */
    public static String generateCacheKey(String locatorString, String description) {
        PlaywrightLocator.Type type = extractType(locatorString);
        String descHash = Integer.toHexString(description.hashCode());

        // Use the full locator string to ensure uniqueness
        // (e.g., getByRole('button', {name: 'A'}) vs getByRole('button', {name: 'B'}) are different)
        return String.format("playwright:%s:%s:%s",
                type.name().toLowerCase(),
                locatorString,
                descHash);
    }
}
