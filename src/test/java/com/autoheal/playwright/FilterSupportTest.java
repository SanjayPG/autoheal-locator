package com.autoheal.playwright;

import com.autoheal.model.LocatorFilter;
import com.autoheal.model.PlaywrightLocator;
import com.autoheal.util.PlaywrightLocatorConverter;
import com.autoheal.util.PlaywrightLocatorParser;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for filter support in AutoHeal
 * Tests:
 * 1. Backward compatibility - existing basic locators still work
 * 2. Filter extraction from Playwright internal format
 * 3. Filter parsing from JavaScript format
 * 4. Round-trip conversion (internal -> JS -> Java)
 */
public class FilterSupportTest {

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

    /**
     * Test 1: Verify existing basic locators still work (backward compatibility)
     */
    @Test
    @DisplayName("Test backward compatibility - basic locators without filters")
    public void testBackwardCompatibility_BasicLocators() {
        System.out.println("\n=== Test 1: Backward Compatibility - Basic Locators ===");

        // Test getByRole without filters
        Locator roleLocator = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit"));
        String roleJs = PlaywrightLocatorConverter.extractLocatorString(roleLocator);
        PlaywrightLocator roleLoc = PlaywrightLocatorParser.parse(roleJs);
        String roleJava = roleLoc.toSelectorString();

        System.out.println("getByRole:");
        System.out.println("  JavaScript: " + roleJs);
        System.out.println("  Java: " + roleJava);

        assertFalse(roleLoc.hasFilters(), "Basic getByRole should have no filters");
        assertTrue(roleJava.contains("page.getByRole(AriaRole.BUTTON"), "Should generate correct Java syntax");

        // Test getByText without filters
        Locator textLocator = page.getByText("Welcome");
        String textJs = PlaywrightLocatorConverter.extractLocatorString(textLocator);
        PlaywrightLocator textLoc = PlaywrightLocatorParser.parse(textJs);
        String textJava = textLoc.toSelectorString();

        System.out.println("\ngetByText:");
        System.out.println("  JavaScript: " + textJs);
        System.out.println("  Java: " + textJava);

        assertFalse(textLoc.hasFilters(), "Basic getByText should have no filters");
        assertEquals("page.getByText(\"Welcome\")", textJava);

        // Test getByLabel without filters
        Locator labelLocator = page.getByLabel("Username");
        String labelJs = PlaywrightLocatorConverter.extractLocatorString(labelLocator);
        PlaywrightLocator labelLoc = PlaywrightLocatorParser.parse(labelJs);

        assertFalse(labelLoc.hasFilters(), "Basic getByLabel should have no filters");

        System.out.println("\n✅ All basic locators work correctly (backward compatible)");
    }

    /**
     * Test 2: Single hasText filter
     */
    @Test
    @DisplayName("Test single hasText filter extraction and conversion")
    public void testSingleHasTextFilter() {
        System.out.println("\n=== Test 2: Single hasText Filter ===");

        // Create filtered locator
        Locator filteredLocator = page.getByRole(AriaRole.LISTITEM)
                .filter(new Locator.FilterOptions().setHasText("Product 2"));

        // Check internal format
        String internal = filteredLocator.toString();
        System.out.println("Playwright internal format: " + internal);
        assertTrue(internal.contains(">>"), "Should contain >> operator");
        assertTrue(internal.contains("has-text"), "Should contain has-text filter");

        // Convert to JavaScript format
        String js = PlaywrightLocatorConverter.extractLocatorString(filteredLocator);
        System.out.println("JavaScript format: " + js);

        assertTrue(js.contains("getByRole('listitem')"));
        assertTrue(js.contains(".filter({ hasText: 'Product 2' })"));

        // Parse back to PlaywrightLocator
        PlaywrightLocator locator = PlaywrightLocatorParser.parse(js);

        // Verify structure
        assertEquals(PlaywrightLocator.Type.GET_BY_ROLE, locator.getType());
        assertEquals("listitem", locator.getValue());
        assertTrue(locator.hasFilters(), "Should have filters");

        List<LocatorFilter> filters = locator.getFilters();
        assertEquals(1, filters.size(), "Should have 1 filter");

        LocatorFilter filter = filters.get(0);
        assertEquals(LocatorFilter.FilterType.HAS_TEXT, filter.getType());
        assertEquals("Product 2", filter.getValue());
        assertFalse(filter.isRegex(), "Should not be regex");

        // Convert to Java syntax
        String javaSyntax = locator.toSelectorString();
        System.out.println("Java format: " + javaSyntax);

        assertTrue(javaSyntax.contains("page.getByRole(AriaRole.LISTITEM)"));
        assertTrue(javaSyntax.contains(".filter(new Locator.FilterOptions().setHasText(\"Product 2\"))"));

        System.out.println("✅ Single hasText filter works correctly");
    }

