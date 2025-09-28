# Spring Boot Integration

## Overview

AutoHeal provides seamless integration with Spring Boot applications, offering auto-configuration, dependency injection, and Spring Test support for easy setup and management.

---

## Quick Setup

### 1. Dependencies

AutoHeal includes optional Spring Boot support. Add to your `pom.xml`:

```xml
<dependencies>
    <!-- AutoHeal with Spring Boot support -->
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>autoheal-locator</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Selenium -->
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.15.0</version>
    </dependency>
</dependencies>
```

### 2. Auto-Configuration

AutoHeal automatically configures itself in Spring Boot applications:

```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class AutoHealSpringBootTest {

    @Autowired
    private AutoHealLocator autoHeal;  // Automatically injected!

    @Autowired
    private WebDriver webDriver;       // Auto-configured WebDriver

    @Test
    @Order(1)
    void testLoginWithAutoHeal() {
        webDriver.get("https://example.com/login");

        // Use AutoHeal directly - fully configured via Spring
        WebElement usernameField = autoHeal.findElement("#username", "username input field");
        usernameField.sendKeys("testuser");

        WebElement loginButton = autoHeal.findElement("Sign In", "login submit button");
        loginButton.click();

        WebElement dashboard = autoHeal.findElement(".dashboard", "user dashboard");
        assertTrue(dashboard.isDisplayed());
    }
}
```

---

## Configuration Properties

Configure AutoHeal through Spring Boot `application.properties`:

### application.properties

```properties
# AutoHeal AI Configuration
autoheal.ai.provider=OPENAI
autoheal.ai.api-key=${OPENAI_API_KEY}
autoheal.ai.model=gpt-4o-mini
autoheal.ai.timeout=30s
autoheal.ai.max-retries=3
autoheal.ai.visual-analysis-enabled=true

# AutoHeal Cache Configuration
autoheal.cache.enabled=true
autoheal.cache.max-size=5000
autoheal.cache.expire-after-write=1h
autoheal.cache.expire-after-access=30m

# AutoHeal Performance Configuration
autoheal.performance.thread-pool-size=4
autoheal.performance.element-timeout=30s
autoheal.performance.execution-strategy=HYBRID

# AutoHeal Reporting Configuration
autoheal.reporting.enabled=true
autoheal.reporting.output-directory=./test-reports/autoheal
autoheal.reporting.generate-html=true
autoheal.reporting.generate-json=true
autoheal.reporting.report-name-prefix=${spring.application.name:MyApp}_AutoHeal

# WebDriver Configuration
autoheal.webdriver.type=CHROME
autoheal.webdriver.headless=false
autoheal.webdriver.window-size=1920x1080
autoheal.webdriver.implicit-wait=10s
```

### application-test.yml (YAML format)

```yaml
autoheal:
  ai:
    provider: OPENAI
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
    timeout: 30s
    max-retries: 3
    visual-analysis-enabled: true

  cache:
    enabled: true
    max-size: 5000
    expire-after-write: 1h

  performance:
    thread-pool-size: 4
    execution-strategy: HYBRID

  reporting:
    enabled: true
    output-directory: ./test-reports/autoheal
    generate-html: true
    report-name-prefix: ${spring.application.name}_AutoHeal

  webdriver:
    type: CHROME
    headless: true  # Headless for CI/CD
    window-size: 1920x1080
```

---

## Spring Boot Test Integration

### 1. Basic Spring Boot Test

```java
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserManagementTest {

    @Autowired
    private AutoHealLocator autoHeal;

    @Autowired
    private WebDriver webDriver;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Test
    void shouldCreateNewUser() {
        webDriver.get(baseUrl + "/admin/users");

        // Using AutoHeal with Spring Boot auto-configuration
        WebElement createButton = autoHeal.findElement("Create User", "create new user button");
        createButton.click();

        WebElement nameField = autoHeal.findElement("#user-name", "user name input field");
        nameField.sendKeys("John Doe");

        WebElement emailField = autoHeal.findElement("#user-email", "user email input field");
        emailField.sendKeys("john@example.com");

        WebElement saveButton = autoHeal.findElement("Save User", "save user button");
        saveButton.click();

        // Verify user creation
        WebElement successMessage = autoHeal.findElement(".success-alert", "user creation success message");
        assertThat(successMessage.getText()).contains("User created successfully");
    }
}
```

