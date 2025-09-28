package com.autoheal.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks AI service costs and usage including actual token consumption
 */
public class CostMetrics {
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong domRequests = new AtomicLong(0);
    private final AtomicLong visualRequests = new AtomicLong(0);
    private final AtomicReference<Double> totalCost = new AtomicReference<>(0.0);
    private final AtomicReference<Double> domCost = new AtomicReference<>(0.0);
    private final AtomicReference<Double> visualCost = new AtomicReference<>(0.0);

    // Token usage tracking
    private final AtomicLong totalTokensUsed = new AtomicLong(0);
    private final AtomicLong domTokensUsed = new AtomicLong(0);
    private final AtomicLong visualTokensUsed = new AtomicLong(0);

    // Cost per token for GPT-4o-mini (as of 2024)
    private static final double COST_PER_INPUT_TOKEN = 0.00015 / 1000;  // $0.15 per 1M input tokens
    private static final double COST_PER_OUTPUT_TOKEN = 0.0006 / 1000;  // $0.60 per 1M output tokens

    // Fallback cost per request when token info unavailable
    private static final double DOM_COST_PER_REQUEST = 0.02; // $0.02 per DOM analysis
    private static final double VISUAL_COST_PER_REQUEST = 0.10; // $0.10 per visual analysis

    public void recordDomRequest() {
        domRequests.incrementAndGet();
        totalRequests.incrementAndGet();

        double newDomCost = domCost.updateAndGet(current -> current + DOM_COST_PER_REQUEST);
        totalCost.updateAndGet(current -> current + DOM_COST_PER_REQUEST);
    }

    public void recordVisualRequest() {
        visualRequests.incrementAndGet();
        totalRequests.incrementAndGet();

        double newVisualCost = visualCost.updateAndGet(current -> current + VISUAL_COST_PER_REQUEST);
        totalCost.updateAndGet(current -> current + VISUAL_COST_PER_REQUEST);
    }

    /**
     * Record DOM request with actual token usage
     */
    public void recordDomRequestWithTokens(long inputTokens, long outputTokens) {
        domRequests.incrementAndGet();
        totalRequests.incrementAndGet();

        long totalTokens = inputTokens + outputTokens;
        domTokensUsed.addAndGet(totalTokens);
        totalTokensUsed.addAndGet(totalTokens);

        double requestCost = calculateTokenCost(inputTokens, outputTokens);
        domCost.updateAndGet(current -> current + requestCost);
        totalCost.updateAndGet(current -> current + requestCost);
    }

    /**
     * Record visual request with actual token usage
     */
    public void recordVisualRequestWithTokens(long inputTokens, long outputTokens) {
        visualRequests.incrementAndGet();
        totalRequests.incrementAndGet();

        long totalTokens = inputTokens + outputTokens;
        visualTokensUsed.addAndGet(totalTokens);
        totalTokensUsed.addAndGet(totalTokens);

        double requestCost = calculateTokenCost(inputTokens, outputTokens);
        visualCost.updateAndGet(current -> current + requestCost);
        totalCost.updateAndGet(current -> current + requestCost);
    }

    private double calculateTokenCost(long inputTokens, long outputTokens) {
        return (inputTokens * COST_PER_INPUT_TOKEN) + (outputTokens * COST_PER_OUTPUT_TOKEN);
    }

    // Getters
    public long getTotalRequests() { return totalRequests.get(); }
    public long getDomRequests() { return domRequests.get(); }
    public long getVisualRequests() { return visualRequests.get(); }
    public double getTotalCost() { return totalCost.get(); }
    public double getDomCost() { return domCost.get(); }
    public double getVisualCost() { return visualCost.get(); }

    // Token getters
    public long getTotalTokensUsed() { return totalTokensUsed.get(); }
    public long getDomTokensUsed() { return domTokensUsed.get(); }
    public long getVisualTokensUsed() { return visualTokensUsed.get(); }
    
    public double getAverageCostPerRequest() {
        long total = getTotalRequests();
        return total > 0 ? getTotalCost() / total : 0.0;
    }

    public double getCostSavingsVsParallel() {
        // Calculate savings compared to always running both DOM and Visual
        long total = getTotalRequests();
        double parallelCost = total * (DOM_COST_PER_REQUEST + VISUAL_COST_PER_REQUEST);
        return parallelCost - getTotalCost();
    }

    public void reset() {
        totalRequests.set(0);
        domRequests.set(0);
        visualRequests.set(0);
        totalCost.set(0.0);
        domCost.set(0.0);
        visualCost.set(0.0);
    }
}