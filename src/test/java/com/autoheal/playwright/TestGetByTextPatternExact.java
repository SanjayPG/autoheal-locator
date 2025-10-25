package com.autoheal.playwright;

import com.microsoft.playwright.*;
import java.util.regex.Pattern;

public class TestGetByTextPatternExact {
    public static void main(String[] args) {
        System.out.println("\n=== Testing getByText() with exact and Pattern ===\n");

        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        Page page = browser.newPage();

        // Test 1: getByText with exact option
        Locator exactLocator = page.getByText("Welcome, John", new Page.GetByTextOptions().setExact(true));
        System.out.println("1. getByText with exact=true:");
        System.out.println("   toString(): " + exactLocator.toString());

        // Test 2: getByText with Pattern
        Locator patternLocator = page.getByText(Pattern.compile("welcome, john$", Pattern.CASE_INSENSITIVE));
        System.out.println("\n2. getByText with Pattern:");
        System.out.println("   toString(): " + patternLocator.toString());

        // Test 3: getByText without options (default)
        Locator defaultLocator = page.getByText("Welcome, John");
        System.out.println("\n3. getByText without options:");
        System.out.println("   toString(): " + defaultLocator.toString());

        page.close();
        browser.close();
        playwright.close();

        System.out.println("\n=== Test Complete ===\n");
    }
}
