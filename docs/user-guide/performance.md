# Performance Optimization Guide

## Overview

AutoHeal Locator offers several configuration options to optimize performance based on your specific needs. This guide covers strategies for reducing latency, minimizing costs, and maximizing throughput.

---

## Performance Strategies

### 1. Execution Strategy Configuration

Choose the right execution strategy based on your requirements:

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .performance(PerformanceConfig.builder()
        .executionStrategy(ExecutionStrategy.DOM_ONLY)      // Fastest, cheapest
        .executionStrategy(ExecutionStrategy.VISUAL_ONLY)   // Visual analysis only
        .executionStrategy(ExecutionStrategy.HYBRID)        // DOM first, visual fallback
        .executionStrategy(ExecutionStrategy.PARALLEL)      // Both simultaneously
        .build())
    .build();
```

#### Strategy Comparison

| Strategy | Speed | Cost | Accuracy | Best For |
|----------|-------|------|----------|----------|
| `DOM_ONLY` | ⚡ Fastest | 💰 Cheapest | ⭐⭐⭐ Good | Simple pages, static content |
| `VISUAL_ONLY` | ⚡⚡ Slow | 💰💰💰 Expensive | ⭐⭐⭐⭐⭐ Excellent | Complex UIs, dynamic content |
| `HYBRID` | ⚡⚡ Medium | 💰💰 Medium | ⭐⭐⭐⭐ Very Good | **Recommended** - Balanced |
| `PARALLEL` | ⚡⚡⚡ Slowest | 💰💰💰💰 Most Expensive | ⭐⭐⭐⭐⭐ Best | Critical tests, high accuracy needed |

---

## Caching Configuration

Implement aggressive caching to reduce AI API calls:

### Basic Caching

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .cache(CacheConfig.builder()
        .enabled(true)
        .maxSize(10000)                           // Cache up to 10k entries
        .expireAfterWrite(Duration.ofHours(24))   // 24-hour TTL
        .expireAfterAccess(Duration.ofHours(6))   // Remove unused entries after 6h
        .build())
    .build();
```

### Advanced Caching with Persistence

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .cache(CacheConfig.builder()
        .enabled(true)
        .maxSize(50000)
        .expireAfterWrite(Duration.ofDays(7))     // Keep cache for a week
        .persistToDisk(true)                      // Survive application restarts
        .cacheDirectory("./autoheal-cache")       // Custom cache location
        .build())
    .build();
```

### Cache Performance Metrics

```java
// Monitor cache effectiveness
AutoHealLocator autoHeal = AutoHealLocator.builder()
    .withConfiguration(config)
    .build();

CacheMetrics metrics = autoHeal.getCacheMetrics();
System.out.println("Cache hit rate: " + metrics.getHitRate() + "%");
System.out.println("Cache size: " + metrics.getSize());
System.out.println("Evictions: " + metrics.getEvictionCount());
```

---

## AI Provider Optimization

### Model Selection for Performance

**For Speed (Development/Testing):**
```java
.ai(AIConfig.builder()
    .provider(AIProvider.OPENAI)
    .model("gpt-4o-mini")                    // Fastest OpenAI model
    .timeout(Duration.ofSeconds(15))         // Short timeout
    .maxRetries(1)                          // Fewer retries
    .build())
```

**For Accuracy (Production):**
```java
.ai(AIConfig.builder()
    .provider(AIProvider.OPENAI)
    .model("gpt-4o")                        // Most accurate
    .timeout(Duration.ofSeconds(45))        // Longer timeout
    .maxRetries(3)                          // More retries
    .build())
```

**For Cost Optimization:**
```java
.ai(AIConfig.builder()
    .provider(AIProvider.GEMINI)
    .model("gemini-1.5-flash")              // Cheapest option
    .visualAnalysisEnabled(false)           // Disable expensive visual analysis
    .build())
```

### Local LLM for Zero-Cost Operation

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.LOCAL_LLM)
        .apiUrl("http://localhost:11434/v1/chat/completions")
        .model("llama3.1:8b")               // Good performance/resource balance
        .timeout(Duration.ofSeconds(60))     // Longer timeout for local processing
        .build())
    .build();
```

