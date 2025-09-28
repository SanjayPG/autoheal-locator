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
 * Quick test specifically for Ollama integration
 */
public class OllamaQuickTest {
    public static void main(String[] args) {
        System.out.println("=== Ollama Quick Test ===");

        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        try {
            // Simple configuration for Ollama testing
            AutoHealConfiguration config = AutoHealConfiguration.builder()
                    .ai(AIConfig.builder()
                            .provider(AIProvider.LOCAL_MODEL)
                            .apiUrl("http://127.0.0.1:11434/api/chat")
                            .model("gemma3:1b")
                            .apiKey("")
                            .timeout(Duration.ofSeconds(180))
                            .maxRetries(1)
                            .visualAnalysisEnabled(false)
                            .build())
                    .performance(PerformanceConfig.builder()
                            .threadPoolSize(1)
                            .elementTimeout(Duration.ofSeconds(300))
                            .enableMetrics(true)
                            .executionStrategy(ExecutionStrategy.DOM_ONLY)
                            .build())
                    .build();

            AutoHealLocator autoHeal = AutoHealLocator.builder()
                    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
                    .withConfiguration(config)
                    .build();

            driver.get("https://www.saucedemo.com/");
            System.out.println("Page loaded: " + driver.getTitle());

            System.out.println("\nTesting Ollama with wrong selector...");
            long startTime = System.currentTimeMillis();

            try {
                WebElement healed = autoHeal.findElement("#wrong-username", "username input field");
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("✅ Ollama healing worked in " + duration + "ms");
                System.out.println("   Found element: " + healed.getAttribute("id"));
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("❌ Ollama healing failed after " + duration + "ms: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}