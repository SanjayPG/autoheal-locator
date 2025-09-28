package com.autoheal.impl.locator;

import com.autoheal.core.AIService;
import com.autoheal.core.ElementLocator;
import com.autoheal.metrics.LocatorMetrics;
import com.autoheal.model.LocatorRequest;
import com.autoheal.model.LocatorResult;
import com.autoheal.model.LocatorStrategy;
import com.autoheal.util.LocatorTypeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * DOM-based element locator implementation
 */
public class DOMElementLocator implements ElementLocator {
    private static final Logger logger = LoggerFactory.getLogger(DOMElementLocator.class);

    private final AIService aiService;
    private final LocatorMetrics metrics;

    public DOMElementLocator(AIService aiService) {
        this.aiService = aiService;
        this.metrics = new LocatorMetrics();
        logger.info("DOMElementLocator initialized");
    }

    @Override
    public CompletableFuture<LocatorResult> locate(LocatorRequest request) {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting DOM analysis for selector: {}", request.getOriginalSelector());

        return request.getAdapter().getPageSource()
                .thenCompose(html -> {
                    logger.debug("Retrieved page source (length: {} chars), analyzing with AI", html.length());
                    return aiService.analyzeDOM(html, request.getDescription(), request.getOriginalSelector());
                })
                .thenCompose(aiResult -> {
                    logger.debug("AI analysis completed, recommended selector: {} (confidence: {})",
                            aiResult.getRecommendedSelector(), aiResult.getConfidence());
                    return validateAndReturnResult(request, aiResult, startTime);
                })
                .exceptionally(throwable -> {
                    logger.error("DOM analysis failed for selector '{}': {}",
                                request.getOriginalSelector(), throwable.getMessage());
                    throw new RuntimeException("DOM analysis failed: " + throwable.getMessage(), throwable);
                })
                .whenComplete((result, throwable) -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    boolean success = throwable == null && result != null && result.getElement() != null;
                    metrics.recordRequest(success, executionTime, false);

                    if (success) {
                        logger.info("DOM locator successfully found element using selector: {}",
                                result.getActualSelector());
                    } else {
                        logger.warn("DOM locator failed for selector: {}", request.getOriginalSelector());
                    }
                });
    }

    @Override
    public boolean supports(LocatorStrategy strategy) {
        return strategy == LocatorStrategy.DOM_ANALYSIS;
    }

    @Override
    public LocatorMetrics getMetrics() {
        return metrics;
    }

    private CompletableFuture<LocatorResult> validateAndReturnResult(LocatorRequest request,
                                                                     com.autoheal.model.AIAnalysisResult aiResult,
                                                                     long startTime) {
        // Use auto-detection for the AI-recommended selector
        org.openqa.selenium.By healedBy = LocatorTypeDetector.autoCreateBy(aiResult.getRecommendedSelector());
        return request.getAdapter().findElements(healedBy)
                .thenCompose(elements -> {
                    if (!elements.isEmpty()) {
                        logger.debug("Validated AI suggestion: found {} elements", elements.size());

                        // Use AI disambiguation if multiple elements found
                        CompletableFuture<org.openqa.selenium.WebElement> elementFuture;
                        if (elements.size() > 1) {
                            logger.debug("Multiple elements found, using AI for disambiguation");
                            elementFuture = aiService.selectBestMatchingElement(elements, request.getDescription());
                        } else {
                            elementFuture = CompletableFuture.completedFuture(elements.get(0));
                        }

                        return elementFuture.thenApply(selectedElement ->
                                LocatorResult.builder()
                                        .element(selectedElement)
                                        .actualSelector(aiResult.getRecommendedSelector())
                                        .strategy(LocatorStrategy.DOM_ANALYSIS)
                                        .executionTime(Duration.ofMillis(System.currentTimeMillis() - startTime))
                                        .fromCache(false)
                                        .confidence(aiResult.getConfidence())
                                        .reasoning(aiResult.getReasoning())
                                        .build());
                    } else {
                        logger.warn("AI suggested selector found no elements: {}", aiResult.getRecommendedSelector());
                        throw new RuntimeException("AI suggested selector found no elements: " +
                                aiResult.getRecommendedSelector());
                    }
                });
    }
}