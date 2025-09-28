package com.autoheal.config;

import com.autoheal.model.AIProvider;
import java.time.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for AI service integration with comprehensive properties file support
 * Supports multiple AI providers with smart defaults and environment variable overrides
 */
public class AIConfig {
    private final AIProvider provider;
    private final String model;
    private final String apiKey;
    private final String apiUrl;
    private final Duration timeout;
    private final int maxRetries;
    private final boolean visualAnalysisEnabled;
    private final int maxTokensDOM;
    private final int maxTokensVisual;
    private final double temperatureDOM;
    private final double temperatureVisual;

    // Provider-specific API endpoints
    private static final Map<AIProvider, String> DEFAULT_API_ENDPOINTS = new HashMap<AIProvider, String>() {{
        put(AIProvider.OPENAI, "https://api.openai.com/v1/chat/completions");
        put(AIProvider.GOOGLE_GEMINI, "https://generativelanguage.googleapis.com/v1/models");
        put(AIProvider.ANTHROPIC_CLAUDE, "https://api.anthropic.com/v1/messages");
        put(AIProvider.DEEPSEEK, "https://api.deepseek.com/chat/completions");
        put(AIProvider.GROK, "https://api.x.ai/v1/chat/completions");
        put(AIProvider.LOCAL_MODEL, "http://localhost:11434/v1/chat/completions");
        put(AIProvider.MOCK, "http://localhost:8080/mock");
    }};

    // Provider-specific environment variable names for API keys
    private static final Map<AIProvider, String> API_KEY_ENV_VARS = new HashMap<AIProvider, String>() {{
        put(AIProvider.OPENAI, "OPENAI_API_KEY");
        put(AIProvider.GOOGLE_GEMINI, "GEMINI_API_KEY");
        put(AIProvider.ANTHROPIC_CLAUDE, "ANTHROPIC_API_KEY");
        put(AIProvider.DEEPSEEK, "DEEPSEEK_API_KEY");
        put(AIProvider.GROK, "GROK_API_KEY");
        put(AIProvider.LOCAL_MODEL, "LOCAL_MODEL_API_KEY");
        put(AIProvider.MOCK, "MOCK_API_KEY");
    }};

    public static Builder builder() {
        return new Builder();
    }

    public static AIConfig defaultConfig() {
        return builder().build();
    }

    /**
     * Creates AIConfig from properties file with smart defaults and environment variable support
     */
    public static AIConfig fromProperties() {
        return fromProperties("autoheal-default.properties");
    }

    /**
     * Creates AIConfig from specified properties file
     */
    public static AIConfig fromProperties(String propertiesFile) {
        Properties props = loadProperties(propertiesFile);
        return fromProperties(props);
    }

    /**
     * Creates AIConfig from Properties object with smart defaults
     */
    public static AIConfig fromProperties(Properties props) {
        Builder builder = builder();

        // Parse AI provider with validation
        String providerStr = getProperty(props, "autoheal.ai.provider", "OPENAI");
        AIProvider provider;
        try {
            provider = AIProvider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Unknown AI provider '" + providerStr + "', falling back to OPENAI");
            provider = AIProvider.OPENAI;
        }
        builder.provider(provider);

        // Smart model selection - user override or provider default
        String userModel = getProperty(props, "autoheal.ai.model", null);
        String model = (userModel != null && !userModel.trim().isEmpty())
            ? userModel.trim() : provider.getDefaultModel();
        builder.model(model);

        // Smart API key selection based on provider
        String apiKey = getApiKeyForProvider(props, provider);
        if (apiKey != null) {
            builder.apiKey(apiKey);
        }

        // Smart API URL - user override or provider default
        String userApiUrl = getProperty(props, "autoheal.ai.api-url", null);
        String apiUrl = (userApiUrl != null && !userApiUrl.trim().isEmpty())
            ? userApiUrl.trim() : DEFAULT_API_ENDPOINTS.get(provider);
        builder.apiUrl(apiUrl);

        // Parse other configuration with defaults
        builder.timeout(parseDuration(getProperty(props, "autoheal.ai.timeout", "30s")));
        builder.maxRetries(Integer.parseInt(getProperty(props, "autoheal.ai.max-retries", "3")));
        builder.visualAnalysisEnabled(Boolean.parseBoolean(getProperty(props, "autoheal.ai.visual-analysis-enabled", "true")));
        builder.maxTokensDOM(Integer.parseInt(getProperty(props, "autoheal.ai.max-tokens-dom", "500")));
        builder.maxTokensVisual(Integer.parseInt(getProperty(props, "autoheal.ai.max-tokens-visual", "1000")));
        builder.temperatureDOM(Double.parseDouble(getProperty(props, "autoheal.ai.temperature-dom", "0.1")));
        builder.temperatureVisual(Double.parseDouble(getProperty(props, "autoheal.ai.temperature-visual", "0.0")));

        return builder.build();
    }

    private static String getApiKeyForProvider(Properties props, AIProvider provider) {
        // First try the generic property
        String apiKey = getProperty(props, "autoheal.ai.api-key", null);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }

        // Then try provider-specific environment variable
        String envVar = API_KEY_ENV_VARS.get(provider);
        if (envVar != null) {
            String envValue = System.getenv(envVar);
            if (envValue != null && !envValue.trim().isEmpty()) {
                return envValue.trim();
            }
        }

