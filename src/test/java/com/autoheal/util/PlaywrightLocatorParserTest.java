package com.autoheal.util;

import com.autoheal.model.PlaywrightLocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlaywrightLocatorParserTest {

    @Test
    public void testParseWithLocatorWrapper() {
        System.out.println("\n=== Testing Parser with locator() wrapper ===");

        // Test case 1: locator() with double quotes
        String input1 = "locator(\"getByRole('button', { name: 'Submit_WRONG' })\")";
        System.out.println("Input 1: " + input1);

        PlaywrightLocator parsed1 = PlaywrightLocatorParser.parse(input1);
        String javaOutput1 = parsed1.toSelectorString();
        System.out.println("Output 1: " + javaOutput1);

        assertEquals(PlaywrightLocator.Type.GET_BY_ROLE, parsed1.getType(),
            "Should parse as GET_BY_ROLE, not CSS_SELECTOR");
        assertEquals("button", parsed1.getValue(), "Should extract role 'button'");
        assertEquals("Submit_WRONG", parsed1.getOption("name"), "Should extract name option");
        assertFalse(javaOutput1.contains("page.locator(\"getByRole"),
            "Java output should not have double wrapping");

        // Test case 2: Already unwrapped
        String input2 = "getByPlaceholder('Username_WRONG')";
        System.out.println("\nInput 2: " + input2);

        PlaywrightLocator parsed2 = PlaywrightLocatorParser.parse(input2);
        String javaOutput2 = parsed2.toSelectorString();
        System.out.println("Output 2: " + javaOutput2);

        assertEquals(PlaywrightLocator.Type.GET_BY_PLACEHOLDER, parsed2.getType());
        assertEquals("Username_WRONG", parsed2.getValue());
        assertEquals("page.getByPlaceholder(\"Username_WRONG\")", javaOutput2);

        // Test case 3: locator() with single quotes
        String input3 = "locator('getByText(\"Login\")')";
        System.out.println("\nInput 3: " + input3);

        PlaywrightLocator parsed3 = PlaywrightLocatorParser.parse(input3);
        String javaOutput3 = parsed3.toSelectorString();
        System.out.println("Output 3: " + javaOutput3);

        assertEquals(PlaywrightLocator.Type.GET_BY_TEXT, parsed3.getType());

        System.out.println("\n=== All tests passed! ===\n");
    }

    @Test
    public void testCacheKeyGeneration() {
        System.out.println("\n=== Testing Cache Key Generation ===");

        String locator1 = "getByPlaceholder('Username_WRONG')";
        String desc1 = "Username input field on login page";
        String key1 = PlaywrightLocatorParser.generateCacheKey(locator1, desc1);
        System.out.println("Key 1: " + key1);

        String locator2 = "getByRole('button', { name: 'Submit_WRONG' })";
        String desc2 = "Login button on login page";
        String key2 = PlaywrightLocatorParser.generateCacheKey(locator2, desc2);
        System.out.println("Key 2: " + key2);

        assertTrue(key1.contains("getByPlaceholder('Username_WRONG')"),
            "Cache key should contain full locator string");
        assertTrue(key2.contains("getByRole('button', { name: 'Submit_WRONG' })"),
            "Cache key should contain full locator string including options");

        System.out.println("\n=== Cache key tests passed! ===\n");
    }
}
