# AutoHeal Locator

[![Build Status](https://github.com/autoheal/autoheal-locator/workflows/CI/badge.svg)](https://github.com/autoheal/autoheal-locator/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Enterprise-grade AI-powered element locator with auto-healing capabilities for Selenium WebDriver.

## ✨ Key Features

- **🤖 AI-Powered Element Location**: Uses advanced AI models to intelligently locate elements when selectors break
- **🔄 Multiple Healing Strategies**: DOM analysis, visual analysis, and hybrid approaches
- **⚡ Intelligent Caching**: High-performance caching with contextual keys and success rate tracking
- **🛡️ Circuit Breaker Pattern**: Resilient AI service integration with fallback mechanisms
- **📊 Comprehensive Metrics**: Real-time monitoring and performance analytics
- **🚀 Spring Boot Integration**: Auto-configuration and properties support
- **⚙️ Async Operations**: Non-blocking element location with CompletableFuture
- **🏢 Enterprise Ready**: Production-grade monitoring, configuration, and error handling

## 🚀 Quick Start

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
AutoHealLocator locator = new AutoHealLocator.Builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .withConfiguration(AutoHealConfiguration.builder()
        .aiConfig(AIConfig.builder()
            .apiKey("your-openai-key")
            .build())
        .build())
    .build();

WebElement element = locator.findElement("#submit-btn", "Submit button");
```

## 📚 Documentation Structure

- **[Getting Started](user-guide/installation.md)** - Installation and setup
- **[User Guide](user-guide/index.md)** - Comprehensive usage documentation
- **[Examples](user-guide/examples/selenium-examples.md)** - Real-world examples
- **[Advanced](advanced/cost-optimization.md)** - Performance optimization and configuration
- **[API Reference](api/core-classes.md)** - Complete API documentation

## 💡 Why AutoHeal Locator?

Traditional test automation breaks when UI changes. AutoHeal Locator solves this by:

1. **Intelligent Fallback**: When selectors fail, AI-powered analysis finds the correct elements
2. **Cost Optimization**: Smart strategies minimize AI API costs while maintaining reliability
3. **Zero Maintenance**: Self-healing tests that adapt to UI changes automatically
4. **Enterprise Grade**: Built for production with monitoring, metrics, and reliability features

## 🤝 Contributing

We welcome contributions! See our [Development Guide](contributing/development.md) for details.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/yourusername/autoheal-locator/blob/main/LICENSE) file for details.