        return null;
    }

    private static String getProperty(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key, defaultValue);
        // Handle environment variable substitution like ${VAR_NAME:defaultValue}
        if (value != null && value.contains("${")) {
            return resolveEnvironmentVariables(value);
        }
        return value;
    }

    private static String resolveEnvironmentVariables(String value) {
        // Simple environment variable resolution for ${VAR:default} pattern
        if (value.startsWith("${") && value.contains("}")) {
            String varPart = value.substring(2, value.length() - 1);
            String[] parts = varPart.split(":", 2);
            String varName = parts[0];
            String defaultVal = parts.length > 1 ? parts[1] : "";

            String envValue = System.getenv(varName);
            return (envValue != null && !envValue.trim().isEmpty()) ? envValue : defaultVal;
        }
        return value;
    }

    private static Duration parseDuration(String duration) {
        if (duration.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(duration.substring(0, duration.length() - 1)));
        } else if (duration.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(duration.substring(0, duration.length() - 1)));
        } else if (duration.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(duration.substring(0, duration.length() - 1)));
        } else {
            return Duration.ofSeconds(Long.parseLong(duration));
        }
    }

    private static Properties loadProperties(String filename) {
        Properties props = new Properties();
        try (InputStream is = AIConfig.class.getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("Warning: Could not find properties file: " + filename);
            }
        } catch (IOException e) {
            System.err.println("Error loading properties file " + filename + ": " + e.getMessage());
        }
        return props;
    }

    public static class Builder {
        private AIProvider provider = AIProvider.OPENAI;
        private String model = null; // Will use provider default if not set
        private String apiKey = getDefaultApiKey();
        private String apiUrl = null; // Will use provider default if not set
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private boolean visualAnalysisEnabled = true;
        private int maxTokensDOM = 500;
        private int maxTokensVisual = 1000;
        private double temperatureDOM = 0.1;
        private double temperatureVisual = 0.0;

        private static String getDefaultApiKey() {
            // First try system property (for programmatic setting)
            String key = System.getProperty("OPENAI_API_KEY");
            if (key != null && !key.trim().isEmpty()) {
                return key.trim();
            }
            // Fallback to environment variable
            key = System.getenv("OPENAI_API_KEY");
            return (key != null && !key.trim().isEmpty()) ? key.trim() : null;
        }

        public Builder provider(AIProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder visualAnalysisEnabled(boolean visualAnalysisEnabled) {
            this.visualAnalysisEnabled = visualAnalysisEnabled;
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder maxTokensDOM(int maxTokensDOM) {
            this.maxTokensDOM = maxTokensDOM;
            return this;
        }

        public Builder maxTokensVisual(int maxTokensVisual) {
            this.maxTokensVisual = maxTokensVisual;
            return this;
        }

        public Builder temperatureDOM(double temperatureDOM) {
            this.temperatureDOM = temperatureDOM;
            return this;
        }

        public Builder temperatureVisual(double temperatureVisual) {
            this.temperatureVisual = temperatureVisual;
            return this;
        }

        public AIConfig build() {
            // Apply smart defaults if not explicitly set
            String finalModel = (model != null) ? model : provider.getDefaultModel();
            String finalApiUrl = (apiUrl != null) ? apiUrl : DEFAULT_API_ENDPOINTS.get(provider);

            // Validate configuration
            validateConfiguration(provider, finalModel, apiKey, finalApiUrl);

            return new AIConfig(provider, finalModel, apiKey, finalApiUrl, timeout, maxRetries,
                              visualAnalysisEnabled, maxTokensDOM, maxTokensVisual, temperatureDOM, temperatureVisual);
        }

        private void validateConfiguration(AIProvider provider, String model, String apiKey, String apiUrl) {
            if (provider == null) {
                throw new IllegalArgumentException("AI provider cannot be null");
            }

            if (model == null || model.trim().isEmpty()) {
                throw new IllegalArgumentException("AI model cannot be null or empty for provider: " + provider);
            }

            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("API URL cannot be null or empty for provider: " + provider);
            }

            // Only require API key for non-local providers
            if (provider != AIProvider.LOCAL_MODEL && provider != AIProvider.MOCK &&
                (apiKey == null || apiKey.trim().isEmpty())) {
                String envVar = API_KEY_ENV_VARS.get(provider);
                throw new IllegalArgumentException(
                    String.format("API key is required for provider %s. " +
                                "Please set environment variable %s or provide apiKey in configuration.",
                                provider, envVar));
            }
        }
    }

    private AIConfig(AIProvider provider, String model, String apiKey, String apiUrl, Duration timeout,
                     int maxRetries, boolean visualAnalysisEnabled, int maxTokensDOM, int maxTokensVisual,
                     double temperatureDOM, double temperatureVisual) {
        this.provider = provider;
        this.model = model;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.visualAnalysisEnabled = visualAnalysisEnabled;
        this.maxTokensDOM = maxTokensDOM;
        this.maxTokensVisual = maxTokensVisual;
        this.temperatureDOM = temperatureDOM;
        this.temperatureVisual = temperatureVisual;
    }

    // Getters
    public AIProvider getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isVisualAnalysisEnabled() {
        return visualAnalysisEnabled;
    }

    public int getMaxTokensDOM() {
        return maxTokensDOM;
    }

    public int getMaxTokensVisual() {
        return maxTokensVisual;
    }

    public double getTemperatureDOM() {
        return temperatureDOM;
    }

    public double getTemperatureVisual() {
        return temperatureVisual;
    }

    @Override
    public String toString() {
        return String.format("AIConfig{provider=%s, model='%s', apiUrl='%s', visualAnalysisEnabled=%s, maxRetries=%d}",
                           provider, model, apiUrl, visualAnalysisEnabled, maxRetries);
    }
}