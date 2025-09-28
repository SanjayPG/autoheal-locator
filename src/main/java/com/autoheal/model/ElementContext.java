package com.autoheal.model;

import java.util.*;

/**
 * Contextual information about an element's position and relationships
 */
public class ElementContext {
    private final String parentContainer;
    private final Position relativePosition;
    private final List<String> siblingElements;
    private final Map<String, String> attributes;
    private final String textContent;
    private final ElementFingerprint fingerprint;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String parentContainer;
        private Position relativePosition;
        private List<String> siblingElements = new ArrayList<>();
        private Map<String, String> attributes = new HashMap<>();
        private String textContent;
        private ElementFingerprint fingerprint;

        public Builder parentContainer(String parentContainer) {
            this.parentContainer = parentContainer;
            return this;
        }

        public Builder relativePosition(Position position) {
            this.relativePosition = position;
            return this;
        }

        public Builder siblingElements(List<String> siblings) {
            this.siblingElements = new ArrayList<>(siblings);
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = new HashMap<>(attributes);
            return this;
        }

        public Builder textContent(String textContent) {
            this.textContent = textContent;
            return this;
        }

        public Builder fingerprint(ElementFingerprint fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder element(Object element) {
            // Store element reference if needed - for now we ignore it
            // since we're focusing on the contextual information
            return this;
        }

        public Builder pageUrl(String pageUrl) {
            // Store page URL in attributes
            this.attributes.put("pageUrl", pageUrl);
            return this;
        }

        public ElementContext build() {
            return new ElementContext(parentContainer, relativePosition, siblingElements,
                    attributes, textContent, fingerprint);
        }
    }

    private ElementContext(String parentContainer, Position relativePosition,
                           List<String> siblingElements, Map<String, String> attributes,
                           String textContent, ElementFingerprint fingerprint) {
        this.parentContainer = parentContainer;
        this.relativePosition = relativePosition;
        this.siblingElements = Collections.unmodifiableList(new ArrayList<>(siblingElements));
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
        this.textContent = textContent;
        this.fingerprint = fingerprint;
    }

    // Getters
    public String getParentContainer() { return parentContainer; }
    public Position getRelativePosition() { return relativePosition; }
    public List<String> getSiblingElements() { return siblingElements; }
    public Map<String, String> getAttributes() { return attributes; }
    public String getTextContent() { return textContent; }
    public ElementFingerprint getFingerprint() { return fingerprint; }
}