    /**
     * Test 3: hasText filter with regex pattern
     */
    @Test
    @DisplayName("Test hasText filter with regex pattern")
    public void testHasTextFilterWithRegex() {
        System.out.println("\n=== Test 3: hasText Filter with Regex ===");

        // Create filtered locator with regex
        Locator filteredLocator = page.getByRole(AriaRole.LISTITEM)
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("product 2", Pattern.CASE_INSENSITIVE)));

        String internal = filteredLocator.toString();
        System.out.println("Playwright internal format: " + internal);

        String js = PlaywrightLocatorConverter.extractLocatorString(filteredLocator);
        System.out.println("JavaScript format: " + js);

        assertTrue(js.contains("getByRole('listitem')"));
        assertTrue(js.contains(".filter({ hasText: /product 2/i })"));

        PlaywrightLocator locator = PlaywrightLocatorParser.parse(js);

        assertTrue(locator.hasFilters());
        List<LocatorFilter> filters = locator.getFilters();
        assertEquals(1, filters.size());

        LocatorFilter filter = filters.get(0);
        assertEquals(LocatorFilter.FilterType.HAS_TEXT, filter.getType());
        assertEquals("/product 2/i", filter.getValue());
        assertTrue(filter.isRegex(), "Should be regex");

        String javaSyntax = locator.toSelectorString();
        System.out.println("Java format: " + javaSyntax);

        assertTrue(javaSyntax.contains("Pattern.compile(\"product 2\", Pattern.CASE_INSENSITIVE)"));

        System.out.println("✅ hasText filter with regex works correctly");
    }

    /**
     * Test 4: hasNotText filter
     */
    @Test
    @DisplayName("Test hasNotText filter")
    public void testHasNotTextFilter() {
        System.out.println("\n=== Test 4: hasNotText Filter ===");

        Locator filteredLocator = page.getByRole(AriaRole.LISTITEM)
                .filter(new Locator.FilterOptions().setHasNotText("Out of stock"));

        String internal = filteredLocator.toString();
        System.out.println("Playwright internal format: " + internal);

        String js = PlaywrightLocatorConverter.extractLocatorString(filteredLocator);
        System.out.println("JavaScript format: " + js);

        assertTrue(js.contains("getByRole('listitem')"));
        assertTrue(js.contains(".filter({ hasNotText: 'Out of stock' })"));

        PlaywrightLocator locator = PlaywrightLocatorParser.parse(js);

        assertTrue(locator.hasFilters());
        List<LocatorFilter> filters = locator.getFilters();
        assertEquals(1, filters.size());

        LocatorFilter filter = filters.get(0);
        assertEquals(LocatorFilter.FilterType.HAS_NOT_TEXT, filter.getType());
        assertEquals("Out of stock", filter.getValue());

        String javaSyntax = locator.toSelectorString();
        System.out.println("Java format: " + javaSyntax);

        assertTrue(javaSyntax.contains(".filter(new Locator.FilterOptions().setHasNotText(\"Out of stock\"))"));

        System.out.println("✅ hasNotText filter works correctly");
    }

    /**
     * Test 5: Chained filters
     */
    @Test
    @DisplayName("Test chained filters")
    public void testChainedFilters() {
        System.out.println("\n=== Test 5: Chained Filters ===");

        Locator filteredLocator = page.getByRole(AriaRole.LISTITEM)
                .filter(new Locator.FilterOptions().setHasText("Product"))
                .filter(new Locator.FilterOptions().setHasNotText("Out of stock"));

        String internal = filteredLocator.toString();
        System.out.println("Playwright internal format: " + internal);

        String js = PlaywrightLocatorConverter.extractLocatorString(filteredLocator);
        System.out.println("JavaScript format: " + js);

        assertTrue(js.contains("getByRole('listitem')"));
        assertTrue(js.contains(".filter({ hasText: 'Product' })"));
        assertTrue(js.contains(".filter({ hasNotText: 'Out of stock' })"));

        PlaywrightLocator locator = PlaywrightLocatorParser.parse(js);

        assertTrue(locator.hasFilters());
        List<LocatorFilter> filters = locator.getFilters();
        assertEquals(2, filters.size(), "Should have 2 filters");

        // First filter
        LocatorFilter filter1 = filters.get(0);
        assertEquals(LocatorFilter.FilterType.HAS_TEXT, filter1.getType());
        assertEquals("Product", filter1.getValue());

        // Second filter
        LocatorFilter filter2 = filters.get(1);
        assertEquals(LocatorFilter.FilterType.HAS_NOT_TEXT, filter2.getType());
        assertEquals("Out of stock", filter2.getValue());

        String javaSyntax = locator.toSelectorString();
        System.out.println("Java format: " + javaSyntax);

        assertTrue(javaSyntax.contains(".filter(new Locator.FilterOptions().setHasText(\"Product\"))"));
        assertTrue(javaSyntax.contains(".filter(new Locator.FilterOptions().setHasNotText(\"Out of stock\"))"));

        System.out.println("✅ Chained filters work correctly");
    }

    /**
     * Test 6: Filter with child locator
     */
    @Test
    @DisplayName("Test filter with child locator")
    public void testFilterWithChildLocator() {
        System.out.println("\n=== Test 6: Filter with Child Locator ===");

        Locator filteredLocator = page.getByRole(AriaRole.LISTITEM)
                .filter(new Locator.FilterOptions().setHasText("Product 2"))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add to cart"));

        String internal = filteredLocator.toString();
        System.out.println("Playwright internal format: " + internal);

        String js = PlaywrightLocatorConverter.extractLocatorString(filteredLocator);
        System.out.println("JavaScript format: " + js);

        assertTrue(js.contains("getByRole('listitem')"));
        assertTrue(js.contains(".filter({ hasText: 'Product 2' })"));
        assertTrue(js.contains(".getByRole('button', { name: 'Add to cart' })"));

        System.out.println("✅ Filter with child locator works correctly");
    }

    /**
     * Test 7: Verify filters work with different locator types
     */
    @Test
    @DisplayName("Test filters with different locator types")
    public void testFiltersWithDifferentLocatorTypes() {
        System.out.println("\n=== Test 7: Filters with Different Locator Types ===");

        // Filter on getByText
        Locator textWithFilter = page.getByText("Welcome")
                .filter(new Locator.FilterOptions().setHasText("User"));
        String textJs = PlaywrightLocatorConverter.extractLocatorString(textWithFilter);
        PlaywrightLocator textLoc = PlaywrightLocatorParser.parse(textJs);
        assertTrue(textLoc.hasFilters(), "getByText should support filters");
        System.out.println("getByText with filter: " + textLoc.toSelectorString());

        // Filter on getByLabel
        Locator labelWithFilter = page.getByLabel("Search")
                .filter(new Locator.FilterOptions().setHasText("Active"));
        String labelJs = PlaywrightLocatorConverter.extractLocatorString(labelWithFilter);
        PlaywrightLocator labelLoc = PlaywrightLocatorParser.parse(labelJs);
        assertTrue(labelLoc.hasFilters(), "getByLabel should support filters");
        System.out.println("getByLabel with filter: " + labelLoc.toSelectorString());

        System.out.println("✅ Filters work with all locator types");
    }

    /**
     * Test 8: Empty filters should not affect basic locators
     */
    @Test
    @DisplayName("Test empty filters list doesn't affect output")
    public void testEmptyFiltersList() {
        System.out.println("\n=== Test 8: Empty Filters List ===");

        PlaywrightLocator locator = PlaywrightLocator.builder()
                .byRole("button", "Submit")
                .build();

        assertFalse(locator.hasFilters());
        assertEquals(0, locator.getFilters().size());

        String javaSyntax = locator.toSelectorString();
        assertFalse(javaSyntax.contains(".filter("), "Should not contain filter");
        assertTrue(javaSyntax.contains("page.getByRole(AriaRole.BUTTON"));

        System.out.println("✅ Empty filters list works correctly");
    }
}
