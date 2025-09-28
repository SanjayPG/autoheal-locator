package com.autoheal.spring;

import java.time.Duration;

/**
 * Configuration for caching behavior
 */
public class CacheConfig {
    private final int maximumSize;
    private final Duration expireAfterWrite;
    private final Duration expireAfterAccess;
    private final boolean recordStats;

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

        public CacheConfig build() {
            return new CacheConfig(maximumSize, expireAfterWrite, expireAfterAccess, recordStats);
        }
    }

    private CacheConfig(int maximumSize, Duration expireAfterWrite,
                        Duration expireAfterAccess, boolean recordStats) {
        this.maximumSize = maximumSize;
        this.expireAfterWrite = expireAfterWrite;
        this.expireAfterAccess = expireAfterAccess;
        this.recordStats = recordStats;
    }

    // Getters
    public int getMaximumSize() { return maximumSize; }
    public Duration getExpireAfterWrite() { return expireAfterWrite; }
    public Duration getExpireAfterAccess() { return expireAfterAccess; }
    public boolean isRecordStats() { return recordStats; }
}
