package com.autoheal.core;

import com.autoheal.metrics.LocatorMetrics;
import com.autoheal.model.LocatorRequest;
import com.autoheal.model.LocatorResult;
import com.autoheal.model.LocatorStrategy;

import java.util.concurrent.CompletableFuture;

/**
 * Enterprise-grade AutoHeal Locator with async processing, circuit breaker,
 * multiple strategies, and advanced caching
 */
public interface ElementLocator {

    /**
     * Locate an element asynchronously using the configured strategy
     *
     * @param request The locator request containing selector, description, and options
     * @return CompletableFuture containing the locate result
     */
    CompletableFuture<LocatorResult> locate(LocatorRequest request);

    /**
     * Check if this locator supports the given strategy
     *
     * @param strategy The locator strategy to check
     * @return true if supported, false otherwise
     */
    boolean supports(LocatorStrategy strategy);

    /**
     * Get performance and usage metrics for this locator
     *
     * @return Current metrics snapshot
     */
    LocatorMetrics getMetrics();
}