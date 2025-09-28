package com.autoheal.model;

import java.util.*;

/**
 * Unique fingerprint for identifying elements across page changes
 */
public class ElementFingerprint {
    private final String parentChain;
    private final Position screenPosition;
    private final Map<String, String> computedStyles;
    private final String textContent;
    private final List<String> nearbyElements;
    private final String visualHash;

    public ElementFingerprint(String parentChain, Position screenPosition,
                              Map<String, String> computedStyles, String textContent,
                              List<String> nearbyElements, String visualHash) {
        this.parentChain = parentChain;
        this.screenPosition = screenPosition;
        this.computedStyles = Collections.unmodifiableMap(new HashMap<>(computedStyles));
        this.textContent = textContent;
        this.nearbyElements = Collections.unmodifiableList(new ArrayList<>(nearbyElements));
        this.visualHash = visualHash;
    }

    // Getters
    public String getParentChain() { return parentChain; }
    public Position getScreenPosition() { return screenPosition; }
    public Map<String, String> getComputedStyles() { return computedStyles; }
    public String getTextContent() { return textContent; }
    public List<String> getNearbyElements() { return nearbyElements; }
    public String getVisualHash() { return visualHash; }

    /**
     * Calculate similarity score between this fingerprint and another
     *
     * @param other The other fingerprint to compare
     * @return Similarity score between 0.0 and 1.0
     */
    public double calculateSimilarity(ElementFingerprint other) {
        double parentSimilarity = calculateStringSimilarity(this.parentChain, other.parentChain);
        double positionSimilarity = calculatePositionSimilarity(this.screenPosition, other.screenPosition);
        double textSimilarity = calculateStringSimilarity(this.textContent, other.textContent);
        double styleSimilarity = calculateStyleSimilarity(this.computedStyles, other.computedStyles);

        return (parentSimilarity * 0.3 + positionSimilarity * 0.2 +
                textSimilarity * 0.3 + styleSimilarity * 0.2);
    }

    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        // Simple similarity calculation - can be enhanced with Levenshtein distance
        return 1.0 - (double) Math.abs(s1.length() - s2.length()) / Math.max(s1.length(), s2.length());
    }

    private double calculatePositionSimilarity(Position p1, Position p2) {
        if (p1 == null || p2 == null) return 0.0;
        double distance = Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
        return Math.max(0.0, 1.0 - distance / 1000.0); // Normalize by max expected distance
    }

    private double calculateStyleSimilarity(Map<String, String> styles1, Map<String, String> styles2) {
        if (styles1.isEmpty() && styles2.isEmpty()) return 1.0;
        Set<String> allKeys = new HashSet<>(styles1.keySet());
        allKeys.addAll(styles2.keySet());

        int matches = 0;
        for (String key : allKeys) {
            if (Objects.equals(styles1.get(key), styles2.get(key))) {
                matches++;
            }
        }
        return (double) matches / allKeys.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tagName;
        private String id;
        private String className;
        private String text;
        private Position position;
        private String parentChain;
        private Map<String, String> computedStyles = new HashMap<>();
        private List<String> nearbyElements = new ArrayList<>();
        private String visualHash;

        public Builder tagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder position(Position position) {
            this.position = position;
            return this;
        }

        public Builder parentChain(String parentChain) {
            this.parentChain = parentChain;
            return this;
        }

        public Builder computedStyles(Map<String, String> styles) {
            this.computedStyles = new HashMap<>(styles);
            return this;
        }

        public Builder nearbyElements(List<String> elements) {
            this.nearbyElements = new ArrayList<>(elements);
            return this;
        }

        public Builder visualHash(String hash) {
            this.visualHash = hash;
            return this;
        }

        public ElementFingerprint build() {
            return new ElementFingerprint(
                parentChain != null ? parentChain : "",
                position,
                computedStyles,
                text != null ? text : "",
                nearbyElements,
                visualHash
            );
        }
    }
}