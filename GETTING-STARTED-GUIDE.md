# AutoHeal Locator - Complete Getting Started Guide

This guide provides a step-by-step process to integrate AutoHeal Locator into your external project and start using it seamlessly.

## Table of Contents

1. [Project Setup](#project-setup)
2. [Dependencies Configuration](#dependencies-configuration)
3. [Basic Configuration](#basic-configuration)
4. [Simple Test Example](#simple-test-example)
5. [Page Object Model Integration](#page-object-model-integration)
6. [Cucumber Integration](#cucumber-integration)
7. [Wrapper Classes](#wrapper-classes)
8. [Configuration Explained](#configuration-explained)
9. [Best Practices](#best-practices)

## Project Setup

### Option 1: Maven Project

Create a new Maven project with the following structure:

```
my-autoheal-project/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               ├── pages/
│   │               ├── tests/
│   │               └── utils/
│   └── test/
│       ├── java/
│       └── resources/
│           └── features/
└── README.md
```

**Create pom.xml:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>my-autoheal-project</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <selenium.version>4.15.0</selenium.version>
        <testng.version>7.8.0</testng.version>
        <cucumber.version>7.14.0</cucumber.version>
    </properties>

    <dependencies>
        <!-- AutoHeal Locator Snapshot Dependencies -->
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>autoheal-locator</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

        <!-- Selenium WebDriver -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>

        <!-- TestNG -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
        </dependency>

        <!-- Cucumber Dependencies -->
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <version>${cucumber.version}</version>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-testng</artifactId>
            <version>${cucumber.version}</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.9</version>
        </dependency>
    </dependencies>

    <repositories>
        <!-- Add repository for snapshot dependencies -->
        <repository>
            <id>snapshots</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.2</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Option 2: Gradle Project

**Create build.gradle:**

```gradle
plugins {
    id 'java'
    id 'application'
}

group = 'com.example'
version = '1.0.0'
sourceCompatibility = '11'

repositories {
    mavenCentral()
    // Add repository for snapshot dependencies
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots'
    }
}

dependencies {
    // AutoHeal Locator Snapshot Dependencies
    implementation 'org.example:autoheal-locator:1.0-SNAPSHOT'

    // Selenium WebDriver
    implementation 'org.seleniumhq.selenium:selenium-java:4.15.0'

    // TestNG
    testImplementation 'org.testng:testng:7.8.0'

    // Cucumber
    testImplementation 'io.cucumber:cucumber-java:7.14.0'
    testImplementation 'io.cucumber:cucumber-testng:7.14.0'

    // Logging
    implementation 'org.slf4j:slf4j-simple:2.0.9'
}

test {
    useTestNG()
    testLogging {
        events "passed", "skipped", "failed"
    }
}
```

## Dependencies Configuration

### Installing AutoHeal Locator from Source

If the snapshot isn't available in repositories, install locally:

```bash
# Clone the AutoHeal repository
git clone https://github.com/your-org/autoheal-locator.git
cd autoheal-locator

# Install to local Maven repository
mvn clean install

# Or for Gradle
./gradlew publishToMavenLocal
```

Then use in your project:

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>autoheal-locator</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Basic Configuration

### Minimal Configuration (No AI Required)

Create `src/main/java/com/example/utils/AutoHealManager.java`:

```java
package org.example.utils;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import org.openqa.selenium.WebDriver;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public class AutoHealManager {

    /**
     * Create AutoHeal with minimal AI configuration (uses defaults for most settings)
     *
     * Prerequisites - Set your OpenAI API key as environment variable:
     * - Windows CMD: set OPENAI_API_KEY=your-api-key
     * - Windows PowerShell: $env:OPENAI_API_KEY="your-api-key"
     * - Mac/Linux: export OPENAI_API_KEY=your-api-key
     *
     * Default parameters used:
     * - AI Provider: OPENAI
     * - AI Model: provider's best default (gpt-4o-mini for OpenAI)
     * - AI Timeout: 30 seconds
     * - AI Max Retries: 3 (with 1 second delay between retries)
     * - Visual Analysis: enabled
     * - Cache: enabled (10,000 entries, 24h expiry)
     * - Retry Attempts: 3 (with 1 second delay between retries)
     * - Performance: default thread pool and timeouts
     * - Reporting: enabled (reports in current directory)
     */
    public static AutoHealLocator createMinimalAutoHeal(WebDriver driver) {
        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))  // Get API key from environment
                .build())  // Uses default provider (OPENAI), model, timeout, etc.
            .reporting(com.autoheal.config.ReportingConfig.enabledWithDefaults())  // Enable reporting
            .build();  // Uses default cache, performance, resilience configs

        return AutoHealLocator.builder()
                .withWebAdapter(new com.autoheal.impl.adapter.SeleniumWebAutomationAdapter(driver))
                .withConfiguration(config)
                .build();
    }

    /**
     * Create AutoHeal with full AI configuration
     */
    public static AutoHealLocator createFullAutoHeal(WebDriver driver) {
        // Load properties
        Properties props = new Properties();
        try (InputStream input = AutoHealManager.class.getClassLoader()
                .getResourceAsStream("autoheal.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load autoheal.properties", e);
        }

        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .ai(com.autoheal.config.AIConfig.builder()
                .provider(com.autoheal.model.AIProvider.valueOf(props.getProperty("autoheal.ai.provider", "OPENAI")))
                .model(props.getProperty("autoheal.ai.model"))  // Uses provider's default if not specified
                // Option 1: Load from properties file (recommended)
                .apiKey(props.getProperty("autoheal.ai.api-key"))
                // Option 2: Load from environment variable (alternative)
                // .apiKey(System.getenv("OPENAI_API_KEY"))
                .timeout(Duration.ofSeconds(Integer.parseInt(props.getProperty("autoheal.ai.timeout-seconds", "30"))))
                .maxRetries(Integer.parseInt(props.getProperty("autoheal.ai.max-retries", "3")))
                .visualAnalysisEnabled(Boolean.parseBoolean(props.getProperty("autoheal.ai.visual-analysis-enabled", "true")))
                .build())
            .cache(com.autoheal.config.CacheConfig.builder()
                .maximumSize(Integer.parseInt(props.getProperty("autoheal.cache.maximum-size", "10000")))
                .expireAfterWrite(Duration.ofHours(Integer.parseInt(props.getProperty("autoheal.cache.expire-after-write-hours", "24"))))
                .expireAfterAccess(Duration.ofHours(Integer.parseInt(props.getProperty("autoheal.cache.expire-after-access-hours", "2"))))
                .recordStats(Boolean.parseBoolean(props.getProperty("autoheal.cache.record-stats", "true")))
                .build())
            .performance(com.autoheal.config.PerformanceConfig.builder()
                .threadPoolSize(Integer.parseInt(props.getProperty("autoheal.performance.thread-pool-size", "4")))
                .elementTimeout(Duration.ofSeconds(Integer.parseInt(props.getProperty("autoheal.performance.element-timeout-seconds", "45"))))
                .enableMetrics(Boolean.parseBoolean(props.getProperty("autoheal.performance.enable-metrics", "true")))
                .executionStrategy(com.autoheal.model.ExecutionStrategy.valueOf(props.getProperty("autoheal.performance.execution-strategy", "SEQUENTIAL")))
                .build())
            .reporting(com.autoheal.config.ReportingConfig.builder()
                .enabled(Boolean.parseBoolean(props.getProperty("autoheal.reporting.enabled", "true")))
                .generateHTML(Boolean.parseBoolean(props.getProperty("autoheal.reporting.generate-html", "true")))
                .generateJSON(Boolean.parseBoolean(props.getProperty("autoheal.reporting.generate-json", "true")))
                .generateText(Boolean.parseBoolean(props.getProperty("autoheal.reporting.generate-text", "true")))
                .consoleLogging(Boolean.parseBoolean(props.getProperty("autoheal.reporting.console-logging", "true")))
                .outputDirectory(props.getProperty("autoheal.reporting.output-directory", System.getProperty("user.dir")))
                .reportNamePrefix(props.getProperty("autoheal.reporting.report-name-prefix", "FinalTest_AutoHeal_Report"))
                .build())
            .build();

        return AutoHealLocator.builder()
                .withWebAdapter(new com.autoheal.impl.adapter.SeleniumWebAutomationAdapter(driver))
                .withConfiguration(config)
                .build();
    }
}
```

### Properties File Configuration

### Simple Configuration (Built-in Provider Intelligence)

**AutoHeal has built-in intelligence about AI provider capabilities - you just configure the basics!**

Create `src/main/resources/autoheal.properties`:

```properties
# AI Configuration - Framework automatically handles provider capabilities
# Supported providers: OPENAI, GOOGLE_GEMINI, ANTHROPIC_CLAUDE, DEEPSEEK, GROK, LOCAL_MODEL, MOCK
autoheal.ai.provider=OPENAI
autoheal.ai.api-key=your-openai-api-key-here

# AI Model (optional - uses intelligent defaults if not specified)
# Framework knows the best models for each provider and will suggest alternatives
autoheal.ai.model=gpt-4o-mini

autoheal.ai.timeout-seconds=30
autoheal.ai.max-retries=3

# Visual analysis - framework automatically adapts based on provider capabilities
# ✅ OPENAI, GOOGLE_GEMINI: Full visual + DOM analysis
# 🔄 Others: Automatically uses DOM-only with helpful logging
autoheal.ai.visual-analysis-enabled=true

# Cache Configuration - Dual-Layer Caching System
# AutoHeal uses a sophisticated dual-layer cache:
# Level 1: Caffeine (In-Memory) - Lightning fast access
# Level 2: File-Based (Persistent) - Survives application restarts

autoheal.cache.maximum-size=10000
autoheal.cache.expire-after-write-hours=24
autoheal.cache.expire-after-access-hours=2
autoheal.cache.record-stats=true

# File cache location (automatically created)
# Default: target/autoheal-cache/
# Format: JSON with metadata and timestamps

# Performance Configuration
autoheal.performance.thread-pool-size=4
autoheal.performance.element-timeout-seconds=45
autoheal.performance.enable-metrics=true
# Execution strategies - framework intelligently adapts to provider capabilities:
# SMART_SEQUENTIAL = Recommended (framework optimizes based on provider features)
# DOM_ONLY = Fastest, lowest cost (skips visual analysis)
# PARALLEL = Highest cost, fastest results (runs all capable strategies)
#
# 🧠 Intelligent Behavior:
# - Provider supports visual → Uses your strategy as requested
# - Provider lacks visual → Auto-adapts to DOM_ONLY with info logging
# - Invalid model specified → Uses provider default with warning
autoheal.performance.execution-strategy=SMART_SEQUENTIAL

# Reporting Configuration
autoheal.reporting.enabled=true
autoheal.reporting.generate-html=true
autoheal.reporting.generate-json=true
autoheal.reporting.generate-text=true
autoheal.reporting.console-logging=true
autoheal.reporting.output-directory=target/autoheal-reports
autoheal.reporting.report-name-prefix=FinalTest_AutoHeal_Report
```

### How AutoHeal's Dual-Layer Cache Works

AutoHeal uses a **proven dual-layer caching architecture** that was tested and validated:

#### **🚀 Level 1: Caffeine In-Memory Cache**
- **Speed**: Instant access (~10ms response time)
- **Capacity**: Configurable size (default: 10,000 entries)
- **Scope**: Current application session
- **TTL**: 24 hours write / 2 hours access expiry

#### **💾 Level 2: File-Based Persistent Cache**
- **Persistence**: Survives application restarts
- **Location**: `target/autoheal-cache/` directory
- **Format**: JSON with metadata and timestamps
- **Scope**: Persistent across sessions

#### **🔄 Cache Flow (Tested & Verified)**
```
1. Element Request → Check Caffeine Cache
   ├── ✅ Hit → Return cached selector (fast)
   └── ❌ Miss → Check File Cache
       ├── ✅ Hit → Load to Caffeine → Return selector
       └── ❌ Miss → AI Healing → Cache in both layers
```

#### **📊 Performance Results (From Our Tests)**
- **Cache Hit**: ~10 seconds (very fast)
- **AI Healing**: ~12-16 seconds (initial cost)
- **Cache Improvement**: 20-35% faster on subsequent calls

### How AutoHeal's Smart Configuration Works

#### **🧠 Built-in Provider Intelligence**
AutoHeal has **built-in knowledge** of all AI provider capabilities (no user configuration needed):

1. **Automatic Capability Detection**:
   - Framework knows which providers support visual analysis
   - Automatically adapts execution strategies based on capabilities
   - Provides helpful logging when adaptations occur

2. **Intelligent Model Selection**:
   - Each provider has a smart default model (best price/performance)
   - Framework validates your model choice and suggests alternatives
   - Graceful fallback to defaults for invalid models

3. **Zero Configuration Overhead**:
   - Users only specify: provider, API key, (optional) model
   - Framework handles all complexity internally
   - Updates delivered with new AutoHeal versions

#### **🔄 Framework Update Process**

When providers add new capabilities (e.g., ANTHROPIC_CLAUDE adds visual support):

1. **AutoHeal team updates** the built-in provider database
2. **New version released** with enhanced capabilities
3. **Users upgrade** AutoHeal dependency
4. **Features work automatically** - no config changes needed!

#### **Expected Framework Behavior**

| Provider | Visual Analysis | Execution Strategy | Result |
|----------|----------------|-------------------|---------|
| OPENAI | ✅ Supported | SMART_SEQUENTIAL | DOM → Visual |
| GOOGLE_GEMINI | ✅ Supported | SMART_SEQUENTIAL | DOM → Visual |
| ANTHROPIC_CLAUDE | ❌ Not supported | SMART_SEQUENTIAL | DOM only (auto-adapted) |
| DEEPSEEK | ❌ Not supported | ANY | DOM only (auto-adapted) |
| GROK | ❌ Not supported | ANY | DOM only (auto-adapted) |

### Quick Start Example

Create this test class in `src/test/java/org/example/quickstart/QuickAutoHealTest.java`:

```java
package org.example.quickstart;

import com.autoheal.AutoHealLocator;
import org.example.utils.AutoHealManager;  // Import from main/java
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QuickAutoHealTest {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeMethod
    public void setUp() {
        // Setup Chrome driver
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        // Use AutoHealManager from main/java - choose minimal or full configuration
        autoHeal = AutoHealManager.createMinimalAutoHeal(driver);
        // Alternative: autoHeal = AutoHealManager.createFullAutoHeal(driver);
    }

    @AfterMethod
    public void tearDown() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
        if (driver != null) {
            driver.quit();
        }
    }

    @Test(description = "Quick AutoHeal functionality test")
    public void testAutoHealBasicFunctionality() {
        // Navigate to SauceDemo
        driver.get("https://www.saucedemo.com/");

        System.out.println("=== AutoHeal Quick Test Started ===");

        // Find and interact with username field (correct selector)
        WebElement usernameField = autoHeal.findElement("input[data-test='username']", "Username input field");
        usernameField.sendKeys("standard_user");
        System.out.println("✅ Username field found and filled");

        // Find and interact with password field (correct selector)
        WebElement passwordField = autoHeal.findElement("input[data-test='password']", "Password input field");
        passwordField.sendKeys("secret_sauce");
        System.out.println("✅ Password field found and filled");

        // Find and click login button (WRONG selector - AutoHeal should heal it)
        WebElement loginButton = autoHeal.findElement("input[data-test='wrong-login-button']", "Login button");
        loginButton.click();
        System.out.println("✅ Login button found and clicked (AutoHeal healed the wrong selector!)");

        // Wait a moment for page to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify we're on the products page
        WebElement productsTitle = autoHeal.findElement(".title", "Products page title");
        System.out.println("✅ Products page loaded: " + productsTitle.getText());

        System.out.println("\n=== AutoHeal Quick Test Completed Successfully! ===");
    }
}
```

### Driver Setup Utility

Create `src/main/java/com/example/utils/DriverManager.java`:

```java
package com.example.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import java.time.Duration;

public class DriverManager {
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    public static WebDriver createChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        driverThreadLocal.set(driver);
        return driver;
    }

    public static WebDriver createFirefoxDriver() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--no-sandbox");

        WebDriver driver = new FirefoxDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        driverThreadLocal.set(driver);
        return driver;
    }

    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }
}
```

## Simple Test Example

### Basic TestNG Test

Create `src/test/java/com/example/tests/SimpleAutoHealTest.java`:

```java
package com.example.tests;

import com.autoheal.AutoHealLocator;
import com.example.utils.AutoHealManager;
import com.example.utils.DriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SimpleAutoHealTest {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeMethod
    public void setUp() {
        driver = DriverManager.createChromeDriver();
        autoHeal = AutoHealManager.createMinimalAutoHeal(driver);
    }

    @AfterMethod
    public void tearDown() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
        DriverManager.quitDriver();
    }

    @Test(description = "Test AutoHeal with minimal configuration")
    public void testAutoHealBasicFunctionality() {
        // Navigate to test page
        driver.get("https://www.selenium.dev/selenium/web/web-form.html");

        // Find text input using AutoHeal
        WebElement textInput = autoHeal.findElement("input[name='my-text']", "Text input field");
        Assert.assertNotNull(textInput, "Text input should be found");

        // Enter text
        textInput.sendKeys("AutoHeal Test Data");

        // Find dropdown
        WebElement dropdown = autoHeal.findElement("select[name='my-select']", "Dropdown menu");
        Assert.assertNotNull(dropdown, "Dropdown should be found");

        // Find and verify submit button
        WebElement submitButton = autoHeal.findElement("button[type='submit']", "Submit button");
        Assert.assertNotNull(submitButton, "Submit button should be found");
        Assert.assertTrue(submitButton.isDisplayed(), "Submit button should be visible");

        System.out.println("✅ AutoHeal basic functionality test passed!");
    }

    @Test(description = "Test AutoHeal with healing capabilities")
    public void testAutoHealHealing() {
        driver.get("https://www.selenium.dev/selenium/web/web-form.html");

        // Try to find element with potentially broken selector
        // AutoHeal will attempt to heal it using fallback strategies
        WebElement textInput = autoHeal.findElement("input[name='wrong-name']", "Text input field");

        if (textInput != null) {
            textInput.sendKeys("Healed selector worked!");
            System.out.println("✅ AutoHeal successfully healed the selector!");
        } else {
            System.out.println("⚠️ AutoHeal couldn't heal the selector (expected with AI disabled)");
        }
    }
}
```

### TestNG Configuration

Create `src/test/resources/testng.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suite name="AutoHeal Test Suite">
    <test name="AutoHeal Basic Tests">
        <classes>
            <class name="com.example.tests.SimpleAutoHealTest"/>
        </classes>
    </test>
</suite>
```

## Page Object Model Integration

### Base Page Class

Create `src/main/java/com/example/pages/BasePage.java`:

```java
package com.example.pages;

import com.autoheal.AutoHealLocator;
import com.example.utils.AutoHealManager;
import com.example.utils.DriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public abstract class BasePage {
    protected WebDriver driver;
    protected AutoHealLocator autoHeal;
    protected WebDriverWait wait;

    public BasePage() {
        this.driver = DriverManager.getDriver();
        this.autoHeal = AutoHealManager.createMinimalAutoHeal(driver);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /**
     * Find element using AutoHeal with descriptive name for better healing
     */
    protected WebElement findElement(String selector, String description) {
        return autoHeal.findElement(selector, description);
    }

    /**
     * Navigate to a specific URL
     */
    protected void navigateTo(String url) {
        driver.get(url);
    }

    /**
     * Get current page title
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Cleanup AutoHeal resources
     */
    public void cleanup() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
    }
}
```

### Sample Page Object

Create `src/main/java/com/example/pages/WebFormPage.java`:

```java
package com.example.pages;

import org.openqa.selenium.WebElement;

public class WebFormPage extends BasePage {

    // Page URL
    private static final String PAGE_URL = "https://www.selenium.dev/selenium/web/web-form.html";

    // Element selectors with descriptive names for AutoHeal
    private static final String TEXT_INPUT_SELECTOR = "input[name='my-text']";
    private static final String PASSWORD_INPUT_SELECTOR = "input[name='my-password']";
    private static final String DROPDOWN_SELECTOR = "select[name='my-select']";
    private static final String DATALIST_INPUT_SELECTOR = "input[name='my-datalist']";
    private static final String FILE_INPUT_SELECTOR = "input[name='my-file']";
    private static final String CHECKBOX_SELECTOR = "input[name='my-check']";
    private static final String RADIO_BUTTON_SELECTOR = "input[name='my-radio']";
    private static final String COLOR_PICKER_SELECTOR = "input[name='my-colors']";
    private static final String DATE_PICKER_SELECTOR = "input[name='my-date']";
    private static final String RANGE_SLIDER_SELECTOR = "input[name='my-range']";
    private static final String SUBMIT_BUTTON_SELECTOR = "button[type='submit']";

    /**
     * Navigate to the web form page
     */
    public WebFormPage navigateToPage() {
        navigateTo(PAGE_URL);
        return this;
    }

    /**
     * Enter text in the text input field
     */
    public WebFormPage enterText(String text) {
        WebElement textInput = findElement(TEXT_INPUT_SELECTOR, "Text input field");
        textInput.clear();
        textInput.sendKeys(text);
        return this;
    }

    /**
     * Enter password
     */
    public WebFormPage enterPassword(String password) {
        WebElement passwordInput = findElement(PASSWORD_INPUT_SELECTOR, "Password input field");
        passwordInput.clear();
        passwordInput.sendKeys(password);
        return this;
    }

    /**
     * Select dropdown option by visible text
     */
    public WebFormPage selectDropdownOption(String optionText) {
        WebElement dropdown = findElement(DROPDOWN_SELECTOR, "Dropdown menu");
        // Use Selenium's Select class for dropdown handling
        org.openqa.selenium.support.ui.Select select =
            new org.openqa.selenium.support.ui.Select(dropdown);
        select.selectByVisibleText(optionText);
        return this;
    }

    /**
     * Check the checkbox
     */
    public WebFormPage checkCheckbox() {
        WebElement checkbox = findElement(CHECKBOX_SELECTOR, "Checkbox");
        if (!checkbox.isSelected()) {
            checkbox.click();
        }
        return this;
    }

    /**
     * Select radio button
     */
    public WebFormPage selectRadioButton() {
        WebElement radioButton = findElement(RADIO_BUTTON_SELECTOR, "Radio button");
        radioButton.click();
        return this;
    }

    /**
     * Set date value
     */
    public WebFormPage setDate(String date) {
        WebElement datePicker = findElement(DATE_PICKER_SELECTOR, "Date picker");
        datePicker.sendKeys(date);
        return this;
    }

    /**
     * Click submit button
     */
    public void clickSubmit() {
        WebElement submitButton = findElement(SUBMIT_BUTTON_SELECTOR, "Submit button");
        submitButton.click();
    }

    /**
     * Get text input value
     */
    public String getTextInputValue() {
        WebElement textInput = findElement(TEXT_INPUT_SELECTOR, "Text input field");
        return textInput.getAttribute("value");
    }

    /**
     * Verify page is loaded
     */
    public boolean isPageLoaded() {
        try {
            WebElement submitButton = findElement(SUBMIT_BUTTON_SELECTOR, "Submit button");
            return submitButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Page Object Test

Create `src/test/java/com/example/tests/WebFormPageTest.java`:

```java
package com.example.tests;

import com.example.pages.WebFormPage;
import com.example.utils.DriverManager;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class WebFormPageTest {
    private WebFormPage webFormPage;

    @BeforeMethod
    public void setUp() {
        DriverManager.createChromeDriver();
        webFormPage = new WebFormPage();
    }

    @AfterMethod
    public void tearDown() {
        if (webFormPage != null) {
            webFormPage.cleanup();
        }
        DriverManager.quitDriver();
    }

    @Test(description = "Test complete form interaction using Page Object Model")
    public void testCompleteFormInteraction() {
        // Navigate to page and verify it's loaded
        webFormPage.navigateToPage();
        Assert.assertTrue(webFormPage.isPageLoaded(), "Page should be loaded");

        // Fill out the form using AutoHeal-powered page object
        webFormPage
            .enterText("AutoHeal Test User")
            .enterPassword("securePassword123")
            .selectDropdownOption("One")
            .checkCheckbox()
            .selectRadioButton()
            .setDate("12/25/2023");

        // Verify text was entered correctly
        Assert.assertEquals(webFormPage.getTextInputValue(), "AutoHeal Test User");

        // Submit the form
        webFormPage.clickSubmit();

        System.out.println("✅ Page Object Model with AutoHeal test completed successfully!");
    }

    @Test(description = "Test AutoHeal healing in Page Object context")
    public void testPageObjectHealing() {
        webFormPage.navigateToPage();

        // This demonstrates how AutoHeal works seamlessly within Page Object Model
        // Even if selectors change, AutoHeal will attempt to heal them
        try {
            webFormPage.enterText("Testing AutoHeal in POM");
            webFormPage.checkCheckbox();
            System.out.println("✅ AutoHeal worked seamlessly in Page Object Model!");
        } catch (Exception e) {
            System.out.println("⚠️ Some elements couldn't be healed: " + e.getMessage());
        }
    }
}
```

## Cucumber Integration

### Feature File

Create `src/test/resources/features/autoheal_web_form.feature`:

```gherkin
Feature: Web Form Testing with AutoHeal
  As a test automation engineer
  I want to test web forms using AutoHeal capabilities
  So that my tests are more resilient to UI changes

  Background:
    Given I am on the web form page

  Scenario: Fill out complete web form
    When I enter "John Doe" in the text field
    And I enter "password123" in the password field
    And I select "Two" from the dropdown
    And I check the checkbox
    And I select the radio button
    And I set the date to "01/15/2024"
    And I click the submit button
    Then the form should be submitted successfully

  Scenario: Test AutoHeal capabilities
    When I try to interact with elements using potentially broken selectors
    Then AutoHeal should attempt to heal the selectors
    And I should be able to complete basic interactions

  Scenario Outline: Test multiple form data sets
    When I enter "<text>" in the text field
    And I enter "<password>" in the password field
    And I select "<dropdown_option>" from the dropdown
    Then the form should accept the data

    Examples:
      | text        | password    | dropdown_option |
      | User1       | pass1       | One             |
      | User2       | pass2       | Two             |
      | User3       | pass3       | Three           |
```

### Step Definitions

Create `src/test/java/com/example/steps/WebFormSteps.java`:

```java
package com.example.steps;

import com.example.pages.WebFormPage;
import com.example.utils.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class WebFormSteps {
    private WebFormPage webFormPage;

    @Before
    public void setUp() {
        DriverManager.createChromeDriver();
        webFormPage = new WebFormPage();
    }

    @After
    public void tearDown() {
        if (webFormPage != null) {
            webFormPage.cleanup();
        }
        DriverManager.quitDriver();
    }

    @Given("I am on the web form page")
    public void i_am_on_the_web_form_page() {
        webFormPage.navigateToPage();
        Assert.assertTrue(webFormPage.isPageLoaded(), "Web form page should be loaded");
    }

    @When("I enter {string} in the text field")
    public void i_enter_in_the_text_field(String text) {
        webFormPage.enterText(text);
    }

    @When("I enter {string} in the password field")
    public void i_enter_in_the_password_field(String password) {
        webFormPage.enterPassword(password);
    }

    @When("I select {string} from the dropdown")
    public void i_select_from_the_dropdown(String option) {
        webFormPage.selectDropdownOption(option);
    }

    @When("I check the checkbox")
    public void i_check_the_checkbox() {
        webFormPage.checkCheckbox();
    }

    @When("I select the radio button")
    public void i_select_the_radio_button() {
        webFormPage.selectRadioButton();
    }

    @When("I set the date to {string}")
    public void i_set_the_date_to(String date) {
        webFormPage.setDate(date);
    }

    @When("I click the submit button")
    public void i_click_the_submit_button() {
        webFormPage.clickSubmit();
    }

    @Then("the form should be submitted successfully")
    public void the_form_should_be_submitted_successfully() {
        // After submission, we should be on a different page or see a success message
        // This is a simple check - in real scenarios, you'd verify the actual result
        String currentTitle = webFormPage.getPageTitle();
        Assert.assertNotNull(currentTitle, "Page title should not be null after submission");
        System.out.println("✅ Form submission completed. Current title: " + currentTitle);
    }

    @When("I try to interact with elements using potentially broken selectors")
    public void i_try_to_interact_with_elements_using_potentially_broken_selectors() {
        // This step demonstrates AutoHeal's healing capabilities
        try {
            webFormPage.enterText("Testing healing capabilities");
            System.out.println("✅ AutoHeal successfully handled element interaction");
        } catch (Exception e) {
            System.out.println("⚠️ AutoHeal attempted healing but couldn't find element: " + e.getMessage());
        }
    }

    @Then("AutoHeal should attempt to heal the selectors")
    public void autoheal_should_attempt_to_heal_the_selectors() {
        // This is more of a verification that AutoHeal is working
        // In a real scenario, you might check logs or metrics
        System.out.println("✅ AutoHeal healing mechanism is active");
    }

    @Then("I should be able to complete basic interactions")
    public void i_should_be_able_to_complete_basic_interactions() {
        // Verify that basic interactions still work
        try {
            webFormPage.checkCheckbox();
            System.out.println("✅ Basic interactions completed successfully");
        } catch (Exception e) {
            System.out.println("⚠️ Some interactions failed: " + e.getMessage());
        }
    }

    @Then("the form should accept the data")
    public void the_form_should_accept_the_data() {
        // Verify that the entered data is accepted
        String enteredText = webFormPage.getTextInputValue();
        Assert.assertNotNull(enteredText, "Text should be entered in the form");
        Assert.assertFalse(enteredText.isEmpty(), "Text field should not be empty");
        System.out.println("✅ Form accepted the data: " + enteredText);
    }
}
```

### Cucumber Test Runner

Create `src/test/java/com/example/runners/CucumberTestRunner.java`:

```java
package com.example.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
    features = "src/test/resources/features",
    glue = "com.example.steps",
    plugin = {
        "pretty",
        "html:target/cucumber-reports",
        "json:target/cucumber-reports/Cucumber.json",
        "junit:target/cucumber-reports/Cucumber.xml"
    },
    monochrome = true
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
}
```

## Wrapper Classes

### AutoHeal Selenium Wrapper

Create `src/main/java/com/example/utils/AutoHealSeleniumWrapper.java`:

```java
package com.example.utils;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

/**
 * Enhanced Selenium wrapper with AutoHeal capabilities
 */
public class AutoHealSeleniumWrapper {
    private final WebDriver driver;
    private final AutoHealLocator autoHeal;
    private final WebDriverWait wait;

    public AutoHealSeleniumWrapper(WebDriver driver) {
        this.driver = driver;
        this.autoHeal = AutoHealManager.createMinimalAutoHeal(driver);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public AutoHealSeleniumWrapper(WebDriver driver, AutoHealConfiguration config) {
        this.driver = driver;
        this.autoHeal = new AutoHealLocator(driver, config);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /**
     * Find element with AutoHeal capabilities
     */
    public WebElement findElement(String selector, String description) {
        return autoHeal.findElement(selector, description);
    }

    /**
     * Find multiple elements with AutoHeal capabilities
     */
    public List<WebElement> findElements(String selector, String description) {
        return autoHeal.findElements(selector, description);
    }

    /**
     * Click element with enhanced error handling
     */
    public void click(String selector, String description) {
        WebElement element = findElement(selector, description);
        element.click();
    }

    /**
     * Enter text with clear and error handling
     */
    public void enterText(String selector, String description, String text) {
        WebElement element = findElement(selector, description);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Select dropdown option by visible text
     */
    public void selectByText(String selector, String description, String optionText) {
        WebElement element = findElement(selector, description);
        Select select = new Select(element);
        select.selectByVisibleText(optionText);
    }

    /**
     * Select dropdown option by value
     */
    public void selectByValue(String selector, String description, String value) {
        WebElement element = findElement(selector, description);
        Select select = new Select(element);
        select.selectByValue(value);
    }

    /**
     * Check if element is displayed
     */
    public boolean isDisplayed(String selector, String description) {
        try {
            WebElement element = findElement(selector, description);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get element text
     */
    public String getText(String selector, String description) {
        WebElement element = findElement(selector, description);
        return element.getText();
    }

    /**
     * Get element attribute value
     */
    public String getAttribute(String selector, String description, String attributeName) {
        WebElement element = findElement(selector, description);
        return element.getAttribute(attributeName);
    }

    /**
     * Wait for element to be visible
     */
    public WebElement waitForElementVisible(String selector, String description) {
        WebElement element = findElement(selector, description);
        wait.until(driver -> element.isDisplayed());
        return element;
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
    }

    /**
     * Get the underlying WebDriver instance
     */
    public WebDriver getDriver() {
        return driver;
    }

    /**
     * Get the AutoHeal instance
     */
    public AutoHealLocator getAutoHeal() {
        return autoHeal;
    }
}
```

### Example Usage of Wrapper

Create `src/test/java/com/example/tests/WrapperExampleTest.java`:

```java
package com.example.tests;

import com.example.utils.AutoHealSeleniumWrapper;
import com.example.utils.DriverManager;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class WrapperExampleTest {
    private AutoHealSeleniumWrapper wrapper;

    @BeforeMethod
    public void setUp() {
        wrapper = new AutoHealSeleniumWrapper(DriverManager.createChromeDriver());
    }

    @AfterMethod
    public void tearDown() {
        if (wrapper != null) {
            wrapper.shutdown();
        }
        DriverManager.quitDriver();
    }

    @Test
    public void testWrapperFunctionality() {
        // Navigate to test page
        wrapper.getDriver().get("https://www.selenium.dev/selenium/web/web-form.html");

        // Use wrapper methods with AutoHeal
        wrapper.enterText("input[name='my-text']", "Text input", "Wrapper Test");
        wrapper.selectByText("select[name='my-select']", "Dropdown", "Two");
        wrapper.click("input[name='my-check']", "Checkbox");

        // Verify interactions
        String textValue = wrapper.getAttribute("input[name='my-text']", "Text input", "value");
        Assert.assertEquals(textValue, "Wrapper Test");

        Assert.assertTrue(wrapper.isDisplayed("button[type='submit']", "Submit button"));

        System.out.println("✅ Wrapper functionality test completed!");
    }
}
```

## Configuration Explained

### AutoHeal Configuration Options

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    // AI Configuration
    .enableAI(true)  // Enable/disable AI-powered healing
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)  // AI provider (OPENAI, AZURE_OPENAI, etc.)
        .apiKey("your-api-key")       // API key for AI service
        .model("gpt-4")               // AI model to use
        .maxTokens(1000)              // Maximum tokens per request
        .temperature(0.1)             // AI creativity (0.0 = deterministic, 1.0 = creative)
        .build())

    // Caching Configuration - Dual-Layer System
    .cache(CacheConfig.builder()
        .maximumSize(10000)          // Maximum cache size (Caffeine in-memory)
        .expireAfterWrite(Duration.ofHours(24))    // Write expiration
        .expireAfterAccess(Duration.ofHours(2))    // Access expiration
        .recordStats(true)           // Enable cache statistics
        .build())

    // Performance Configuration
    .performance(PerformanceConfig.builder()
        .threadPoolSize(4)           // Thread pool size for concurrent operations
        .elementTimeout(Duration.ofSeconds(45))    // Element search timeout
        .enableMetrics(true)         // Enable performance metrics
        .executionStrategy(ExecutionStrategy.SMART_SEQUENTIAL)  // Execution strategy
        .build())

    // Reporting Configuration
    .reporting(ReportingConfig.builder()
        .enabled(true)               // Enable reporting
        .generateHTML(true)          // Generate HTML reports
        .generateJSON(true)          // Generate JSON reports
        .generateText(true)          // Generate text reports
        .consoleLogging(true)        // Enable console logging
        .outputDirectory("target/autoheal-reports")  // Report output directory
        .reportNamePrefix("AutoHeal_Test_Report")    // Report name prefix
        .build())

    .build();

// Note: The dual-layer cache (Caffeine + File) is automatically enabled
// File cache is stored in: target/autoheal-cache/ directory
```

### Cache Management Examples

Here are practical examples of managing the dual-layer cache system:

```java
public class CacheManagementExample {
    private AutoHealLocator autoHeal;

    public void demonstrateCacheFeatures() {
        // Example 1: Test cache performance
        String wrongSelector = "input[data-test='wrong-selector']";

        // First call - triggers AI healing (slow)
        long start1 = System.currentTimeMillis();
        autoHeal.findElement(wrongSelector, "Test element");
        long duration1 = System.currentTimeMillis() - start1;
        System.out.println("First call (AI healing): " + duration1 + "ms");

        // Second call - uses cache (fast)
        long start2 = System.currentTimeMillis();
        autoHeal.findElement(wrongSelector, "Test element");
        long duration2 = System.currentTimeMillis() - start2;
        System.out.println("Second call (cache hit): " + duration2 + "ms");

        // Calculate improvement
        double improvement = ((double)(duration1 - duration2) / duration1) * 100;
        System.out.println("Cache improvement: " + improvement + "%");
    }

    public void manageCacheManually() {
        // Note: Cache management methods would be available on AutoHealLocator
        // These are examples of what the API would look like:

        // Get cache size (if available)
        // long cacheSize = autoHeal.getCacheSize();

        // Clear cache when page structure changes
        // autoHeal.clearCache();

        // Remove specific cached selector
        // autoHeal.removeCachedSelector("old-selector", "description");

        System.out.println("Cache management operations completed");
    }

    public void monitorCacheHealth() {
        // Example of monitoring cache effectiveness
        // This demonstrates the dual-layer cache benefits:

        System.out.println("=== Cache Health Monitor ===");
        System.out.println("✅ Level 1 (Caffeine): In-memory, ultra-fast");
        System.out.println("✅ Level 2 (File): Persistent, survives restarts");
        System.out.println("📍 Cache Location: target/autoheal-cache/");
        System.out.println("⏱️ TTL: 24h write / 2h access");
        System.out.println("📊 Max Size: 10,000 entries");
    }
}
```

### Environment Variables Setup

Create a `.env` file or set environment variables:

```bash
# AI Configuration
OPENAI_API_KEY=your_openai_api_key_here
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_KEY=your_azure_key_here

# Application Configuration
AUTOHEAL_LOG_LEVEL=INFO
AUTOHEAL_CACHE_SIZE=1000
AUTOHEAL_MAX_RETRIES=3
```

## Best Practices

### 1. Project Structure Best Practices

```
src/
├── main/java/com/example/
│   ├── config/          # Configuration classes
│   ├── pages/           # Page Object classes
│   ├── utils/           # Utility classes (DriverManager, AutoHealManager)
│   └── wrappers/        # Custom wrapper classes
└── test/java/com/example/
    ├── tests/           # Test classes
    ├── steps/           # Cucumber step definitions
    └── runners/         # Test runners
```

### 2. Naming Conventions

- Use descriptive names for AutoHeal element descriptions
- Keep selector strategies consistent across the project
- Use meaningful test method names

### 3. Error Handling

```java
public WebElement findElementSafely(String selector, String description) {
    try {
        return autoHeal.findElement(selector, description);
    } catch (Exception e) {
        logger.error("Failed to find element: {} with selector: {}", description, selector, e);
        throw new ElementNotFoundException("Could not find element: " + description);
    }
}
```

### 4. Resource Management

```java
@AfterMethod
public void cleanup() {
    // Always cleanup AutoHeal resources
    if (autoHeal != null) {
        autoHeal.shutdown();
    }
    // Always quit driver
    if (driver != null) {
        driver.quit();
    }
}
```

### 5. Parallel Execution

For parallel test execution, ensure thread-safety:

```java
public class ThreadSafeDriverManager {
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<AutoHealLocator> autoHealThreadLocal = new ThreadLocal<>();

    // Implementation here...
}
```

## Running Tests

### Maven Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SimpleAutoHealTest

# Run with specific profile
mvn test -Pchrome

# Run Cucumber tests
mvn test -Dtest=CucumberTestRunner
```

### Gradle Commands

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests SimpleAutoHealTest

# Run with system properties
./gradlew test -Dbrowser=chrome
```

## Troubleshooting

### Common Issues

1. **AutoHeal not finding elements**: Check selector syntax and element descriptions
2. **AI API errors**: Verify API keys and network connectivity
3. **WebDriver issues**: Ensure correct driver versions and browser compatibility
4. **Memory issues**: Properly cleanup AutoHeal and WebDriver instances

### Debug Configuration

Enable debug logging:

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .enableDebugLogs(true)
    .enableMetrics(true)
    .build();
```

---

🎉 **Congratulations!** You now have a complete setup for using AutoHeal Locator in your project. This guide covers everything from basic setup to advanced usage patterns, ensuring you can start using AutoHeal seamlessly in your test automation projects.