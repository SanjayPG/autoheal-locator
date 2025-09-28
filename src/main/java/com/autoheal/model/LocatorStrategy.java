package com.autoheal.model;

/**
 * Enumeration of available element location strategies
 */
public enum LocatorStrategy {
    /**
     * Use the original selector without any AI assistance
     */
    ORIGINAL_SELECTOR,

    /**
     * Use AI to analyze DOM structure and find alternative selectors
     */
    DOM_ANALYSIS,

    /**
     * Use AI to analyze visual screenshots and locate elements
     */
    VISUAL_ANALYSIS,

    /**
     * Combine multiple strategies to find the best result
     */
    HYBRID,

    /**
     * Retrieve selector from cache based on previous successful attempts
     */
    CACHED
}