# Troubleshooting Guide

Common issues and solutions when using AutoHeal Locator.

## Element Not Found Issues

### Problem: Element still not found after AI healing

**Symptoms:**
- `ElementNotFoundException` even with AI enabled
- Error message: "Element not found after all healing attempts"

**Solutions:**

1. **Check AI Configuration**
   ```java
   // Verify API key is set
   AIConfig aiConfig = AIConfig.builder()
       .apiKey("your-api-key")  // Must be valid
       .provider(AIProvider.OPENAI)
       .build();
   ```

2. **Enable Debug Logging**
   ```java
   AutoHealConfiguration config = AutoHealConfiguration.builder()
       .enableDebugLogging(true)
       .build();
   ```

3. **Try Different Execution Strategy**
   ```java
   // Switch to visual-first for complex pages
   locator = new AutoHealLocator.Builder()
       .withExecutionStrategy(ExecutionStrategy.VISUAL_FIRST)
       .build();
   ```

### Problem: Intermittent element location failures

**Solutions:**

1. **Increase Timeout**
   ```java
   locator.findElement("button", Duration.ofSeconds(10));
   ```

2. **Add Wait Conditions**
   ```java
   // Wait for page to stabilize
   Thread.sleep(1000);
   WebElement element = locator.findElement("Submit button");
   ```

## Performance Issues

### Problem: Slow element location

**Symptoms:**
- High response times (>5 seconds)
- Frequent AI API calls

**Solutions:**

1. **Enable Caching**
   ```java
   AutoHealConfiguration config = AutoHealConfiguration.builder()
       .cacheConfig(CacheConfig.builder()
           .enableCaching(true)
           .maxCacheSize(1000)
           .build())
       .build();
   ```

2. **Use DOM_ONLY Strategy**
   ```java
   // For simple pages without layout changes
   locator = new AutoHealLocator.Builder()
       .withExecutionStrategy(ExecutionStrategy.DOM_ONLY)
       .build();
   ```

3. **Optimize Element Descriptions**
   ```java
   // Good: Specific and unique
   locator.findElement("Blue Submit button in footer");

   // Bad: Too generic
   locator.findElement("button");
   ```

## AI API Issues

### Problem: API rate limits or errors

**Symptoms:**
- `AIServiceException` with rate limit messages
- HTTP 429 errors

**Solutions:**

1. **Configure Circuit Breaker**
   ```java
   AIConfig aiConfig = AIConfig.builder()
       .circuitBreakerFailureThreshold(5)
       .circuitBreakerRecoveryTimeout(Duration.ofMinutes(1))
       .build();
   ```

2. **Add Retry Logic**
   ```java
   PerformanceConfig perfConfig = PerformanceConfig.builder()
       .maxRetries(3)
       .retryDelay(Duration.ofSeconds(2))
       .build();
   ```

### Problem: High AI costs

**Solutions:**

1. **Use SMART_SEQUENTIAL Strategy**
   ```java
   // Automatically optimizes AI usage
   locator = new AutoHealLocator.Builder()
       .withExecutionStrategy(ExecutionStrategy.SMART_SEQUENTIAL)
       .build();
   ```

2. **Enable Aggressive Caching**
   ```java
   CacheConfig cacheConfig = CacheConfig.builder()
       .enableCaching(true)
       .cacheTimeout(Duration.ofHours(24))  // Longer cache
       .build();
   ```

## Configuration Issues

### Problem: Spring Boot auto-configuration not working

**Solutions:**

1. **Check Dependencies**
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter</artifactId>
   </dependency>
   ```

2. **Verify Properties**
   ```properties
   autoheal.ai.api-key=your-key
   autoheal.ai.provider=OPENAI
   autoheal.execution-strategy=SMART_SEQUENTIAL
   ```

3. **Enable Auto-configuration**
   ```java
   @SpringBootApplication
   @EnableAutoHealConfiguration  // Add this annotation
   public class Application {
       public static void main(String[] args) {
           SpringApplication.run(Application.class, args);
       }
   }
   ```

## Debugging Tips

### Enable Detailed Logging

```properties
# application.properties
logging.level.com.autoheal=DEBUG
logging.level.com.autoheal.ai=TRACE
```

### Monitor Metrics

```java
LocatorMetrics metrics = locator.getMetrics();
System.out.println("Success rate: " + metrics.getSuccessRate());
System.out.println("Average response time: " + metrics.getAverageResponseTime());
System.out.println("Cache hit rate: " + metrics.getCacheHitRate());
```

### Capture Screenshots on Failure

```java
try {
    WebElement element = locator.findElement("button");
} catch (ElementNotFoundException e) {
    // Capture screenshot for debugging
    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    Files.copy(screenshot.toPath(), Paths.get("debug-screenshot.png"));
    throw e;
}
```

## Getting Help

If you're still experiencing issues:

1. **Check the logs** for detailed error messages
2. **Enable debug mode** to see internal operations
3. **Share configuration** and error messages when seeking help
4. **Create minimal reproduction** example

For additional support, please [create an issue](https://github.com/yourusername/autoheal-locator/issues) on GitHub.