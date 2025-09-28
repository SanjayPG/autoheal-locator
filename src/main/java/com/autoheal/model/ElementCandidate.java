package com.autoheal.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a candidate element with selector and confidence information
 */
public class ElementCandidate {
    private final String selector;
    private final double confidence;
    private final String description;
    private final ElementContext context;
    private final Map<String, Object> properties;

    public ElementCandidate(String selector, double confidence, String description,
                            ElementContext context, Map<String, Object> properties) {
        this.selector = selector;
        this.confidence = confidence;
        this.description = description;
        this.context = context;
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
    }

    // Getters
    public String getSelector() { return selector; }
    public double getConfidence() { return confidence; }
    public String getDescription() { return description; }
    public ElementContext getContext() { return context; }
    public Map<String, Object> getProperties() { return properties; }
}