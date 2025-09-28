# Playwright Examples

This page provides practical **Java** examples of using AutoHeal Locator with Playwright for robust test automation.

## Basic Example

```java
import com.autoheal.AutoHealLocator;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.microsoft.playwright.*;
import org.openqa.selenium.WebElement;

public class BasicPlaywrightExample {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();

            // Create AutoHeal adapter for Playwright
            PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
            AutoHealLocator autoHeal = new AutoHealLocator(adapter);

            // Navigate to test site
            page.navigate("https://demo.playwright.dev/todomvc");

            // Find elements with AutoHeal - even if selectors break, AutoHeal will fix them
            WebElement todoInput = autoHeal.findElement(".new-todo", "new todo input field");
            todoInput.sendKeys("Learn AutoHeal with Playwright");
            todoInput.sendKeys("\n");

            // Verify todo was added
            var todos = autoHeal.findElements(".todo-list li", "todo list items");
            System.out.println("Added " + todos.size() + " todo items");

            browser.close();
        }
    }
}
```

## Page Object Example

```java
import com.autoheal.AutoHealLocator;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.microsoft.playwright.Page;
import org.openqa.selenium.WebElement;

public class TodoPage {
    private final Page page;
    private final AutoHealLocator autoHeal;

    // Selectors that might break over time
    private static final String NEW_TODO_INPUT = ".new-todo";
    private static final String TODO_LIST = ".todo-list";
    private static final String TODO_ITEMS = ".todo-list li";
    private static final String TOGGLE_ALL = ".toggle-all";

    public TodoPage(Page page) {
        this.page = page;
        this.autoHeal = new AutoHealLocator(new PlaywrightWebAutomationAdapter(page));
    }

    public TodoPage addTodo(String todoText) {
        // AutoHeal will find the input even if the selector changes
        WebElement todoInput = autoHeal.findElement(NEW_TODO_INPUT, "new todo input field");
        todoInput.sendKeys(todoText);
        todoInput.sendKeys("\n");
        return this;
    }

    public int getTodoCount() {
        var todos = autoHeal.findElements(TODO_ITEMS, "todo list items");
        return todos.size();
    }

    public TodoPage toggleAllTodos() {
        WebElement toggleAll = autoHeal.findElement(TOGGLE_ALL, "toggle all todos checkbox");
        toggleAll.click();
        return this;
    }

    public boolean hasTodo(String todoText) {
        var todos = autoHeal.findElements(TODO_ITEMS, "todo list items");
        return todos.stream()
            .anyMatch(todo -> todo.getText().contains(todoText));
    }
}
```

## Test Class Example

```java
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class TodoAppTest {
    private Playwright playwright;
    private Browser browser;
    private Page page;
    private TodoPage todoPage;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        todoPage = new TodoPage(page);

        page.navigate("https://demo.playwright.dev/todomvc");
    }

    @AfterEach
    void tearDown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Test
    void canAddTodoItems() {
        todoPage.addTodo("Learn Playwright");
        todoPage.addTodo("Learn AutoHeal");

        assertEquals(2, todoPage.getTodoCount());
        assertTrue(todoPage.hasTodo("Learn Playwright"));
        assertTrue(todoPage.hasTodo("Learn AutoHeal"));
    }

    @Test
    void canToggleAllTodos() {
        todoPage.addTodo("First todo");
        todoPage.addTodo("Second todo");

        todoPage.toggleAllTodos();

        // All todos should be completed
        assertEquals(2, todoPage.getTodoCount());
    }

    @Test
    void handlesChangingSelectors() {
        // This test simulates what happens when selectors change
        // AutoHeal will automatically adapt to find the right elements

        // Even if the internal CSS classes change, AutoHeal uses the description
        // to intelligently locate the "new todo input field"
        todoPage.addTodo("AutoHeal handles selector changes");

        assertTrue(todoPage.hasTodo("AutoHeal handles selector changes"));
    }
}
```

## Advanced Example with Configuration

```java
import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import com.autoheal.config.LocatorOptions;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.microsoft.playwright.*;

public class AdvancedPlaywrightExample {

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            // Configure AutoHeal with AI capabilities
            AutoHealConfiguration config = AutoHealConfiguration.builder()
                .aiConfig(AIConfig.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .model("gpt-4")
                    .build())
                .enableCaching(true)
                .enableVisualAnalysis(true)
                .build();

            PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
            AutoHealLocator autoHeal = new AutoHealLocator(adapter, config);

            page.navigate("https://example.com");

            // Use advanced healing options
            LocatorOptions options = LocatorOptions.builder()
                .enableVisualAnalysis(true)
                .maxRetries(3)
                .timeout(10000)
                .build();

            // AutoHeal will use AI and visual analysis if needed
            WebElement complexElement = autoHeal.findElement(
                "#potentially-broken-selector",
                "Submit form button",
                options
            );

            complexElement.click();

            browser.close();
        }
    }
}
```

