# AutoHeal Locator

**AutoHeal Locator** is an enterprise-grade AI-powered element locator with auto-healing capabilities for **Java** test automation using **Selenium WebDriver** and **Playwright**. When your test selectors break due to DOM changes, AutoHeal uses advanced AI models to intelligently locate elements and keep your tests running.

```{note}
AutoHeal Locator is designed for production environments and provides robust, cost-effective test automation with minimal maintenance.
```

## Key Features

- **🤖 AI-Powered Element Location**: Advanced AI models intelligently locate elements when selectors break
- **🔄 Multiple Healing Strategies**: DOM analysis, visual analysis, and hybrid approaches
- **🎭 Multi-Framework Support**: Works with Selenium WebDriver and Playwright
- **⚡ Intelligent Caching**: High-performance caching with contextual keys and success rate tracking
- **🛡️ Circuit Breaker Pattern**: Resilient AI service integration with fallback mechanisms
- **📊 Comprehensive Metrics**: Real-time monitoring and performance analytics
- **🚀 Spring Boot Integration**: Auto-configuration and properties support
- **⚙️ Async Operations**: Non-blocking element location with CompletableFuture
- **🏢 Enterprise Ready**: Production-grade monitoring, configuration, and error handling

## Quick Examples

### Selenium WebDriver
```java
// Create AutoHeal locator for Selenium
AutoHealLocator locator = new AutoHealLocator.Builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .withConfiguration(AutoHealConfiguration.builder()
        .aiConfig(AIConfig.builder()
            .apiKey("your-openai-key")
            .build())
        .build())
    .build();

// Find element with AI-powered healing
WebElement button = locator.findElement("#submit-btn", "Submit button");
button.click();
```

### Playwright
```java
// Create AutoHeal locator for Playwright
PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
AutoHealLocator locator = new AutoHealLocator(adapter);

// Same API - AutoHeal handles framework differences
WebElement button = locator.findElement("#submit-btn", "Submit button");
button.click();
```

## Tutorial

New to AutoHeal Locator? Start here to learn the basics and get your first tests running with AI-powered healing.

```{toctree}
:maxdepth: 1

docs/user-guide/installation
docs/user-guide/quick-start
docs/user-guide/index
```

## User Guide

Learn how to configure and use AutoHeal Locator in different scenarios and environments.

```{toctree}
:maxdepth: 1

docs/user-guide/ai-configuration
docs/user-guide/selenium-integration
docs/user-guide/playwright-integration
docs/user-guide/spring-boot-integration
docs/user-guide/performance
docs/user-guide/reporting
docs/user-guide/multiple-elements
```

## Examples

Ready-to-use examples and patterns for common testing scenarios.

```{toctree}
:maxdepth: 1

docs/user-guide/examples/selenium-examples
docs/user-guide/examples/playwright-examples
docs/user-guide/examples/page-object-examples
docs/user-guide/examples/cucumber-examples
```

## Advanced Topics

Deep dive into advanced configurations, cost optimization, and troubleshooting.

```{toctree}
:maxdepth: 1

docs/advanced/cost-optimization
docs/advanced/architecture
docs/advanced/configuration-reference
docs/advanced/troubleshooting
```

## Contributing

Help make AutoHeal Locator better for everyone.

```{toctree}
:maxdepth: 1

docs/contributing/development
```

## Resources

- **[GitHub Repository](https://github.com/yourusername/autoheal-locator)** - Source code and issue tracking
- **[Maven Central](https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator)** - Latest releases and dependency information
- **[API Reference](https://javadoc.io/doc/com.autoheal/autoheal-locator)** - Complete JavaDoc documentation