package com.autoheal.model;

import java.util.Objects;

/**
 * Represents a filter applied to a Playwright locator
 * Supports: hasText, hasNotText, has (nested locator), hasNot (nested locator)
 */
public class LocatorFilter {

    public enum FilterType {
        HAS_TEXT,           // filter({ hasText: 'text' })
        HAS_NOT_TEXT,       // filter({ hasNotText: 'text' })
        HAS,                // filter({ has: locator }) - nested locator (Phase 2)
        HAS_NOT             // filter({ hasNot: locator }) - nested locator (Phase 2)
    }

    private final FilterType type;
    private final String value;         // Text value or nested locator string
    private final boolean isRegex;      // True if value is a regex pattern like /pattern/flags

    public LocatorFilter(FilterType type, String value, boolean isRegex) {
        this.type = type;
        this.value = value;
        this.isRegex = isRegex;
    }

    public FilterType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean isRegex() {
        return isRegex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocatorFilter that = (LocatorFilter) o;
        return isRegex == that.isRegex &&
                type == that.type &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, isRegex);
    }

    @Override
    public String toString() {
        return "LocatorFilter{type=" + type + ", value='" + value + "', isRegex=" + isRegex + "}";
    }

    /**
     * Convert filter to JavaScript-style format for caching
     * Examples:
     *   - HAS_TEXT("Product") -> filter({ hasText: 'Product' })
     *   - HAS_TEXT("/product/i", true) -> filter({ hasText: /product/i })
     *   - HAS_NOT_TEXT("Out") -> filter({ hasNotText: 'Out' })
     */
    public String toJavaScriptString() {
        String optionName = switch (type) {
            case HAS_TEXT -> "hasText";
            case HAS_NOT_TEXT -> "hasNotText";
            case HAS -> "has";
            case HAS_NOT -> "hasNot";
        };

        String valueStr;
        if (isRegex) {
            // Regex pattern: /pattern/flags
            valueStr = value;
        } else if (type == FilterType.HAS || type == FilterType.HAS_NOT) {
            // Nested locator: don't quote
            valueStr = value;
        } else {
            // Regular text: quote it
            valueStr = "'" + value.replace("'", "\\'") + "'";
        }

        return String.format("filter({ %s: %s })", optionName, valueStr);
    }

    /**
     * Convert filter to Java Playwright code
     * Examples:
     *   - HAS_TEXT("Product") -> .filter(new Locator.FilterOptions().setHasText("Product"))
     *   - HAS_TEXT("/product/i", true) -> .filter(new Locator.FilterOptions().setHasText(Pattern.compile("product", Pattern.CASE_INSENSITIVE)))
     */
    public String toJavaString() {
        String methodName = switch (type) {
            case HAS_TEXT -> "setHasText";
            case HAS_NOT_TEXT -> "setHasNotText";
            case HAS -> "setHas";
            case HAS_NOT -> "setHasNot";
        };

        String valueStr;
        if (isRegex && (type == FilterType.HAS_TEXT || type == FilterType.HAS_NOT_TEXT)) {
            // Convert /pattern/flags to Pattern.compile()
            valueStr = convertRegexToJavaPattern(value);
        } else if (type == FilterType.HAS || type == FilterType.HAS_NOT) {
            // Nested locator: use as-is (will be converted separately)
            valueStr = value;
        } else {
            // Regular text: quote it
            valueStr = "\"" + value.replace("\"", "\\\"") + "\"";
        }

        return String.format(".filter(new Locator.FilterOptions().%s(%s))", methodName, valueStr);
    }

    /**
     * Convert JavaScript regex literal to Java Pattern.compile() call
     * Example: /submit/i -> Pattern.compile("submit", Pattern.CASE_INSENSITIVE)
     */
    private String convertRegexToJavaPattern(String regexLiteral) {
        if (!regexLiteral.startsWith("/")) {
            return "\"" + regexLiteral.replace("\"", "\\\"") + "\"";
        }

        int lastSlash = regexLiteral.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "\"" + regexLiteral.replace("\"", "\\\"") + "\"";
        }

        String pattern = regexLiteral.substring(1, lastSlash);
        String flags = regexLiteral.substring(lastSlash + 1);

        // Escape special characters in pattern
        pattern = pattern.replace("\\", "\\\\").replace("\"", "\\\"");

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
            return String.format("Pattern.compile(\"%s\", %s)", pattern, javaFlags.toString());
        } else {
            return String.format("Pattern.compile(\"%s\")", pattern);
        }
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FilterType type;
        private String value;
        private boolean isRegex = false;

        public Builder type(FilterType type) {
            this.type = type;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder isRegex(boolean isRegex) {
            this.isRegex = isRegex;
            return this;
        }

        public Builder hasText(String text) {
            this.type = FilterType.HAS_TEXT;
            this.value = text;
            return this;
        }

        public Builder hasTextPattern(String pattern) {
            this.type = FilterType.HAS_TEXT;
            this.value = pattern;
            this.isRegex = true;
            return this;
        }

        public Builder hasNotText(String text) {
            this.type = FilterType.HAS_NOT_TEXT;
            this.value = text;
            return this;
        }

        public LocatorFilter build() {
            if (type == null || value == null) {
                throw new IllegalStateException("Type and value must be set");
            }
            return new LocatorFilter(type, value, isRegex);
        }
    }
}