### 2. Page Object with Spring Dependency Injection

```java
@Component
public class LoginPage {

    @Autowired
    private AutoHealLocator autoHeal;

    @Autowired
    private WebDriver webDriver;

    // Locator constants
    private static final String USERNAME_FIELD = "#username";
    private static final String PASSWORD_FIELD = "#password";
    private static final String LOGIN_BUTTON = "Sign In";
    private static final String ERROR_MESSAGE = ".error-message";

    // Descriptions
    private static final String USERNAME_DESC = "username input on login page";
    private static final String PASSWORD_DESC = "password input on login page";
    private static final String LOGIN_BTN_DESC = "login submit button";
    private static final String ERROR_DESC = "login error message";

    public void navigate(String baseUrl) {
        webDriver.get(baseUrl + "/login");
    }

    public void enterCredentials(String username, String password) {
        WebElement usernameField = autoHeal.findElement(USERNAME_FIELD, USERNAME_DESC);
        usernameField.clear();
        usernameField.sendKeys(username);

        WebElement passwordField = autoHeal.findElement(PASSWORD_FIELD, PASSWORD_DESC);
        passwordField.clear();
        passwordField.sendKeys(password);
    }

    public void clickLogin() {
        WebElement loginButton = autoHeal.findElement(LOGIN_BUTTON, LOGIN_BTN_DESC);
        loginButton.click();
    }

    public String getErrorMessage() {
        try {
            return autoHeal.findElement(ERROR_MESSAGE, ERROR_DESC).getText();
        } catch (Exception e) {
            return null;
        }
    }
}

// Using the Page Object in tests
@SpringBootTest
public class LoginIntegrationTest {

    @Autowired
    private LoginPage loginPage;  // Spring-managed Page Object

    @Value("${app.base-url}")
    private String baseUrl;

    @Test
    void shouldLoginSuccessfully() {
        loginPage.navigate(baseUrl);
        loginPage.enterCredentials("validuser", "validpass");
        loginPage.clickLogin();

        // Verify login success
        assertThat(webDriver.getCurrentUrl()).contains("/dashboard");
    }
}
```

### 3. Test Configuration Class

```java
@TestConfiguration
@EnableConfigurationProperties({AutoHealProperties.class, WebDriverProperties.class})
public class TestConfig {

    @Bean
    @Primary
    public WebDriver testWebDriver(WebDriverProperties properties) {
        ChromeOptions options = new ChromeOptions();

        if (properties.isHeadless()) {
            options.addArguments("--headless");
        }

        options.addArguments("--window-size=" + properties.getWindowSize());
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(properties.getImplicitWait());

        return driver;
    }

    @Bean
    public AutoHealLocator autoHealLocator(WebDriver webDriver, AutoHealProperties properties) {
        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .provider(properties.getAi().getProvider())
                .apiKey(properties.getAi().getApiKey())
                .model(properties.getAi().getModel())
                .timeout(properties.getAi().getTimeout())
                .maxRetries(properties.getAi().getMaxRetries())
                .visualAnalysisEnabled(properties.getAi().isVisualAnalysisEnabled())
                .build())
            .cache(CacheConfig.builder()
                .enabled(properties.getCache().isEnabled())
                .maxSize(properties.getCache().getMaxSize())
                .expireAfterWrite(properties.getCache().getExpireAfterWrite())
                .build())
            .reporting(ReportingConfig.builder()
                .enabled(properties.getReporting().isEnabled())
                .outputDirectory(properties.getReporting().getOutputDirectory())
                .generateHTML(properties.getReporting().isGenerateHtml())
                .generateJSON(properties.getReporting().isGenerateJson())
                .reportNamePrefix(properties.getReporting().getReportNamePrefix())
                .build())
            .build();

        return AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(webDriver))
            .withConfiguration(config)
            .build();
    }

    @PreDestroy
    public void cleanup() {
        // Spring will handle AutoHeal shutdown automatically
    }
}
```

---

## Profile-Based Configuration

### Different Profiles for Different Environments

#### application-dev.properties
```properties
# Development - Fast feedback
autoheal.ai.model=gpt-4o-mini
autoheal.ai.visual-analysis-enabled=false
autoheal.webdriver.headless=false
autoheal.performance.execution-strategy=DOM_ONLY
```

