package com.autoheal.playwright;

import com.autoheal.util.PlaywrightLocatorConverter;
import com.autoheal.util.PlaywrightLocatorParser;
import com.autoheal.model.PlaywrightLocator;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.util.regex.Pattern;

/**
 * Test getByText() conversion between internal format and Java syntax
 */
public class GetByTextConversionTest {

    private static Playwright playwright;
    private static Browser browser;
    private Page page;

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
    void createPage() {
        page = browser.newPage();
    }

    @AfterEach
    void closePage() {
        if (page != null) {
            page.close();
        }
    }

    @Test
    @DisplayName("Test getByText() with exact option - extraction and conversion")
    void testGetByTextExactConversion() {
        System.out.println("\n=== Test 1: getByText() with exact=true ===");

        // Create locator
        Locator exactLocator = page.getByText("Welcome, John", new Page.GetByTextOptions().setExact(true));

        // Extract internal format
        String internal = exactLocator.toString();
        System.out.println("Playwright internal: " + internal);
        // Expected: Locator@internal:text="Welcome, John"s

        // Extract to JavaScript-style
        String jsStyle = PlaywrightLocatorConverter.extractLocatorString(exactLocator);
        System.out.println("JavaScript-style:    " + jsStyle);
        // Expected: getByText('Welcome, John', { exact: true })

        // Parse to PlaywrightLocator object
        PlaywrightLocator pwLocator = PlaywrightLocatorParser.parse(jsStyle);
        System.out.println("Parsed type:         " + pwLocator.getType());
        System.out.println("Parsed value:        " + pwLocator.getValue());
        System.out.println("Parsed exact option: " + pwLocator.getOption("exact"));

        // Convert to Java syntax
        String javaSyntax = pwLocator.toSelectorString();
        System.out.println("Java syntax:         " + javaSyntax);
        // Expected: page.getByText("Welcome, John", new Page.GetByTextOptions().setExact(true))

        Assertions.assertEquals(PlaywrightLocator.Type.GET_BY_TEXT, pwLocator.getType());
        Assertions.assertEquals("Welcome, John", pwLocator.getValue());
        Assertions.assertEquals("true", pwLocator.getOption("exact"));
        Assertions.assertTrue(javaSyntax.contains(".setExact(true)"));

        System.out.println("✓ Exact option conversion works!\n");
    }

    @Test
    @DisplayName("Test getByText() with Pattern - extraction and conversion")
    void testGetByTextPatternConversion() {
        System.out.println("\n=== Test 2: getByText() with Pattern ===");

        // Create locator with Pattern
        Locator patternLocator = page.getByText(
            Pattern.compile("welcome, john$", Pattern.CASE_INSENSITIVE)
        );

        // Extract internal format
        String internal = patternLocator.toString();
        System.out.println("Playwright internal: " + internal);
        // Expected: Locator@internal:text=/welcome, john$/i

        // Extract to JavaScript-style
        String jsStyle = PlaywrightLocatorConverter.extractLocatorString(patternLocator);
        System.out.println("JavaScript-style:    " + jsStyle);
        // Expected: getByText(/welcome, john$/i)

        // Parse to PlaywrightLocator object
        PlaywrightLocator pwLocator = PlaywrightLocatorParser.parse(jsStyle);
        System.out.println("Parsed type:         " + pwLocator.getType());
        System.out.println("Parsed value:        " + pwLocator.getValue());
        System.out.println("Parsed isRegex:      " + pwLocator.getOption("isRegex"));

        // Convert to Java syntax
        String javaSyntax = pwLocator.toSelectorString();
        System.out.println("Java syntax:         " + javaSyntax);
        // Expected: page.getByText(Pattern.compile("welcome, john$", Pattern.CASE_INSENSITIVE))

        Assertions.assertEquals(PlaywrightLocator.Type.GET_BY_TEXT, pwLocator.getType());
        Assertions.assertEquals("/welcome, john$/i", pwLocator.getValue());
        Assertions.assertEquals("true", pwLocator.getOption("isRegex"));
        Assertions.assertTrue(javaSyntax.contains("Pattern.compile"));
        Assertions.assertTrue(javaSyntax.contains("Pattern.CASE_INSENSITIVE"));

        System.out.println("✓ Pattern conversion works!\n");
    }

