# 🔵 Playwright AutoHeal Guide

Complete guide for using AutoHeal with Microsoft Playwright Java.

---

## 📋 Table of Contents

- [Why Playwright + AutoHeal?](#-why-playwright--autoheal)
- [Quick Start](#-quick-start)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage Examples](#-usage-examples)
- [Filter Support](#-filter-support)
- [Advanced Features](#-advanced-features)
- [Best Practices](#-best-practices)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Why Playwright + AutoHeal?

Playwright is a modern automation framework with powerful features, but locators can still break when the UI changes. AutoHeal adds intelligent self-healing capabilities:

✅ **Zero-Rewrite Migration** - Use native Playwright Locator objects directly
✅ **Full Filter Support** - hasText, hasNotText, and more
✅ **Framework-Aware AI** - Understands Playwright's semantic locators
✅ **Visual + DOM Healing** - Multiple healing strategies
✅ **Native Return Types** - Returns `Locator` objects, not wrappers
✅ **Performance Caching** - Remembers successful fixes

---

## 🚀 Quick Start

### 1. Add Dependency

```xml
<dependency>
  <groupId>io.github.sanjaypg</groupId>
  <artifactId>autoheal-locator</artifactId>
  <version>1.0.3</version>
</dependency>

<dependency>
  <groupId>com.microsoft.playwright</groupId>
  <artifactId>playwright</artifactId>
  <version>1.41.0</version>
</dependency>
```

### 2. Set API Key

```bash
# Windows
set GEMINI_API_KEY=your-api-key-here

# Mac/Linux
export GEMINI_API_KEY=your-api-key-here
```

### 3. Create AutoHeal Manager

**AutoHealManager.java:**
```java
package com.yourcompany.utils;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import com.autoheal.config.CacheConfig;
import com.autoheal.model.AIProvider;
import com.microsoft.playwright.Page;
import java.time.Duration;
import static com.autoheal.config.ReportingConfig.enabledWithDefaults;

public class AutoHealManager {

    public static AutoHealLocator createPlaywrightAutoHeal(Page page) {
        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .ai(AIConfig.builder()
                        .provider(AIProvider.GOOGLE_GEMINI)
                        .apiKey(System.getenv("GEMINI_API_KEY"))
                        .timeout(Duration.ofSeconds(60))
                        .maxRetries(3)
                        .build())
                .cache(CacheConfig.builder()
                        .cacheType(CacheConfig.CacheType.PERSISTENT_FILE)
                        .maximumSize(10000)
                        .expireAfterWrite(Duration.ofHours(24))
                        .build())
                .reporting(enabledWithDefaults())
                .build();

        return AutoHealLocator.builder()
                .withWebAdapter(new com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter(page))
                .withConfiguration(config)
                .build();
    }
}
```

### 4. Use in Your Tests

```java
package com.yourcompany.tests;

import com.yourcompany.utils.AutoHealManager;
import com.autoheal.AutoHealLocator;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.testng.annotations.*;
import org.testng.Assert;

public class LoginTest {
    private Playwright playwright;
    private Browser browser;
    private Page page;
    private AutoHealLocator autoHeal;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        autoHeal = AutoHealManager.createPlaywrightAutoHeal(page);
        page.navigate("https://demo.playwright.dev/todomvc");
    }

    @Test
    public void testAddTodo() {
        // Use native Playwright locators - AutoHeal wraps them automatically!
        Locator input = autoHeal.find(page,
            page.getByPlaceholder("What needs to be done?"),
            "Todo input field");

        input.fill("Buy groceries");
        input.press("Enter");

        // Verify the todo was added
        Locator todoItem = autoHeal.find(page,
            page.getByRole(AriaRole.LISTITEM),
            "First todo item");

        Assert.assertTrue(todoItem.textContent().contains("Buy groceries"));
    }

    @AfterMethod
    public void tearDown() {
        if (page != null) page.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
```

---

## 📦 Installation

### Option 1: Maven Central (Recommended)

```xml
<dependency>
  <groupId>io.github.sanjaypg</groupId>
  <artifactId>autoheal-locator</artifactId>
  <version>1.0.3</version>
</dependency>
```

### Option 2: Build from Source

```bash
git clone https://github.com/SanjayPG/autoheal-locator.git
cd autoheal-locator
mvn clean install -DskipTests
```

---

## ⚙️ Configuration

Create `src/test/resources/autoheal.properties`:

```properties
# AI Configuration
autoheal.ai.provider=GOOGLE_GEMINI
autoheal.ai.api-key=${GEMINI_API_KEY}
autoheal.ai.model=gemini-2.0-flash
autoheal.ai.timeout=30s
autoheal.ai.max-retries=3
autoheal.ai.visual-analysis-enabled=true

# Playwright-Specific Configuration
autoheal.playwright.healing-strategy=SMART_SEQUENTIAL  # Options: DOM_ONLY, VISUAL_ONLY, SMART_SEQUENTIAL, PARALLEL, VISUAL_FIRST, SEQUENTIAL
autoheal.playwright.enable-filters=true                # Enable filter parsing (default: true)

# Cache Configuration
autoheal.cache.type=PERSISTENT_FILE                    # Options: CAFFEINE, REDIS, PERSISTENT_FILE, HYBRID
autoheal.cache.maximum-size=10000
autoheal.cache.expire-after-write=24h
autoheal.cache.expire-after-access=2h
autoheal.cache.record-stats=true

# Performance Configuration
autoheal.performance.thread-pool-size=4
autoheal.performance.element-timeout=45s
autoheal.performance.enable-metrics=true

# Reporting Configuration
autoheal.reporting.enabled=true
autoheal.reporting.generate-html=true
autoheal.reporting.generate-json=true
autoheal.reporting.generate-text=true
autoheal.reporting.output-directory=target/autoheal-reports
autoheal.reporting.generate-individual-reports=true    # Generate separate report per test
```

---

## 💡 Usage Examples

### Basic Locators

#### Method 1: Native Playwright Locators (Zero-Rewrite)
```java
// Use your existing Playwright locators directly!
Locator button = autoHeal.find(page,
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")),
    "Submit button");

Locator input = autoHeal.find(page,
    page.getByPlaceholder("Username"),
    "Username field");

Locator text = autoHeal.find(page,
    page.getByText("Welcome"),
    "Welcome message");

Locator testId = autoHeal.find(page,
    page.getByTestId("submit-btn"),
    "Submit button by test ID");
```

#### Method 2: JavaScript-Style Strings
```java
// Or use JavaScript-style string format
Locator button = autoHeal.find(page,
    "getByRole('button', { name: 'Submit' })",
    "Submit button");

Locator input = autoHeal.find(page,
    "getByPlaceholder('Username')",
    "Username field");

Locator text = autoHeal.find(page,
    "getByText('Welcome')",
    "Welcome message");
```

#### Method 3: CSS Selectors and XPath
```java
// CSS selectors
Locator element = autoHeal.find(page,
    "#submit-btn",
    "Submit button");

// XPath
Locator element = autoHeal.find(page,
    "//button[@id='submit']",
    "Submit button");
```

---

## 🎯 Filter Support

AutoHeal fully supports Playwright's powerful filter features!

### hasText Filter (String)

```java
// Filter by exact text
Locator product = autoHeal.find(page,
    page.getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText("Product 1")),
    "Product 1 item");

// Or use string format
Locator product = autoHeal.find(page,
    "getByRole('listitem').filter({ hasText: 'Product 1' })",
    "Product 1 item");
```

### hasText Filter (Regex)

```java
// Filter by regex pattern
Locator product = autoHeal.find(page,
    page.getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText(Pattern.compile("Product \\d+"))),
    "Product item");

// Or use string format
Locator product = autoHeal.find(page,
    "getByRole('listitem').filter({ hasText: /Product \\d+/ })",
    "Product item");
```

### hasNotText Filter

```java
// Exclude items with specific text
Locator inStockItems = autoHeal.find(page,
    page.getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasNotText("Out of stock")),
    "In stock items");
```

### Chained Filters

```java
// Multiple filters
Locator product = autoHeal.find(page,
    page.getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText("Product 1"))
        .filter(new Locator.FilterOptions().setHasNotText("Out of stock")),
    "Product 1 in stock");
```

### Filter + Child Locator

```java
// Filter then find child element
Locator addToCartBtn = autoHeal.find(page,
    page.getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText("Product 1"))
        .getByRole(AriaRole.BUTTON),
    "Add to cart button for Product 1");
```

### Filter Support Status

| Filter Type | Support | Example |
|------------|---------|---------|
| **hasText (string)** | ✅ Full | `.filter(new Locator.FilterOptions().setHasText("text"))` |
| **hasText (regex)** | ✅ Full | `.filter(new Locator.FilterOptions().setHasText(Pattern.compile("pattern")))` |
| **hasNotText (string)** | ✅ Full | `.filter(new Locator.FilterOptions().setHasNotText("text"))` |
| **hasNotText (regex)** | ✅ Full | `.filter(new Locator.FilterOptions().setHasNotText(Pattern.compile("pattern")))` |
| **has (nested locator)** | ⚠️ Partial | Basic support, may need enhancement for complex cases |
| **hasNot (nested locator)** | ⚠️ Partial | Basic support, may need enhancement for complex cases |
| **visible** | ❌ Not yet | Not currently supported |

For detailed filter support information, see [FILTER_SUPPORT_STATUS.md](FILTER_SUPPORT_STATUS.md).

---

## 🎭 Advanced Features

### Healing Strategies

AutoHeal supports multiple healing strategies for Playwright:

#### 1. SMART_SEQUENTIAL (Default - Recommended)
```java
// Tries DOM analysis first, then Visual if DOM fails
// Best balance of speed and accuracy
```

#### 2. DOM_ONLY
```java
// Fastest - only uses DOM analysis
// Lower cost, no screenshot analysis
```

#### 3. VISUAL_ONLY
```java
// Uses only visual/screenshot analysis
// Higher cost but useful for complex UI
```

#### 4. PARALLEL
```java
// Runs DOM and Visual analysis in parallel
// Fastest total time but uses more resources
```

#### 5. VISUAL_FIRST
```java
// Tries Visual first, then DOM
// Useful when visual clues are more reliable
```

Configure in `autoheal.properties`:
```properties
autoheal.playwright.healing-strategy=SMART_SEQUENTIAL
```

### Page Object Model

```java
public class TodoPage {
    private final Page page;
    private final AutoHealLocator autoHeal;

    public TodoPage(Page page, AutoHealLocator autoHeal) {
        this.page = page;
        this.autoHeal = autoHeal;
    }

    public void addTodo(String todoText) {
        Locator input = autoHeal.find(page,
            page.getByPlaceholder("What needs to be done?"),
            "Todo input");
        input.fill(todoText);
        input.press("Enter");
    }

    public void completeTodo(String todoText) {
        Locator todoItem = autoHeal.find(page,
            page.getByRole(AriaRole.LISTITEM)
                .filter(new Locator.FilterOptions().setHasText(todoText)),
            "Todo: " + todoText);

        Locator checkbox = autoHeal.find(page,
            todoItem.getByRole(AriaRole.CHECKBOX),
            "Checkbox for: " + todoText);
        checkbox.check();
    }

    public int getTodoCount() {
        Locator todos = autoHeal.find(page,
            page.getByRole(AriaRole.LISTITEM),
            "All todos");
        return todos.count();
    }
}
```

### Working with Multiple Elements

```java
// Find multiple elements
Locator items = autoHeal.find(page,
    page.getByRole(AriaRole.LISTITEM),
    "All list items");

int count = items.count();
for (int i = 0; i < count; i++) {
    System.out.println(items.nth(i).textContent());
}
```

### Dynamic Content

```java
// AutoHeal handles dynamic content automatically
Locator notification = autoHeal.find(page,
    page.getByRole(AriaRole.ALERT),
    "Success notification");

// No need for explicit waits - AutoHeal handles it
notification.waitFor();
```

---

## 🎯 Best Practices

### 1. Use Descriptive Names

```java
// Good - Descriptive
Locator button = autoHeal.find(page,
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")),
    "Submit button on login form");

// Bad - Vague
Locator button = autoHeal.find(page,
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")),
    "button");
```

### 2. Prefer Semantic Locators

```java
// Good - Uses semantic locators
Locator input = autoHeal.find(page,
    page.getByLabel("Username"),
    "Username input");

// Less ideal - CSS selector
Locator input = autoHeal.find(page,
    "#username",
    "Username input");
```

### 3. Use Filters for Specificity

```java
// Good - Specific with filter
Locator btn = autoHeal.find(page,
    page.getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText("Product 1"))
        .getByRole(AriaRole.BUTTON),
    "Add to cart for Product 1");

// Less ideal - Generic
Locator btn = autoHeal.find(page,
    page.getByRole(AriaRole.BUTTON),
    "Add to cart button");
```

### 4. Reuse AutoHeal Instance

```java
// Good - One AutoHeal per page
@BeforeMethod
public void setUp() {
    page = browser.newPage();
    autoHeal = AutoHealManager.createPlaywrightAutoHeal(page);
}

// Bad - Creating new AutoHeal for each element
// autoHeal = AutoHealManager.createPlaywrightAutoHeal(page); // Don't do this repeatedly
```

### 5. Handle Cache Appropriately

```properties
# For stable applications - longer cache
autoheal.cache.expire-after-write=7d

# For frequently changing UI - shorter cache
autoheal.cache.expire-after-write=1h

# For development - disable cache
autoheal.cache.type=CAFFEINE
autoheal.cache.expire-after-write=1m
```

---

## 🔍 Debugging

### Enable Debug Mode

```properties
autoheal.advanced.debug-mode=true
autoheal.advanced.save-screenshots=true
autoheal.reporting.console-logging=true
```

### Check Reports

After test execution, check:
```
target/autoheal-reports/
├── AutoHeal_2024-01-01_12-00-00_AutoHeal_Report.html
├── AutoHeal_2024-01-01_12-00-00_AutoHeal_Report.json
└── AutoHeal_2024-01-01_12-00-00_AutoHeal_Report.txt
```

The HTML report shows:
- Original selector used
- Whether it was healed
- Healing strategy (DOM/Visual/Cache)
- Time taken
- Tokens used (AI cost)

---

## 🐛 Troubleshooting

### Issue: Filter locator not working

**Solution**: Make sure you're using version 1.0.3 or later which includes the filter fix.

```bash
mvn dependency:tree | grep autoheal-locator
```

Should show:
```
[INFO] +- io.github.sanjaypg:autoheal-locator:jar:1.0.3:compile
```

### Issue: "Original Selector" shows JavaScript format

This is **not an issue**! The report shows both:
- **Original Selector**: For display (may show internal format)
- **Actual Selector**: The final Java Playwright code

Visual analysis returns CSS selectors which is normal:
```java
page.locator("ul > li:nth-child(1) + li > button")  // This is correct!
```

### Issue: Visual analysis not working

Check that:
1. Visual analysis is enabled:
```properties
autoheal.ai.visual-analysis-enabled=true
```

2. Your AI provider supports vision (Gemini, GPT-4, Claude):
```properties
autoheal.ai.provider=GOOGLE_GEMINI  # Supports vision
autoheal.ai.model=gemini-2.0-flash  # Vision-capable model
```

3. For local models, vision is usually not supported:
```properties
autoheal.ai.provider=LOCAL_MODEL
autoheal.ai.visual-analysis-enabled=false  # Disable for local models
```

### Issue: Slow performance

**Solutions:**
```properties
# Use DOM-only for speed
autoheal.playwright.healing-strategy=DOM_ONLY

# Reduce timeouts
autoheal.ai.timeout=15s
autoheal.performance.element-timeout=20s

# Increase cache retention
autoheal.cache.expire-after-write=7d
```

### Issue: High AI costs

**Solutions:**
```properties
# Use cheaper providers
autoheal.ai.provider=GOOGLE_GEMINI  # or DEEPSEEK

# Disable visual analysis
autoheal.ai.visual-analysis-enabled=false

# Use local models (free)
autoheal.ai.provider=LOCAL_MODEL
autoheal.ai.base-url=http://localhost:11434/v1
autoheal.ai.model=llama3.2:3b

# Longer cache retention
autoheal.cache.expire-after-write=30d
```

---

## 📊 Performance Metrics

AutoHeal tracks performance metrics for Playwright tests:

```java
// Get metrics from AutoHeal
AIServiceMetrics metrics = autoHeal.getAIService().getMetrics();

System.out.println("Total AI Calls: " + metrics.getTotalRequests());
System.out.println("Success Rate: " + metrics.getSuccessRate() + "%");
System.out.println("Average Response Time: " + metrics.getAverageResponseTime() + "ms");
System.out.println("Cache Hit Rate: " + metrics.getCacheHitRate() + "%");
```

---

## 🔗 Related Documentation

- [Main README](README.md) - Overview and installation
- [Selenium Guide](selenium-usage-guide.md) - Selenium-specific documentation
- [Filter Support Status](FILTER_SUPPORT_STATUS.md) - Detailed filter support information
- [Extended Documentation](extended-documentation.md) - Advanced configuration

---

## 🤝 Contributing

Found a bug or have a feature request for Playwright support?

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/SanjayPG/autoheal-locator/issues)
- **💡 Feature Requests**: [GitHub Discussions](https://github.com/SanjayPG/autoheal-locator/discussions)

---

<div align="center">

**🔵 Playwright + AutoHeal = Resilient Test Automation**

[⬆ Back to Top](#-playwright-autoheal-guide)

</div>