## Async Operations Example

```java
import java.util.concurrent.CompletableFuture;
import java.util.List;

public class AsyncPlaywrightExample {

    public void demonstrateAsyncOperations(Page page) {
        AutoHealLocator autoHeal = new AutoHealLocator(new PlaywrightWebAutomationAdapter(page));

        // Find elements asynchronously
        CompletableFuture<WebElement> futureButton = autoHeal.findElementAsync(
            "#async-button",
            "Asynchronously loaded button"
        );

        CompletableFuture<List<WebElement>> futureItems = autoHeal.findElementsAsync(
            ".dynamic-item",
            "Dynamically loaded list items"
        );

        // Combine async operations
        CompletableFuture<Void> combinedOperations = CompletableFuture.allOf(
            futureButton,
            futureItems
        );

        combinedOperations.thenRun(() -> {
            try {
                WebElement button = futureButton.get();
                List<WebElement> items = futureItems.get();

                System.out.println("Found button: " + button.getText());
                System.out.println("Found " + items.size() + " items");

                button.click();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

## Cross-Browser Example

```java
@ParameterizedTest
@ValueSource(strings = {"chromium", "firefox", "webkit"})
void testAcrossBrowsers(String browserType) {
    Browser browser = switch (browserType) {
        case "chromium" -> playwright.chromium().launch();
        case "firefox" -> playwright.firefox().launch();
        case "webkit" -> playwright.webkit().launch();
        default -> throw new IllegalArgumentException("Unknown browser: " + browserType);
    };

    Page page = browser.newPage();
    TodoPage todoPage = new TodoPage(page);

    page.navigate("https://demo.playwright.dev/todomvc");

    // AutoHeal works consistently across all browsers
    todoPage.addTodo("Cross-browser test with " + browserType);
    assertTrue(todoPage.hasTodo("Cross-browser test with " + browserType));

    browser.close();
}
```

## Error Handling Example

```java
public class RobustPlaywrightExample {

    public void demonstrateErrorHandling(Page page) {
        AutoHealLocator autoHeal = new AutoHealLocator(new PlaywrightWebAutomationAdapter(page));

        try {
            // AutoHeal will try multiple strategies to find this element
            WebElement element = autoHeal.findElement(
                "#might-not-exist",
                "Optional element that may not be present"
            );

            element.click();
            System.out.println("Successfully interacted with element");

        } catch (Exception e) {
            System.out.println("AutoHeal couldn't find the element: " + e.getMessage());

            // Fallback strategy
            try {
                WebElement fallback = autoHeal.findElement(
                    ".alternative-selector",
                    "Fallback element with similar purpose"
                );
                fallback.click();
                System.out.println("Used fallback element successfully");
            } catch (Exception fallbackError) {
                System.out.println("No suitable element found: " + fallbackError.getMessage());
            }
        }
    }
}
```

## Integration with TestNG

```java
import org.testng.annotations.*;
import com.microsoft.playwright.*;

public class TestNGPlaywrightExample {
    private Playwright playwright;
    private Browser browser;
    private ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();
    private ThreadLocal<AutoHealLocator> autoHealThreadLocal = new ThreadLocal<>();

    @BeforeSuite
    void setupSuite() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeMethod
    void setupTest() {
        Page page = browser.newPage();
        pageThreadLocal.set(page);

        AutoHealLocator autoHeal = new AutoHealLocator(new PlaywrightWebAutomationAdapter(page));
        autoHealThreadLocal.set(autoHeal);
    }

    @Test
    void testWithAutoHeal() {
        Page page = pageThreadLocal.get();
        AutoHealLocator autoHeal = autoHealThreadLocal.get();

        page.navigate("https://example.com");

        WebElement element = autoHeal.findElement("#test-element", "Test element");
        element.click();
    }

    @AfterMethod
    void tearDownTest() {
        Page page = pageThreadLocal.get();
        if (page != null) {
            page.close();
            pageThreadLocal.remove();
            autoHealThreadLocal.remove();
        }
    }

    @AfterSuite
    void tearDownSuite() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
```

These examples demonstrate how AutoHeal Locator seamlessly integrates with Playwright to provide robust, self-healing test automation that adapts to changing applications while maintaining the same simple API across different browsers and test frameworks.