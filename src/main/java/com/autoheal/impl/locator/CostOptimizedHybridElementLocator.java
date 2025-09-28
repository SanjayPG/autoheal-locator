package com.autoheal.impl.locator;

import com.autoheal.core.ElementLocator;
import com.autoheal.metrics.LocatorMetrics;
import com.autoheal.model.ExecutionStrategy;
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
 * Cost-optimized hybrid element locator with configurable execution strategies
 */
public class CostOptimizedHybridElementLocator implements ElementLocator {
    private static final Logger logger = LoggerFactory.getLogger(CostOptimizedHybridElementLocator.class);

    private final List<ElementLocator> locators;
    private final LocatorMetrics metrics;
    private final ExecutionStrategy executionStrategy;

    public CostOptimizedHybridElementLocator(List<ElementLocator> locators, ExecutionStrategy executionStrategy) {
        this.locators = new ArrayList<>(locators);
        this.metrics = new LocatorMetrics();
        this.executionStrategy = executionStrategy;
        logger.info("CostOptimizedHybridElementLocator initialized with {} strategies using {} execution", 
                   locators.size(), executionStrategy);
    }

    @Override
    public CompletableFuture<LocatorResult> locate(LocatorRequest request) {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting {} location strategy for selector: {}", executionStrategy, request.getOriginalSelector());

        return switch (executionStrategy) {
            case SEQUENTIAL -> executeSequential(request, startTime);
            case PARALLEL -> executeParallel(request, startTime);
            case SMART_SEQUENTIAL -> executeSmartSequential(request, startTime);
            case DOM_ONLY -> executeDomOnly(request, startTime);
            case VISUAL_FIRST -> executeVisualFirst(request, startTime);
        };
    }

    /**
     * Execute locators sequentially, stop at first success
     * Cost: Lowest (only pays for successful strategy)
     */
    private CompletableFuture<LocatorResult> executeSequential(LocatorRequest request, long startTime) {
        return executeSequentialRecursive(request, startTime, 0);
    }

    private CompletableFuture<LocatorResult> executeSequentialRecursive(LocatorRequest request, long startTime, int index) {
        if (index >= locators.size()) {
            return CompletableFuture.failedFuture(
                new RuntimeException("All locator strategies failed for selector: " + request.getOriginalSelector())
            );
        }

        ElementLocator locator = locators.get(index);
        logger.debug("Trying locator {}: {}", index + 1, locator.getClass().getSimpleName());

        return locator.locate(request)
                .thenCompose(result -> {
                    if (result != null && result.getElement() != null) {
                        logger.info("Sequential strategy succeeded with {}: confidence={}", 
                                   locator.getClass().getSimpleName(), result.getConfidence());
                        return CompletableFuture.completedFuture(buildHybridResult(result, startTime));
                    } else {
                        logger.debug("Locator {} failed, trying next", locator.getClass().getSimpleName());
                        return executeSequentialRecursive(request, startTime, index + 1);
                    }
                })
                .exceptionally(throwable -> {
                    logger.debug("Locator {} failed with exception: {}", locator.getClass().getSimpleName(), throwable.getMessage());
                    return executeSequentialRecursive(request, startTime, index + 1).join();
                });
    }