---

## Thread Pool Configuration

Optimize parallel processing for high-throughput scenarios:

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .performance(PerformanceConfig.builder()
        .threadPoolSize(8)                   // Match CPU cores
        .maxConcurrentRequests(20)           // Limit concurrent AI requests
        .requestQueueSize(100)               // Buffer for high load
        .build())
    .build();
```

### Thread Pool Sizing Guidelines

| Scenario | Thread Pool Size | Max Concurrent Requests |
|----------|------------------|------------------------|
| Development | 2-4 | 5 |
| CI/CD Pipeline | 4-6 | 10 |
| Load Testing | 8-16 | 20-50 |
| Production Suite | CPU cores × 2 | 50-100 |

---

## Network Optimization

### Connection Pooling

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .network(NetworkConfig.builder()
        .connectionPoolSize(20)              // Reuse HTTP connections
        .connectionTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(30))
        .retryOnConnectionFailure(true)
        .build())
    .build();
```

### Request Batching

```java
// Batch multiple healing requests
List<LocatorRequest> requests = Arrays.asList(
    new LocatorRequest("login button", "#login-btn"),
    new LocatorRequest("username field", "#username"),
    new LocatorRequest("password field", "#password")
);

// Process in batch for better performance
List<LocatorResult> results = autoHeal.healLocatorsBatch(requests);
```

---

## Memory Optimization

### DOM Analysis Optimization

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .dom(DOMConfig.builder()
        .maxHtmlSize(1024 * 1024)           // 1MB limit for HTML analysis
        .removeUnnecessaryElements(true)     // Strip scripts, styles, comments
        .compressWhitespace(true)           // Reduce token count
        .build())
    .build();
```

### Visual Analysis Optimization

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .visual(VisualConfig.builder()
        .screenshotFormat(ScreenshotFormat.JPEG)  // Smaller file size
        .imageQuality(0.8)                       // 80% quality for balance
        .maxImageSize(800, 600)                  // Resize large screenshots
        .enableImageCompression(true)            // Further reduce size
        .build())
    .build();
```

---

## Performance Monitoring

### Built-in Metrics

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .monitoring(MonitoringConfig.builder()
        .metricsEnabled(true)
        .performanceLogging(true)
        .slowOperationThreshold(Duration.ofSeconds(5))  // Log slow operations
        .build())
    .build();

