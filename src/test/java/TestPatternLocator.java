import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

public class TestPatternLocator {
    public static void main(String[] args) {
        System.out.println("\n=== Testing Pattern-based Locators ===\n");

        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        Page page = browser.newPage();

        // Create a Pattern-based locator
        Locator patternLocator = page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(
                Pattern.compile("submit", Pattern.CASE_INSENSITIVE)
            )
        );

        System.out.println("Pattern Locator toString(): " + patternLocator.toString());

        // Try to extract using reflection
        try {
            Class<?> locatorClass = patternLocator.getClass();
            java.lang.reflect.Field selectorField = locatorClass.getDeclaredField("selector");
            selectorField.setAccessible(true);
            Object selectorValue = selectorField.get(patternLocator);
            System.out.println("Extracted selector: " + selectorValue);
        } catch (Exception e) {
            System.out.println("Reflection failed: " + e.getMessage());
        }

        page.close();
        browser.close();
        playwright.close();

        System.out.println("\n=== Test Complete ===\n");
    }
}
