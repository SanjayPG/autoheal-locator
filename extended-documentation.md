# AutoHeal Locator - Extended Documentation

This document contains additional sections that can be integrated into the main selenium-usage-guide.md if needed.

## Quick Start (5-Minute Setup)

### Instant AutoHeal Demo

**Step 1: Add Dependency**
```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>autoheal-locator</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**Step 2: Set Environment Variable**
```bash
# Windows
set GOOGLE_GEMINI_API_KEY=your-api-key-here

# Mac/Linux
export GOOGLE_GEMINI_API_KEY=your-api-key-here
```

**Step 3: Replace WebDriver Initialization**
```java
// Before AutoHeal
WebDriver driver = new ChromeDriver();
WebElement button = driver.findElement(By.id("submit-btn"));

// After AutoHeal
WebDriver driver = new ChromeDriver();
AutoHealLocator autoHeal = AutoHealManager.createMinimalAutoHeal(driver);
WebElement button = autoHeal.findElement("button[data-test='wrong-submit']", "Submit button");
```

**Step 4: Run Your Test**
```bash
mvn test
```

**Result**: AutoHeal automatically fixes wrong selectors using AI! 🎉

---

## Troubleshooting Guide

### Common Issues and Solutions

#### **1. API Key Configuration Issues**

**Problem:** `AI provider authentication failed`

**Solutions:**
```bash
# Verify environment variable is set
echo $GOOGLE_GEMINI_API_KEY  # Mac/Linux
echo %GOOGLE_GEMINI_API_KEY%  # Windows

# Test API key validity
curl -H "Authorization: Bearer $GOOGLE_GEMINI_API_KEY" \
  https://generativelanguage.googleapis.com/v1/models
```

**Alternative:** Use properties file instead
```properties
autoheal.ai.api-key=your-actual-api-key-here
```

#### **2. Cache Directory Issues**

**Problem:** `Failed to create cache directory`

**Solutions:**
```bash
# Check permissions
ls -la target/  # Should show autoheal-cache directory

# Manual creation
mkdir -p target/autoheal-cache
chmod 755 target/autoheal-cache

# Alternative location
autoheal.cache.directory=/tmp/autoheal-cache
```

#### **3. ChromeDriver Issues**

**Problem:** `ChromeDriver not found`

**Solutions:**
```xml
<!-- Add WebDriverManager dependency -->
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.3.2</version>
</dependency>
```

```java
@BeforeMethod
public void setUp() {
    WebDriverManager.chromedriver().setup();
    driver = new ChromeDriver();
}
```

#### **4. Element Still Not Found**

**Problem:** AutoHeal fails to heal selector

**Debug Steps:**
```properties
# Enable debug mode
autoheal.advanced.debug-mode=true
autoheal.advanced.save-screenshots=true
autoheal.advanced.cache-debug=true
```

**Check logs for:**
- `AI provider response: [details]`
- `Cache hit/miss statistics`
- `Screenshot saved: [path]`

#### **5. Slow Test Execution**

**Problem:** Tests take too long

**Optimizations:**
```properties
# Use faster strategy
autoheal.performance.execution-strategy=DOM_ONLY

# Reduce timeouts
autoheal.performance.element-timeout=15s
autoheal.ai.timeout=10s

# Optimize cache
autoheal.cache.expire-after-access=30m
```

#### **6. High AI API Costs**

**Problem:** Too many API calls

**Cost Reduction:**
```properties
# Longer cache retention
autoheal.cache.expire-after-write=7d
autoheal.cache.expire-after-access=24h

# Skip visual analysis
autoheal.ai.visual-analysis-enabled=false

