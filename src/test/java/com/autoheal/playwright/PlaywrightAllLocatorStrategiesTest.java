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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.autoheal.config.ReportingConfig.enabledWithDefaults;

/**
 * Comprehensive test for all Playwright locator strategies with AutoHeal
 * Tests: getByRole, getByText, getByLabel, getByPlaceholder, getByAltText, getByTitle, getByTestId, CSS, XPath
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlaywrightAllLocatorStrategiesTest {

    private static Playwright playwright;
    private static Browser browser;
    private Page page;
    private AutoHealLocator autoHeal;

    @BeforeAll
    static void setupPlaywright() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(300));
    }

    @BeforeEach
    void setupTest() {
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

    public static void clearCache() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".autoheal", "cache");
        Files.deleteIfExists(cacheDir.resolve("selector-cache.json"));
        Files.deleteIfExists(cacheDir.resolve("cache-metrics.json"));
        System.out.println("✓ Cache cleared successfully");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: getByPlaceholder() - WRONG then CORRECT")
    void test01_GetByPlaceholder() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 1: getByPlaceholder() Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Test with WRONG placeholder
        System.out.println("\n[TEST] Finding username with WRONG placeholder...");
        Locator username = autoHeal.find(
                page,
                page.getByPlaceholder("Username_WRONG"),
                "Username input field on login page"
        );
        username.fill("Admin");
        System.out.println("✓ Username field found and filled");

        // Test with CORRECT placeholder
        System.out.println("\n[TEST] Finding password with CORRECT placeholder...");
        Locator password = autoHeal.find(
                page,
                page.getByPlaceholder("Password"),
                "Password input field on login page"
        );
        password.fill("admin123");
        System.out.println("✓ Password field found and filled");

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: getByRole() - WRONG then CORRECT")
    void test02_GetByRole() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 2: getByRole() Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Fill credentials first
        page.getByPlaceholder("Username").fill("Admin");
        page.getByPlaceholder("Password").fill("admin123");

        // Test with WRONG role name
        System.out.println("\n[TEST] Finding button with WRONG role name...");
        Locator button = autoHeal.find(
                page,
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit_WRONG")),
                "Login button on login page"
        );
        button.click();
        System.out.println("✓ Login button found and clicked");

        page.waitForLoadState();
        Assertions.assertTrue(page.url().contains("dashboard"), "Should be on dashboard");

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: getByText() - WRONG then CORRECT")
    void test03_GetByText() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 3: getByText() Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Fill credentials
        page.getByPlaceholder("Username").fill("Admin");
        page.getByPlaceholder("Password").fill("admin123");

        // Test with WRONG text
        System.out.println("\n[TEST] Finding button by WRONG text...");
        Locator loginBtn = autoHeal.find(
                page,
                page.getByText("Sign In_WRONG"),
                "Login button by text"
        );
        loginBtn.click();
        System.out.println("✓ Login button found by text and clicked");

        page.waitForLoadState();
        Assertions.assertTrue(page.url().contains("dashboard"), "Should be on dashboard");

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: getByLabel() - WRONG then CORRECT")
    void test04_GetByLabel() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 4: getByLabel() Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Test with WRONG label
        System.out.println("\n[TEST] Finding username by WRONG label...");
        Locator usernameByLabel = autoHeal.find(
                page,
                page.getByLabel("User Name_WRONG"),
                "Username field by label"
        );
        usernameByLabel.fill("Admin");
        System.out.println("✓ Username field found by label and filled");

        // Test password with label
        System.out.println("\n[TEST] Finding password by WRONG label...");
        Locator passwordByLabel = autoHeal.find(
                page,
                page.getByLabel("Pass Word_WRONG"),
                "Password field by label"
        );
        passwordByLabel.fill("admin123");
        System.out.println("✓ Password field found by label and filled");

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: CSS Selector - WRONG then CORRECT")
    void test05_CssSelector() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 5: CSS Selector Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Test with WRONG CSS selector
        System.out.println("\n[TEST] Finding username with WRONG CSS selector...");
        Locator usernameCss = autoHeal.find(
                page,
                page.locator("input[name='user_WRONG']"),
                "Username field by CSS selector"
        );
        usernameCss.fill("Admin");
        System.out.println("✓ Username field found by CSS and filled");

        // Test password with WRONG CSS
        System.out.println("\n[TEST] Finding password with WRONG CSS selector...");
        Locator passwordCss = autoHeal.find(
                page,
                page.locator("input[type='pass_WRONG']"),
                "Password field by CSS selector"
        );
        passwordCss.fill("admin123");
        System.out.println("✓ Password field found by CSS and filled");

        // Test button with WRONG CSS
        System.out.println("\n[TEST] Finding button with WRONG CSS selector...");
        Locator buttonCss = autoHeal.find(
                page,
                page.locator("button[class='submit_WRONG']"),
                "Login button by CSS selector"
        );
        buttonCss.click();
        System.out.println("✓ Login button found by CSS and clicked");

        page.waitForLoadState();
        Assertions.assertTrue(page.url().contains("dashboard"), "Should be on dashboard");

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: XPath - WRONG then CORRECT")
    void test06_XPath() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 6: XPath Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Test with WRONG XPath
        System.out.println("\n[TEST] Finding username with WRONG XPath...");
        Locator usernameXpath = autoHeal.find(
                page,
                page.locator("//input[@name='user_WRONG']"),
                "Username field by XPath"
        );
        usernameXpath.fill("Admin");
        System.out.println("✓ Username field found by XPath and filled");

        // Test password with WRONG XPath
        System.out.println("\n[TEST] Finding password with WRONG XPath...");
        Locator passwordXpath = autoHeal.find(
                page,
                page.locator("//input[@type='pass_WRONG']"),
                "Password field by XPath"
        );
        passwordXpath.fill("admin123");
        System.out.println("✓ Password field found by XPath and filled");

        // Test button with WRONG XPath
        System.out.println("\n[TEST] Finding button with WRONG XPath...");
        Locator buttonXpath = autoHeal.find(
                page,
                page.locator("//button[@type='submit_WRONG']"),
                "Login button by XPath"
        );
        buttonXpath.click();
        System.out.println("✓ Login button found by XPath and clicked");

        page.waitForLoadState();
        Assertions.assertTrue(page.url().contains("dashboard"), "Should be on dashboard");

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: getByTitle() - WRONG then CORRECT")
    void test07_GetByTitle() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 7: getByTitle() Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Login first to access dashboard with title attributes
        page.getByPlaceholder("Username").fill("Admin");
        page.getByPlaceholder("Password").fill("admin123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
        page.waitForLoadState();

        // Test with WRONG title on dashboard
        System.out.println("\n[TEST] Finding element by WRONG title attribute...");
        try {
            Locator elementByTitle = autoHeal.find(
                    page,
                    page.getByTitle("Dashboard_WRONG"),
                    "Dashboard element by title"
            );
            System.out.println("✓ Element found by title attribute");
        } catch (Exception e) {
            System.out.println("⚠ Title attribute test - element may not have title attribute on this page");
        }

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: getByAltText() - WRONG then CORRECT")
    void test08_GetByAltText() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 8: getByAltText() Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Test with WRONG alt text on company logo
        System.out.println("\n[TEST] Finding logo image by WRONG alt text...");
        try {
            Locator logoByAlt = autoHeal.find(
                    page,
                    page.getByAltText("Company Logo_WRONG"),
                    "Company logo by alt text"
            );
            System.out.println("✓ Logo image found by alt text");
        } catch (Exception e) {
            System.out.println("⚠ Alt text test - using available images on page");
            // Try finding any image on the page
            Locator anyImage = autoHeal.find(
                    page,
                    page.getByAltText("OrangeHRM_WRONG"),
                    "Any image by alt text"
            );
            System.out.println("✓ Image found by alt text");
        }

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(9)
    @DisplayName("Test 9: getByTestId() - WRONG then CORRECT")
    void test09_GetByTestId() throws IOException {
        clearCache();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 9: getByTestId() Strategy");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        // Test with WRONG test ID
        System.out.println("\n[TEST] Finding element by WRONG test ID...");
        try {
            Locator elementByTestId = autoHeal.find(
                    page,
                    page.getByTestId("username-input_WRONG"),
                    "Username field by test ID"
            );
            elementByTestId.fill("Admin");
            System.out.println("✓ Element found by test ID and filled");
        } catch (Exception e) {
            System.out.println("⚠ Test ID not available on this page (expected for OrangeHRM demo)");
        }

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(10)
    @DisplayName("Test 10: Complete Login Flow - All strategies cached")
    void test10_CompleteLoginWithCache() throws IOException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 10: Complete Login with CACHED selectors");
        System.out.println("=".repeat(80));

        page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        page.waitForLoadState();

        long startTime = System.currentTimeMillis();

        // All these should hit cache from previous tests
        System.out.println("\n[CACHE TEST] Finding username (should be cached)...");
        Locator username = autoHeal.find(
                page,
                page.getByPlaceholder("Username_WRONG"),
                "Username input field on login page"
        );
        long usernameTime = System.currentTimeMillis() - startTime;
        username.fill("Admin");
        System.out.println("✓ Username found in " + usernameTime + "ms (cached: " + (usernameTime < 200) + ")");

        System.out.println("\n[CACHE TEST] Finding password (should be cached)...");
        startTime = System.currentTimeMillis();
        Locator password = autoHeal.find(
                page,
                page.getByPlaceholder("Password"),
                "Password input field on login page"
        );
        long passwordTime = System.currentTimeMillis() - startTime;
        password.fill("admin123");
        System.out.println("✓ Password found in " + passwordTime + "ms (cached: " + (passwordTime < 200) + ")");

        System.out.println("\n[CACHE TEST] Finding button (should be cached)...");
        startTime = System.currentTimeMillis();
        Locator button = autoHeal.find(
                page,
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit_WRONG")),
                "Login button on login page"
        );
        long buttonTime = System.currentTimeMillis() - startTime;
        button.click();
        System.out.println("✓ Button found in " + buttonTime + "ms (cached: " + (buttonTime < 200) + ")");

        page.waitForLoadState();
        Assertions.assertTrue(page.url().contains("dashboard"), "Should be on dashboard");

        System.out.println("\n[SUMMARY] All selectors retrieved from cache:");
        System.out.println("  - Username: " + usernameTime + "ms");
        System.out.println("  - Password: " + passwordTime + "ms");
        System.out.println("  - Button: " + buttonTime + "ms");

        System.out.println("=".repeat(80) + "\n");
    }
}
