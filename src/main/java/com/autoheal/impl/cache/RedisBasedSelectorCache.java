package com.autoheal.impl.cache;

import com.autoheal.config.CacheConfig;
import com.autoheal.core.SelectorCache;
import com.autoheal.metrics.CacheMetrics;
import com.autoheal.model.CachedSelector;
import com.autoheal.model.ElementContext;
import com.autoheal.model.Position;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based persistent cache implementation for AutoHeal selectors
 * Provides persistent storage that survives application restarts
 */
public class RedisBasedSelectorCache implements SelectorCache {
    private static final Logger logger = LoggerFactory.getLogger(RedisBasedSelectorCache.class);
    private static final String CACHE_KEY_PREFIX = "autoheal:selector:";

    private final JedisPool jedisPool;
    private final CacheMetrics metrics;
    private final ObjectMapper objectMapper;
    private final CacheConfig config;

    public RedisBasedSelectorCache(CacheConfig config, String redisHost, int redisPort) {
        this(config, redisHost, redisPort, null);
    }

    public RedisBasedSelectorCache(CacheConfig config, String redisHost, int redisPort, String password) {
        this.config = config;
        this.metrics = new CacheMetrics();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Configure Jedis pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        try {
            if (password != null && !password.trim().isEmpty()) {
                this.jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, password);
            } else {
                this.jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000);
            }

            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                logger.info("RedisBasedSelectorCache initialized successfully. Host: {}:{}", redisHost, redisPort);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Redis connection to {}:{}", redisHost, redisPort, e);
            throw new RuntimeException("Redis connection failed", e);
        }
    }

    @Override
    public Optional<CachedSelector> get(String key) {
        long startTime = System.currentTimeMillis();
        String redisKey = CACHE_KEY_PREFIX + key;

        try (Jedis jedis = jedisPool.getResource()) {
            String jsonValue = jedis.get(redisKey);

            if (jsonValue != null) {
                CachedSelector cachedSelector = objectMapper.readValue(jsonValue, CachedSelector.class);
                metrics.recordHit();
                logger.debug("Redis cache hit for key: {}", key);
                System.out.println("[REDIS-DEBUG] Cache HIT: " + key);
                return Optional.of(cachedSelector);
            } else {
                metrics.recordMiss();
                logger.debug("Redis cache miss for key: {}", key);
                System.out.println("[REDIS-DEBUG] Cache MISS: " + key);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Redis cache get failed for key: {}", key, e);
            metrics.recordMiss();
            System.out.println("[REDIS-DEBUG] Cache GET ERROR: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, CachedSelector selector) {
        long startTime = System.currentTimeMillis();
        String redisKey = CACHE_KEY_PREFIX + key;

        try (Jedis jedis = jedisPool.getResource()) {
            String jsonValue = objectMapper.writeValueAsString(selector);

            // Set with TTL based on config
            long ttlSeconds = config.getExpireAfterWrite().getSeconds();
            jedis.setex(redisKey, ttlSeconds, jsonValue);

            metrics.recordLoad(System.currentTimeMillis() - startTime);
            logger.debug("Cached selector in Redis for key: {} (TTL: {}s)", key, ttlSeconds);
            System.out.println("[REDIS-DEBUG] Cache STORED: " + key + " (TTL: " + ttlSeconds + "s)");
        } catch (Exception e) {
            logger.error("Redis cache put failed for key: {}", key, e);
            System.out.println("[REDIS-DEBUG] Cache STORE ERROR: " + e.getMessage());
        }
    }

    @Override
    public void updateSuccess(String key, boolean success) {
        String redisKey = CACHE_KEY_PREFIX + key;

        try (Jedis jedis = jedisPool.getResource()) {
            String jsonValue = jedis.get(redisKey);
            if (jsonValue != null) {
                CachedSelector cachedSelector = objectMapper.readValue(jsonValue, CachedSelector.class);
                cachedSelector.recordUsage(success);

                // Update with same TTL
                long ttl = jedis.ttl(redisKey);
                if (ttl > 0) {
                    String updatedJson = objectMapper.writeValueAsString(cachedSelector);
                    jedis.setex(redisKey, ttl, updatedJson);
                    logger.debug("Updated success rate for Redis key: {} (success: {})", key, success);
                }
            }
        } catch (Exception e) {
            logger.error("Redis cache updateSuccess failed for key: {}", key, e);
        }
    }

    @Override
    public CacheMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void evictExpired() {
        // Redis handles TTL automatically, no manual eviction needed
        logger.debug("Redis TTL-based eviction is automatic, no manual action required");
    }

    @Override
    public void clearAll() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Get count before clearing
            Object result = jedis.eval(
                "return #redis.call('keys', ARGV[1])",
                0,
                CACHE_KEY_PREFIX + "*"
            );
            long sizeBefore = (result instanceof Long) ? (Long) result : 0L;

            // Delete all cache keys
            String pattern = CACHE_KEY_PREFIX + "*";
            var keys = jedis.keys(pattern);
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
            }

            metrics.recordEviction();
            System.out.println("[REDIS-DEBUG] Cache cleared: " + sizeBefore + " entries removed");
            logger.info("Redis cache cleared completely: {} entries removed", sizeBefore);
        } catch (Exception e) {
            logger.error("Redis cache clearAll failed", e);
        }
    }

    @Override
    public boolean remove(String key) {
        String redisKey = CACHE_KEY_PREFIX + key;

        try (Jedis jedis = jedisPool.getResource()) {
            long deleted = jedis.del(redisKey);
            if (deleted > 0) {
                metrics.recordEviction();
                System.out.println("[REDIS-DEBUG] Cache entry removed: " + key);
                logger.debug("Redis cache entry removed: {}", key);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Redis cache remove failed for key: {}", key, e);
            return false;
        }
    }

    @Override
    public long size() {
        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(
                "return #redis.call('keys', ARGV[1])",
                0,
                CACHE_KEY_PREFIX + "*"
            );
            return (result instanceof Long) ? (Long) result : 0L;
        } catch (Exception e) {
            logger.error("Redis cache size calculation failed", e);
            return 0;
        }
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
     * Check Redis connection health
     */
    public boolean isHealthy() {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = jedis.ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return false;
        }
    }

    /**
     * Get Redis connection info
     */
    public String getConnectionInfo() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "Redis connected: " + jedis.info("server").split("\r\n")[1];
        } catch (Exception e) {
            return "Redis connection failed: " + e.getMessage();
        }
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis connection pool closed");
        }
    }
}