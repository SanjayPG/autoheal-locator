package com.autoheal.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

public class TestFilteredLocators {
    public static void main(String[] args) {
        System.out.println("\n=== Testing Filtered Locators ===\n");

        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        Page page = browser.newPage();

        // Test 1: filter with hasText
        Locator filterHasText = page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions().setHasText("Product 2"));
        System.out.println("1. filter({ hasText: 'Product 2' }):");
        System.out.println("   toString(): " + filterHasText.toString());

        // Test 2: filter with hasText Pattern
        Locator filterHasTextPattern = page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions().setHasText(Pattern.compile("product 2", Pattern.CASE_INSENSITIVE)));
        System.out.println("\n2. filter({ hasText: /product 2/i }):");
        System.out.println("   toString(): " + filterHasTextPattern.toString());

        // Test 3: filter with has (child locator)
        Locator filterHas = page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Product 2"))));
        System.out.println("\n3. filter({ has: getByRole('heading', { name: 'Product 2' }) }):");
        System.out.println("   toString(): " + filterHas.toString());

        // Test 4: filter with hasNotText
        Locator filterHasNotText = page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions().setHasNotText("Out of stock"));
        System.out.println("\n4. filter({ hasNotText: 'Out of stock' }):");
        System.out.println("   toString(): " + filterHasNotText.toString());

        // Test 5: filter with hasNot (child locator)
        Locator filterHasNot = page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions()
                .setHasNot(page.getByText("Out of stock")));
        System.out.println("\n5. filter({ hasNot: getByText('Out of stock') }):");
        System.out.println("   toString(): " + filterHasNot.toString());

        // Test 6: Chained filters
        Locator chainedFilters = page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions().setHasText("Product"))
            .filter(new Locator.FilterOptions().setHasNotText("Out of stock"));
        System.out.println("\n6. Chained filters:");
        System.out.println("   toString(): " + chainedFilters.toString());

        // Test 7: Complex nested example
        Locator complexFilter = page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Product 2"))))
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add to cart"));
        System.out.println("\n7. Complex nested filter with child locator:");
        System.out.println("   toString(): " + complexFilter.toString());

        page.close();
        browser.close();
        playwright.close();

        System.out.println("\n=== Test Complete ===\n");
    }
}
