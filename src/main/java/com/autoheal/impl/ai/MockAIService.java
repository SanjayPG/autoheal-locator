package com.autoheal.impl.ai;

import com.autoheal.core.AIService;
import com.autoheal.metrics.AIServiceMetrics;
import com.autoheal.model.AIAnalysisResult;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock AI service for testing purposes
 */
public class MockAIService implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(MockAIService.class);

    private final Map<String, AIAnalysisResult> mockResponses = new ConcurrentHashMap<>();
    private final AIServiceMetrics metrics = new AIServiceMetrics();

    public MockAIService() {
        logger.debug("MockAIService initialized");
    }

    public void addMockResponse(String input, AIAnalysisResult result) {
        mockResponses.put(input, result);
        logger.debug("Added mock AI response for input: {}", input);
    }

    public void addMockResponse(String description, String selector, double confidence) {
        AIAnalysisResult result = AIAnalysisResult.builder()
                .recommendedSelector(selector)
                .confidence(confidence)
                .reasoning("Mock AI response for: " + description)
                .build();
        mockResponses.put(description, result);
        logger.debug("Added mock AI response: {} -> {} (confidence: {})", description, selector, confidence);
    }

    @Override
    public CompletableFuture<AIAnalysisResult> analyzeDOM(String html, String description, String previousSelector) {
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100); // Simulate processing time

                AIAnalysisResult result = mockResponses.getOrDefault(description,
                        AIAnalysisResult.builder()
                                .recommendedSelector("button[data-testid='mock-element']")
                                .confidence(0.85)
                                .reasoning("Mock AI analysis for: " + description)
                                .build());

                metrics.recordRequest(true, System.currentTimeMillis() - startTime);
                logger.debug("Mock AI analyzed DOM for: {} -> {}", description, result.getRecommendedSelector());
                return result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                metrics.recordRequest(false, System.currentTimeMillis() - startTime);
                throw new RuntimeException("Mock AI interrupted", e);
            }
        });
    }

    @Override
    public CompletableFuture<AIAnalysisResult> analyzeVisual(byte[] screenshot, String description) {
        AIAnalysisResult result = AIAnalysisResult.builder()
                .recommendedSelector("visual-mock-selector")
                .confidence(0.75)
                .reasoning("Mock visual analysis for: " + description)
                .build();
        logger.debug("Mock AI visual analysis for: {} -> {}", description, result.getRecommendedSelector());
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public AIServiceMetrics getMetrics() {
        return metrics;
    }

    @Override
    public CompletableFuture<WebElement> selectBestMatchingElement(List<WebElement> elements, String description) {
        if (elements.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Elements list cannot be empty"));
        }

        if (elements.size() == 1) {
            return CompletableFuture.completedFuture(elements.get(0));
        }

        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Mock AI disambiguation for {} elements with description: {}", elements.size(), description);

            // Simple mock logic - look for element with matching text
            String descLower = description.toLowerCase();

            for (WebElement element : elements) {
                try {
                    String elementText = element.getText();
                    if (elementText != null && elementText.toLowerCase().contains(descLower)) {
                        logger.debug("Mock AI selected element with text: {}", elementText);
                        return element;
                    }
                } catch (Exception e) {
                    // Continue to next element if there's an issue getting text
                }
            }

            // If no element text matches, try matching by attributes
            for (WebElement element : elements) {
                try {
                    String ariaLabel = element.getAttribute("aria-label");
                    String id = element.getAttribute("id");
                    String className = element.getAttribute("class");

                    if ((ariaLabel != null && ariaLabel.toLowerCase().contains(descLower)) ||
                        (id != null && id.toLowerCase().contains(descLower)) ||
                        (className != null && className.toLowerCase().contains(descLower))) {
                        logger.debug("Mock AI selected element by attributes");
                        return element;
                    }
                } catch (Exception e) {
                    // Continue to next element if there's an issue getting attributes
                }
            }

            // Fallback to first element
            logger.debug("Mock AI falling back to first element");
            return elements.get(0);
        });
    }
}