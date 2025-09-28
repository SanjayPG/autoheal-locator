# AutoHeal Locator - Playwright Integration

## Quick Setup

The AutoHeal Locator now supports Playwright through its adapter pattern. Here's how to get started:

### 1. Add Dependencies

Add both AutoHeal Locator and Playwright to your `pom.xml`:

```xml
<dependencies>
    <!-- AutoHeal Locator -->
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>autoheal-locator</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

    <!-- Playwright -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.40.0</version>
    </dependency>
</dependencies>
```

### 2. Basic Usage

```java
import com.autoheal.AutoHealLocator;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.microsoft.playwright.*;
import org.openqa.selenium.WebElement;

// Initialize Playwright
Playwright playwright = Playwright.create();
Browser browser = playwright.chromium().launch();
Page page = browser.newPage();

// Create AutoHeal adapter for Playwright
PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
AutoHealLocator autoHeal = new AutoHealLocator(adapter);

// Navigate and use AutoHeal
page.navigate("https://example.com");
WebElement element = autoHeal.findElement("#submit-btn", "Submit button");
element.click();
```

### 3. What Works

All AutoHeal features work with Playwright:

- ✅ **Element Location & Healing**: Same API as Selenium
- ✅ **Visual Analysis**: Uses Playwright's screenshot capabilities
- ✅ **Selector Generation**: CSS selectors and XPath
- ✅ **Performance Caching**: Same caching mechanisms
- ✅ **Healing Analytics**: Full reporting support
- ✅ **Async Operations**: Built on CompletableFuture

### 4. Key Differences from Selenium

| Feature | Selenium | Playwright |
|---------|----------|------------|
| Element Type | `WebElement` | `PlaywrightElementWrapper.WrappedElement` |
| Screenshots | WebDriver API | Playwright Page API |
| Element Actions | Direct WebElement methods | Mapped to Playwright ElementHandle |
| Wait Strategies | WebDriverWait | Built into Playwright selectors |

### 5. Advanced Usage

```java
// Async element finding
CompletableFuture<WebElement> futureElement =
    autoHeal.findElementAsync("#dynamic-content", "Dynamic content");

// Multiple elements
List<WebElement> elements =
    autoHeal.findElements(".item", "List items");

// With healing options
LocatorOptions options = LocatorOptions.builder()
    .enableVisualAnalysis(true)
    .maxRetries(3)
    .build();

WebElement healedElement = autoHeal.findElement(
    "#broken-selector",
    "Important button",
    options
);
```

### 6. Migration from Selenium

If you're migrating from Selenium to Playwright:

1. Replace `WebDriver` initialization with `Page` initialization
2. Create `PlaywrightWebAutomationAdapter` instead of `SeleniumWebAutomationAdapter`
3. All AutoHeal API calls remain exactly the same
4. Element interactions work identically

### 7. Example Test Class

```java
@Test
public void testWithPlaywrightAndAutoHeal() {
    try (Playwright playwright = Playwright.create()) {
        Browser browser = playwright.chromium().launch();
        Page page = browser.newPage();

        PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
        AutoHealLocator autoHeal = new AutoHealLocator(adapter);

        page.navigate("https://my-app.com");

        // AutoHeal will fix broken selectors automatically
        WebElement loginBtn = autoHeal.findElement("#login-btn", "Login button");
        loginBtn.click();

        WebElement username = autoHeal.findElement("#username", "Username field");
        username.sendKeys("testuser");

        // Even if selectors break due to app changes, AutoHeal handles it
        WebElement password = autoHeal.findElement("#pwd-field", "Password field");
        password.sendKeys("password123");

        WebElement submit = autoHeal.findElement(".submit", "Submit button");
        submit.click();

        // Verify navigation worked
        Assertions.assertTrue(page.url().contains("dashboard"));
    }
}
```

The AutoHeal Locator's adapter pattern makes it easy to switch between Selenium and Playwright while keeping all the intelligent healing capabilities.