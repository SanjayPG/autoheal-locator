package com.autoheal.util;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.config.PerformanceConfig;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import com.autoheal.monitoring.HealthStatus;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;


import java.time.Duration;

/**
 * JUnit 5 extension for AutoHeal testing
 */
public class AutoHealExtensions {

    /**
     * JUnit 5 Extension for easy test integration
     */
    public static class AutoHealExtension implements BeforeEachCallback, AfterEachCallback {
        private AutoHealLocator autoHeal;
        private WebDriver driver;

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            // Setup WebDriver for tests
            this.driver = createTestWebDriver();

            com.autoheal.config.AutoHealConfiguration config = com.autoheal.config.AutoHealConfiguration.builder()
                    .cache(CacheConfig.builder()
                            .maximumSize(100)
                            .expireAfterWrite(Duration.ofMinutes(5))
                            .build())
                    .ai(AIConfig.builder()
                            .provider(AIProvider.MOCK)
                            .timeout(Duration.ofSeconds(5))
                            .build())
                    .performance(PerformanceConfig.builder()
                            .threadPoolSize(2)
                            .elementTimeout(Duration.ofSeconds(10))
                            .build())
                    .build();

            this.autoHeal = AutoHealLocator.builder()
                    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
                    .withConfiguration(config)
                    .build();

