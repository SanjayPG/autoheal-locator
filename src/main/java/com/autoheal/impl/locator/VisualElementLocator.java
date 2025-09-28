package com.autoheal.impl.locator;

import com.autoheal.core.AIService;
import com.autoheal.core.ElementLocator;
import com.autoheal.metrics.LocatorMetrics;
import com.autoheal.model.AIAnalysisResult;
import com.autoheal.model.ElementCandidate;
import com.autoheal.model.LocatorRequest;
import com.autoheal.model.LocatorResult;
import com.autoheal.model.LocatorStrategy;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Visual element locator using AI-powered screenshot analysis
 */
public class VisualElementLocator implements ElementLocator {
    private static final Logger logger = LoggerFactory.getLogger(VisualElementLocator.class);

    private final AIService aiService;
    private final LocatorMetrics metrics;

    public VisualElementLocator(AIService aiService) {
        this.aiService = aiService;
        this.metrics = new LocatorMetrics();
        logger.info("VisualElementLocator initialized");
    }

    @Override
    public CompletableFuture<LocatorResult> locate(LocatorRequest request) {
        if (!request.getOptions().isEnableVisualAnalysis()) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException("Visual analysis is disabled for this request")
            );
        }

        logger.debug("Starting visual analysis for selector: {}", request.getOriginalSelector());
        long startTime = System.currentTimeMillis();

        return takeScreenshotAndAnalyze(request, startTime)
                .thenCompose(aiResult -> validateAndReturnResult(aiResult, request, startTime))
                .exceptionally(throwable -> {
                    logger.error("Visual analysis failed for selector: {}", request.getOriginalSelector(), throwable);
                    throw new RuntimeException("Visual analysis failed", throwable);
                });
    }

    private CompletableFuture<AIAnalysisResult> takeScreenshotAndAnalyze(LocatorRequest request, long startTime) {
        return request.getAdapter().takeScreenshot()
                .thenCompose(screenshot -> {
                    logger.debug("Screenshot taken (size: {} bytes), analyzing with AI", screenshot.length);
                    return aiService.analyzeVisual(screenshot, request.getDescription());
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to take screenshot or analyze visually", throwable);
                    } else {
                        logger.debug("Visual analysis completed with confidence: {}", result.getConfidence());
                    }
                });
    }

    private CompletableFuture<LocatorResult> validateAndReturnResult(AIAnalysisResult aiResult,
                                                                     LocatorRequest request,
                                                                     long startTime) {
        String recommendedSelector = aiResult.getRecommendedSelector();

        logger.debug("Trying primary selector from enhanced visual analysis: {}", recommendedSelector);

        // Try the AI-recommended primary selector
        return request.getAdapter().findElements(recommendedSelector)
                .thenApply(elements -> {
                    if (!elements.isEmpty()) {
                        WebElement element = selectBestElement(elements, request);

                        logger.debug("Primary visual selector succeeded: {} (confidence: {})",
                                    recommendedSelector, aiResult.getConfidence());

                        return LocatorResult.builder()
                                .element(element)
                                .actualSelector(recommendedSelector)
                                .strategy(LocatorStrategy.VISUAL_ANALYSIS)
                                .executionTime(Duration.ofMillis(System.currentTimeMillis() - startTime))
                                .fromCache(false)
                                .confidence(aiResult.getConfidence())
                                .reasoning("Enhanced Visual AI analysis (primary): " + aiResult.getReasoning())
                                .build();
                    } else {
                        // Try robust alternative selectors if primary failed
                        logger.debug("Primary visual selector failed, trying {} alternatives",
                                    aiResult.getAlternatives().size());
                        return tryRobustAlternativeSelectors(aiResult, request, startTime);
                    }
                });
    }

    private LocatorResult tryRobustAlternativeSelectors(AIAnalysisResult aiResult,
                                                       LocatorRequest request,
                                                       long startTime) {
        List<ElementCandidate> alternatives = aiResult.getAlternatives();

        // Sort alternatives by confidence (highest first) for better success rate
        alternatives.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        for (int i = 0; i < alternatives.size(); i++) {
            ElementCandidate candidate = alternatives.get(i);
            String altSelector = candidate.getSelector();

            try {
                logger.debug("Trying robust alternative {} of {}: {} (confidence: {})",
                           i + 1, alternatives.size(), altSelector, candidate.getConfidence());

                List<WebElement> elements = request.getAdapter().findElements(altSelector).get();
                if (!elements.isEmpty()) {
                    WebElement element = selectBestElement(elements, request);

                    // Determine the type of alternative for better reasoning
                    String alternativeType = "alternative";
                    if (candidate.getDescription().contains("text-based")) {
                        alternativeType = "text-based";
                    } else if (candidate.getDescription().contains("attribute-based")) {
                        alternativeType = "attribute-based";
                    }

                    logger.debug("Robust {} selector succeeded: {} (confidence: {})",
                               alternativeType, altSelector, candidate.getConfidence());

                    return LocatorResult.builder()
                            .element(element)
                            .actualSelector(altSelector)
                            .strategy(LocatorStrategy.VISUAL_ANALYSIS)
                            .executionTime(Duration.ofMillis(System.currentTimeMillis() - startTime))
                            .fromCache(false)
                            .confidence(candidate.getConfidence())
                            .reasoning(String.format("Enhanced Visual AI analysis (%s fallback): %s",
                                     alternativeType, aiResult.getReasoning()))
                            .build();
                }
            } catch (Exception e) {
                logger.debug("Robust alternative selector failed: {} - {}", altSelector, e.getMessage());
            }
        }

        // All robust selectors failed - provide comprehensive error information
        List<String> attemptedSelectors = alternatives.stream()
                .map(ElementCandidate::getSelector)
                .collect(Collectors.toList());

        logger.warn("All enhanced visual analysis selectors failed. Primary: {}, Alternatives: {}",
                   aiResult.getRecommendedSelector(), attemptedSelectors);

        throw new RuntimeException("Enhanced visual analysis found " + (1 + alternatives.size()) +
                                 " robust selectors but none worked. Primary: " + aiResult.getRecommendedSelector() +
                                 ", Alternatives: " + attemptedSelectors +
                                 ". This suggests the page structure may have changed significantly.");
    }

    // Keep the old method for backward compatibility
    private LocatorResult tryAlternativeSelectors(AIAnalysisResult aiResult,
                                                  LocatorRequest request,
                                                  long startTime) {
        return tryRobustAlternativeSelectors(aiResult, request, startTime);
    }

    private WebElement selectBestElement(List<WebElement> elements, LocatorRequest request) {
        if (elements.size() == 1) {
            return elements.get(0);
        }

        // If multiple elements found, use heuristics to select the best one
        // Priority: visible elements, elements with text matching description, etc.
        
        // First, try to find visible elements
        List<WebElement> visibleElements = elements.stream()
                .filter(element -> {
                    try {
                        return element.isDisplayed();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (!visibleElements.isEmpty()) {
            elements = visibleElements;
        }

        // If we still have multiple elements, try to match by text content
        String description = request.getDescription().toLowerCase();
        for (WebElement element : elements) {
            try {
                String text = element.getText().toLowerCase();
                if (text.contains(description) || description.contains(text)) {
                    logger.debug("Selected element based on text match: '{}'", element.getText());
                    return element;
                }
            } catch (Exception e) {
                // Ignore and continue
            }
        }

        // Fallback: return the first element
        logger.debug("Multiple elements found, returning first one");
        return elements.get(0);
    }

    @Override
    public boolean supports(LocatorStrategy strategy) {
        return strategy == LocatorStrategy.VISUAL_ANALYSIS;
    }

    @Override
    public LocatorMetrics getMetrics() {
        return metrics;
    }

    public boolean canHandle(LocatorRequest request) {
        return request.getOptions().isEnableVisualAnalysis() && 
               aiService.isHealthy();
    }
}