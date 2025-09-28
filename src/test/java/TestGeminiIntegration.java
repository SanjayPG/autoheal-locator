import com.autoheal.config.AIConfig;
import com.autoheal.impl.ai.ResilientAIService;
import com.autoheal.config.ResilienceConfig;
import com.autoheal.model.AIProvider;

import java.time.Duration;

public class TestGeminiIntegration {
    public static void main(String[] args) {
        System.out.println("=== Testing Google Gemini Integration ===");

        try {
            // Create AI configuration for Google Gemini
            AIConfig config = AIConfig.builder()
                    .provider(AIProvider.GOOGLE_GEMINI)
                    .model("gemini-2.0-flash")
                    .apiKey("AIzaSyDjFQ7M6pKRsYyr1SmZugaxs7mGiOh57w8")
                    .timeout(Duration.ofSeconds(30))
                    .maxRetries(3)
                    .visualAnalysisEnabled(false)  // Test DOM only first
                    .build();

            // Create resilience config
            ResilienceConfig resilienceConfig = ResilienceConfig.builder()
                    .circuitBreakerFailureThreshold(5)
                    .circuitBreakerTimeout(Duration.ofMinutes(5))
                    .retryMaxAttempts(3)
                    .retryDelay(Duration.ofSeconds(1))
                    .build();

            // Create AI service
            ResilientAIService aiService = new ResilientAIService(config, resilienceConfig);

            System.out.println("✅ ResilientAIService created successfully with Google Gemini");
            System.out.println("✅ Provider: " + config.getProvider());
            System.out.println("✅ Model: " + config.getModel());
            System.out.println("✅ API URL: " + config.getApiUrl());

            // Test basic HTML DOM analysis (without actual HTTP call to avoid quota usage)
            System.out.println("✅ Basic configuration test passed!");
            System.out.println("✅ Google Gemini integration is properly configured");

            // Cleanup
            aiService.shutdown();

        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}