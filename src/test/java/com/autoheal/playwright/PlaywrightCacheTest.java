package com.autoheal.playwright;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import static com.autoheal.config.ReportingConfig.enabledWithDefaults;

/**
 * Test to verify Playwright caching works correctly
 */
public class PlaywrightCacheTest {

    private static Playwright playwright;
    private static Browser browser;
    private Page page;
    private AutoHealLocator autoHeal;

    @BeforeAll
    static void setupPlaywright() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(500));
    }

    @BeforeEach
    void setupTest() {
        page = browser.newPage();
        page.setDefaultTimeout(30000);

        // Clear cache before each test
        try {
            Files.deleteIfExists(Paths.get(System.getProperty("user.home") + "/.autoheal/cache/selector-cache.json"));
            Files.deleteIfExists(Paths.get(System.getProperty("user.home") + "/.autoheal/cache/cache-metrics.json"));
        } catch (Exception e) {
            // Ignore
        }

        // Create AutoHeal with persistent file cache
        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .ai(AIConfig.builder()
                        .provider(AIProvider.GOOGLE_GEMINI)
                        .apiKey("AIzaSyAEy9KEzpk6rxj-S1zRwKGBMDzfaHCpfmE")
                        .build())
                .cache(CacheConfig.builder()
                        .cacheType(CacheConfig.CacheType.PERSISTENT_FILE)
                        .build())
                .reporting(enabledWithDefaults())
                .build();

        autoHeal = AutoHealLocator.builder()
                .withWebAdapter(new PlaywrightWebAutomationAdapter(page))
                .withConfiguration(config)
                .build();
    }

    @AfterEach
    void teardownTest() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
        if (page != null) {
            page.close();
        }
    }

    @AfterAll
    static void teardownPlaywright() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @DisplayName("Test 1: First run - AI healing should occur")
    void testFirstRun_AiHealing() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 1: First Run - AI Healing Expected");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Test 1: Wrong username locator (should trigger AI healing)
        System.out.println("\n[TEST] Finding username with WRONG locator...");
        Locator username = autoHeal.find(
                page,
                page.getByPlaceholder("Username_WRONG"),
                "Username input field on login page"
        );
        username.fill("Admin");
        System.out.println("[SUCCESS] Username field found and filled");

        // Test 2: Correct password locator (should work immediately)
        System.out.println("\n[TEST] Finding password with CORRECT locator...");
        Locator password = autoHeal.find(
                page,
                page.getByPlaceholder("Password"),
                "Password input field on login page"
        );
        password.fill("admin123");
        System.out.println("[SUCCESS] Password field found and filled");

        // Test 3: Wrong button locator (should trigger AI healing)
        System.out.println("\n[TEST] Finding button with WRONG locator...");
        Locator button = autoHeal.find(
                page,
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit_WRONG")),
                "Login button on login page"
        );
        button.click();
        System.out.println("[SUCCESS] Login button found and clicked");

        page.waitForLoadState();

        Assertions.assertTrue(page.url().contains("dashboard"), "Should be on dashboard");

        System.out.println("\n[RESULT] First run completed - Check report for DOM_ANALYSIS");
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @DisplayName("Test 2: Second run - Cache should be used")
    void testSecondRun_CacheHit() {
        // Run first to populate cache
        testFirstRun_AiHealing();

        // Reset page and AutoHeal
        page.close();
        autoHeal.shutdown();

        page = browser.newPage();
        page.setDefaultTimeout(30000);

        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .ai(AIConfig.builder()
                        .provider(AIProvider.GOOGLE_GEMINI)
                        .apiKey("AIzaSyAEy9KEzpk6rxj-S1zRwKGBMDzfaHCpfmE")
                        .build())
                .cache(CacheConfig.builder()
                        .cacheType(CacheConfig.CacheType.PERSISTENT_FILE)
                        .build())
                .reporting(enabledWithDefaults())
                .build();

        autoHeal = AutoHealLocator.builder()
                .withWebAdapter(new PlaywrightWebAutomationAdapter(page))
                .withConfiguration(config)
                .build();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 2: Second Run - Cache Hit Expected");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        long startTime = System.currentTimeMillis();

        // Test 1: Username (should use cache)
        System.out.println("\n[TEST] Finding username - CACHE expected...");
        Locator username = autoHeal.find(
                page,
                page.getByPlaceholder("Username_WRONG"),
                "Username input field on login page"
        );
        long usernameTime = System.currentTimeMillis() - startTime;
        username.fill("Admin");
        System.out.println("[SUCCESS] Username found in " + usernameTime + "ms (should be <100ms if cached)");

        // Test 2: Password (should work immediately)
        System.out.println("\n[TEST] Finding password...");
        Locator password = autoHeal.find(
                page,
                page.getByPlaceholder("Password"),
                "Password input field on login page"
        );
        password.fill("admin123");
        System.out.println("[SUCCESS] Password found");

        // Test 3: Button (should use cache)
        System.out.println("\n[TEST] Finding button - CACHE expected...");
        startTime = System.currentTimeMillis();
        Locator button = autoHeal.find(
                page,
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit_WRONG")),
                "Login button on login page"
        );
        long buttonTime = System.currentTimeMillis() - startTime;
        button.click();
        System.out.println("[SUCCESS] Button found in " + buttonTime + "ms (should be <100ms if cached)");

        page.waitForLoadState();

        Assertions.assertTrue(page.url().contains("dashboard"), "Should be on dashboard");

        System.out.println("\n[RESULT] Second run completed");
        System.out.println("Username time: " + usernameTime + "ms");
        System.out.println("Button time: " + buttonTime + "ms");
        System.out.println("Check report - both should show strategy: CACHED");
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @DisplayName("Test 3: Verify cache key generation")
    void testCacheKeyGeneration() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 3: Cache Key Generation");
        System.out.println("=".repeat(80));

        // Test extracting from native locators
        Locator usernameLocator = page.locator("input[name='username']"); // Dummy, won't actually use
        Locator buttonLocator = page.locator("button"); // Dummy

        String extracted1 = com.autoheal.util.PlaywrightLocatorConverter.extractLocatorString(
                page.getByPlaceholder("Username_WRONG"));
        System.out.println("\nExtracted from getByPlaceholder: " + extracted1);

        String extracted2 = com.autoheal.util.PlaywrightLocatorConverter.extractLocatorString(
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit_WRONG")));
        System.out.println("Extracted from getByRole: " + extracted2);

        // Generate cache keys
        String key1 = com.autoheal.util.PlaywrightLocatorParser.generateCacheKey(
                extracted1, "Username input field on login page");
        System.out.println("\nCache key 1: " + key1);

        String key2 = com.autoheal.util.PlaywrightLocatorParser.generateCacheKey(
                extracted2, "Login button on login page");
        System.out.println("Cache key 2: " + key2);

        // Parse and convert to Java syntax
        com.autoheal.model.PlaywrightLocator parsed1 =
                com.autoheal.util.PlaywrightLocatorParser.parse(extracted1);
        System.out.println("\nJava syntax 1: " + parsed1.toSelectorString());

        com.autoheal.model.PlaywrightLocator parsed2 =
                com.autoheal.util.PlaywrightLocatorParser.parse(extracted2);
        System.out.println("Java syntax 2: " + parsed2.toSelectorString());

        System.out.println("\n=".repeat(80) + "\n");

        Assertions.assertFalse(extracted1.contains("page.locator"),
                "Extracted selector should not have page.locator wrapper");
        Assertions.assertFalse(extracted2.contains("page.locator"),
                "Extracted selector should not have page.locator wrapper");
    }
}
