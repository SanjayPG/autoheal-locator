package com.autoheal.spring;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import com.autoheal.config.CacheConfig;
import com.autoheal.config.PerformanceConfig;
import com.autoheal.exception.AutoHealException;
import com.autoheal.exception.ErrorCode;
import com.autoheal.model.AIProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigurationValidator {

    public static ValidationResult validate(AutoHealConfiguration config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validate cache config
        CacheConfig cacheConfig = config.getCacheConfig();
        if (cacheConfig.getMaximumSize() <= 0) {
            errors.add("Cache maximum size must be positive");
        }
        if (cacheConfig.getMaximumSize() > 100000) {
            warnings.add("Cache size very large, may impact memory usage");
        }

        // Validate AI config
        AIConfig aiConfig = config.getAiConfig();
        if (aiConfig.getProvider() == AIProvider.OPENAI &&
                (aiConfig.getApiKey() == null || aiConfig.getApiKey().isEmpty())) {
            errors.add("OpenAI API key is required when using OpenAI provider");
        }
        if (aiConfig.getTimeout().toMillis() < 1000) {
            warnings.add("AI timeout below 1 second may cause frequent failures");
        }

        // Validate performance config
        PerformanceConfig perfConfig = config.getPerformanceConfig();
        if (perfConfig.getThreadPoolSize() <= 0) {
            errors.add("Thread pool size must be positive");
        }
        if (perfConfig.getThreadPoolSize() > 100) {
            warnings.add("Very large thread pool may impact performance");
        }

        return new ValidationResult(errors, warnings);
    }

    public static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(List<String> errors, List<String> warnings) {
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public void throwIfInvalid() {
            if (!isValid()) {
                throw new AutoHealException(ErrorCode.CONFIGURATION_INVALID,
                        "Configuration validation failed: " + String.join(", ", errors));
            }
        }
    }
}