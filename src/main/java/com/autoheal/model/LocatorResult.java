package com.autoheal.model;

import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Represents the result of an element location operation
 */
public class LocatorResult {
    private final WebElement element;
    private final String actualSelector;
    private final LocatorStrategy strategy;
    private final Duration executionTime;
    private final boolean fromCache;
    private final double confidence;
    private final String reasoning;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private WebElement element;
        private String actualSelector;
        private LocatorStrategy strategy;
        private Duration executionTime;
        private boolean fromCache;
        private double confidence = 1.0;
        private String reasoning;

        public Builder element(WebElement element) {
            this.element = element;
            return this;
        }

        public Builder actualSelector(String actualSelector) {
            this.actualSelector = actualSelector;
            return this;
        }

        public Builder strategy(LocatorStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder executionTime(Duration executionTime) {
            this.executionTime = executionTime;
            return this;
        }

        public Builder fromCache(boolean fromCache) {
            this.fromCache = fromCache;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public LocatorResult build() {
            return new LocatorResult(element, actualSelector, strategy, executionTime,
                    fromCache, confidence, reasoning);
        }
    }

    private LocatorResult(WebElement element, String actualSelector, LocatorStrategy strategy,
                          Duration executionTime, boolean fromCache, double confidence, String reasoning) {
        this.element = element;
        this.actualSelector = actualSelector;
        this.strategy = strategy;
        this.executionTime = executionTime;
        this.fromCache = fromCache;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    // Getters
    public WebElement getElement() { return element; }
    public String getActualSelector() { return actualSelector; }
    public LocatorStrategy getStrategy() { return strategy; }
    public Duration getExecutionTime() { return executionTime; }
    public boolean isFromCache() { return fromCache; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
}