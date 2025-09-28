package com.autoheal.unit;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.config.AIConfig;
import com.autoheal.model.AIProvider;
import com.autoheal.core.WebAutomationAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AutoHealLocator
 */
@ExtendWith(MockitoExtension.class)
class AutoHealLocatorTest {

    @Mock
    private WebDriver mockDriver;

    @Mock
    private WebElement mockElement;

    @Mock
    private WebAutomationAdapter mockAdapter;

    private AutoHealLocator autoHealLocator;

    @BeforeEach
    void setUp() {
        AIConfig mockAiConfig = AIConfig.builder()
                .provider(AIProvider.MOCK)
                .apiKey("test-key")
                .build();

        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .cache(CacheConfig.builder()
                        .maximumSize(100)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .build())
                .ai(mockAiConfig)
                .build();

        autoHealLocator = AutoHealLocator.builder()
                .withWebAdapter(mockAdapter)
                .withConfiguration(config)
                .build();
    }

    @Test
    void shouldFindElementWithOriginalSelector() {
        // Given
        when(mockAdapter.findElements(any(By.class)))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(mockElement)));

        // When
        CompletableFuture<WebElement> result = autoHealLocator.findElementAsync("#test", "Test element");

        // Then
        assertNotNull(result);
        WebElement element = result.join();
        assertEquals(mockElement, element);
        verify(mockAdapter).findElements(By.cssSelector("#test"));
    }

    @Test
    void shouldReturnHealthStatus() {
        // When
        var healthStatus = autoHealLocator.getHealthStatus();

        // Then
        assertNotNull(healthStatus);
        assertTrue(healthStatus.getSuccessRate() >= 0.0);
        assertTrue(healthStatus.getCacheHitRate() >= 0.0);
    }

    @Test
    void shouldProvideMetrics() {
        // When
        var metrics = autoHealLocator.getMetrics();

        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getLocatorMetrics());
        assertNotNull(metrics.getCacheMetrics());
    }

    @Test
    void shouldShutdownGracefully() {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> autoHealLocator.shutdown());
    }

    @Test
    void shouldCheckElementPresence() {
        // Given
        when(mockAdapter.findElements(any(By.class)))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(mockElement)));

        // When
        CompletableFuture<Boolean> result = autoHealLocator.isElementPresentAsync("#test", "Test element");

        // Then
        assertTrue(result.join());
    }

    @Test
    void shouldHandleElementNotFound() {
        // Given
        when(mockAdapter.findElements(any(By.class)))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList()));

        // When
        CompletableFuture<Boolean> result = autoHealLocator.isElementPresentAsync("#test", "Test element");

        // Then
        assertFalse(result.join());
    }
}