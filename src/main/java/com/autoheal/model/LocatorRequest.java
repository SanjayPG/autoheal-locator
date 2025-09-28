package com.autoheal.model;

import com.autoheal.config.LocatorOptions;
import com.autoheal.core.WebAutomationAdapter;
import org.openqa.selenium.By;

/**
 * Represents a request to locate an element with auto-healing capabilities
 */
public class LocatorRequest {
    private final String originalSelector;
    private final String description;
    private final LocatorOptions options;
    private final WebAutomationAdapter adapter;
    private final ElementContext context;
    private final LocatorType locatorType;
    private final By seleniumBy;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String originalSelector;
        private String description;
        private LocatorOptions options = LocatorOptions.defaultOptions();
        private WebAutomationAdapter adapter;
        private ElementContext context;
        private LocatorType locatorType;
        private By seleniumBy;

        public Builder selector(String selector) {
            this.originalSelector = selector;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder options(LocatorOptions options) {
            this.options = options;
            return this;
        }

        public Builder adapter(WebAutomationAdapter adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder context(ElementContext context) {
            this.context = context;
            return this;
        }

        public Builder locatorType(LocatorType locatorType) {
            this.locatorType = locatorType;
            return this;
        }

        public Builder seleniumBy(By seleniumBy) {
            this.seleniumBy = seleniumBy;
            return this;
        }

        public LocatorRequest build() {
            return new LocatorRequest(originalSelector, description, options, adapter, context, locatorType, seleniumBy);
        }
    }

    private LocatorRequest(String originalSelector, String description, LocatorOptions options,
                           WebAutomationAdapter adapter, ElementContext context, LocatorType locatorType, By seleniumBy) {
        this.originalSelector = originalSelector;
        this.description = description;
        this.options = options;
        this.adapter = adapter;
        this.context = context;
        this.locatorType = locatorType;
        this.seleniumBy = seleniumBy;
    }

    // Getters
    public String getOriginalSelector() {
        return originalSelector;
    }

    public String getDescription() {
        return description;
    }

    public LocatorOptions getOptions() {
        return options;
    }

    public WebAutomationAdapter getAdapter() {
        return adapter;
    }

    public ElementContext getContext() {
        return context;
    }

    public LocatorType getLocatorType() {
        return locatorType;
    }

    public By getSeleniumBy() {
        return seleniumBy;
    }
}