# Use cheaper strategy
autoheal.performance.execution-strategy=DOM_ONLY
```

---

## Real-World Examples

### Example 1: E-commerce Website Testing

```java
public class EcommerceTest {
    @Test
    public void testProductSearch() {
        // AutoHeal handles dynamic product IDs
        autoHeal.findElement("input[data-test='search-box-wrong']", "Product search input")
               .sendKeys("laptop");

        autoHeal.findElement("button[class*='search-btn-old']", "Search submit button")
               .click();

        // Handles pagination changes
        List<WebElement> products = autoHeal.findElements(
            "div[class*='product-item-old']", "Product listing items");

        Assert.assertTrue(products.size() > 0, "Should find products");
    }
}
```

### Example 2: Form Automation with Dynamic Fields

```java
public class DynamicFormTest {
    @Test
    public void testFormSubmission() {
        // AutoHeal adapts to changing form structure
        autoHeal.findElement("input[name='firstName-v1']", "First name field")
               .sendKeys("John");

        autoHeal.findElement("input[id*='email-field']", "Email input")
               .sendKeys("john@example.com");

        // Handles different submit button variations
        autoHeal.findElement("button[type='submit'][class*='primary']", "Form submit button")
               .click();
    }
}
```

### Example 3: Multi-Language Website Testing

```java
public class MultiLanguageTest {
    @Test
    public void testLanguageSwitching() {
        // AutoHeal finds elements regardless of text changes
        autoHeal.findElement("a[data-lang='en']", "English language link").click();

        // Finds "Login" button in English
        autoHeal.findElement("button[contains(text(),'Login')]", "Login button").click();

        // Switch to Spanish
        autoHeal.findElement("a[data-lang='es']", "Spanish language link").click();

        // Finds "Iniciar Sesión" button in Spanish
        autoHeal.findElement("button[contains(text(),'Iniciar')]", "Login button").click();
    }
}
```

---

## Performance Optimization Guide

### Optimizing for Large Test Suites

#### **1. Strategy Selection Matrix**

| Test Suite Size | Recommended Strategy | Cache Settings | Expected Performance |
|----------------|---------------------|----------------|---------------------|
| **< 50 tests** | `SMART_SEQUENTIAL` | Default | ~10% overhead |
| **50-200 tests** | `DOM_ONLY` | Extended retention | ~5% overhead |
| **200+ tests** | `DOM_ONLY` + Redis | Persistent + Shared | ~2% overhead |

#### **2. Cache Optimization**

**For Development:**
```properties
autoheal.cache.type=MEMORY_ONLY
autoheal.cache.expire-after-access=1h
autoheal.cache.maximum-size=1000
```

**For CI/CD:**
```properties
autoheal.cache.type=PERSISTENT_FILE
autoheal.cache.expire-after-write=7d
autoheal.cache.maximum-size=50000
```

**For Large Teams:**
```properties
autoheal.cache.type=REDIS
autoheal.cache.redis.host=your-redis-server
autoheal.cache.expire-after-write=30d
```

#### **3. Cost Optimization Strategies**

**Minimize AI Calls:**
```properties
# Longer cache retention
autoheal.cache.expire-after-write=30d
autoheal.cache.expire-after-access=7d

# Conservative healing
autoheal.advanced.fail-fast-on-ai-errors=true
autoheal.ai.max-retries=1

# Skip expensive features
autoheal.ai.visual-analysis-enabled=false
```

**Smart Resource Usage:**
```properties
# Optimize threading
autoheal.performance.thread-pool-size=2
autoheal.performance.element-timeout=20s

# Batch processing
autoheal.performance.execution-strategy=DOM_ONLY
```

---

## CI/CD Integration Examples

### GitHub Actions Integration

```yaml
name: AutoHeal Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'

      - name: Run AutoHeal Tests
        env:
          GOOGLE_GEMINI_API_KEY: ${{ secrets.GOOGLE_GEMINI_API_KEY }}
        run: mvn test

      - name: Upload AutoHeal Reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: autoheal-reports
          path: target/autoheal-reports/
```

### Jenkins Pipeline Integration

```groovy
pipeline {
    agent any

    environment {
        GOOGLE_GEMINI_API_KEY = credentials('google-gemini-api-key')
    }

    stages {
        stage('Test with AutoHeal') {
            steps {
                sh 'mvn clean test'
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/autoheal-reports',
                        reportFiles: '*.html',
                        reportName: 'AutoHeal Report'
                    ])
                }
            }
        }
    }
}
```

### Docker Integration

```dockerfile
FROM openjdk:11-jdk-slim

# Install Chrome for testing
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable

# Copy test project
COPY . /app
WORKDIR /app

# Set environment variables
ENV GOOGLE_GEMINI_API_KEY=${GOOGLE_GEMINI_API_KEY}

# Run tests with AutoHeal
CMD ["mvn", "test"]
```

---

## Migration Guide

### Converting Existing Selenium Tests to AutoHeal

#### **Step 1: Identify Conversion Candidates**

**Good candidates for AutoHeal:**
- Tests with frequently changing selectors
- Cross-browser compatibility issues
- Dynamic content applications
- Tests that break during UI updates

**Priority conversion order:**
1. Most flaky tests first
2. Critical user journey tests
3. Cross-browser test suites
4. Regression test suites

#### **Step 2: Minimal Migration Pattern**

**Before AutoHeal:**
```java
public class BeforeAutoHeal {
    private WebDriver driver;

