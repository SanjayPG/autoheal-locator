package com.autoheal.performance;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.config.AIConfig;
import com.autoheal.config.PerformanceConfig;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for AutoHeal system
 */
class AutoHealPerformanceTest {

    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);

        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .cache(CacheConfig.builder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofHours(1))
                        .build())
                .ai(AIConfig.builder()
                        .provider(AIProvider.MOCK)
                        .timeout(Duration.ofSeconds(5))
                        .visualAnalysisEnabled(true)
                        .build())
                .performance(PerformanceConfig.builder()
                        .threadPoolSize(16)
                        .enableMetrics(true)
                        .maxConcurrentRequests(100)
                        .build())
                .build();

        autoHeal = AutoHealLocator.builder()
                .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
                .withConfiguration(config)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void shouldHandleConcurrentRequests() {
        // Given
        driver.get("data:text/html,<html><body>" +
                "<button id='btn1'>Button 1</button>" +
                "<button id='btn2'>Button 2</button>" +
                "<button id='btn3'>Button 3</button>" +
                "</body></html>");

        // When - make concurrent requests
        long startTime = System.currentTimeMillis();

        CompletableFuture<?>[] futures = IntStream.range(0, 20)
                .mapToObj(i -> autoHeal.findElementAsync("#btn" + (i % 3 + 1), "Button " + (i % 3 + 1)))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        long executionTime = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(executionTime < 10000, "Concurrent requests should complete within 10 seconds");

        var metrics = autoHeal.getMetrics();
        assertEquals(20, metrics.getLocatorMetrics().getTotalRequests());
        assertTrue(metrics.getLocatorMetrics().getSuccessRate() > 0.9);
    }

    @Test
    void shouldCacheEffectively() {
        // Given
        driver.get("data:text/html,<html><body><button id='cache-test'>Cache Test</button></body></html>");

        // When - perform multiple lookups of same element
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 50; i++) {
            autoHeal.findElement("#cache-test", "Cache test button");
        }

        long executionTime = System.currentTimeMillis() - startTime;

        // Then
        var metrics = autoHeal.getMetrics();
        assertTrue(metrics.getCacheMetrics().getHitRate() > 0.8, "Cache hit rate should be > 80%");
        assertTrue(executionTime < 5000, "Cached lookups should be fast");
    }

    @Test
    void shouldMaintainPerformanceUnderLoad() {
        // Given
        StringBuilder htmlBuilder = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            htmlBuilder.append("<div id='element").append(i).append("'>Element ").append(i).append("</div>");
        }
        htmlBuilder.append("</body></html>");

        driver.get("data:text/html," + htmlBuilder.toString());

        // When - find many different elements
        long startTime = System.currentTimeMillis();

        IntStream.range(0, 50)
                .parallel()
                .forEach(i -> {
                    try {
                        autoHeal.findElement("#element" + i, "Element " + i);
                    } catch (Exception e) {
                        // Some may fail, that's OK for performance test
                    }
                });

        long executionTime = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(executionTime < 30000, "Should handle load within 30 seconds");

        var metrics = autoHeal.getMetrics();
        assertTrue(metrics.getLocatorMetrics().getAverageExecutionTime() < 1000,
                "Average execution time should be reasonable");
    }
}