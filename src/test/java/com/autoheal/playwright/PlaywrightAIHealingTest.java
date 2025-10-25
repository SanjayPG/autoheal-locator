package com.autoheal.playwright;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.*;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.impl.ai.ResilientAIService;
import com.autoheal.model.AIProvider;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Playwright AutoHeal with real AI healing using Gemini
 */
public class PlaywrightAIHealingTest {

    private static Playwright playwright;
    private static Browser browser;
    private Page page;
    private AutoHealLocator autoHeal;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        System.out.println("🚀 Playwright browser launched for AI healing test");
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        System.out.println("🔒 Playwright browser closed");
    }

    @BeforeEach
    void setup() throws Exception {
        page = browser.newPage();

        // Create Gemini AI config (matching working Selenium test)
        AIConfig aiConfig = AIConfig.builder()
                .provider(AIProvider.GOOGLE_GEMINI)
                .model("gemini-2.0-flash")  // Use same model as working Selenium test
                .apiKey("AIzaSyDjFQ7M6pKRsYyr1SmZugaxs7mGiOh57w8")
                .timeout(java.time.Duration.ofSeconds(30))
                .maxRetries(2)
                .maxTokensDOM(1000)
                .temperatureDOM(0.1)
                .visualAnalysisEnabled(false)
                .build();

        ResilienceConfig resilienceConfig = ResilienceConfig.builder().build();

        // Create real AI service with Gemini
        ResilientAIService aiService = new ResilientAIService(aiConfig, resilienceConfig);

        // Create AutoHeal configuration with AI config
        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .ai(aiConfig)
                .resilience(resilienceConfig)
                .build();

        // Create cache
        com.autoheal.impl.cache.CaffeineBasedSelectorCache cache =
                new com.autoheal.impl.cache.CaffeineBasedSelectorCache(
                        com.autoheal.config.CacheConfig.defaultConfig());

        // Create adapter and AutoHeal instance using builder
        PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
        autoHeal = AutoHealLocator.builder()
                .withWebAdapter(adapter)
                .withConfiguration(config)
                .withCache(cache)
                .withAIService(aiService)
                .build();

        System.out.println("🤖 AutoHeal initialized with Gemini AI");
    }

    @AfterEach
    void tearDown() {
        if (page != null) {
            page.close();
        }
    }

    @Test
    @DisplayName("Test AI healing with wrong selector - Gemini should fix it")
    void testAIHealingWithWrongSelector() {
        System.out.println("\n🧪 Test: AI Healing with Wrong Selector");

        // Create a simple form page
        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Test Page</title></head>
            <body>
                <h1>Login Form</h1>
                <form>
                    <label for="user">Username</label>
                    <input id="user" name="username" type="text" placeholder="Enter username" />

                    <label for="pass">Password</label>
                    <input id="pass" name="password" type="password" placeholder="Enter password" />

                    <button type="submit" role="button" aria-label="Submit Form">Login</button>
                </form>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with login form");

        // Test 1: Use WRONG selector - should trigger AI healing
        System.out.println("\n🔴 Using WRONG selector: getByTestId('username-input')");
        System.out.println("   (This selector doesn't exist - AI should find the correct one)");

        long startTime = System.currentTimeMillis();

        // This will fail with original selector, then AI should heal it
        com.microsoft.playwright.Locator usernameInput = autoHeal.find(
                page,
                "getByTestId('username-input')",  // WRONG - this doesn't exist
                "Username input field"             // Description for AI
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(usernameInput, "AI should have healed the locator");
        assertTrue(usernameInput.count() > 0, "Healed locator should find the element");

        // Try to interact with the healed element
        usernameInput.fill("testuser");
        assertEquals("testuser", usernameInput.inputValue());

        System.out.println("✅ AI healing successful! Found element in " + duration + "ms");
        System.out.println("   Healed locator found username input and filled it successfully");

        // Test 2: Use another WRONG selector for password
        System.out.println("\n🔴 Using WRONG selector: getByLabel('PassField')");
        System.out.println("   (Label text is 'Password', not 'PassField' - AI should fix)");

        startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator passwordInput = autoHeal.find(
                page,
                "getByLabel('PassField')",  // WRONG - actual label is "Password"
                "Password input field"
        );

        duration = System.currentTimeMillis() - startTime;

        assertNotNull(passwordInput, "AI should have healed the password locator");
        assertTrue(passwordInput.count() > 0, "Healed password locator should find the element");

        passwordInput.fill("secret123");
        assertEquals("secret123", passwordInput.inputValue());

        System.out.println("✅ AI healing successful! Found password in " + duration + "ms");

        // Test 3: Wrong button selector
        System.out.println("\n🔴 Using WRONG selector: getByRole(button, {name: 'Submit'})");
        System.out.println("   (Button name is 'Submit Form', not 'Submit' - AI should fix)");

        startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator submitButton = autoHeal.find(
                page,
                "getByRole(button, {name: 'Submit'})",  // WRONG - actual name is "Submit Form"
                "Submit button"
        );

        duration = System.currentTimeMillis() - startTime;

        assertNotNull(submitButton, "AI should have healed the button locator");
        assertTrue(submitButton.count() > 0, "Healed button locator should find the element");

        System.out.println("✅ AI healing successful! Found button in " + duration + "ms");
        System.out.println("\n🎉 All AI healing tests passed with Gemini!");
    }

    @Test
    @DisplayName("Test AI healing with completely wrong CSS selector")
    void testAIHealingWithWrongCSSSelector() {
        System.out.println("\n🧪 Test: AI Healing with Wrong CSS Selector");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="container">
                    <input type="email" placeholder="Enter your email" class="email-input" />
                    <button type="button" class="subscribe-btn">Subscribe</button>
                </div>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with subscription form");

        // Use completely wrong CSS selector
        System.out.println("\n🔴 Using WRONG CSS selector: #email-field");
        System.out.println("   (Element has class 'email-input', not id 'email-field')");

        long startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator emailInput = autoHeal.find(
                page,
                "#email-field",  // WRONG CSS selector
                "Email input field"
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(emailInput, "AI should have healed the CSS selector");
        assertTrue(emailInput.count() > 0, "Healed CSS selector should find the element");

        emailInput.fill("test@example.com");
        assertEquals("test@example.com", emailInput.inputValue());

        System.out.println("✅ AI healing successful! Fixed CSS selector in " + duration + "ms");
        System.out.println("   AI found the correct element using smart analysis");
    }
}
