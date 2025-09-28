package com.autoheal.config;

import java.time.Duration;

/**
 * Configuration for selector caching behavior
 */
public class CacheConfig {
    private final int maximumSize;
    private final Duration expireAfterWrite;
    private final Duration expireAfterAccess;
    private final boolean recordStats;
    private final CacheType cacheType;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;

    public enum CacheType {
        CAFFEINE,      // In-memory only (default)
        REDIS,         // Persistent Redis cache
        PERSISTENT_FILE, // File-based persistent cache
        HYBRID         // Caffeine L1 + Redis L2
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CacheConfig defaultConfig() {
        return builder().build();
    }

    public static class Builder {
        private int maximumSize = 10000;
        private Duration expireAfterWrite = Duration.ofHours(24);
        private Duration expireAfterAccess = Duration.ofHours(2);
        private boolean recordStats = true;
        private CacheType cacheType = CacheType.CAFFEINE;
        private String redisHost = "localhost";
        private int redisPort = 6379;
        private String redisPassword = null;

        public Builder maximumSize(int maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public Builder expireAfterWrite(Duration duration) {
            this.expireAfterWrite = duration;
            return this;
        }

        public Builder expireAfterAccess(Duration duration) {
            this.expireAfterAccess = duration;
            return this;
        }

        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        public Builder cacheType(CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        public Builder redisHost(String redisHost) {
            this.redisHost = redisHost;
            return this;
        }

        public Builder redisPort(int redisPort) {
            this.redisPort = redisPort;
            return this;
        }

        public Builder redisPassword(String redisPassword) {
            this.redisPassword = redisPassword;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(maximumSize, expireAfterWrite, expireAfterAccess, recordStats,
                                 cacheType, redisHost, redisPort, redisPassword);
        }
    }

    private CacheConfig(int maximumSize, Duration expireAfterWrite,
                        Duration expireAfterAccess, boolean recordStats,
                        CacheType cacheType, String redisHost, int redisPort, String redisPassword) {
        this.maximumSize = maximumSize;
        this.expireAfterWrite = expireAfterWrite;
        this.expireAfterAccess = expireAfterAccess;
        this.recordStats = recordStats;
        this.cacheType = cacheType;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisPassword = redisPassword;
    }

    // Getters
    public int getMaximumSize() { return maximumSize; }
    public Duration getExpireAfterWrite() { return expireAfterWrite; }
    public Duration getExpireAfterAccess() { return expireAfterAccess; }
    public boolean isRecordStats() { return recordStats; }
    public CacheType getCacheType() { return cacheType; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }
}