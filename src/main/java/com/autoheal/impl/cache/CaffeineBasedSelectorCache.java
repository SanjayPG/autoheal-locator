package com.autoheal.impl.cache;

import com.autoheal.config.CacheConfig;
import com.autoheal.core.SelectorCache;
import com.autoheal.metrics.CacheMetrics;
import com.autoheal.model.CachedSelector;
import com.autoheal.model.ElementContext;
import com.autoheal.model.Position;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Enterprise-grade cache implementation using Caffeine
 */
public class CaffeineBasedSelectorCache implements SelectorCache {
    private static final Logger logger = LoggerFactory.getLogger(CaffeineBasedSelectorCache.class);

    private final Cache<String, CachedSelector> cache;
    private final CacheMetrics metrics;

    public CaffeineBasedSelectorCache(CacheConfig config) {
        this.metrics = new CacheMetrics();
        this.cache = Caffeine.newBuilder()
                .maximumSize(config.getMaximumSize())
                .expireAfterWrite(config.getExpireAfterWrite())
                .expireAfterAccess(config.getExpireAfterAccess())
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        metrics.recordEviction();
                        logger.debug("Cache entry evicted: {} (cause: {})", key, cause);
                    }
                })
                .build();

        logger.info("CaffeineBasedSelectorCache initialized with max size: {}, expireAfterWrite: {}",
                config.getMaximumSize(), config.getExpireAfterWrite());
    }

    public CaffeineBasedSelectorCache(CacheConfig cacheConfig, Cache<String, CachedSelector> cache, CacheMetrics metrics) {
        this.cache = cache;
        this.metrics = metrics;
    }

    @Override
    public Optional<CachedSelector> get(String key) {
        long startTime = System.currentTimeMillis();
        CachedSelector result = cache.getIfPresent(key);

        if (result != null) {
            metrics.recordHit();
            logger.debug("Cache hit for key: {}", key);
        } else {
            metrics.recordMiss();
            logger.debug("Cache miss for key: {}", key);
        }

        return Optional.ofNullable(result);
    }

    @Override
    public void put(String key, CachedSelector selector) {
        long startTime = System.currentTimeMillis();
        cache.put(key, selector);
        metrics.recordLoad(System.currentTimeMillis() - startTime);
        logger.debug("Cached selector for key: {}", key);
    }

    @Override
    public void updateSuccess(String key, boolean success) {
        CachedSelector cached = cache.getIfPresent(key);
        if (cached != null) {
            cached.recordUsage(success);
            logger.debug("Updated success rate for key: {} (success: {})", key, success);
        }
    }

    @Override
    public CacheMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void evictExpired() {
        cache.cleanUp();
        logger.debug("Evicted expired cache entries");
    }

    @Override
    public void clearAll() {
        long sizeBefore = cache.estimatedSize();
        cache.invalidateAll();
        cache.cleanUp();
        metrics.recordEviction(); // Record as eviction for metrics
        System.out.println("[CACHE-DEBUG] Cache cleared: " + sizeBefore + " entries removed");
        logger.info("Cache cleared completely: {} entries removed", sizeBefore);
    }

    @Override
    public boolean remove(String key) {
        CachedSelector existing = cache.getIfPresent(key);
        if (existing != null) {
            cache.invalidate(key);
            cache.cleanUp();
            metrics.recordEviction(); // Record as eviction for metrics
            System.out.println("[CACHE-DEBUG] Cache entry removed: " + key);
            logger.debug("Cache entry removed: {}", key);
            return true;
        }
        logger.debug("Attempted to remove non-existent cache entry: {}", key);
        return false;
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * Generate a contextual cache key that includes element context
     *
     * @param originalSelector the original CSS selector
     * @param description human-readable element description
     * @param context element context information
     * @return contextual cache key
     */
    public String generateContextualKey(String originalSelector, String description, ElementContext context) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(originalSelector).append("|").append(description);

        if (context != null) {
            if (context.getParentContainer() != null) {
                keyBuilder.append("|parent:").append(context.getParentContainer());
            }
            if (context.getRelativePosition() != null) {
                Position pos = context.getRelativePosition();
                keyBuilder.append("|pos:").append(pos.getX()).append(",").append(pos.getY());
            }
            if (!context.getSiblingElements().isEmpty()) {
                keyBuilder.append("|siblings:").append(String.join(",", context.getSiblingElements()));
            }
        }

        return keyBuilder.toString();
    }

    /**
     * Get the underlying Caffeine cache for advanced operations
     *
     * @return the Caffeine cache instance
     */
    public Cache<String, CachedSelector> getUnderlyingCache() {
        return cache;
    }
}