package com.autoheal.util;

/**
 * Legacy options class for backward compatibility
 */
public class AutoHealOptions {
    private final String openAIKey;
    private final int timeoutSeconds;
    private final int cacheSize;
    private final int cacheTTLMinutes;
    private final boolean enableAI;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String openAIKey = System.getenv("OPENAI_API_KEY");
        private int timeoutSeconds = 30;
        private int cacheSize = 1000;
        private int cacheTTLMinutes = 60;
        private boolean enableAI = true;

        public Builder openAIKey(String key) {
            this.openAIKey = key;
            return this;
        }

        public Builder timeoutSeconds(int timeout) {
            this.timeoutSeconds = timeout;
            return this;
        }

        public Builder cacheSize(int size) {
            this.cacheSize = size;
            return this;
        }

        public Builder cacheTTLMinutes(int ttl) {
            this.cacheTTLMinutes = ttl;
            return this;
        }

        public Builder enableAI(boolean enable) {
            this.enableAI = enable;
            return this;
        }

        public AutoHealOptions build() {
            return new AutoHealOptions(openAIKey, timeoutSeconds, cacheSize, cacheTTLMinutes, enableAI);
        }
    }

    private AutoHealOptions(String openAIKey, int timeoutSeconds, int cacheSize,
                            int cacheTTLMinutes, boolean enableAI) {
        this.openAIKey = openAIKey;
        this.timeoutSeconds = timeoutSeconds;
        this.cacheSize = cacheSize;
        this.cacheTTLMinutes = cacheTTLMinutes;
        this.enableAI = enableAI;
    }

    // Getters
    public String getOpenAIKey() {
        return openAIKey;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getCacheTTLMinutes() {
        return cacheTTLMinutes;
    }

    public boolean isEnableAI() {
        return enableAI;
    }
}