// Access metrics
PerformanceMetrics metrics = autoHeal.getPerformanceMetrics();
System.out.println("Average healing time: " + metrics.getAverageHealingTime() + "ms");
System.out.println("Success rate: " + metrics.getSuccessRate() + "%");
System.out.println("Cache hit rate: " + metrics.getCacheHitRate() + "%");
```

### Custom Performance Tracking

```java
public class PerformanceTracker {
    public void trackHealingPerformance() {
        long startTime = System.currentTimeMillis();

        WebElement element = autoHeal.findElement(driver, "Submit button", "#submit");

        long healingTime = System.currentTimeMillis() - startTime;

        if (healingTime > 3000) {
            logger.warn("Slow healing detected: {}ms for selector", healingTime);
        }

        // Send metrics to monitoring system
        metricsCollector.recordHealingTime(healingTime);
    }
}
```

---

## Environment-Specific Configurations

### Development Environment

```java
public static AutoHealConfiguration getDevelopmentConfig() {
    return AutoHealConfiguration.builder()
        .ai(AIConfig.builder()
            .provider(AIProvider.MOCK)          // No AI costs
            .build())
        .cache(CacheConfig.builder()
            .enabled(false)                     // Always fresh results
            .build())
        .monitoring(MonitoringConfig.builder()
            .verboseLogging(true)               // Detailed logging
            .build())
        .build();
}
```

### CI/CD Environment

```java
public static AutoHealConfiguration getCIConfig() {
    return AutoHealConfiguration.builder()
        .ai(AIConfig.builder()
            .provider(AIProvider.OPENAI)
            .model("gpt-4o-mini")               // Fast and cheap
            .timeout(Duration.ofSeconds(20))    // Fail fast
            .maxRetries(1)                      // Don't waste time
            .build())
        .cache(CacheConfig.builder()
            .enabled(true)
            .maxSize(1000)                      // Limited cache
            .build())
        .performance(PerformanceConfig.builder()
            .threadPoolSize(4)                  // Limited resources
            .executionStrategy(ExecutionStrategy.DOM_ONLY)
            .build())
        .build();
}
```

### Production Environment

```java
public static AutoHealConfiguration getProductionConfig() {
    return AutoHealConfiguration.builder()
        .ai(AIConfig.builder()
            .provider(AIProvider.OPENAI)
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .model("gpt-4o-mini")
            .timeout(Duration.ofSeconds(30))
            .maxRetries(3)
            .build())
        .cache(CacheConfig.builder()
            .enabled(true)
            .maxSize(50000)                     // Large cache
            .expireAfterWrite(Duration.ofDays(7))
            .persistToDisk(true)
            .build())
        .performance(PerformanceConfig.builder()
            .threadPoolSize(Runtime.getRuntime().availableProcessors() * 2)
            .executionStrategy(ExecutionStrategy.HYBRID)
            .build())
        .resilience(ResilienceConfig.builder()
            .circuitBreakerEnabled(true)        // Fault tolerance
            .build())
        .build();
}
```

---

## Performance Benchmarks

### Typical Performance Metrics

| Operation | DOM Only | Visual Only | Hybrid | Parallel |
|-----------|----------|-------------|--------|----------|
| Simple element | 200-500ms | 1-3s | 300-800ms | 1-3s |
| Complex element | 500-1s | 2-5s | 800ms-2s | 2-5s |
| Failed element | 1-2s | 3-8s | 1.5-3s | 3-8s |

### Optimization Impact

| Optimization | Speed Improvement | Cost Reduction | Complexity |
|-------------|------------------|----------------|------------|
| Caching | 80-95% for cache hits | 80-95% | Low |
| DOM-only strategy | 70-80% | 60-70% | Low |
| Local LLM | Variable | 100% | High |
| Connection pooling | 10-20% | None | Medium |

---

## Best Practices

### 1. Start with Conservative Settings

```java
// Begin with balanced configuration
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .model("gpt-4o-mini")
        .timeout(Duration.ofSeconds(30))
        .build())
    .cache(CacheConfig.builder()
        .enabled(true)
        .maxSize(5000)
        .build())
    .performance(PerformanceConfig.builder()
        .executionStrategy(ExecutionStrategy.HYBRID)
        .build())
    .build();
```

### 2. Monitor and Adjust

- Track healing times and success rates
- Monitor AI API costs
- Adjust timeouts based on actual performance
- Tune cache size based on hit rates

### 3. Environment-Specific Tuning

- Development: Prioritize speed and debugging
- CI/CD: Balance speed and reliability
- Production: Optimize for reliability and cost

### 4. Gradual Optimization

1. Start with default settings
2. Enable caching
3. Tune execution strategy
4. Optimize AI model selection
5. Fine-tune thread pools and timeouts

---

## Troubleshooting Performance Issues

### Slow Healing Times

**Symptoms:** Healing takes >5 seconds consistently

**Solutions:**
- Reduce HTML size limits
- Switch to DOM-only strategy
- Use faster AI model (gpt-4o-mini)
- Decrease timeout values
- Check network connectivity

### High AI Costs

**Symptoms:** Unexpected high API bills

**Solutions:**
- Enable aggressive caching
- Disable visual analysis
- Switch to cheaper models
- Implement request batching
- Use local LLM for development

### Memory Issues

**Symptoms:** OutOfMemoryError or high memory usage

**Solutions:**
- Reduce cache size
- Limit concurrent requests
- Enable HTML compression
- Reduce image sizes
- Clear cache periodically

### Low Success Rates

**Symptoms:** Elements frequently not found

**Solutions:**
- Increase timeouts
- Enable visual analysis
- Use more accurate AI model
- Check HTML preprocessing settings
- Review element descriptions

---

## Next Steps

1. [Configuration Guide](./ai-configuration.md)
2. [Usage Examples](./examples/)
3. [Troubleshooting Guide](./troubleshooting.md)