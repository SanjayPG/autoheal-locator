package com.autoheal.impl.cache;

import com.autoheal.config.CacheConfig;
import com.autoheal.core.SelectorCache;
import com.autoheal.metrics.CacheMetrics;
import com.autoheal.model.CachedSelector;
import com.autoheal.model.ElementContext;
import com.autoheal.model.ElementFingerprint;
import com.autoheal.model.Position;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-based persistent cache implementation for AutoHeal selectors
 * Provides persistent storage that survives application restarts using JSON files
 */
public class PersistentFileSelectorCache implements SelectorCache {
    private static final Logger logger = LoggerFactory.getLogger(PersistentFileSelectorCache.class);

    private static final String DEFAULT_CACHE_DIR = System.getProperty("user.home") + "/.autoheal/cache";
    private static final String CACHE_FILE = "selector-cache.json";
    private static final String METRICS_FILE = "cache-metrics.json";

    private final Cache<String, CachedSelector> memoryCache;
    private final CacheMetrics metrics;
    private final ObjectMapper objectMapper;
    private final CacheConfig config;
    private final String cacheDirectory;
    private final String cacheFilePath;
    private final String metricsFilePath;

    // In-memory backup for metrics
    private final Map<String, FileCacheMetrics> fileMetrics = new ConcurrentHashMap<>();

    /**
     * File-based cache metrics for persistence
     */
    public static class FileCacheMetrics {
        public int attempts = 0;
        public int successes = 0;
        public Instant lastUsed = Instant.now();
        public long lastAccessTime = System.currentTimeMillis();

        public FileCacheMetrics() {}

        public double getSuccessRate() {
            return attempts > 0 ? (double) successes / attempts : 0.0;
        }

        public boolean isExpired(Duration expireAfterAccess) {
            return System.currentTimeMillis() - lastAccessTime > expireAfterAccess.toMillis();
        }

        public void recordUsage(boolean success) {
            attempts++;
            if (success) successes++;
            lastUsed = Instant.now();
            lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * Serializable cache entry for file persistence
     */
    public static class FileCacheEntry {
        public String selector;
        public double successRate;
        public int usageCount;
        public Instant lastUsed;
        public Instant createdAt;
        public long lastAccessTime;

        public FileCacheEntry() {}

        public FileCacheEntry(CachedSelector cached) {
            this.selector = cached.getSelector();
            this.successRate = cached.getCurrentSuccessRate();
            this.usageCount = cached.getUsageCount();
            this.lastUsed = cached.getLastUsed();
            this.createdAt = cached.getCreatedAt();
            this.lastAccessTime = System.currentTimeMillis();
        }

        public CachedSelector toCachedSelector() {
            // Create a dummy ElementFingerprint for now since we're only storing selector
            // In future, we could enhance this to store and restore the full fingerprint
            Position dummyPosition = new Position(0, 0, 0, 0);
            ElementFingerprint dummyFingerprint = new ElementFingerprint(
                "",
                dummyPosition,
                java.util.Collections.emptyMap(),
                "",
                java.util.Collections.emptyList(),
                ""
            );
            CachedSelector cached = new CachedSelector(selector, dummyFingerprint);

            // Simulate the usage history by recording attempts
            for (int i = 0; i < usageCount; i++) {
                cached.recordUsage(i < (usageCount * successRate));
            }

            return cached;
        }

        public boolean isExpired(Duration expireAfterWrite, Duration expireAfterAccess) {
            long now = System.currentTimeMillis();
            long createdTime = createdAt != null ? createdAt.toEpochMilli() : now;

            // Check write expiry
            if (now - createdTime > expireAfterWrite.toMillis()) {
                return true;
            }

            // Check access expiry
            if (now - lastAccessTime > expireAfterAccess.toMillis()) {
                return true;
            }

            return false;
        }
    }

    public PersistentFileSelectorCache(CacheConfig config) {
        this.config = config;
        this.metrics = new CacheMetrics();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Setup cache directory
        this.cacheDirectory = DEFAULT_CACHE_DIR;
        this.cacheFilePath = cacheDirectory + "/" + CACHE_FILE;
        this.metricsFilePath = cacheDirectory + "/" + METRICS_FILE;

        // Create Caffeine cache for in-memory performance
        this.memoryCache = Caffeine.newBuilder()
                .maximumSize(config.getMaximumSize())
                .expireAfterWrite(config.getExpireAfterWrite())
                .expireAfterAccess(config.getExpireAfterAccess())
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        metrics.recordEviction();
                        logger.debug("Memory cache entry evicted: {} (cause: {})", key, cause);
                    }
                })
                .build();

