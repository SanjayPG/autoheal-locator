package com.autoheal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing a Playwright locator with its type and parameters
 * Now supports filters: .filter({ hasText: 'text' })
 */
public class PlaywrightLocator {

    /**
     * Enum representing Playwright locator types
     */
    public enum Type {
        GET_BY_ROLE,
        GET_BY_LABEL,
        GET_BY_PLACEHOLDER,
        GET_BY_TEXT,
        GET_BY_ALT_TEXT,
        GET_BY_TITLE,
        GET_BY_TEST_ID,
        CSS_SELECTOR,
        XPATH
    }

    private final Type type;
    private final String value;
    private final Map<String, Object> options;
    private final List<LocatorFilter> filters;  // New: Support for chained filters

    /**
     * Create a new PlaywrightLocator
     *
     * @param type The locator type
     * @param value The primary value (e.g., "button" for role, "Username" for label)
     * @param options Additional options (e.g., {name: "Submit"} for role)
     * @param filters List of filters applied to this locator
     */
    public PlaywrightLocator(Type type, String value, Map<String, Object> options, List<LocatorFilter> filters) {
        this.type = type;
        this.value = value;
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
        this.filters = filters != null ? new ArrayList<>(filters) : new ArrayList<>();
    }

    /**
     * Create a new PlaywrightLocator without filters
     *
     * @param type The locator type
     * @param value The primary value
     * @param options Additional options
     */
    public PlaywrightLocator(Type type, String value, Map<String, Object> options) {
        this(type, value, options, null);
    }

    /**
     * Create a new PlaywrightLocator without options or filters
     *
     * @param type The locator type
     * @param value The primary value
     */
    public PlaywrightLocator(Type type, String value) {
        this(type, value, null, null);
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Map<String, Object> getOptions() {
        return new HashMap<>(options);
    }

    public Object getOption(String key) {
        return options.get(key);
    }

    public List<LocatorFilter> getFilters() {
        return new ArrayList<>(filters);
    }

    public boolean hasFilters() {
        return filters != null && !filters.isEmpty();
    }

    /**
     * Convert to Java Playwright code syntax that can be directly used in scripts
     * Now supports filters: page.getByRole(...).filter(...).filter(...)
     *
     * @return Java code representation (e.g., "page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText(\"Product 2\"))")
     */
    public String toSelectorString() {
        // Build base locator string
        String baseLocator = buildBaseLocatorString();

        // Add filters if present
        if (hasFilters()) {
            StringBuilder result = new StringBuilder(baseLocator);
            for (LocatorFilter filter : filters) {
                result.append(filter.toJavaString());
            }
            return result.toString();
        }

        return baseLocator;
    }

    /**
     * Build the base locator string without filters
     */
    private String buildBaseLocatorString() {
        return switch (type) {
            case GET_BY_ROLE -> {
                // Convert role to AriaRole enum (uppercase with underscores)
                String roleEnum = value.toUpperCase().replace(" ", "_");
                String name = (String) options.get("name");
                String isRegex = (String) options.get("isRegex");

                if (name != null) {
                    // Check if it's a regex pattern (starts with / and ends with /flags)
                    if ("true".equals(isRegex) && name.startsWith("/")) {
                        // It's a regex pattern like /submit/i
                        // Convert /pattern/flags to Pattern.compile("pattern", Pattern.FLAGS)
                        String regexStr = convertRegexToJavaPattern(name);
                        yield String.format("page.getByRole(AriaRole.%s, new Page.GetByRoleOptions().setName(%s))",
                                           roleEnum, regexStr);
                    } else {
                        // It's a regular string
                        yield String.format("page.getByRole(AriaRole.%s, new Page.GetByRoleOptions().setName(\"%s\"))",
                                           roleEnum, escapeJavaString(name));
                    }
                } else {
                    yield String.format("page.getByRole(AriaRole.%s)", roleEnum);
                }
            }
            case GET_BY_LABEL -> String.format("page.getByLabel(\"%s\")", escapeJavaString(value));
            case GET_BY_PLACEHOLDER -> String.format("page.getByPlaceholder(\"%s\")", escapeJavaString(value));
            case GET_BY_TEXT -> {
                String isRegex = (String) options.get("isRegex");
                String exact = (String) options.get("exact");

                // Check if it's a regex pattern (starts with / and ends with /flags)
                if ("true".equals(isRegex) && value.startsWith("/")) {
                    // It's a regex pattern like /welcome/i
                    // Convert /pattern/flags to Pattern.compile("pattern", Pattern.FLAGS)
                    String regexStr = convertRegexToJavaPattern(value);
                    yield String.format("page.getByText(%s)", regexStr);
                } else if ("true".equals(exact)) {
                    // It's a string with exact option
                    yield String.format("page.getByText(\"%s\", new Page.GetByTextOptions().setExact(true))",
                                       escapeJavaString(value));
                } else {
                    // It's a regular string (default behavior)
                    yield String.format("page.getByText(\"%s\")", escapeJavaString(value));
                }
            }
            case GET_BY_ALT_TEXT -> String.format("page.getByAltText(\"%s\")", escapeJavaString(value));
            case GET_BY_TITLE -> String.format("page.getByTitle(\"%s\")", escapeJavaString(value));
            case GET_BY_TEST_ID -> String.format("page.getByTestId(\"%s\")", escapeJavaString(value));
            case CSS_SELECTOR -> String.format("page.locator(\"%s\")", escapeJavaString(value));
            case XPATH -> String.format("page.locator(\"%s\")", escapeJavaString(value));
        };
    }

    /**
     * Escape special characters in Java strings
     */
    private String escapeJavaString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Convert JavaScript regex literal to Java Pattern.compile() call
     * Example: /submit/i -> Pattern.compile("submit", Pattern.CASE_INSENSITIVE)
     */
    private String convertRegexToJavaPattern(String regexLiteral) {
        // Parse /pattern/flags format
        if (!regexLiteral.startsWith("/")) {
            return "\"" + escapeJavaString(regexLiteral) + "\""; // Fallback to string
        }

        int lastSlash = regexLiteral.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "\"" + escapeJavaString(regexLiteral) + "\""; // Fallback to string
        }

        String pattern = regexLiteral.substring(1, lastSlash);
        String flags = regexLiteral.substring(lastSlash + 1);

        // Convert flags to Java Pattern constants
        StringBuilder javaFlags = new StringBuilder();
        if (flags.contains("i")) {
            javaFlags.append("Pattern.CASE_INSENSITIVE");
        }
        if (flags.contains("m")) {
            if (javaFlags.length() > 0) javaFlags.append(" | ");
            javaFlags.append("Pattern.MULTILINE");
        }
        if (flags.contains("s")) {
            if (javaFlags.length() > 0) javaFlags.append(" | ");
            javaFlags.append("Pattern.DOTALL");
        }

        if (javaFlags.length() > 0) {
            return String.format("Pattern.compile(\"%s\", %s)", escapeJavaString(pattern), javaFlags.toString());
        } else {
            return String.format("Pattern.compile(\"%s\")", escapeJavaString(pattern));
        }
    }

