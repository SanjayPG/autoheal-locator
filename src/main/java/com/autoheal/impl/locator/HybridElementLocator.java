package com.autoheal.impl.locator;

import com.autoheal.core.ElementLocator;
import com.autoheal.metrics.LocatorMetrics;
import com.autoheal.model.LocatorRequest;
import com.autoheal.model.LocatorResult;
import com.autoheal.model.LocatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Hybrid element locator combining multiple strategies
 */
public class HybridElementLocator implements ElementLocator {
    private static final Logger logger = LoggerFactory.getLogger(HybridElementLocator.class);

    private final List<ElementLocator> locators;
    private final LocatorMetrics metrics;

    public HybridElementLocator(List<ElementLocator> locators) {
        this.locators = new ArrayList<>(locators);
        this.metrics = new LocatorMetrics();
        logger.info("HybridElementLocator initialized with {} strategies", locators.size());
    }

    @Override
    public CompletableFuture<LocatorResult> locate(LocatorRequest request) {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting hybrid location strategy for selector: {}", request.getOriginalSelector());

        // Run all locators in parallel and return the best result
        List<CompletableFuture<LocatorResult>> futures = locators.stream()
                .map(locator -> {
                    logger.debug("Starting locator: {}", locator.getClass().getSimpleName());
                    return locator.locate(request)
                            .exceptionally(throwable -> {
                                logger.debug("Locator {} failed: {}",
                                        locator.getClass().getSimpleName(), throwable.getMessage());
                                return null;
                            });
                })
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<LocatorResult> results = futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .filter(result -> result.getElement() != null)
                            .collect(Collectors.toList());

                    logger.debug("Hybrid strategy completed: {} successful results out of {} attempts",
                            results.size(), locators.size());

                    if (results.isEmpty()) {
                        throw new RuntimeException("All locator strategies failed for selector: " +
                                request.getOriginalSelector());
                    }

                    // Select best result based on confidence
                    LocatorResult bestResult = results.stream()
                            .max(Comparator.comparing(LocatorResult::getConfidence))
                            .orElseThrow(() -> new RuntimeException("No valid results"));

                    logger.info("Best result selected: strategy={}, confidence={}, selector={}",
                            bestResult.getStrategy(), bestResult.getConfidence(), bestResult.getActualSelector());

                    // Update result to reflect hybrid strategy
                    return LocatorResult.builder()
                            .element(bestResult.getElement())
                            .actualSelector(bestResult.getActualSelector())
                            .strategy(LocatorStrategy.HYBRID)
                            .executionTime(Duration.ofMillis(System.currentTimeMillis() - startTime))
                            .fromCache(false)
                            .confidence(bestResult.getConfidence())
                            .reasoning("Hybrid strategy selected: " + bestResult.getReasoning())
                            .build();
                })
                .whenComplete((result, throwable) -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    boolean success = throwable == null && result != null && result.getElement() != null;
                    metrics.recordRequest(success, executionTime, false);

                    if (success) {
                        logger.info("Hybrid locator successfully found element in {}ms", executionTime);
                    } else {
                        logger.warn("Hybrid locator failed after {}ms", executionTime);
                    }
                });
    }

    @Override
    public boolean supports(LocatorStrategy strategy) {
        return strategy == LocatorStrategy.HYBRID;
    }

    @Override
    public LocatorMetrics getMetrics() {
        return metrics;
    }

    /**
     * Get the underlying locators for inspection
     *
     * @return list of configured locators
     */
    public List<ElementLocator> getLocators() {
        return new ArrayList<>(locators);
    }
}