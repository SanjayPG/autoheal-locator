import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.PerformanceConfig;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import com.autoheal.model.ExecutionStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.time.Duration;

/**
 * Quick diagnostic test to isolate the timeout issue
 */
public class QuickDiagnosticTest {
    public static void main(String[] args) {
        System.out.println("=== Quick Diagnostic Test ===");

        // Set the Gemini API key
        System.setProperty("GEMINI_API_KEY", "AIzaSyDjFQ7M6pKRsYyr1SmZugaxs7mGiOh57w8");

        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        try {
            // Create simple configuration with DOM only
            AutoHealConfiguration config = AutoHealConfiguration.builder()
                    .ai(AIConfig.builder()
                            .provider(AIProvider.GOOGLE_GEMINI)
                            .apiKey("AIzaSyDjFQ7M6pKRsYyr1SmZugaxs7mGiOh57w8")
                            .timeout(Duration.ofSeconds(60))  // Longer AI timeout
                            .maxRetries(1)
                            .visualAnalysisEnabled(false)  // DOM only for now
                            .build())
                    .performance(PerformanceConfig.builder()
                            .threadPoolSize(2)
                            .elementTimeout(Duration.ofSeconds(90))  // Longer element timeout
                            .enableMetrics(true)
                            .executionStrategy(ExecutionStrategy.DOM_ONLY)  // Force DOM only
                            .build())
                    .build();

            AutoHealLocator autoHeal = AutoHealLocator.builder()
                    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
                    .withConfiguration(config)
                    .build();

            driver.get("https://www.saucedemo.com/");
            System.out.println("Page loaded: " + driver.getTitle());

            System.out.println("\nTesting with correct selector first...");
            try {
                WebElement correct = autoHeal.findElement("#user-name", "username field");
                System.out.println("✅ Correct selector worked: " + correct.getTagName());
            } catch (Exception e) {
                System.out.println("❌ Correct selector failed: " + e.getMessage());
            }

            System.out.println("\nTesting with wrong selector (DOM healing expected)...");
            try {
                long startTime = System.currentTimeMillis();
                WebElement healed = autoHeal.findElement("#userrrr-name", "username input field");
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("✅ DOM healing worked in " + duration + "ms: " + healed.getAttribute("id"));
            } catch (Exception e) {
                System.out.println("❌ DOM healing failed: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}