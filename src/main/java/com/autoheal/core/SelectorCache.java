package com.autoheal.core;

import com.autoheal.model.CachedSelector;
import com.autoheal.metrics.CacheMetrics;

import java.util.Optional;

/**
 * Interface for selector caching implementations
 */
public interface SelectorCache {

    /**
     * Retrieve a cached selector by key
     *
     * @param key The cache key
     * @return Optional containing cached selector if found
     */
    Optional<CachedSelector> get(String key);

    /**
     * Store a selector in the cache
     *
     * @param key The cache key
     * @param selector The selector to cache
     */
    void put(String key, CachedSelector selector);

    /**
     * Update the success rate of a cached selector
     *
     * @param key The cache key
     * @param success Whether the selector was successful
     */
    void updateSuccess(String key, boolean success);

    /**
     * Get cache performance metrics
     *
     * @return Current cache metrics
     */
    CacheMetrics getMetrics();

    /**
     * Remove expired entries from the cache
     */
    void evictExpired();

    /**
     * Clear all entries from the cache
     */
    void clearAll();

    /**
     * Remove a specific entry from the cache
     *
     * @param key The cache key to remove
     * @return true if the entry was removed, false if it didn't exist
     */
    boolean remove(String key);

    /**
     * Get the current size of the cache
     *
     * @return Number of entries in the cache
     */
    long size();
}