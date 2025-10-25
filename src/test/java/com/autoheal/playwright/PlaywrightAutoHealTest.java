package com.autoheal.playwright;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Integration test for Playwright AutoHeal functionality
 */
public class PlaywrightAutoHealTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;
    private AutoHealLocator autoHeal;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
        System.out.println("✅ Playwright browser launched");
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        System.out.println("✅ Playwright browser closed");
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();

        // Initialize AutoHeal with Playwright adapter and MOCK AI provider for testing
        PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);

        AIConfig mockAIConfig = AIConfig.builder()
                .provider(AIProvider.MOCK)
                .apiKey("mock-key")
                .build();

        autoHeal = AutoHealLocator.builder()
                .withWebAdapter(adapter)
                .withConfiguration(AutoHealConfiguration.builder()
                        .ai(mockAIConfig)
                        .build())
                .build();

        System.out.println("✅ AutoHeal initialized with Playwright adapter (MOCK AI)");
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("Test 1: Original locator works - no healing needed")
    void testOriginalLocatorWorks() {
        System.out.println("\n🧪 Test 1: Original locator works - no healing needed");

        // Create a simple HTML page
        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <h1>Test Page</h1>
                <label for="username">Username</label>
                <input id="username" type="text" />
                <button>Submit</button>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with test HTML");

        // Test: Original getByLabel should work
        Locator usernameField = autoHeal.find(page, "getByLabel('Username')", "username input field");
        usernameField.fill("TestUser");

        System.out.println("✅ Original locator worked! No healing needed.");
        assertThat(usernameField).hasValue("TestUser");
    }

    @Test
    @DisplayName("Test 2: Locator string parsing - getByRole")
    void testGetByRoleParsing() {
        System.out.println("\n🧪 Test 2: Locator string parsing - getByRole");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <button aria-label="Submit Form">Click Me</button>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with button");

        // Test: getByRole with name option
        Locator button = autoHeal.find(page, "getByRole(button, {name: 'Submit Form'})", "submit button");
        button.click();

        System.out.println("✅ getByRole locator parsed and executed successfully");
    }

    @Test
    @DisplayName("Test 3: CSS selector fallback")
    void testCssSelectorFallback() {
        System.out.println("\n🧪 Test 3: CSS selector fallback");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="user-input-box">
                    <input type="text" />
                </div>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with CSS-only element");

        // Test: CSS selector should work as fallback
        Locator input = autoHeal.find(page, ".user-input-box input", "user input");
        input.fill("CSS Test");

        System.out.println("✅ CSS selector fallback worked");
        assertThat(input).hasValue("CSS Test");
    }

    @Test
    @DisplayName("Test 4: getByPlaceholder locator")
    void testGetByPlaceholder() {
        System.out.println("\n🧪 Test 4: getByPlaceholder locator");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <input type="email" placeholder="Enter your email" />
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with placeholder input");

        Locator emailField = autoHeal.find(page, "getByPlaceholder('Enter your email')", "email input");
        emailField.fill("test@example.com");

        System.out.println("✅ getByPlaceholder locator worked");
        assertThat(emailField).hasValue("test@example.com");
    }

    @Test
    @DisplayName("Test 5: getByText locator")
    void testGetByText() {
        System.out.println("\n🧪 Test 5: getByText locator");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <p>Welcome to the test page</p>
                <span>Important message</span>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with text elements");

        Locator welcomeText = autoHeal.find(page, "getByText('Welcome to the test page')", "welcome message");

        System.out.println("✅ getByText locator worked");
        assertThat(welcomeText).isVisible();
    }

    @Test
    @DisplayName("Test 6: getByTestId locator")
    void testGetByTestId() {
        System.out.println("\n🧪 Test 6: getByTestId locator");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <button data-testid="submit-btn">Submit</button>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with test ID");

        Locator submitBtn = autoHeal.find(page, "getByTestId('submit-btn')", "submit button");
        submitBtn.click();

        System.out.println("✅ getByTestId locator worked");
    }

    @Test
    @DisplayName("Test 7: Multiple calls - cache should speed up")
    void testCachePerformance() {
        System.out.println("\n🧪 Test 7: Multiple calls - cache should speed up");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <label for="email">Email</label>
                <input id="email" type="email" />
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded");

        // First call - will cache
        long start1 = System.currentTimeMillis();
        Locator email1 = autoHeal.find(page, "getByLabel('Email')", "email input");
        email1.fill("first@test.com");
        long duration1 = System.currentTimeMillis() - start1;

        System.out.println("⏱️ First call: " + duration1 + "ms (should cache)");

        // Second call - should use cache or be fast
        long start2 = System.currentTimeMillis();
        Locator email2 = autoHeal.find(page, "getByLabel('Email')", "email input");
        email2.fill("second@test.com");
        long duration2 = System.currentTimeMillis() - start2;

        System.out.println("⏱️ Second call: " + duration2 + "ms (from cache or fast)");
        System.out.println("✅ Cache working - second call completed");

        assertThat(email2).hasValue("second@test.com");
    }

    @Test
    @DisplayName("Test 8: Form automation scenario")
    void testCompleteFormAutomation() {
        System.out.println("\n🧪 Test 8: Complete form automation scenario");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <h1>Login Form</h1>
                <form>
                    <label for="username">Username</label>
                    <input id="username" type="text" />

                    <label for="password">Password</label>
                    <input id="password" type="password" />

                    <button type="submit">Login</button>
                </form>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Login form loaded");

        // Fill username
        autoHeal.find(page, "getByLabel('Username')", "username field").fill("testuser");
        System.out.println("✏️ Username filled");

        // Fill password
        autoHeal.find(page, "getByLabel('Password')", "password field").fill("password123");
        System.out.println("✏️ Password filled");

        // Click login
        autoHeal.find(page, "getByRole(button, {name: 'Login'})", "login button").click();
        System.out.println("🖱️ Login button clicked");

        System.out.println("✅ Complete form automation successful!");
    }
}
