# AutoHeal Locator - Complete Selenium Usage Guide

[![Build Status](https://github.com/autoheal/autoheal-locator/workflows/CI/badge.svg)](https://github.com/autoheal/autoheal-locator/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A comprehensive guide to using AutoHeal Locator with Selenium WebDriver for enterprise-grade test automation with AI-powered self-healing capabilities.

> **🧠 Smart Provider Support**: AutoHeal includes built-in intelligence about AI provider capabilities. The framework automatically adapts features based on what each provider supports - no complex configuration needed!

## Table of Contents
- [Project Setup](#project-setup)
- [Maven Configuration](#maven-configuration)
- [Properties Configuration](#properties-configuration)
- [AutoHealManager Utility](#autohealmanager-utility)
- [TestNG Page Object Model](#testng-page-object-model)
- [Running Tests](#running-tests)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Project Setup

Create a Maven project with the following structure:

```
my-autoheal-project/
├── pom.xml
├── src/main/resources/
│   └── autoheal.properties
├── src/main/java/
│   ├── TestEnhancedAutoHeal.java
│   └── org/example/
│       ├── utils/
│       │   ├── AutoHealManager.java
│       │   ├── AutoHealReportAggregator.java
│       │   ├── AutoHealSuiteConfig.java
│       │   └── AutoHealSuiteListener.java
│       └── pages/
│           ├── BasePage.java
│           ├── LoginPage.java
│           └── InventoryPage.java
├── src/test/java/org/example/
│   ├── quickstart/
│   │   └── QuickAutoHealTest.java
│   └── tests/
│       └── SauceDemoTest.java
└── src/test/resources/
    └── testng.xml
```

## How AutoHeal Works

AutoHeal Locator uses a sophisticated multi-layered approach to find elements, automatically healing broken selectors using AI when needed:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          AutoHeal Mechanism Flow                        │
└─────────────────────────────────────────────────────────────────────────┘

1. Original Selector Attempt
┌─────────────────────────┐
│  Try Original Selector  │ ──┐
│  input[data-test='btn'] │   │
└─────────────────────────┘   │
                              │
                              ▼
                         ┌─────────┐
                         │ Found?  │ ────── YES ──► Return Element ✅
                         └─────────┘
                              │
                              NO
                              ▼

2. Cache Lookup (Lightning Fast)
┌─────────────────────────┐
│   Check Cache Layer     │ ──┐
│  • Caffeine In-Memory   │   │
│  • File-Based Storage   │   │
└─────────────────────────┘   │
                              │
                              ▼
                         ┌─────────┐
                         │ Cached? │ ────── YES ──► Use Cached Selector ✅
                         └─────────┘
                              │
                              NO
                              ▼

3. AI Healing Process (Strategy-Based)
┌─────────────────────────────────────────────────────────────────────────┐
│                         AI Healing Strategies                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Strategy 1: DOM Analysis (All Providers)                             │
│  ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐   │
│  │  Extract HTML   │───►│   Send to AI     │───►│  Get Alternative│   │
│  │  DOM Structure  │    │   (GPT/Gemini)   │    │   Selectors     │   │
│  └─────────────────┘    └──────────────────┘    └─────────────────┘   │
│                                                                         │
│  Strategy 2: Visual Analysis (OpenAI/Gemini Only)                     │
│  ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐   │
│  │ Take Screenshot │───►│ AI Visual Analysis│───►│ Locate Element  │   │
│  │ of Current Page │    │ with Description  │    │ by Coordinates  │   │
│  └─────────────────┘    └──────────────────┘    └─────────────────┘   │
│                                                                         │
│  Execution Strategy (Configurable):                                    │
│  • SMART_SEQUENTIAL: DOM first, then Visual if available              │
│  • DOM_ONLY: Fast, cost-effective                                     │
│  • PARALLEL: Run both simultaneously (fastest but higher cost)        │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                         ┌─────────┐
                         │ Healed? │ ────── YES ──► Cache & Return ✅
                         └─────────┘
                              │
                              NO
                              ▼
                         ┌─────────┐
                         │  FAIL   │ ────── Throw Exception ❌
                         └─────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                            Cache Storage                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Level 1: In-Memory (Caffeine)                                        │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Key: "input[data-test='wrong-btn']|Login button"                │   │
│  │ Value: "input[data-test='login-button']"                        │   │
│  │ TTL: 24 hours | Max Size: 10,000 entries                       │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  Level 2: File-Based (Persistent)                                     │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Location: target/autoheal-cache/                                │   │
│  │ Format: JSON with metadata                                      │   │
│  │ Survives: Application restarts                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘

Example Healing Process:
═══════════════════════════

Input:  autoHeal.findElement("input[data-test='wrong-login-btn']", "Login button")

Step 1: Try "input[data-test='wrong-login-btn']" ❌ (Not found)
Step 2: Check cache for key "wrong-login-btn|Login button" ❌ (Not cached)
Step 3: AI DOM Analysis:
        - Extract page HTML
        - Send to Gemini: "Find element described as 'Login button'"
        - AI Response: "input[data-test='login-button']"
Step 4: Try healed selector "input[data-test='login-button']" ✅ (Found!)
Step 5: Cache the mapping for future use
Step 6: Return the WebElement

Result: Transparent healing - your test continues as if the selector was correct!
```

### Key Benefits:

- **🚀 Performance**: Cache ensures healed selectors are instant on subsequent runs
- **💰 Cost-Effective**: Cache minimizes AI API calls
- **🔄 Persistent**: File-based cache survives application restarts
- **⚙️ Configurable**: Choose strategy based on your needs (speed vs accuracy)
- **🤖 Intelligent**: AI understands element context and visual appearance
- **📊 Transparent**: Detailed reporting shows what was healed and cached

## Maven Configuration

Create a Maven project with the following structure:

```bash
mvn archetype:generate -DgroupId=org.example -DartifactId=my-autoheal-project -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd my-autoheal-project
```

Update your `pom.xml` with the complete configuration:

<details>
<summary><strong>📋 pom.xml - Click to expand Maven configuration</strong></summary>

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.example</groupId>
  <artifactId>my-autoheal-project</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <name>my-autoheal-project</name>
  <url>http://maven.apache.org</url>

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

</details>

## Properties Configuration

<details>
<summary><strong>⚙️ autoheal.properties - Click to expand configuration file</strong></summary>

Create `src/main/resources/autoheal.properties` with the following content:

```properties
# ==================== AI CONFIGURATION ====================
# Framework automatically handles provider capabilities and intelligent fallbacks

# AI Provider Selection (Required)
autoheal.ai.provider=GOOGLE_GEMINI                    # (Choose: OPENAI, GOOGLE_GEMINI, ANTHROPIC_CLAUDE, DEEPSEEK, GROK, LOCAL_MODEL, MOCK)

# API Credentials (Required)
autoheal.ai.api-key=your-google-gemini-api-key-here   # (Your AI provider API key - use environment variables in production)

# AI Model Configuration (Optional)
autoheal.ai.model=gemini-2.0-flash                    # (Framework uses intelligent defaults if not specified)

# API Communication Settings
autoheal.ai.timeout=30s                               # (Maximum wait time for AI API responses)
autoheal.ai.max-retries=3                             # (Number of retry attempts for failed AI calls)

# Visual Analysis Capabilities
autoheal.ai.visual-analysis-enabled=true              # (Enable screenshot-based healing - requires compatible provider)

# ==================== CACHE CONFIGURATION ====================
# Dual-layer caching system for optimal performance and persistence

# Cache Type Selection
autoheal.cache.type=PERSISTENT_FILE                   # (Options: MEMORY_ONLY, PERSISTENT_FILE, REDIS, HYBRID)

# Cache Size and Expiration
autoheal.cache.maximum-size=10000                     # (Maximum number of cached entries)
autoheal.cache.expire-after-write=24h                 # (Cache entries expire after this time from creation)
autoheal.cache.expire-after-access=2h                 # (Cache entries expire after this time from last access)

# Cache Monitoring
autoheal.cache.record-stats=true                      # (Enable cache hit/miss statistics for performance monitoring)

# Cache Directory (Auto-created by AutoHealManager)
# autoheal.cache.directory=target/autoheal-cache      # (Custom cache directory - uses default if not specified)

# ==================== PERFORMANCE CONFIGURATION ====================
# Threading, timeouts, and execution optimization

# Threading Configuration
autoheal.performance.thread-pool-size=4               # (Number of threads for parallel AI operations)
autoheal.performance.element-timeout=45s              # (Maximum wait time for element location operations)

# Performance Monitoring
autoheal.performance.enable-metrics=true              # (Enable performance metrics collection)

# Healing Strategy Selection
autoheal.performance.execution-strategy=SMART_SEQUENTIAL  # (Options: SMART_SEQUENTIAL, DOM_ONLY, PARALLEL, VISUAL_FIRST)

# ==================== REPORTING CONFIGURATION ====================
# Comprehensive reporting system for healing activities

# Report Generation Control
autoheal.reporting.enabled=true                       # (Enable/disable all reporting features)
autoheal.reporting.generate-html=true                 # (Generate HTML reports with filtering and statistics)
autoheal.reporting.generate-json=true                 # (Generate JSON reports for programmatic access)
autoheal.reporting.generate-text=true                 # (Generate plain text summary reports)

# Console Output
autoheal.reporting.console-logging=true               # (Enable detailed console output during test execution)

# Report Output Configuration
autoheal.reporting.output-directory=target/autoheal-reports  # (Directory for generated reports)
autoheal.reporting.report-name-prefix=FinalTest_AutoHeal_Report  # (Prefix for report file names)

# ==================== SUITE INTEGRATION CONFIGURATION ====================
# Settings for TestNG suite integration and consolidated reporting

# Suite Listener Behavior (Used by AutoHealSuiteListener)
autoheal.suite.auto-run-after-suite=true              # (Automatically generate consolidated reports after suite completion)
autoheal.suite.generate-individual-reports=true      # (Generate individual test reports in addition to consolidated)
autoheal.suite.output-directory=test-reports          # (Base directory for suite reports - creates timestamped subdirectories)

# ==================== ADVANCED CONFIGURATION ====================
# Fine-tuning options for specific use cases

# Error Handling
autoheal.advanced.fail-fast-on-ai-errors=false       # (Stop execution on AI provider errors vs continue with fallback)
autoheal.advanced.retry-delay-ms=1000                 # (Delay between retry attempts in milliseconds)

# Debug and Development
autoheal.advanced.debug-mode=false                    # (Enable verbose debug logging for troubleshooting)
autoheal.advanced.save-screenshots=false              # (Save screenshots of failed healing attempts for analysis)
autoheal.advanced.cache-debug=false                   # (Enable detailed cache operation logging)
```

> **Note**: The example shows Google Gemini configuration. Replace the API key with your actual key, or use environment variables for security.

</details>

## TestNG Page Object Model

### Base Page Class

**BasePage.java** - *Abstract base class that provides AutoHeal-powered element interaction methods and page validation for all page objects.*

<details>
<summary><strong>📄 BasePage.java - Click to expand complete code</strong></summary>

Create `src/main/java/org/example/pages/BasePage.java`:

```java
package org.example.pages;

import com.autoheal.AutoHealLocator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base page class with AutoHeal integration
 */
public abstract class BasePage {
    protected static final Logger logger = LoggerFactory.getLogger(BasePage.class);

    protected final WebDriver driver;
    protected final AutoHealLocator autoHeal;

    protected BasePage(WebDriver driver, AutoHealLocator autoHeal) {
        this.driver = driver;
        this.autoHeal = autoHeal;
        logger.info("Initialized {} page", this.getClass().getSimpleName());
    }

    // ==================== ENHANCED ELEMENT INTERACTIONS ====================

    protected WebElement findElement(String selector, String description) {
        System.out.println("🔍 BasePage.findElement() called - Selector: " + selector + " | Description: " + description);

        try {
            WebElement element = autoHeal.findElement(selector, description);
            System.out.println("✅ Element found successfully: " + selector);
            return element;
        } catch (Exception e) {
            System.out.println("❌ Element NOT found: " + selector + " | Error: " + e.getMessage());
            throw e;
        }
    }

    private String getElementInfo(WebElement element) {
        try {
            String id = element.getAttribute("id");
            String className = element.getAttribute("class");
            String info = "";
            if (id != null && !id.isEmpty()) {
                info += "#" + id;
            }
            if (className != null && !className.isEmpty()) {
                info += "." + className.split(" ")[0]; // first class only
            }
            return info;
        } catch (Exception e) {
            return "";
        }
    }

    protected void click(String selector, String description) {
        findElement(selector, description).click();
    }

    protected void type(String selector, String text, String description) {
        WebElement element = findElement(selector, description);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(String selector, String description) {
        return findElement(selector, description).getText();
    }

    protected boolean isDisplayed(String selector, String description) {
        try {
            return findElement(selector, description).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isElementPresent(String selector, String description) {
        try {
            findElement(selector, description);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== PAGE VALIDATION METHODS ====================

    /**
     * Validate that the page is loaded correctly
     */
    public abstract boolean isPageLoaded();

    /**
     * Wait for page to be loaded
     */
    public void waitForPageToLoad() {
        if (!isPageLoaded()) {
            throw new RuntimeException("Page failed to load properly: " + this.getClass().getSimpleName());
        }
    }
}
```

</details>

### Login Page Implementation

**LoginPage.java** - *Page object class for the Sauce Demo login page with intentionally wrong selectors to demonstrate AutoHeal functionality.*

<details>
<summary><strong>📄 LoginPage.java - Click to expand complete code</strong></summary>

Create `src/main/java/org/example/pages/LoginPage.java`:

```java
package org.example.pages;

import com.autoheal.AutoHealLocator;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPage extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(LoginPage.class);

    // Selectors for Sauce Demo
    private static final String USERNAME_FIELD = "input[data-test='username']";
    private static final String PASSWORD_FIELD = "input[data-test='password1']";
    private static final String LOGIN_BUTTON = "input[data-test='login-button-wrong']";
    private static final String ERROR_MESSAGE = "[data-test='error']";
    private static final String LOGO = ".login_logo";

    public LoginPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    public void login(String username, String password) {
        logger.info("Performing login with username: {}", username);
        type(USERNAME_FIELD, username, "Username input field");
        type(PASSWORD_FIELD, password, "Password input field");
        click(LOGIN_BUTTON, "Login submit button");
    }

    public String getErrorMessage() {
        if (isElementPresent(ERROR_MESSAGE, "Error message")) {
            return getText(ERROR_MESSAGE, "Error message");
        }
        return null;
    }

    public boolean isErrorDisplayed() {
        return isElementPresent(ERROR_MESSAGE, "Error message");
    }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(USERNAME_FIELD, "Username field") &&
               isDisplayed(PASSWORD_FIELD, "Password field") &&
               isDisplayed(LOGIN_BUTTON, "Login button");
    }
}
```

</details>

### Inventory Page Implementation

**InventoryPage.java** - *Page object class for the Sauce Demo inventory page with methods for product interaction and cart management.*

<details>
<summary><strong>📄 InventoryPage.java - Click to expand complete code</strong></summary>

Create `src/main/java/org/example/pages/InventoryPage.java`:

```java
package org.example.pages;

import com.autoheal.AutoHealLocator;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryPage extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(InventoryPage.class);

    // Selectors for Sauce Demo Inventory
    private static final String PAGE_TITLE = ".title";
    private static final String INVENTORY_ITEMS = ".inventory_item";
    private static final String ADD_TO_CART_BUTTON = "button[data-test*='add-to-cart']";
    private static final String REMOVE_BUTTON = "button[data-test*='remove']";
    private static final String SHOPPING_CART_BADGE = ".shopping_cart_badge";
    private static final String SHOPPING_CART_LINK = ".shopping_cart_link";
    private static final String SORT_DROPDOWN = ".product_sort_container";

    public InventoryPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    public String getPageTitle() {
        return getText(PAGE_TITLE, "Inventory page title");
    }

    public int getInventoryItemCount() {
        // Using AutoHeal to find all inventory items
        try {
            return autoHeal.findElements(INVENTORY_ITEMS, "Inventory items").size();
        } catch (Exception e) {
            logger.error("Failed to count inventory items: {}", e.getMessage());
            return 0;
        }
    }

    public void addFirstItemToCart() {
        click(ADD_TO_CART_BUTTON, "First add to cart button");
    }

    public void goToShoppingCart() {
        click(SHOPPING_CART_LINK, "Shopping cart link");
    }

    public String getCartItemCount() {
        if (isElementPresent(SHOPPING_CART_BADGE, "Shopping cart badge")) {
            return getText(SHOPPING_CART_BADGE, "Shopping cart badge");
        }
        return "0";
    }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(PAGE_TITLE, "Page title") &&
               isElementPresent(INVENTORY_ITEMS, "Inventory items");
    }
}
```

</details>

### Complete TestNG Test Class

**SauceDemoTest.java** - *Main test class that demonstrates AutoHeal functionality with login, inventory, and healing tests using the Page Object Model.*

<details>
<summary><strong>🧪 SauceDemoTest.java - Click to expand complete test class</strong></summary>

Create `src/test/java/org/example/tests/SauceDemoTest.java`:

```java
package org.example.tests;

import com.autoheal.AutoHealLocator;
import org.example.pages.LoginPage;
import org.example.pages.InventoryPage;
import org.example.utils.AutoHealManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

public class SauceDemoTest {
    private static final Logger logger = LoggerFactory.getLogger(SauceDemoTest.class);

    private WebDriver driver;
    private AutoHealLocator autoHeal;
    private LoginPage loginPage;
    private InventoryPage inventoryPage;

    private static final String BASE_URL = "https://www.saucedemo.com";
    private static final String VALID_USERNAME = "standard_user";
    private static final String VALID_PASSWORD = "secret_sauce";

    @BeforeMethod
    public void setUp() {
        logger.info("Setting up test environment");

        // Setup Chrome driver
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        // Create AutoHeal instance
        autoHeal = AutoHealManager.createFullAutoHeal(driver);

        // Initialize page objects
        loginPage = new LoginPage(driver, autoHeal);
        inventoryPage = new InventoryPage(driver, autoHeal);

        // Navigate to Sauce Demo
        driver.get(BASE_URL);
    }

    @AfterMethod
    public void tearDown() {
        logger.info("Cleaning up test environment");

        if (autoHeal != null) {
            autoHeal.shutdown();
        }
        if (driver != null) {
            driver.quit();
        }
    }

    @Test(description = "Test successful login flow")
    public void testSuccessfulLogin() {
        logger.info("=== Testing Successful Login ===");

        // Verify login page loaded
        Assert.assertTrue(loginPage.isPageLoaded(), "Login page should be loaded");

        // Perform login
        loginPage.login(VALID_USERNAME, VALID_PASSWORD);

        // Wait for page transition
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify successful navigation to inventory page
        Assert.assertTrue(inventoryPage.isPageLoaded(), "Should be on inventory page");
        Assert.assertEquals(inventoryPage.getPageTitle(), "Products", "Page title should be 'Products'");

        logger.info("✅ Login test completed successfully");
    }

    @Test(description = "Test login with invalid credentials")
    public void testInvalidLogin() {
        logger.info("=== Testing Invalid Login ===");

        // Attempt login with invalid credentials
        loginPage.login("invalid_user", "invalid_password");

        // Wait for error message
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify error is displayed
        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should be displayed");
        String errorMessage = loginPage.getErrorMessage();
        Assert.assertNotNull(errorMessage, "Error message should not be null");
        Assert.assertTrue(errorMessage.contains("do not match"), "Should show credentials error");

        logger.info("✅ Invalid login test completed successfully");
    }

    @Test(description = "Test AutoHeal healing functionality", dependsOnMethods = "testSuccessfulLogin")
    public void testAutoHealFunctionality() {
        logger.info("=== Testing AutoHeal Healing ===");

        // Login first
        loginPage.login(VALID_USERNAME, VALID_PASSWORD);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Test AutoHeal with wrong selector (should heal automatically)
        try {
            // This uses a wrong selector that AutoHeal should fix
            autoHeal.findElement("button[data-test='wrong-add-to-cart-sauce-labs-backpack']",
                               "Add backpack to cart button").click();

            // Verify item was added to cart
            String cartCount = inventoryPage.getCartItemCount();
            Assert.assertEquals(cartCount, "1", "One item should be in cart");

            logger.info("✅ AutoHeal successfully healed wrong selector and added item to cart");

        } catch (Exception e) {
            logger.error("AutoHeal test failed: {}", e.getMessage());
            Assert.fail("AutoHeal should have fixed the wrong selector");
        }

        logger.info("✅ AutoHeal functionality test completed successfully");
    }

    @Test(description = "Test inventory page functionality", dependsOnMethods = "testSuccessfulLogin")
    public void testInventoryPageFunctionality() {
        logger.info("=== Testing Inventory Page ===");

        // Login first
        loginPage.login(VALID_USERNAME, VALID_PASSWORD);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify inventory items are present
        int itemCount = inventoryPage.getInventoryItemCount();
        Assert.assertTrue(itemCount > 0, "Should have inventory items available");
        logger.info("Found {} inventory items", itemCount);

        // Add item to cart
        inventoryPage.addFirstItemToCart();

        // Verify cart count updated
        String cartCount = inventoryPage.getCartItemCount();
        Assert.assertEquals(cartCount, "1", "Cart should contain 1 item");

        logger.info("✅ Inventory page functionality test completed successfully");
    }
}
```

</details>

## AutoHealManager Utility

**AutoHealManager.java** - *Factory class that creates and configures AutoHeal instances with AI providers, caching, and reporting settings.*

<details>
<summary><strong>📁 AutoHealManager.java - Click to expand complete code</strong></summary>

Create `src/main/java/org/example/utils/AutoHealManager.java`:

```java
package org.example.utils;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import com.autoheal.config.CacheConfig;
import com.autoheal.model.AIProvider;
import org.openqa.selenium.WebDriver;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import static com.autoheal.config.ReportingConfig.enabledWithDefaults;

public class AutoHealManager {

    /**
     * Create AutoHeal with minimal AI configuration (uses defaults for most settings)
     *
     * Prerequisites - Set your API key as environment variable:
     * - Windows CMD: set GOOGLE_GEMINI_API_KEY=your-api-key
     * - Windows PowerShell: $env:GOOGLE_GEMINI_API_KEY="your-api-key"
     * - Mac/Linux: export GOOGLE_GEMINI_API_KEY=your-api-key
     *
     * Default parameters used:
     * - AI Provider: OPENAI (or configure via environment)
     * - AI Model: Default model for provider
     * - AI Timeout: 30 seconds
     * - AI Max Retries: 3 (with 1 second delay between retries)
     * - Visual Analysis: enabled
     * - Cache: dual-layer (Caffeine + File, 10,000 entries, 24h expiry)
     * - Retry Attempts: 3 (with 1 second delay between retries)
     * - Performance: default thread pool and timeouts
     * - Reporting: enabled (reports in current directory)
     */
    public static AutoHealLocator createMinimalAutoHeal(WebDriver driver) {
        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .ai(AIConfig.builder()
                        .provider(AIProvider.GOOGLE_GEMINI)
                        .apiKey(System.getenv("GEMINI_API_KEY"))
                        .build())
                .cache(CacheConfig.builder()
                        .cacheType(CacheConfig.CacheType.PERSISTENT_FILE)
                        .maximumSize(10000)
                        .expireAfterWrite(Duration.ofHours(24))
                        .expireAfterAccess(Duration.ofHours(2))
                        .recordStats(true)
                        .build())
                .reporting(enabledWithDefaults())
                .build();

        return AutoHealLocator.builder()
                .withWebAdapter(new com.autoheal.impl.adapter.SeleniumWebAutomationAdapter(driver))
                .withConfiguration(config)
                .build();
    }

    /**
     * Create AutoHeal with full AI configuration
     */
    public static AutoHealLocator createFullAutoHeal(WebDriver driver) {
        // Create cache directory if it doesn't exist
        createCacheDirectoryIfNeeded();

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
                        .provider(com.autoheal.model.AIProvider.valueOf(props.getProperty("autoheal.ai.provider", "GOOGLE_GEMINI")))
                        // Option 1: Load from properties file (recommended)
                        .apiKey(props.getProperty("autoheal.ai.api-key"))
                        // Option 2: Load from environment variable (alternative)
                        // .apiKey(System.getenv("GOOGLE_GEMINI_API_KEY"))
                        .timeout(Duration.ofSeconds(30))
                        .maxRetries(Integer.parseInt(props.getProperty("autoheal.ai.max-retries", "3")))
                        .visualAnalysisEnabled(Boolean.parseBoolean(props.getProperty("autoheal.ai.visual-analysis-enabled", "true")))
                        .build())
                .cache(CacheConfig.builder()
                        .cacheType(CacheConfig.CacheType.valueOf(props.getProperty("autoheal.cache.type", "PERSISTENT_FILE")))
                        .maximumSize(Integer.parseInt(props.getProperty("autoheal.cache.maximum-size", "10000")))
                        .expireAfterWrite(Duration.ofHours(24))
                        .expireAfterAccess(Duration.ofHours(2))
                        .recordStats(Boolean.parseBoolean(props.getProperty("autoheal.cache.record-stats", "true")))
                        .build())
                .performance(com.autoheal.config.PerformanceConfig.builder()
                        .threadPoolSize(Integer.parseInt(props.getProperty("autoheal.performance.thread-pool-size", "4")))
                        .elementTimeout(Duration.ofSeconds(45))
                        .enableMetrics(Boolean.parseBoolean(props.getProperty("autoheal.performance.enable-metrics", "true")))
                        .executionStrategy(com.autoheal.model.ExecutionStrategy.valueOf(props.getProperty("autoheal.performance.execution-strategy", "SMART_SEQUENTIAL")))
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

    /**
     * Create cache directory if it doesn't exist
     * This ensures the persistent file cache can work properly
     */
    private static void createCacheDirectoryIfNeeded() {
        // Default cache directory for PERSISTENT_FILE cache type
        java.io.File cacheDir = new java.io.File("target/autoheal-cache");

        if (!cacheDir.exists()) {
            boolean created = cacheDir.mkdirs();
            if (created) {
                System.out.println("✅ Created AutoHeal cache directory: " + cacheDir.getAbsolutePath());
            } else {
                System.out.println("❌ Failed to create AutoHeal cache directory: " + cacheDir.getAbsolutePath());
            }
        } else {
            System.out.println("📁 AutoHeal cache directory already exists: " + cacheDir.getAbsolutePath());

            // List existing cache files for debugging
            java.io.File[] cacheFiles = cacheDir.listFiles();
            if (cacheFiles != null && cacheFiles.length > 0) {
                System.out.println("🗂️  Found " + cacheFiles.length + " cache files:");
                for (java.io.File file : cacheFiles) {
                    System.out.println("   - " + file.getName() + " (" + file.length() + " bytes)");
                }
            } else {
                System.out.println("📝 Cache directory is empty - first run or cache expired");
            }
        }
    }
}
```

</details>

## Additional Utility Classes

### AutoHeal Suite Configuration

**AutoHealSuiteConfig.java** - *Configuration manager that generates unique run IDs and manages output directories for AutoHeal test reports.*

<details>
<summary><strong>⚙️ AutoHealSuiteConfig.java - Click to expand suite configuration</strong></summary>

Create `src/main/java/org/example/utils/AutoHealSuiteConfig.java`:

```java
package org.example.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuration class for AutoHeal Suite Report Aggregator
 */
public class AutoHealSuiteConfig {
    private static boolean generateIndividualReports = true;
    private static String outputDirectory = "test-reports";
    private static boolean autoRunAfterSuite = true;
    private static String currentRunId = null;

    // Generate unique run ID for current test execution
    static {
        currentRunId = "run_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }

    public static boolean isGenerateIndividualReports() {
        return generateIndividualReports;
    }

    public static void setGenerateIndividualReports(boolean generateIndividualReports) {
        AutoHealSuiteConfig.generateIndividualReports = generateIndividualReports;
    }

    public static String getOutputDirectory() {
        return outputDirectory;
    }

    public static void setOutputDirectory(String outputDirectory) {
        AutoHealSuiteConfig.outputDirectory = outputDirectory;
    }

    public static boolean isAutoRunAfterSuite() {
        return autoRunAfterSuite;
    }

    public static void setAutoRunAfterSuite(boolean autoRunAfterSuite) {
        AutoHealSuiteConfig.autoRunAfterSuite = autoRunAfterSuite;
    }

    public static String getCurrentRunId() {
        return currentRunId;
    }

    public static String getCurrentRunDirectory() {
        return outputDirectory + "/" + currentRunId;
    }

    public static void resetRunId() {
        currentRunId = "run_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }
}
```

</details>

### AutoHeal Suite Listener

**AutoHealSuiteListener.java** - *TestNG listener that automatically creates report directories on suite start and generates consolidated reports on suite completion.*

<details>
<summary><strong>🎧 AutoHealSuiteListener.java - Click to expand TestNG listener</strong></summary>

Create `src/main/java/org/example/utils/AutoHealSuiteListener.java`:

```java
package org.example.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * TestNG Suite Listener for AutoHeal Report Aggregation
 * Automatically generates consolidated report after suite completion
 */
public class AutoHealSuiteListener implements ISuiteListener {
    private static final Logger logger = LoggerFactory.getLogger(AutoHealSuiteListener.class);

    @Override
    public void onStart(ISuite suite) {
        logger.info("🚀 Starting AutoHeal Test Suite: {} (Run ID: {})",
                   suite.getName(), AutoHealSuiteConfig.getCurrentRunId());

        // Create run directory
        String runDirectory = AutoHealSuiteConfig.getCurrentRunDirectory();
        java.io.File dir = new java.io.File(runDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
            logger.info("Created report directory: {}", runDirectory);
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("🏁 Finishing AutoHeal Test Suite: {}", suite.getName());

        // Generate consolidated report if auto-run is enabled
        if (AutoHealSuiteConfig.isAutoRunAfterSuite()) {
            try {
                AutoHealReportAggregator.generateConsolidatedReport();
                logger.info("✅ AutoHeal consolidated report generated automatically");
            } catch (Exception e) {
                logger.error("Failed to generate consolidated AutoHeal report", e);
            }
        } else {
            logger.info("ℹ️ Auto-run disabled. Run manually: java AutoHealReportAggregator {}",
                       AutoHealSuiteConfig.getCurrentRunDirectory());
        }
    }
}
```

</details>

### AutoHeal Report Aggregator

**AutoHealReportAggregator.java** - *Utility that parses individual JSON reports and generates consolidated HTML reports with filtering and statistics.*

<details>
<summary><strong>📊 AutoHealReportAggregator.java - Click to expand report aggregator</strong></summary>

Create `src/main/java/org/example/utils/AutoHealReportAggregator.java`:

```java
package org.example.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to aggregate individual AutoHeal reports into a consolidated suite report
 */
public class AutoHealReportAggregator {
    private static final Logger logger = LoggerFactory.getLogger(AutoHealReportAggregator.class);

    public static class SelectorReport {
        public String originalSelector;
        public String actualSelector;
        public String strategy;
        public long executionTime;
        public boolean success;
        public String elementDescription;
        public int tokensUsed;
        public String reasoning;
        public String testContext;

        public SelectorReport(String originalSelector, String actualSelector, String strategy,
                            long executionTime, boolean success, String elementDescription,
                            int tokensUsed, String reasoning, String testContext) {
            this.originalSelector = originalSelector;
            this.actualSelector = actualSelector;
            this.strategy = strategy;
            this.executionTime = executionTime;
            this.success = success;
            this.elementDescription = elementDescription;
            this.tokensUsed = tokensUsed;
            this.reasoning = reasoning;
            this.testContext = testContext;
        }
    }

    /**
     * Auto-run aggregation for current test run
     */
    public static void generateConsolidatedReport() {
        String runDirectory = AutoHealSuiteConfig.getCurrentRunDirectory();

        // Debug: Also check project root for individual reports
        String projectRoot = System.getProperty("user.dir");
        System.out.println("🔧 Aggregator Debug - Looking for reports in run directory: " + runDirectory);
        System.out.println("🔧 Aggregator Debug - Also checking project root: " + projectRoot);

        // Try run directory first, then project root as fallback
        generateConsolidatedReport(runDirectory);

        // If no reports found in run directory, try project root
        File runDir = new File(runDirectory);
        File[] jsonFiles = runDir.listFiles((d, name) -> name.endsWith("_AutoHeal_Report.json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("⚠️ No reports in run directory, trying project root...");
            generateConsolidatedReportFromProjectRoot(projectRoot, runDirectory);
        }
    }

    /**
     * Generate consolidated report from project root (fallback)
     */
    private static void generateConsolidatedReportFromProjectRoot(String projectRoot, String outputDir) {
        try {
            logger.info("Fallback: Looking for AutoHeal reports in project root: {}", projectRoot);

            File rootDir = new File(projectRoot);
            File[] jsonFiles = rootDir.listFiles((d, name) ->
                name.contains("AutoHeal") && name.endsWith(".json") && !name.contains("TestSuite"));

            if (jsonFiles == null || jsonFiles.length == 0) {
                logger.warn("No individual AutoHeal JSON report files found in project root");
                return;
            }

            logger.info("Found {} individual AutoHeal report files in project root", jsonFiles.length);

            // Parse each JSON report file
            List<SelectorReport> allReports = new ArrayList<>();
            for (File jsonFile : jsonFiles) {
                try {
                    List<SelectorReport> reports = parseJsonReport(jsonFile);
                    allReports.addAll(reports);
                    logger.debug("Parsed {} selector reports from {}", reports.size(), jsonFile.getName());
                } catch (Exception e) {
                    logger.error("Failed to parse report file: {}", jsonFile.getName(), e);
                }
            }

            if (allReports.isEmpty()) {
                logger.warn("No selector reports found in any files");
                return;
            }

            // Generate consolidated HTML report in the output directory
            String consolidatedReportPath = outputDir + "/AutoHeal_Suite_Consolidated_Report_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".html";

            generateConsolidatedHtmlReport(allReports, consolidatedReportPath);

            logger.info("✅ Fallback: Consolidated AutoHeal suite report generated from project root: {}", consolidatedReportPath);

        } catch (Exception e) {
            logger.error("Failed to generate consolidated AutoHeal report from project root", e);
        }
    }

    /**
     * Manual aggregation for specified directory
     */
    public static void generateConsolidatedReport(String reportDirectory) {
        try {
            logger.info("Starting AutoHeal report aggregation from directory: {}", reportDirectory);

            File dir = new File(reportDirectory);
            if (!dir.exists() || !dir.isDirectory()) {
                logger.warn("Report directory does not exist: {}", reportDirectory);
                return;
            }

            // Find all AutoHeal JSON report files
            List<SelectorReport> allReports = new ArrayList<>();
            File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith("_AutoHeal_Report.json"));

            if (jsonFiles == null || jsonFiles.length == 0) {
                logger.warn("No AutoHeal JSON report files found in: {}", reportDirectory);
                return;
            }

            logger.info("Found {} AutoHeal report files to process", jsonFiles.length);

            // Parse each JSON report file
            for (File jsonFile : jsonFiles) {
                try {
                    List<SelectorReport> reports = parseJsonReport(jsonFile);
                    allReports.addAll(reports);
                    logger.debug("Parsed {} selector reports from {}", reports.size(), jsonFile.getName());
                } catch (Exception e) {
                    logger.error("Failed to parse report file: {}", jsonFile.getName(), e);
                }
            }

            if (allReports.isEmpty()) {
                logger.warn("No selector reports found in any files");
                return;
            }

            // Generate consolidated HTML report
            String consolidatedReportPath = reportDirectory + "/AutoHeal_Suite_Consolidated_Report_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".html";

            generateConsolidatedHtmlReport(allReports, consolidatedReportPath);

            logger.info("✅ Consolidated AutoHeal suite report generated: {}", consolidatedReportPath);

        } catch (Exception e) {
            logger.error("Failed to generate consolidated AutoHeal report", e);
        }
    }

    // Additional helper methods (parseJsonReport, generateConsolidatedHtmlReport, etc.)
    // ... [Full implementation continues - truncated for brevity in this display]

    /**
     * Main method for manual execution
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java AutoHealReportAggregator <report-directory>");
            System.out.println("Example: java AutoHealReportAggregator test-reports/run_2025-09-23_15-30-45");
            return;
        }

        String reportDirectory = args[0];
        generateConsolidatedReport(reportDirectory);
    }
}
```

> **Note**: This is a partial display of the AutoHealReportAggregator class. The full implementation includes JSON parsing, HTML report generation, and filtering functionality.

</details>

## Running Tests

### Create TestNG XML Configuration

<details>
<summary><strong>⚙️ testng.xml - Click to expand configuration</strong></summary>

Create `src/test/resources/testng.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suite name="AutoHeal Test Suite" verbose="1">
    <listeners>
        <listener class-name="org.example.utils.AutoHealSuiteListener"/>
    </listeners>

    <test name="Sauce Demo Tests">
        <classes>
            <class name="org.example.tests.SauceDemoTest"/>
            <!-- Add more test classes here -->
            <!-- <class name="org.example.tests.LoginTest"/> -->
            <!-- <class name="org.example.tests.CheckoutTest"/> -->
        </classes>
    </test>
</suite>
```

</details>

### Run Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SauceDemoTest

# Run specific test method
mvn test -Dtest=SauceDemoTest#testSuccessfulLogin
```

When tests are run, two files will be created:
1. **Consolidated Report**: Overall test suite report with all AutoHeal activities
2. **Individual Locator Reports**: Detailed reports for each healed locator with filters and metrics

---

## 📚 Additional Documentation

For advanced topics and extended guidance, see our comprehensive documentation:

### **🔗 [Extended AutoHeal Documentation](extended-documentation.md)**

**Covers advanced topics including:**

- **🚀 Quick Start Guide** - 5-minute setup for immediate results
- **🔧 Troubleshooting** - Common issues, solutions, and debug strategies
- **💡 Real-World Examples** - E-commerce, forms, multi-language testing
- **⚡ Performance Optimization** - Large test suites, cost reduction, caching strategies
- **🔄 CI/CD Integration** - GitHub Actions, Jenkins, Docker configurations
- **📦 Migration Guide** - Converting existing Selenium tests step-by-step
- **🎯 Best Practices** - Advanced patterns, team collaboration, environment configs

**Perfect for:**
- Teams scaling AutoHeal across large test suites
- Production deployments and CI/CD integration
- Advanced customization and optimization
- Troubleshooting and performance tuning

---

## Summary

This guide provides a complete TestNG-based implementation using AutoHeal Locator with Sauce Demo:

- ✅ **Project Setup**: Maven project structure with all required files
- ✅ **Configuration**: Complete pom.xml and properties setup
- ✅ **Page Object Model**: TestNG-based page objects with AutoHeal
- ✅ **Test Implementation**: Working Sauce Demo test examples
- ✅ **Utility Classes**: Complete suite management and reporting system
- ✅ **File Paths**: Copy-paste ready code with exact locations
- ✅ **Cache Management**: Automatic directory creation and persistent caching
- ✅ **Best Practices**: Selector strategies and test organization
- ✅ **Maven Publishing**: Ready for dependency creation and distribution

The AutoHeal Locator provides AI-powered self-healing capabilities that automatically fix broken selectors, reducing test maintenance and improving stability. Use the exact file paths and code examples provided to quickly implement AutoHeal in your TestNG projects.


