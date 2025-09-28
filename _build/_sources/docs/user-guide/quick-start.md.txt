# Quick Start Guide

Get up and running with AutoHeal Locator in minutes.

## Prerequisites

- Java 8 or higher
- Maven or Gradle
- Selenium WebDriver
- OpenAI API key (for AI-powered features)

## Installation

### Maven

```xml
<dependency>
    <groupId>com.autoheal</groupId>
    <artifactId>autoheal-locator</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.autoheal:autoheal-locator:2.0.0'
```

## Basic Example

```java
import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class QuickStartExample {
    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();

        // Create AutoHeal locator
        AutoHealLocator locator = new AutoHealLocator.Builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(AutoHealConfiguration.builder()
                .aiConfig(AIConfig.builder()
                    .apiKey("your-openai-api-key")
                    .build())
                .build())
            .build();

        try {
            driver.get("https://example.com");

            // Find element with CSS selector and description
            WebElement button = locator.findElement("#submit-btn", "Submit button");
            button.click();

            // Find element with XPath selector and description
            WebElement input = locator.findElement(
                "//input[@type='email']",  // XPath selector
                "Email input field"        // description for AI healing
            );
            input.sendKeys("user@example.com");

        } finally {
            driver.quit();
        }
    }
}
```

## Configuration Options

### Execution Strategies

```java
// Cost-optimized (recommended)
AutoHealLocator locator = new AutoHealLocator.Builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .withConfiguration(AutoHealConfiguration.builder()
        .executionStrategy(ExecutionStrategy.SMART_SEQUENTIAL)
        .build())
    .build();

// DOM-only (fastest, cheapest)
AutoHealLocator locator = new AutoHealLocator.Builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .withConfiguration(AutoHealConfiguration.builder()
        .executionStrategy(ExecutionStrategy.DOM_ONLY)
        .build())
    .build();

// Visual-first (most accurate)
AutoHealLocator locator = new AutoHealLocator.Builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .withConfiguration(AutoHealConfiguration.builder()
        .executionStrategy(ExecutionStrategy.VISUAL_FIRST)
        .build())
    .build();
```

### Performance Tuning

```java
AutoHealLocator locator = new AutoHealLocator.Builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .withConfiguration(AutoHealConfiguration.builder()
        .performanceConfig(PerformanceConfig.builder()
            .enableCaching(true)
            .maxCacheSize(1000)
            .cacheTimeout(Duration.ofMinutes(30))
            .build())
        .build())
    .build();
```

## Next Steps

- **[Configuration](ai-configuration.md)** - Detailed configuration options
- **[Examples](examples/selenium-examples.md)** - More comprehensive examples
- **[Performance](performance.md)** - Optimization and tuning
- **[Spring Boot Integration](spring-boot-integration.md)** - Spring Boot setup