        // Initialize cache
        createCacheDirectory();
        loadCacheFromFile();
        setupShutdownHook();

        System.out.println("[FILE-CACHE] Initialized persistent file cache at: " + cacheDirectory);
        logger.info("PersistentFileSelectorCache initialized. Directory: {}, Loaded entries: {}",
                   cacheDirectory, memoryCache.estimatedSize());
    }

    @Override
    public Optional<CachedSelector> get(String key) {
        long startTime = System.currentTimeMillis();

        // First try memory cache
        CachedSelector result = memoryCache.getIfPresent(key);

        if (result != null) {
            // Update access time for file cache
            updateFileMetrics(key, true);
            metrics.recordHit();
            System.out.println("[FILE-CACHE] Cache HIT: " + key);
            logger.debug("Memory cache hit for key: {}", key);
            return Optional.of(result);
        } else {
            metrics.recordMiss();
            System.out.println("[FILE-CACHE] Cache MISS: " + key);
            logger.debug("Cache miss for key: {}", key);
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, CachedSelector selector) {
        long startTime = System.currentTimeMillis();

        // Store in memory cache
        memoryCache.put(key, selector);

        // Update file metrics
        updateFileMetrics(key, true);

        // Async save to file to avoid blocking
        saveToFileAsync();

        metrics.recordLoad(System.currentTimeMillis() - startTime);
        System.out.println("[FILE-CACHE] Cache STORED: " + key + " (expires in " +
                          config.getExpireAfterWrite().toHours() + " hours)");
        logger.debug("Cached selector for key: {}", key);
    }

    @Override
    public void updateSuccess(String key, boolean success) {
        CachedSelector cached = memoryCache.getIfPresent(key);
        if (cached != null) {
            cached.recordUsage(success);
            updateFileMetrics(key, success);
            logger.debug("Updated success rate for key: {} (success: {})", key, success);
        }
    }

    @Override
    public CacheMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void evictExpired() {
        // Caffeine handles TTL automatically for memory cache
        // Clean up file metrics
        cleanupExpiredFileMetrics();
        logger.debug("Evicted expired cache entries");
    }

    @Override
    public void clearAll() {
        long sizeBefore = memoryCache.estimatedSize();
        memoryCache.invalidateAll();
        memoryCache.cleanUp();
        fileMetrics.clear();

        // Clear file cache
        clearFileCache();

        metrics.recordEviction();
        System.out.println("[FILE-CACHE] Cache cleared: " + sizeBefore + " entries removed");
        logger.info("Cache cleared completely: {} entries removed", sizeBefore);
    }

    @Override
    public boolean remove(String key) {
        CachedSelector existing = memoryCache.getIfPresent(key);
        if (existing != null) {
            memoryCache.invalidate(key);
            memoryCache.cleanUp();
            fileMetrics.remove(key);
            metrics.recordEviction();
            System.out.println("[FILE-CACHE] Cache entry removed: " + key);
            logger.debug("Cache entry removed: {}", key);

            // Save updated cache to file
            saveToFileAsync();
            return true;
        }
        logger.debug("Attempted to remove non-existent cache entry: {}", key);
        return false;
    }

    @Override
    public long size() {
        return memoryCache.estimatedSize();
    }

    /**
     * Generate a contextual cache key that includes element context
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
     * Create cache directory if it doesn't exist
     */
    private void createCacheDirectory() {
        try {
            Path cacheDir = Paths.get(cacheDirectory);
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
                logger.debug("Created cache directory: {}", cacheDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create cache directory: {}", cacheDirectory, e);
        }
    }

    /**
     * Load cache from file on startup
     */
    private void loadCacheFromFile() {
        loadCacheEntries();
        loadFileMetrics();
    }

    /**
     * Load cache entries from file
     */
    private void loadCacheEntries() {
        File file = new File(cacheFilePath);
        if (file.exists()) {
            try {
                TypeReference<Map<String, FileCacheEntry>> typeRef = new TypeReference<Map<String, FileCacheEntry>>() {};
                Map<String, FileCacheEntry> loadedEntries = objectMapper.readValue(file, typeRef);

                int loadedCount = 0;
                int expiredCount = 0;

                for (Map.Entry<String, FileCacheEntry> entry : loadedEntries.entrySet()) {
                    FileCacheEntry cacheEntry = entry.getValue();

                    // Check if entry is expired
                    if (!cacheEntry.isExpired(config.getExpireAfterWrite(), config.getExpireAfterAccess())) {
                        CachedSelector cachedSelector = cacheEntry.toCachedSelector();
                        memoryCache.put(entry.getKey(), cachedSelector);
                        loadedCount++;
                    } else {
                        expiredCount++;
                    }
                }

                System.out.println("[FILE-CACHE] Loaded " + loadedCount + " entries from cache file, " +
                                 expiredCount + " expired entries skipped");
                logger.info("Loaded {} cache entries from file, {} expired entries skipped",
                           loadedCount, expiredCount);
            } catch (IOException e) {
                logger.error("Failed to load cache from file: {}", cacheFilePath, e);
            }
        } else {
            logger.debug("Cache file does not exist, starting with empty cache: {}", cacheFilePath);
        }
    }

    /**
     * Load file metrics from file
     */
    private void loadFileMetrics() {
        File file = new File(metricsFilePath);
        if (file.exists()) {
            try {
                TypeReference<Map<String, FileCacheMetrics>> typeRef = new TypeReference<Map<String, FileCacheMetrics>>() {};
                Map<String, FileCacheMetrics> loadedMetrics = objectMapper.readValue(file, typeRef);

                // Filter out expired metrics
                for (Map.Entry<String, FileCacheMetrics> entry : loadedMetrics.entrySet()) {
                    if (!entry.getValue().isExpired(config.getExpireAfterAccess())) {
                        fileMetrics.put(entry.getKey(), entry.getValue());
                    }
                }

                logger.info("Loaded {} file metrics from cache", fileMetrics.size());
            } catch (IOException e) {
                logger.error("Failed to load file metrics: {}", metricsFilePath, e);
            }
        }
    }

    /**
     * Save cache to file asynchronously
     */
    private void saveToFileAsync() {
        // Use a separate thread to avoid blocking the main cache operations
        Thread saveThread = new Thread(this::saveCacheToFile);
        saveThread.setDaemon(true);
        saveThread.start();
    }

    /**
     * Save cache to file
     */
    private void saveCacheToFile() {
        try {
            // Save cache entries
            Map<String, FileCacheEntry> entriesToSave = new HashMap<>();
            Map<String, CachedSelector> currentCache = memoryCache.asMap();

            for (Map.Entry<String, CachedSelector> entry : currentCache.entrySet()) {
                entriesToSave.put(entry.getKey(), new FileCacheEntry(entry.getValue()));
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(cacheFilePath), entriesToSave);

            // Save file metrics
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(metricsFilePath), fileMetrics);

            logger.debug("Cache saved to files: {} entries, {} metrics", entriesToSave.size(), fileMetrics.size());
        } catch (IOException e) {
            logger.error("Failed to save cache to file", e);
        }
    }

    /**
     * Clear file cache
     */
    private void clearFileCache() {
        try {
            Files.deleteIfExists(Paths.get(cacheFilePath));
            Files.deleteIfExists(Paths.get(metricsFilePath));
            logger.debug("Cache files cleared");
        } catch (IOException e) {
            logger.error("Failed to clear cache files", e);
        }
    }

    /**
     * Update file metrics
     */
    private void updateFileMetrics(String key, boolean success) {
        fileMetrics.computeIfAbsent(key, k -> new FileCacheMetrics()).recordUsage(success);
    }

    /**
     * Clean up expired file metrics
     */
    private void cleanupExpiredFileMetrics() {
        fileMetrics.entrySet().removeIf(entry -> entry.getValue().isExpired(config.getExpireAfterAccess()));
    }

    /**
     * Setup shutdown hook to save cache
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Saving cache before shutdown...");
            System.out.println("[FILE-CACHE] Saving cache to file before shutdown...");
            saveCacheToFile();
        }));
    }

    /**
     * Get cache file path for debugging
     */
    public String getCacheFilePath() {
        return cacheFilePath;
    }

    /**
     * Force save cache to file (for testing)
     */
    public void forceSave() {
        saveCacheToFile();
    }
}