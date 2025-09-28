package com.autoheal.model;

/**
 * Execution strategy for healing locators to optimize cost and performance
 */
public enum ExecutionStrategy {
    /**
     * Run locators sequentially, stopping at first success
     * Cost: Lowest (only pays for successful strategy)
     * Speed: Slower (sequential execution)
     */
    SEQUENTIAL,

    /**
     * Run all locators in parallel, select best result
     * Cost: Highest (pays for all strategies)
     * Speed: Fastest (parallel execution)
     */
    PARALLEL,

    /**
     * Smart strategy: Try DOM first, then Visual if DOM fails
     * Cost: Medium (DOM is cheaper, Visual only if needed)
     * Speed: Medium (optimized sequential)
     */
    SMART_SEQUENTIAL,

    /**
     * Cost-optimized: Only use DOM analysis, skip visual
     * Cost: Lowest (DOM only)
     * Speed: Fast (single strategy)
     */
    DOM_ONLY,

    /**
     * Visual-first: Try visual first, then DOM if visual fails
     * Cost: High (Visual is expensive)
     * Speed: Medium (visual analysis first)
     */
    VISUAL_FIRST
}