    @Test
    @DisplayName("Test getByText() without options - extraction and conversion")
    void testGetByTextDefaultConversion() {
        System.out.println("\n=== Test 3: getByText() without options ===");

        // Create locator without options
        Locator defaultLocator = page.getByText("Welcome, John");

        // Extract internal format
        String internal = defaultLocator.toString();
        System.out.println("Playwright internal: " + internal);
        // Expected: Locator@internal:text="Welcome, John"i

        // Extract to JavaScript-style
        String jsStyle = PlaywrightLocatorConverter.extractLocatorString(defaultLocator);
        System.out.println("JavaScript-style:    " + jsStyle);
        // Expected: getByText('Welcome, John')

        // Parse to PlaywrightLocator object
        PlaywrightLocator pwLocator = PlaywrightLocatorParser.parse(jsStyle);
        System.out.println("Parsed type:         " + pwLocator.getType());
        System.out.println("Parsed value:        " + pwLocator.getValue());

        // Convert to Java syntax
        String javaSyntax = pwLocator.toSelectorString();
        System.out.println("Java syntax:         " + javaSyntax);
        // Expected: page.getByText("Welcome, John")

        Assertions.assertEquals(PlaywrightLocator.Type.GET_BY_TEXT, pwLocator.getType());
        Assertions.assertEquals("Welcome, John", pwLocator.getValue());

        System.out.println("✓ Default conversion works!\n");
    }

    @Test
    @DisplayName("Test complete round-trip: Locator -> JS -> Parse -> Java")
    void testCompleteRoundTrip() {
        System.out.println("\n=== Test 4: Complete Round-Trip Test ===\n");

        // Test 1: Exact option
        System.out.println("Round-trip test 1: Exact option");
        Locator exact = page.getByText("Hello", new Page.GetByTextOptions().setExact(true));
        String jsExact = PlaywrightLocatorConverter.extractLocatorString(exact);
        PlaywrightLocator pwExact = PlaywrightLocatorParser.parse(jsExact);
        String javaExact = pwExact.toSelectorString();
        System.out.println("  Final: " + javaExact);
        Assertions.assertTrue(javaExact.contains("setExact(true)"));

        // Test 2: Pattern with CASE_INSENSITIVE
        System.out.println("\nRound-trip test 2: Pattern CASE_INSENSITIVE");
        Locator pattern1 = page.getByText(Pattern.compile("hello", Pattern.CASE_INSENSITIVE));
        String jsPattern1 = PlaywrightLocatorConverter.extractLocatorString(pattern1);
        PlaywrightLocator pwPattern1 = PlaywrightLocatorParser.parse(jsPattern1);
        String javaPattern1 = pwPattern1.toSelectorString();
        System.out.println("  Final: " + javaPattern1);
        Assertions.assertTrue(javaPattern1.contains("Pattern.compile"));
        Assertions.assertTrue(javaPattern1.contains("Pattern.CASE_INSENSITIVE"));

        // Test 3: Pattern with MULTILINE
        System.out.println("\nRound-trip test 3: Pattern MULTILINE");
        Locator pattern2 = page.getByText(Pattern.compile("^hello$", Pattern.MULTILINE));
        String jsPattern2 = PlaywrightLocatorConverter.extractLocatorString(pattern2);
        PlaywrightLocator pwPattern2 = PlaywrightLocatorParser.parse(jsPattern2);
        String javaPattern2 = pwPattern2.toSelectorString();
        System.out.println("  Final: " + javaPattern2);
        Assertions.assertTrue(javaPattern2.contains("Pattern.compile"));
        Assertions.assertTrue(javaPattern2.contains("Pattern.MULTILINE"));

        // Test 4: Pattern with combined flags
        System.out.println("\nRound-trip test 4: Pattern CASE_INSENSITIVE | DOTALL");
        Locator pattern3 = page.getByText(
            Pattern.compile("hello.*world", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
        );
        String jsPattern3 = PlaywrightLocatorConverter.extractLocatorString(pattern3);
        PlaywrightLocator pwPattern3 = PlaywrightLocatorParser.parse(jsPattern3);
        String javaPattern3 = pwPattern3.toSelectorString();
        System.out.println("  Final: " + javaPattern3);
        Assertions.assertTrue(javaPattern3.contains("Pattern.compile"));
        Assertions.assertTrue(javaPattern3.contains("Pattern.CASE_INSENSITIVE"));
        Assertions.assertTrue(javaPattern3.contains("Pattern.DOTALL"));

        System.out.println("\n✓ All round-trip tests passed!\n");
    }
}
