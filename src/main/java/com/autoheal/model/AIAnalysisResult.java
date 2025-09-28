package com.autoheal.model;

import java.util.*;

/**
 * Result of AI analysis containing recommended selectors and confidence scores
 */
public class AIAnalysisResult {
    private final String recommendedSelector;
    private final double confidence;
    private final String reasoning;
    private final List<ElementCandidate> alternatives;
    private final Map<String, Object> metadata;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String recommendedSelector;
        private double confidence = 0.0;
        private String reasoning;
        private List<ElementCandidate> alternatives = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();

        public Builder recommendedSelector(String selector) {
            this.recommendedSelector = selector;
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
            return new AIAnalysisResult(recommendedSelector, confidence, reasoning, alternatives, metadata);
        }
    }

    private AIAnalysisResult(String recommendedSelector, double confidence, String reasoning,
                             List<ElementCandidate> alternatives, Map<String, Object> metadata) {
        this.recommendedSelector = recommendedSelector;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    // Getters
    public String getRecommendedSelector() { return recommendedSelector; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public List<ElementCandidate> getAlternatives() { return alternatives; }
    public Map<String, Object> getMetadata() { return metadata; }
}