package com.autoheal.playwright;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.util.regex.Pattern;

import static com.autoheal.config.ReportingConfig.enabledWithDefaults;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test getByText() with various options:
 * 1. Default substring matching
 * 2. Exact matching with .setExact(true)
 * 3. Pattern-based matching with Pattern.compile()
 */
public class GetByTextOptionsTest {

    private static Playwright playwright;
    private static Browser browser;
    private Page page;
    private AutoHealLocator autoHeal;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
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
    void closeContext() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
        if (page != null) {
            page.close();
        }
    }

    @Test
    @DisplayName("Test getByText() with default substring matching")
    void testGetByTextDefaultSubstring() {
        // Create a simple HTML page
        page.setContent(
            "<html><body>" +
            "<div>Welcome, John Doe</div>" +
            "<div>Welcome, Jane Smith</div>" +
            "<button>Submit</button>" +
            "</body></html>"
        );

        // Default getByText() does substring matching (case-insensitive)
        Locator welcomeLocator = autoHeal.find(
            page,
            page.getByText("Welcome"),
            "Welcome text (substring match)"
        );

        assertThat(welcomeLocator).isVisible();
        System.out.println("✓ Default substring matching works");
    }

    @Test
    @DisplayName("Test getByText() with exact matching using .setExact(true)")
    void testGetByTextExactMatch() {
        // Create a simple HTML page
        page.setContent(
            "<html><body>" +
            "<div>Welcome</div>" +
            "<div>Welcome, John</div>" +
            "<button>Submit</button>" +
            "</body></html>"
        );

        // Exact match: should find "Welcome" but not "Welcome, John"
        Locator exactLocator = autoHeal.find(
            page,
            page.getByText("Welcome", new Page.GetByTextOptions().setExact(true)),
            "Exact 'Welcome' text"
        );

        assertThat(exactLocator).isVisible();

        // Verify there's only one exact match
        Assertions.assertEquals(1, exactLocator.count(),
            "Should find exactly one 'Welcome' element");

        System.out.println("✓ Exact matching with .setExact(true) works");
    }

    @Test
    @DisplayName("Test getByText() with Pattern for case-insensitive matching")
    void testGetByTextPatternCaseInsensitive() {
        // Create a simple HTML page
        page.setContent(
            "<html><body>" +
            "<div>welcome, john</div>" +
            "<div>WELCOME, JANE</div>" +
            "<div>Welcome, Bob</div>" +
            "<button>Submit</button>" +
            "</body></html>"
        );

        // Pattern matching: case-insensitive regex
        Locator patternLocator = autoHeal.find(
            page,
            page.getByText(
                Pattern.compile("welcome, (john|jane)", Pattern.CASE_INSENSITIVE)
            ),
            "Welcome text with Pattern (case-insensitive)"
        );

        assertThat(patternLocator.first()).isVisible();

        // Verify it matches multiple elements
        int matchCount = patternLocator.count();
        Assertions.assertTrue(matchCount >= 2,
            "Should find at least 2 elements matching the pattern, found: " + matchCount);

        System.out.println("✓ Pattern matching with CASE_INSENSITIVE works");
    }

    @Test
    @DisplayName("Test getByText() with Pattern for end-of-string matching")
    void testGetByTextPatternEndAnchor() {
        // Create a simple HTML page
        page.setContent(
            "<html><body>" +
            "<div>Hello, John</div>" +
            "<div>Hello, Jane</div>" +
            "<div>Goodbye, John</div>" +
            "</body></html>"
        );

        // Pattern matching: match strings ending with "John"
        Locator endAnchorLocator = autoHeal.find(
            page,
            page.getByText(
                Pattern.compile("john$", Pattern.CASE_INSENSITIVE)
            ),
            "Text ending with 'john' (case-insensitive)"
        );

        assertThat(endAnchorLocator.first()).isVisible();

        // Verify it matches 2 elements (both ending with "John")
        int matchCount = endAnchorLocator.count();
        Assertions.assertEquals(2, matchCount,
            "Should find exactly 2 elements ending with 'john', found: " + matchCount);

        System.out.println("✓ Pattern matching with end anchor ($) works");
    }

    @Test
    @DisplayName("Test all getByText() variants in one comprehensive test")
    void testAllGetByTextVariants() {
        System.out.println("\n=== Testing All getByText() Variants ===\n");

        // Create a comprehensive HTML page
        page.setContent(
            "<html><body>" +
            "<h1>Test Page</h1>" +
            "<div id='exact'>Welcome</div>" +
            "<div id='substring'>Welcome, John</div>" +
            "<div id='pattern'>WELCOME, JANE</div>" +
            "<div id='regex'>Hello, World!</div>" +
            "</body></html>"
        );

        // Test 1: Default substring matching
        System.out.println("Test 1: Default substring matching");
        Locator substringLoc = autoHeal.find(
            page,
            page.getByText("Welcome"),
            "Substring match 'Welcome'"
        );
        assertThat(substringLoc.first()).isVisible();
        System.out.println("  ✓ Found " + substringLoc.count() + " elements");

        // Test 2: Exact matching
        System.out.println("\nTest 2: Exact matching with .setExact(true)");
        Locator exactLoc = autoHeal.find(
            page,
            page.getByText("Welcome", new Page.GetByTextOptions().setExact(true)),
            "Exact match 'Welcome'"
        );
        assertThat(exactLoc).isVisible();
        Assertions.assertEquals(1, exactLoc.count());
        System.out.println("  ✓ Found exactly 1 element");

        // Test 3: Case-insensitive Pattern
        System.out.println("\nTest 3: Pattern with CASE_INSENSITIVE");
        Locator patternLoc = autoHeal.find(
            page,
            page.getByText(
                Pattern.compile("welcome", Pattern.CASE_INSENSITIVE)
            ),
            "Pattern 'welcome' (case-insensitive)"
        );
        assertThat(patternLoc.first()).isVisible();
        System.out.println("  ✓ Found " + patternLoc.count() + " elements");

        // Test 4: Complex regex pattern
        System.out.println("\nTest 4: Complex regex pattern with end anchor");
        Locator regexLoc = autoHeal.find(
            page,
            page.getByText(
                Pattern.compile("world!$", Pattern.CASE_INSENSITIVE)
            ),
            "Regex 'world!$' (case-insensitive)"
        );
        assertThat(regexLoc).isVisible();
        System.out.println("  ✓ Found " + regexLoc.count() + " element(s)");

        System.out.println("\n=== All getByText() variants work correctly! ===\n");
    }
}
