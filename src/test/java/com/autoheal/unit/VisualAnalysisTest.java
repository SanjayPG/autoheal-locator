package com.autoheal.unit;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.config.LocatorOptions;
import com.autoheal.core.WebAutomationAdapter;
import com.autoheal.model.AIProvider;
import com.autoheal.model.ElementContext;
import org.openqa.selenium.By;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for visual analysis functionality
 */
@ExtendWith(MockitoExtension.class)
class VisualAnalysisTest {

    @Mock
    private WebAutomationAdapter mockAdapter;

    @Mock
    private WebElement mockElement;

    private AutoHealLocator autoHealLocator;

    @BeforeEach
    void setUp() {
        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .cache(CacheConfig.builder()
                        .maximumSize(100)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .build())
                .ai(AIConfig.builder()
                        .provider(AIProvider.MOCK)
                        .apiKey("test-key")
                        .visualAnalysisEnabled(true)
                        .timeout(Duration.ofSeconds(10))
                        .build())
                .build();

        autoHealLocator = AutoHealLocator.builder()
                .withWebAdapter(mockAdapter)
                .withConfiguration(config)
                .build();
    }

    @Test
    @org.junit.jupiter.api.Disabled("Temporarily disabled until visual analysis mocking is fixed")
    void shouldUseVisualAnalysisWhenEnabled() {
        // Given - Make original selector fail, then visual analysis succeeds
        byte[] mockScreenshot = "mock-screenshot-data".getBytes();
        when(mockAdapter.takeScreenshot())
                .thenReturn(CompletableFuture.completedFuture(mockScreenshot));
        when(mockAdapter.getPageSource())
                .thenReturn(CompletableFuture.completedFuture("<html><button>Submit</button></html>"));
        ElementContext mockContext = ElementContext.builder()
                .textContent("Submit")
                .build();
        when(mockAdapter.getElementContext(any()))
                .thenReturn(CompletableFuture.completedFuture(mockContext));
        
        // First call (original selector) fails, second call (from visual analysis) succeeds
        when(mockAdapter.findElements("#broken-selector"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList())); // Original fails
        when(mockAdapter.findElements("visual-mock-selector"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(mockElement))); // Visual succeeds
        // Also handle potential DOM analysis fallback
        when(mockAdapter.findElements("button[data-testid='mock-element']"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(mockElement)));

        LocatorOptions options = LocatorOptions.builder()
                .enableVisualAnalysis(true)
                .build();

        // When
        CompletableFuture<WebElement> result = autoHealLocator
                .findElementAsync("#broken-selector", "Submit button", options);

        // Then
        assertNotNull(result);
        WebElement element = result.join();
        assertEquals(mockElement, element);
        
        // Verify screenshot was taken for visual analysis
        verify(mockAdapter, atLeastOnce()).takeScreenshot();
    }

    @Test
    void shouldFallbackWhenVisualAnalysisDisabled() {
        // Given
        when(mockAdapter.findElements(any(By.class)))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(mockElement)));

        LocatorOptions options = LocatorOptions.builder()
                .enableVisualAnalysis(false)
                .build();

        // When
        CompletableFuture<WebElement> result = autoHealLocator
                .findElementAsync("#working-selector", "Submit button", options);

        // Then
        assertNotNull(result);
        WebElement element = result.join();
        assertEquals(mockElement, element);

        // Verify screenshot was NOT taken when visual analysis is disabled
        verify(mockAdapter, never()).takeScreenshot();
    }

    @Test
    @org.junit.jupiter.api.Disabled("Temporarily disabled until visual analysis mocking is fixed")
    void shouldHandleVisualAnalysisFailure() {
        // Given - Visual analysis fails, but DOM analysis succeeds
        byte[] mockScreenshot = "mock-screenshot-data".getBytes();
        when(mockAdapter.takeScreenshot())
                .thenReturn(CompletableFuture.completedFuture(mockScreenshot));
        when(mockAdapter.getPageSource())
                .thenReturn(CompletableFuture.completedFuture("<html><button>Submit</button></html>"));
        ElementContext mockContext = ElementContext.builder()
                .textContent("Submit")
                .build();
        when(mockAdapter.getElementContext(any()))
                .thenReturn(CompletableFuture.completedFuture(mockContext));
        
        // Original selector fails, but DOM analysis finds it
        when(mockAdapter.findElements("#broken-selector"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList())); // Original fails
        when(mockAdapter.findElements("visual-mock-selector"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList())); // Visual fails
        when(mockAdapter.findElements("button[data-testid='mock-element']"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(mockElement))); // DOM analysis succeeds

        LocatorOptions options = LocatorOptions.builder()
                .enableVisualAnalysis(true)
                .build();

        // When
        CompletableFuture<WebElement> result = autoHealLocator
                .findElementAsync("#broken-selector", "Submit button", options);

        // Then - Should still succeed using DOM analysis fallback
        assertNotNull(result);
        WebElement element = result.join();
        assertEquals(mockElement, element);
    }
}