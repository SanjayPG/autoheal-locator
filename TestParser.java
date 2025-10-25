import com.autoheal.model.PlaywrightLocator;
import com.autoheal.util.PlaywrightLocatorParser;

public class TestParser {
    public static void main(String[] args) {
        System.out.println("\n=== Testing Parser ===\n");

        String input = "locator(\"getByRole('button', { name: 'Submit_WRONG' })\")";
        System.out.println("Input: [" + input + "]");
        System.out.println("Length: " + input.length());
        System.out.println("Starts with 'locator(\"': " + input.startsWith("locator(\""));
        System.out.println("Ends with '\")': " + input.endsWith("\")"));

        if (input.startsWith("locator(\"") && input.endsWith("\")")) {
            String unwrapped = input.substring(9, input.length() - 2);
            System.out.println("Unwrapped would be: [" + unwrapped + "]");
        }

        PlaywrightLocator parsed = PlaywrightLocatorParser.parse(input);
        System.out.println("\nParsed type: " + parsed.getType());
        System.out.println("Parsed value: " + parsed.getValue());
        System.out.println("Parsed options: " + parsed.getOptions());
        System.out.println("Java output: " + parsed.toSelectorString());
    }
}