    @Test
    public void testLogin() {
        driver.findElement(By.id("username")).sendKeys("user");
        driver.findElement(By.id("password")).sendKeys("pass");
        driver.findElement(By.className("submit-btn")).click();
    }
}
```

**After AutoHeal (Minimal Changes):**
```java
public class AfterAutoHeal {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        autoHeal = AutoHealManager.createMinimalAutoHeal(driver);
    }

    @Test
    public void testLogin() {
        autoHeal.findElement("#username", "Username field").sendKeys("user");
        autoHeal.findElement("#password", "Password field").sendKeys("pass");
        autoHeal.findElement(".submit-btn", "Submit button").click();
    }
}
```

#### **Step 3: Gradual Migration Strategy**

**Phase 1: Wrapper Approach**
```java
public class MigrationHelper {
    private AutoHealLocator autoHeal;
    private WebDriver driver;

    // Backward compatible method
    public WebElement findElement(By locator) {
        return autoHeal.findElement(locator.toString(), "Legacy element");
    }

    // New AutoHeal method
    public WebElement findElement(String selector, String description) {
        return autoHeal.findElement(selector, description);
    }
}
```

**Phase 2: Page Object Conversion**
```java
// Convert one page object at a time
public class LoginPageV2 extends AutoHealBasePage {
    public void login(String username, String password) {
        // Use AutoHeal methods
        type("#username", username, "Username field");
        type("#password", password, "Password field");
        click(".submit-btn", "Login button");
    }
}
```

**Phase 3: Full AutoHeal Integration**
```java
// Complete integration with reporting
public class FullAutoHealTest {
    @Test
    public void testCompleteUserJourney() {
        loginPage.login("user", "pass");
        inventoryPage.addToCart("product-1");
        checkoutPage.completeOrder();

        // AutoHeal generates comprehensive reports
        // showing all healing activities
    }
}
```

#### **Step 4: Validation Checklist**

**Before going live:**
- [ ] All critical tests pass with AutoHeal
- [ ] Cache performance is acceptable
- [ ] Reporting provides useful insights
- [ ] Cost impact is within budget
- [ ] Team is trained on AutoHeal features
- [ ] Fallback plan exists for issues

**Monitoring after migration:**
- [ ] Track healing success rates
- [ ] Monitor API usage costs
- [ ] Review cache hit rates
- [ ] Analyze test execution times
- [ ] Collect team feedback

---

## Best Practices and Patterns

### Advanced Usage Patterns

#### **1. Custom AutoHeal Factory**
```java
public class CompanyAutoHealFactory {
    public static AutoHealLocator createForEnvironment(String env, WebDriver driver) {
        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .provider(getProviderForEnv(env))
                .apiKey(getApiKeyForEnv(env))
                .build())
            .cache(getCacheConfigForEnv(env))
            .reporting(getReportingConfigForEnv(env))
            .build();

        return AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(config)
            .build();
    }
}
```

#### **2. Environment-Specific Configurations**
```java
public class EnvironmentConfigs {
    public static AutoHealConfiguration development() {
        return AutoHealConfiguration.builder()
            .ai(AIConfig.builder().provider(AIProvider.MOCK).build())
            .cache(CacheConfig.builder().cacheType(MEMORY_ONLY).build())
            .reporting(ReportingConfig.builder().enabled(false).build())
            .build();
    }

    public static AutoHealConfiguration production() {
        return AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .provider(AIProvider.GOOGLE_GEMINI)
                .apiKey(System.getenv("PROD_GEMINI_KEY"))
                .build())
            .cache(CacheConfig.builder()
                .cacheType(REDIS)
                .expireAfterWrite(Duration.ofDays(30))
                .build())
            .reporting(ReportingConfig.enabledWithDefaults())
            .build();
    }
}
```

#### **3. Team Collaboration Patterns**
```java
public class TeamAutoHealPatterns {
    // Shared cache for team
    public static AutoHealLocator createSharedCache(WebDriver driver) {
        return AutoHealLocator.builder()
            .withConfiguration(AutoHealConfiguration.builder()
                .cache(CacheConfig.builder()
                    .cacheType(REDIS)
                    .redisHost("team-redis.company.com")
                    .build())
                .build())
            .build();
    }

    // Environment-aware setup
    public static AutoHealLocator createForCI(WebDriver driver) {
        String ciProvider = System.getenv("CI_PROVIDER");
        return CompanyAutoHealFactory.createForEnvironment(ciProvider, driver);
    }
}
```

This extended documentation provides comprehensive coverage of advanced AutoHeal usage, troubleshooting, and real-world implementation patterns. Review these sections and let me know which ones you'd like to integrate into the main guide.