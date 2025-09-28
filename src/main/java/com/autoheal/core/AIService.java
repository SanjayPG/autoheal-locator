package com.autoheal.core;

import com.autoheal.model.AIAnalysisResult;
import com.autoheal.metrics.AIServiceMetrics;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI-powered element analysis services
 */
public interface AIService {

    /**
     * Analyze DOM structure to find element selectors
     *
     * @param html The HTML content to analyze
     * @param description Human-readable description of the target element
     * @param previousSelector The selector that previously worked but now fails
     * @return CompletableFuture containing AI analysis result
     */
    CompletableFuture<AIAnalysisResult> analyzeDOM(String html, String description, String previousSelector);

    /**
     * Analyze visual screenshot to find element locations
     *
     * @param screenshot The screenshot data as byte array
     * @param description Human-readable description of the target element
     * @return CompletableFuture containing AI analysis result
     */
    CompletableFuture<AIAnalysisResult> analyzeVisual(byte[] screenshot, String description);

    /**
     * Check if the AI service is healthy and responsive
     *
     * @return true if service is healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Get performance metrics for the AI service
     *
     * @return Current AI service metrics
     */
    AIServiceMetrics getMetrics();

    /**
     * Select the best matching element from a list based on description
     *
     * @param elements List of candidate WebElements
     * @param description Human-readable description of the target element
     * @return CompletableFuture containing the best matching WebElement
     */
    CompletableFuture<WebElement> selectBestMatchingElement(List<WebElement> elements, String description);
}