    @Override
    public String toString() {
        return toSelectorString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaywrightLocator that = (PlaywrightLocator) o;
        return type == that.type &&
                Objects.equals(value, that.value) &&
                Objects.equals(options, that.options) &&
                Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, options, filters);
    }

    // Builder pattern for fluent API
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Type type;
        private String value;
        private Map<String, Object> options = new HashMap<>();
        private List<LocatorFilter> filters = new ArrayList<>();

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder option(String key, Object value) {
            this.options.put(key, value);
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = new HashMap<>(options);
            return this;
        }

        public Builder filter(LocatorFilter filter) {
            this.filters.add(filter);
            return this;
        }

        public Builder filters(List<LocatorFilter> filters) {
            this.filters = new ArrayList<>(filters);
            return this;
        }

        public Builder addFilter(LocatorFilter filter) {
            this.filters.add(filter);
            return this;
        }

        // Convenience methods for common locators
        public Builder byRole(String role) {
            this.type = Type.GET_BY_ROLE;
            this.value = role;
            return this;
        }

        public Builder byRole(String role, String name) {
            this.type = Type.GET_BY_ROLE;
            this.value = role;
            this.options.put("name", name);
            return this;
        }

        public Builder byLabel(String label) {
            this.type = Type.GET_BY_LABEL;
            this.value = label;
            return this;
        }

        public Builder byPlaceholder(String placeholder) {
            this.type = Type.GET_BY_PLACEHOLDER;
            this.value = placeholder;
            return this;
        }

        public Builder byText(String text) {
            this.type = Type.GET_BY_TEXT;
            this.value = text;
            return this;
        }

        public Builder byTestId(String testId) {
            this.type = Type.GET_BY_TEST_ID;
            this.value = testId;
            return this;
        }

        public Builder byAltText(String altText) {
            this.type = Type.GET_BY_ALT_TEXT;
            this.value = altText;
            return this;
        }

        public Builder byTitle(String title) {
            this.type = Type.GET_BY_TITLE;
            this.value = title;
            return this;
        }

        public Builder byCss(String cssSelector) {
            this.type = Type.CSS_SELECTOR;
            this.value = cssSelector;
            return this;
        }

        public Builder cssSelector(String cssSelector) {
            return byCss(cssSelector);
        }

        public Builder xpath(String xpathExpression) {
            this.type = Type.XPATH;
            this.value = xpathExpression;
            return this;
        }

        public PlaywrightLocator build() {
            if (type == null || value == null) {
                throw new IllegalStateException("Type and value must be set");
            }
            return new PlaywrightLocator(type, value, options, filters);
        }
    }
}
