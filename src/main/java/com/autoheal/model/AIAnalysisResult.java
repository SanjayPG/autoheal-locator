package com.autoheal.model;

import java.util.*;

/**
 * Result of AI analysis containing recommended selectors and confidence scores
 * Supports both Selenium (CSS/XPath strings) and Playwright (user-facing locators)
 */
public class AIAnalysisResult {
    private final String recommendedSelector;
    private final PlaywrightLocator playwrightLocator;
    private final AutomationFramework targetFramework;
    private final double confidence;
    private final String reasoning;
    private final List<ElementCandidate> alternatives;
    private final Map<String, Object> metadata;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String recommendedSelector;
        private PlaywrightLocator playwrightLocator;
        private AutomationFramework targetFramework = AutomationFramework.SELENIUM;
        private double confidence = 0.0;
        private String reasoning;
        private List<ElementCandidate> alternatives = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();

        public Builder recommendedSelector(String selector) {
            this.recommendedSelector = selector;
            return this;
        }

        public Builder playwrightLocator(PlaywrightLocator locator) {
            this.playwrightLocator = locator;
            return this;
        }

        public Builder targetFramework(AutomationFramework framework) {
            this.targetFramework = framework;
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

        public Builder alternatives(List<ElementCandidate> alternatives) {
            this.alternatives = new ArrayList<>(alternatives);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public AIAnalysisResult build() {
            return new AIAnalysisResult(recommendedSelector, playwrightLocator, targetFramework,
                    confidence, reasoning, alternatives, metadata);
        }
    }

    private AIAnalysisResult(String recommendedSelector, PlaywrightLocator playwrightLocator,
                             AutomationFramework targetFramework, double confidence, String reasoning,
                             List<ElementCandidate> alternatives, Map<String, Object> metadata) {
        this.recommendedSelector = recommendedSelector;
        this.playwrightLocator = playwrightLocator;
        this.targetFramework = targetFramework;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    // Getters
    public String getRecommendedSelector() { return recommendedSelector; }
    public PlaywrightLocator getPlaywrightLocator() { return playwrightLocator; }
    public AutomationFramework getTargetFramework() { return targetFramework; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public List<ElementCandidate> getAlternatives() { return alternatives; }
    public Map<String, Object> getMetadata() { return metadata; }

    /**
     * Check if this result is for Playwright framework
     *
     * @return true if targetFramework is PLAYWRIGHT
     */
    public boolean isPlaywright() {
        return targetFramework == AutomationFramework.PLAYWRIGHT;
    }

    /**
     * Check if this result is for Selenium framework
     *
     * @return true if targetFramework is SELENIUM
     */
    public boolean isSelenium() {
        return targetFramework == AutomationFramework.SELENIUM;
    }
}