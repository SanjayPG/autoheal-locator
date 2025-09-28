# AutoHeal Locator

[![Build Status](https://github.com/autoheal/autoheal-locator/workflows/CI/badge.svg)](https://github.com/autoheal/autoheal-locator/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Coverage](https://codecov.io/gh/autoheal/autoheal-locator/branch/main/graph/badge.svg)](https://codecov.io/gh/autoheal/autoheal-locator)

Enterprise-grade AI-powered element locator with auto-healing capabilities for **Java** test automation using **Selenium WebDriver** and **Playwright**.

## Features

- **AI-Powered Element Location**: Uses machine learning to intelligently locate elements when selectors break
- **Multiple Healing Strategies**: DOM analysis, visual analysis, and hybrid approaches
- **Intelligent Caching**: High-performance caching with contextual keys and success rate tracking
- **Circuit Breaker Pattern**: Resilient AI service integration with fallback mechanisms
- **Comprehensive Metrics**: Real-time monitoring and performance analytics
- **Spring Boot Integration**: Auto-configuration and properties support
- **Async Operations**: Non-blocking element location with CompletableFuture
- **Enterprise Ready**: Production-grade monitoring, configuration, and error handling

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.autoheal</groupId>
    <artifactId>autoheal-locator</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Basic Usage

```java
// Simple setup
WebDriver driver = new ChromeDriver();
AutoHealLocator autoHeal = new AutoHealLocator(driver);

// Find element with healing
WebElement button = autoHeal.findElement("#submit-btn", "Submit button");
button.click();

autoHeal.shutdown();
```

### Advanced Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .cache(CacheConfig.builder()
        .maximumSize(5000)
        .expireAfterWrite(Duration.ofHours(12))
        .build())
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .apiKey("your-api-key")
        .timeout(Duration.ofSeconds(15))
        .visualAnalysisEnabled(true)
        .build())
    .performance(PerformanceConfig.builder()
        .threadPoolSize(8)
        .enableMetrics(true)
        .build())
    .build();

AutoHealLocator autoHeal = AutoHealLocator.builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .withConfiguration(config)
    .build();
```

### Async Operations

```java
// Non-blocking element location
CompletableFuture<WebElement> futureElement = autoHeal
    .findElementAsync("#login-btn", "Login button")
    .thenApply(element -> {
        element.click();
        return element;
    });

// Multiple parallel operations
CompletableFuture<WebElement> username = autoHeal.findElementAsync("#username", "Username field");
CompletableFuture<WebElement> password = autoHeal.findElementAsync("#password", "Password field");
CompletableFuture<WebElement> submit = autoHeal.findElementAsync("#submit", "Submit button");

CompletableFuture.allOf(username, password, submit)
    .thenRun(() -> {
        // All elements located, proceed with test
    });
```

## Spring Boot Integration

### Auto-Configuration

Add the dependency and AutoHeal will be automatically configured:

```java
@Autowired
private AutoHealLocator autoHeal;

@Test
public void testWithAutoHeal() {
    WebElement element = autoHeal.findElement("#test-element", "Test element");
    assertNotNull(element);
}
```

### Configuration Properties

```yaml
autoheal:
  cache:
    maximum-size: 10000
    expire-after-write: 24h
    expire-after-access: 2h
  ai:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    timeout: 30s
    visual-analysis-enabled: false
  performance:
    thread-pool-size: 16
    element-timeout: 10s
    enable-metrics: true
  resilience:
    circuit-breaker-failure-threshold: 5
    circuit-breaker-timeout: 5m
    retry-max-attempts: 3
```

## Monitoring and Metrics

### Getting Metrics

```java
AutoHealMetrics metrics = autoHeal.getMetrics();
System.out.println("Success rate: " + metrics.getLocatorMetrics().getSuccessRate());
System.out.println("Cache hit rate: " + metrics.getCacheMetrics().getHitRate());
System.out.println("AI response time: " + metrics.getAiServiceMetrics().getAverageResponseTime());
```

### Health Checks

```java
HealthStatus health = autoHeal.getHealthStatus();
if (!health.isOverall()) {
    System.out.println("AutoHeal system issues detected!");
    // Handle degraded performance
}
```

### Real-time Monitoring

```java
AutoHealMonitor monitor = new AutoHealMonitor(autoHeal);
monitor.addListener((metrics, health) -> {
    // Send metrics to monitoring system
    sendToPrometheus(metrics.toMap());
});
monitor.startMonitoring(Duration.ofSeconds(30));
```

## Configuration Options

### Cache Configuration

- `maximumSize`: Maximum number of cached selectors
- `expireAfterWrite`: Cache entry TTL from creation
- `expireAfterAccess`: Cache entry TTL from last access
- `recordStats`: Enable cache statistics

### AI Configuration

- `provider`: AI service provider (OPENAI, LOCAL_MODEL, MOCK)
- `apiKey`: API key for external AI services
- `timeout`: Request timeout for AI services
- `maxRetries`: Maximum retry attempts
- `visualAnalysisEnabled`: Enable visual element analysis

### Performance Configuration

- `threadPoolSize`: Size of async execution thread pool
- `elementTimeout`: Timeout for element location operations
- `enableMetrics`: Enable performance metrics collection
- `maxConcurrentRequests`: Maximum concurrent AI requests

### Resilience Configuration

- `circuitBreakerFailureThreshold`: Failures before circuit breaker opens
- `circuitBreakerTimeout`: Circuit breaker timeout duration
- `retryMaxAttempts`: Maximum retry attempts for failed operations
- `retryDelay`: Delay between retry attempts

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

### Performance Tests

```bash
mvn test -Pperformance
```

### Test Utilities

```java
@ExtendWith(AutoHealExtension.class)
class MyTest {
    
    @Test
    void testWithAutoHeal(ExtensionContext context) {
        AutoHealLocator autoHeal = context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get("autoHeal", AutoHealLocator.class);
        
        WebElement element = autoHeal.findElement("#test", "Test element");
        assertNotNull(element);
    }
}
```

## Best Practices

1. **Provide Descriptive Element Descriptions**: Better descriptions lead to more accurate AI suggestions
2. **Use Contextual Information**: Provide element context when available for better disambiguation
3. **Monitor Performance**: Regularly check metrics and health status
4. **Configure Timeouts Appropriately**: Balance responsiveness with reliability
5. **Handle Failures Gracefully**: Implement fallback strategies for critical operations

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   AutoHeal      │    │   Selector      │    │   AI Service    │
│   Locator       │────│   Cache         │────│   (OpenAI)      │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Element       │    │   Circuit       │    │   Metrics &     │
│   Locators      │    │   Breaker       │    │   Monitoring    │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- 📖 [Documentation](https://autoheal.github.io/autoheal-locator)
- 🐛 [Issue Tracker](https://github.com/autoheal/autoheal-locator/issues)
- 💬 [Discussions](https://github.com/autoheal/autoheal-locator/discussions)
- 📧 [Email Support](mailto:support@autoheal.com)