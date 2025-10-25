import com.autoheal.AutoHealLocator;
import com.autoheal.config.AIConfig;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.CacheConfig;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;

import static com.autoheal.config.ReportingConfig.enabledWithDefaults;

public class SimpleCacheVerification {
    public static void main(String[] args) {
        System.out.println("\n=== CACHE VERIFICATION TEST ===\n");

        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(500));
        Page page = browser.newPage();
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

        AutoHealLocator autoHeal = AutoHealLocator.builder()
                .withWebAdapter(new PlaywrightWebAutomationAdapter(page))
                .withConfiguration(config)
                .build();

        try {
            page.navigate("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
            page.waitForLoadState();

            System.out.println("\n=== Finding button with WRONG locator (should use cache) ===");
            long startTime = System.currentTimeMillis();

            Locator button = autoHeal.find(
                    page,
                    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit_WRONG")),
                    "Login button on login page"
            );

            long duration = System.currentTimeMillis() - startTime;

            System.out.println("✓ Button found in " + duration + "ms");
            System.out.println("✓ If cached, should be < 100ms");
            System.out.println("✓ If NOT cached (AI healing), should be > 1000ms");

            button.click();
            page.waitForLoadState();

            if (page.url().contains("dashboard")) {
                System.out.println("✓ Successfully navigated to dashboard");
            }

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        } finally {
            autoHeal.shutdown();
            page.close();
            browser.close();
            playwright.close();
        }

        System.out.println("\n=== TEST COMPLETE ===\n");
    }
}
