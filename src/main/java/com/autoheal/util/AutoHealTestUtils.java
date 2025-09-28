package com.autoheal.util;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.core.WebAutomationAdapter;
import com.autoheal.impl.ai.MockAIService;
import com.autoheal.impl.adapter.MockWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * AutoHeal Test Utilities for easier testing
 */
public class AutoHealTestUtils {

    /**
     * Create a mock AutoHeal instance for testing
     *
     * @return configured AutoHeal instance with mock dependencies
     */
    public static AutoHealLocator createMockAutoHeal() {
        MockWebAutomationAdapter mockAdapter = new MockWebAutomationAdapter();
        MockAIService mockAI = new MockAIService();

        return AutoHealLocator.builder()
                .withWebAdapter(mockAdapter)
                .withAIService(mockAI)
                .withConfiguration(AutoHealConfiguration.builder()
                        .ai(AIConfig.builder()
                                .provider(AIProvider.MOCK)
                                .build())
                        .build())
                .build();
    }

    /**
     * Setup mock responses for testing
     *
     * @param autoHeal the AutoHeal instance to configure
     * @param mockElements map of selectors to mock elements
     */
    public static void setupMockResponses(AutoHealLocator autoHeal,
                                          Map<String, WebElement> mockElements) {
        WebAutomationAdapter adapter = getWebAdapter(autoHeal);
        if (adapter instanceof MockWebAutomationAdapter) {
            MockWebAutomationAdapter mockAdapter = (MockWebAutomationAdapter) adapter;
            mockElements.forEach(mockAdapter::addMockElement);
        }
    }

    /**
     * Create test configuration with reduced timeouts
     *
     * @return test-optimized configuration
     */
    public static AutoHealConfiguration createTestConfiguration() {
        return AutoHealConfiguration.builder()
                .ai(AIConfig.builder()
                        .provider(AIProvider.MOCK)
                        .timeout(java.time.Duration.ofSeconds(2))
                        .maxRetries(1)
                        .build())
                .performance(com.autoheal.config.PerformanceConfig.builder()
                        .elementTimeout(java.time.Duration.ofSeconds(5))
                        .threadPoolSize(2)
                        .build())
                .cache(com.autoheal.config.CacheConfig.builder()
                        .maximumSize(50)
                        .expireAfterWrite(java.time.Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Wait for async operations to complete in tests
     *
     * @param timeoutMs maximum time to wait in milliseconds
     */
    public static void waitForCompletion(long timeoutMs) {
        try {
            Thread.sleep(Math.min(timeoutMs, 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static WebAutomationAdapter getWebAdapter(AutoHealLocator autoHeal) {
        // This would require exposing the adapter through a getter method
        // For now, return null as this is internal implementation
        return null;
    }
}