#### application-ci.properties
```properties
# CI/CD - Reliable and fast
autoheal.ai.model=gpt-4o-mini
autoheal.ai.max-retries=2
autoheal.webdriver.headless=true
autoheal.performance.thread-pool-size=2
autoheal.cache.enabled=true
```

#### application-prod.properties
```properties
# Production Testing - Full accuracy
autoheal.ai.model=gpt-4o
autoheal.ai.visual-analysis-enabled=true
autoheal.performance.execution-strategy=HYBRID
autoheal.reporting.enabled=true
autoheal.cache.enabled=true
autoheal.cache.max-size=10000
```

---

## Spring Boot Actuator Integration

Monitor AutoHeal health and metrics:

### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Custom Health Indicator
```java
@Component
public class AutoHealHealthIndicator implements HealthIndicator {

    @Autowired
    private AutoHealLocator autoHeal;

    @Override
    public Health health() {
        try {
            HealthStatus status = autoHeal.getHealthStatus();

            return Health.up()
                .withDetail("cacheHitRate", status.getCacheHitRate())
                .withDetail("aiServiceStatus", status.getAiServiceStatus())
                .withDetail("averageHealingTime", status.getAverageHealingTime())
                .withDetail("totalElementsHealed", status.getTotalElementsHealed())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Metrics Endpoint
```java
@RestController
@RequestMapping("/api/autoheal")
public class AutoHealMetricsController {

    @Autowired
    private AutoHealLocator autoHeal;

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        AutoHealMetrics metrics = autoHeal.getMetrics();

        Map<String, Object> response = Map.of(
            "successRate", metrics.getSuccessRate(),
            "cacheHitRate", metrics.getCacheHitRate(),
            "avgHealingTime", metrics.getAverageHealingTime(),
            "totalCost", metrics.getTotalCost(),
            "savedCost", metrics.getSavedCost()
        );

        return ResponseEntity.ok(response);
    }
}
```

### Access Metrics
```bash
# Health check
curl http://localhost:8080/actuator/health/autoHeal

# Custom metrics
curl http://localhost:8080/api/autoheal/metrics
```

---

## Integration Testing with TestContainers

### Full Integration Test with Real Browser

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class FullIntegrationTest {

    @Container
    static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
            .withCapabilities(new ChromeOptions());

    @Autowired
    private AutoHealLocator autoHeal;

    @LocalServerPort
    private int port;

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = chrome.getWebDriver();
        // Configure AutoHeal with TestContainers WebDriver
        autoHeal.reconfigure(new SeleniumWebAutomationAdapter(driver));
    }

    @Test
    void shouldTestRealBrowserIntegration() {
        driver.get("http://host.testcontainers.internal:" + port + "/");

        // Test with real browser in container
        WebElement welcomeMessage = autoHeal.findElement("h1", "welcome message on homepage");
        assertThat(welcomeMessage.getText()).contains("Welcome");
    }
}
```

---

## Best Practices

### 1. Configuration Management
```java
// Use Spring profiles for different environments
@Value("${autoheal.ai.api-key}")
private String apiKey;

// Validate configuration at startup
@EventListener(ApplicationReadyEvent.class)
public void validateConfiguration() {
    if (StringUtils.isEmpty(apiKey)) {
        throw new IllegalStateException("AutoHeal AI API key not configured");
    }
}
```

### 2. Bean Lifecycle Management
```java
@Component
public class AutoHealLifecycleManager {

    @Autowired
    private AutoHealLocator autoHeal;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        // Ensure reports are generated
        autoHeal.shutdown();
    }
}
```

### 3. Test Slices
```java
// Custom test slice for AutoHeal tests
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import(AutoHealTestConfiguration.class)
public @interface AutoHealTest {
}

// Usage
@AutoHealTest
public class UserFlowTest {
    // Test implementation
}
```

---

## Spring Boot DevTools Integration

Enable hot reloading during test development:

```properties
# application-dev.properties
spring.devtools.restart.enabled=true
spring.devtools.restart.additional-paths=src/test/java
autoheal.cache.enabled=false  # Disable cache during development
```

---

## Next Steps

1. [Page Object Examples](./examples/page-object-examples.md) - Spring-managed Page Objects
2. [Reporting Guide](./reporting.md) - Spring Boot Actuator integration
3. [Performance Optimization](./performance.md) - Production Spring Boot settings