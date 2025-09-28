package com.autoheal.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a cached selector with usage statistics and success tracking
 */
public class CachedSelector {
    private final String selector;
    private final double successRate;
    private final AtomicInteger usageCount;
    private final Instant lastUsed;
    private final Instant createdAt;
    private final ElementFingerprint fingerprint;
    private final AtomicInteger successes;
    private final AtomicInteger attempts;

    public CachedSelector(String selector, ElementFingerprint fingerprint) {
        this.selector = selector;
        this.fingerprint = fingerprint;
        this.createdAt = Instant.now();
        this.lastUsed = Instant.now();
        this.usageCount = new AtomicInteger(0);
        this.successes = new AtomicInteger(1); // Start with 1 success since it just worked during healing
        this.attempts = new AtomicInteger(1);  // Start with 1 attempt
        this.successRate = 1.0;
    }

    /**
     * Record a usage attempt and whether it was successful
     *
     * @param success true if the selector worked, false otherwise
     */
    public void recordUsage(boolean success) {
        this.usageCount.incrementAndGet();
        this.attempts.incrementAndGet();
        if (success) {
            this.successes.incrementAndGet();
        }
    }

    /**
     * Calculate the current success rate based on recorded attempts
     *
     * @return Success rate between 0.0 and 1.0
     */
    public double getCurrentSuccessRate() {
        int totalAttempts = attempts.get();
        return totalAttempts > 0 ? (double) successes.get() / totalAttempts : 0.0;
    }

    // Getters
    public String getSelector() { return selector; }
    public double getSuccessRate() { return successRate; }
    public int getUsageCount() { return usageCount.get(); }
    public Instant getLastUsed() { return lastUsed; }
    public Instant getCreatedAt() { return createdAt; }
    public ElementFingerprint getFingerprint() { return fingerprint; }
    public int getSuccesses() { return successes.get(); }
    public int getAttempts() { return attempts.get(); }
}