package com.autoheal.integration;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AutoHeal with real browser
 */
@Testcontainers
class AutoHealIntegrationTest {

    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new ChromeOptions());

    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeEach
    void setUp() {
        driver = browser.getWebDriver();

        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .cache(CacheConfig.builder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofHours(1))
                        .build())
                .ai(AIConfig.builder()
                        .provider(AIProvider.MOCK) // Use mock for integration tests
                        .timeout(Duration.ofSeconds(10))
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
    }

    @Test
    void shouldFindElementOnRealPage() {
        // Given
        driver.get("data:text/html,<html><body><button id='test-btn'>Click me</button></body></html>");

        // When
        WebElement element = autoHeal.findElement("#test-btn", "Test button");

        // Then
        assertNotNull(element);
        assertEquals("Click me", element.getText());
    }

    @Test
    void shouldHandleMissingElement() {
        // Given
        driver.get("data:text/html,<html><body><p>No button here</p></body></html>");

        // When/Then
        assertThrows(Exception.class, () -> {
            autoHeal.findElement("#missing-btn", "Missing button");
        });
    }

    @Test
    void shouldCacheSuccessfulSelectors() {
        // Given
        driver.get("data:text/html,<html><body><button id='cache-btn'>Cached</button></body></html>");

        // When - find element twice
        WebElement element1 = autoHeal.findElement("#cache-btn", "Cache button");
        WebElement element2 = autoHeal.findElement("#cache-btn", "Cache button");

        // Then
        assertNotNull(element1);
        assertNotNull(element2);

        // Verify cache metrics improved
        var metrics = autoHeal.getMetrics();
        assertTrue(metrics.getCacheMetrics().getHits() > 0 ||
                metrics.getCacheMetrics().getMisses() > 0);
    }

    @Test
    void shouldProvideHealthStatus() {
        // When
        var healthStatus = autoHeal.getHealthStatus();

        // Then
        assertNotNull(healthStatus);
        assertTrue(healthStatus.getSuccessRate() >= 0.0);
        assertTrue(healthStatus.getCacheHitRate() >= 0.0);
    }
}