    /**
     * Execute all locators in parallel, select best result
     * Cost: Highest (pays for all strategies)
     */
    private CompletableFuture<LocatorResult> executeParallel(LocatorRequest request, long startTime) {
        List<CompletableFuture<LocatorResult>> futures = locators.stream()
                .map(locator -> {
                    logger.debug("Starting parallel locator: {}", locator.getClass().getSimpleName());
                    return locator.locate(request)
                            .exceptionally(throwable -> {
                                logger.debug("Parallel locator {} failed: {}", 
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

                    if (results.isEmpty()) {
                        throw new RuntimeException("All parallel locator strategies failed for selector: " + 
                                                 request.getOriginalSelector());
                    }

                    LocatorResult bestResult = results.stream()
                            .max(Comparator.comparing(LocatorResult::getConfidence))
                            .orElseThrow();

                    logger.info("Parallel strategy selected best result: strategy={}, confidence={}", 
                               bestResult.getStrategy(), bestResult.getConfidence());

                    return buildHybridResult(bestResult, startTime);
                });
    }

    /**
     * Smart strategy: Try DOM first (cheaper), then Visual if DOM fails
     * Cost: Medium (DOM is cheaper, Visual only if needed)
     */
    private CompletableFuture<LocatorResult> executeSmartSequential(LocatorRequest request, long startTime) {
        // Find DOM locator first (cheaper)
        ElementLocator domLocator = locators.stream()
                .filter(loc -> loc instanceof DOMElementLocator)
                .findFirst()
                .orElse(null);

        if (domLocator != null) {
            logger.debug("Smart strategy: Trying DOM analysis first (cost-effective)");
            return domLocator.locate(request)
                    .thenCompose(result -> {
                        if (result != null && result.getElement() != null) {
                            logger.info("Smart strategy: DOM analysis succeeded, skipping visual (cost saved!)");
                            return CompletableFuture.completedFuture(buildHybridResult(result, startTime));
                        } else {
                            logger.debug("Smart strategy: DOM failed, trying visual analysis");
                            return tryRemainingLocators(request, startTime, domLocator);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.debug("Smart strategy: DOM failed with exception, trying visual analysis");
                        return tryRemainingLocators(request, startTime, domLocator).join();
                    });
        } else {
            // No DOM locator, fall back to sequential
            return executeSequential(request, startTime);
        }
    }

    /**
     * DOM-only strategy: Skip visual analysis entirely
     * Cost: Lowest (DOM only)
     */
    private CompletableFuture<LocatorResult> executeDomOnly(LocatorRequest request, long startTime) {
        ElementLocator domLocator = locators.stream()
                .filter(loc -> loc instanceof DOMElementLocator)
                .findFirst()
                .orElse(null);

        if (domLocator == null) {
            return CompletableFuture.failedFuture(
                new RuntimeException("DOM_ONLY strategy requested but no DOM locator available")
            );
        }

        logger.debug("DOM-only strategy: Using only DOM analysis (maximum cost savings)");
        return domLocator.locate(request)
                .thenApply(result -> {
                    if (result != null && result.getElement() != null) {
                        logger.info("DOM-only strategy succeeded: confidence={}", result.getConfidence());
                        return buildHybridResult(result, startTime);
                    } else {
                        throw new RuntimeException("DOM-only strategy failed for selector: " + request.getOriginalSelector());
                    }
                });
    }

    /**
     * Visual-first strategy: Try visual first, then DOM if visual fails
     * Cost: High (Visual is expensive)
     */
    private CompletableFuture<LocatorResult> executeVisualFirst(LocatorRequest request, long startTime) {
        ElementLocator visualLocator = locators.stream()
                .filter(loc -> loc instanceof VisualElementLocator)
                .findFirst()
                .orElse(null);

        if (visualLocator != null) {
            logger.debug("Visual-first strategy: Trying visual analysis first");
            return visualLocator.locate(request)
                    .thenCompose(result -> {
                        if (result != null && result.getElement() != null) {
                            logger.info("Visual-first strategy: Visual analysis succeeded");
                            return CompletableFuture.completedFuture(buildHybridResult(result, startTime));
                        } else {
                            logger.debug("Visual-first strategy: Visual failed, trying DOM analysis");
                            return tryRemainingLocators(request, startTime, visualLocator);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.debug("Visual-first strategy: Visual failed with exception, trying DOM analysis");
                        return tryRemainingLocators(request, startTime, visualLocator).join();
                    });
        } else {
            // No visual locator, fall back to sequential
            return executeSequential(request, startTime);
        }
    }

    private CompletableFuture<LocatorResult> tryRemainingLocators(LocatorRequest request, long startTime, ElementLocator excludeLocator) {
        List<ElementLocator> remainingLocators = locators.stream()
                .filter(loc -> !loc.equals(excludeLocator))
                .collect(Collectors.toList());

        return new CostOptimizedHybridElementLocator(remainingLocators, ExecutionStrategy.SEQUENTIAL)
                .locate(request);
    }

    private LocatorResult buildHybridResult(LocatorResult originalResult, long startTime) {
        return LocatorResult.builder()
                .element(originalResult.getElement())
                .actualSelector(originalResult.getActualSelector())
                .strategy(LocatorStrategy.HYBRID)
                .executionTime(Duration.ofMillis(System.currentTimeMillis() - startTime))
                .fromCache(false)
                .confidence(originalResult.getConfidence())
                .reasoning("Cost-optimized " + executionStrategy + " strategy: " + originalResult.getReasoning())
                .build();
    }

    @Override
    public boolean supports(LocatorStrategy strategy) {
        return strategy == LocatorStrategy.HYBRID;
    }

    @Override
    public LocatorMetrics getMetrics() {
        return metrics;
    }

    public ExecutionStrategy getExecutionStrategy() {
        return executionStrategy;
    }

    public List<ElementLocator> getLocators() {
        return new ArrayList<>(locators);
    }
}