            // Store in test context
            context.getStore(ExtensionContext.Namespace.GLOBAL)
                    .put("autoHeal", autoHeal);
            context.getStore(ExtensionContext.Namespace.GLOBAL)
                    .put("webDriver", driver);
        }


        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            if (autoHeal != null) {
                autoHeal.shutdown();
            }
            if (driver != null) {
                driver.quit();
            }
        }

        private WebDriver createTestWebDriver() {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            return new ChromeDriver(options);
        }
    }

    /**
     * Test configuration builder for common test scenarios
     */
    public static class TestConfigurationBuilder {

        /**
         * Create configuration optimized for unit tests
         */
        public static AutoHealConfiguration forUnitTests() {
            return AutoHealConfiguration.builder()
                    .ai(AIConfig.builder()
                            .provider(AIProvider.MOCK)
                            .timeout(Duration.ofSeconds(1))
                            .maxRetries(1)
                            .build())
                    .cache(CacheConfig.builder()
                            .maximumSize(10)
                            .expireAfterWrite(Duration.ofSeconds(30))
                            .build())
                    .performance(PerformanceConfig.builder()
                            .threadPoolSize(1)
                            .elementTimeout(Duration.ofSeconds(2))
                            .build())
                    .build();
        }

        /**
         * Create configuration optimized for integration tests
         */
        public static AutoHealConfiguration forIntegrationTests() {
            return AutoHealConfiguration.builder()
                    .ai(AIConfig.builder()
                            .provider(AIProvider.MOCK)
                            .timeout(Duration.ofSeconds(5))
                            .maxRetries(2)
                            .build())
                    .cache(CacheConfig.builder()
                            .maximumSize(50)
                            .expireAfterWrite(Duration.ofMinutes(1))
                            .build())
                    .performance(PerformanceConfig.builder()
                            .threadPoolSize(2)
                            .elementTimeout(Duration.ofSeconds(10))
                            .build())
                    .build();
        }

        /**
         * Create configuration optimized for performance tests
         */
        public static AutoHealConfiguration forPerformanceTests() {
            return AutoHealConfiguration.builder()
                    .ai(AIConfig.builder()
                            .provider(AIProvider.MOCK)
                            .timeout(Duration.ofSeconds(2))
                            .maxRetries(1)
                            .build())
                    .cache(CacheConfig.builder()
                            .maximumSize(1000)
                            .expireAfterWrite(Duration.ofMinutes(5))
                            .build())
                    .performance(PerformanceConfig.builder()
                            .threadPoolSize(8)
                            .elementTimeout(Duration.ofSeconds(5))
                            .maxConcurrentRequests(100)
                            .build())
                    .build();
        }
    }

    /**
     * Helper methods for test assertions
     */
    public static class TestAssertions {

        /**
         * Assert that AutoHeal is healthy
         *
         * @param autoHeal the AutoHeal instance to check
         */
        public static void assertHealthy(AutoHealLocator autoHeal) {
            HealthStatus health = autoHeal.getHealthStatus();
            if (!health.isHealthy()) {
                throw new AssertionError("AutoHeal system is not healthy: " + health);
            }
        }


        /**
         * Assert that cache hit rate meets minimum threshold
         *
         * @param autoHeal the AutoHeal instance
         * @param minHitRate minimum acceptable hit rate (0.0 to 1.0)
         */
        public static void assertCacheEfficiency(AutoHealLocator autoHeal, double minHitRate) {
            var metrics = autoHeal.getMetrics();
            double hitRate = metrics.getCacheMetrics().getHitRate();
            if (hitRate < minHitRate) {
                throw new AssertionError(String.format("Cache hit rate %.2f below threshold %.2f",
                        hitRate, minHitRate));
            }
        }

        /**
         * Assert that success rate meets minimum threshold
         *
         * @param autoHeal the AutoHeal instance
         * @param minSuccessRate minimum acceptable success rate (0.0 to 1.0)
         */
        public static void assertSuccessRate(AutoHealLocator autoHeal, double minSuccessRate) {
            var metrics = autoHeal.getMetrics();
            double successRate = metrics.getLocatorMetrics().getSuccessRate();
            if (successRate < minSuccessRate) {
                throw new AssertionError(String.format("Success rate %.2f below threshold %.2f",
                        successRate, minSuccessRate));
            }
        }

        /**
         * Assert that average execution time is below threshold
         *
         * @param autoHeal the AutoHeal instance
         * @param maxExecutionTimeMs maximum acceptable execution time in milliseconds
         */
        public static void assertPerformance(AutoHealLocator autoHeal, double maxExecutionTimeMs) {
            var metrics = autoHeal.getMetrics();
            double avgTime = metrics.getLocatorMetrics().getAverageExecutionTime();
            if (avgTime > maxExecutionTimeMs) {
                throw new AssertionError(String.format("Average execution time %.2fms above threshold %.2fms",
                        avgTime, maxExecutionTimeMs));
            }
        }

        /**
         * Assert that no requests have failed
         *
         * @param autoHeal the AutoHeal instance
         */
        public static void assertNoFailures(AutoHealLocator autoHeal) {
            var metrics = autoHeal.getMetrics();
            long totalRequests = metrics.getLocatorMetrics().getTotalRequests();
            long successfulRequests = metrics.getLocatorMetrics().getSuccessfulRequests();

            if (totalRequests > 0 && successfulRequests < totalRequests) {
                long failures = totalRequests - successfulRequests;
                throw new AssertionError(String.format("Found %d failures out of %d total requests",
                        failures, totalRequests));
            }
        }
    }

    /**
     * Test data helpers for creating mock scenarios
     */
    public static class TestDataHelpers {

        /**
         * Create a simple HTML page for testing
         *
         * @param elements elements to include in the page
         * @return HTML string
         */
        public static String createTestPage(String... elements) {
            StringBuilder html = new StringBuilder();
            html.append("<html><body>");
            for (String element : elements) {
                html.append(element);
            }
            html.append("</body></html>");
            return html.toString();
        }

        /**
         * Create a button element for testing
         *
         * @param id button ID
         * @param text button text
         * @return HTML button element
         */
        public static String createButton(String id, String text) {
            return String.format("<button id='%s'>%s</button>", id, text);
        }

        /**
         * Create an input field for testing
         *
         * @param id input ID
         * @param type input type
         * @return HTML input element
         */
        public static String createInput(String id, String type) {
            return String.format("<input id='%s' type='%s' />", id, type);
        }

        /**
         * Create a complex form for testing
         *
         * @param formId form ID
         * @return HTML form with multiple elements
         */
        public static String createTestForm(String formId) {
            return String.format("""
                <form id='%s'>
                    <input id='username' type='text' placeholder='Username' />
                    <input id='password' type='password' placeholder='Password' />
                    <input id='email' type='email' placeholder='Email' />
                    <button id='submit' type='submit'>Submit</button>
                    <button id='cancel' type='button'>Cancel</button>
                </form>
                """, formId);
        }
    }
}