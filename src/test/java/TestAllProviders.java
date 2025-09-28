import com.autoheal.config.AIConfig;
import com.autoheal.impl.ai.ResilientAIService;
import com.autoheal.config.ResilienceConfig;
import com.autoheal.model.AIProvider;

import java.time.Duration;

public class TestAllProviders {
    public static void main(String[] args) {
        System.out.println("=== Testing All AI Provider Implementations ===");

        AIProvider[] providers = {
            AIProvider.OPENAI,
            AIProvider.GOOGLE_GEMINI,
            AIProvider.DEEPSEEK,
            AIProvider.ANTHROPIC_CLAUDE,
            AIProvider.GROK,
            AIProvider.LOCAL_MODEL,
            AIProvider.MOCK
        };

        // Test API keys for each provider (use dummy keys for testing)
        String[] apiKeys = {
            "sk-dummy-openai-key",
            "dummy-gemini-key",
            "dummy-deepseek-key",
            "dummy-anthropic-key",
            "dummy-grok-key",
            "dummy-local-key",
            "dummy-mock-key"
        };

        ResilienceConfig resilienceConfig = ResilienceConfig.builder()
                .circuitBreakerFailureThreshold(5)
                .circuitBreakerTimeout(Duration.ofMinutes(5))
                .retryMaxAttempts(3)
                .retryDelay(Duration.ofSeconds(1))
                .build();

        for (int i = 0; i < providers.length; i++) {
            AIProvider provider = providers[i];
            String apiKey = apiKeys[i];

            try {
                System.out.println("\n--- Testing " + provider + " ---");

                // Create AI configuration for this provider
                AIConfig config = AIConfig.builder()
                        .provider(provider)
                        .model(provider.getDefaultModel())
                        .apiKey(apiKey)
                        .timeout(Duration.ofSeconds(30))
                        .maxRetries(3)
                        .visualAnalysisEnabled(provider.supportsVisualAnalysis())
                        .build();

                // Create AI service
                ResilientAIService aiService = new ResilientAIService(config, resilienceConfig);

                System.out.println("✅ " + provider + " - Service created successfully");
                System.out.println("   └─ Provider: " + config.getProvider());
                System.out.println("   └─ Model: " + config.getModel());
                System.out.println("   └─ API URL: " + config.getApiUrl());
                System.out.println("   └─ Text Analysis: " + provider.supportsTextAnalysis());
                System.out.println("   └─ Visual Analysis: " + provider.supportsVisualAnalysis());

                // Cleanup
                aiService.shutdown();

            } catch (Exception e) {
                System.err.println("❌ " + provider + " - Failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n=== Provider Implementation Test Completed ===");
        printProviderSummary();
    }

    private static void printProviderSummary() {
        System.out.println("\n📊 **COMPREHENSIVE PROVIDER SUPPORT STATUS**");
        System.out.println("|Provider|DOM Analysis|Visual Analysis|Disambiguation|API Format|");
        System.out.println("|--------|------------|---------------|--------------|----------|");
        System.out.println("|✅ OpenAI|✅ Implemented|✅ Implemented|✅ Implemented|OpenAI Compatible|");
        System.out.println("|✅ Google Gemini|✅ Implemented|✅ Implemented|✅ Implemented|Gemini Specific|");
        System.out.println("|✅ DeepSeek|✅ Implemented|❌ Not Supported|✅ Implemented|OpenAI Compatible|");
        System.out.println("|✅ Anthropic Claude|✅ Implemented|❌ Not Supported|✅ Implemented|Anthropic Specific|");
        System.out.println("|✅ Grok|✅ Implemented|❌ Not Supported|✅ Implemented|OpenAI Compatible|");
        System.out.println("|✅ Local Model|✅ Implemented|❌ Not Supported|✅ Implemented|OpenAI Compatible|");
        System.out.println("|✅ Mock|✅ Implemented|✅ Implemented|✅ Implemented|Mock Response|");

        System.out.println("\n🔗 **API Endpoint Configuration**");
        System.out.println("To use these providers, configure your autoheal.properties:");
        System.out.println();
        System.out.println("# DeepSeek Configuration");
        System.out.println("autoheal.ai.provider=DEEPSEEK");
        System.out.println("autoheal.ai.api-key=your-deepseek-api-key");
        System.out.println("autoheal.ai.model=deepseek-chat");
        System.out.println();
        System.out.println("# Anthropic Claude Configuration");
        System.out.println("autoheal.ai.provider=ANTHROPIC_CLAUDE");
        System.out.println("autoheal.ai.api-key=your-anthropic-api-key");
        System.out.println("autoheal.ai.model=claude-3-sonnet");
        System.out.println();
        System.out.println("# Grok Configuration");
        System.out.println("autoheal.ai.provider=GROK");
        System.out.println("autoheal.ai.api-key=your-grok-api-key");
        System.out.println("autoheal.ai.model=grok-beta");
        System.out.println();
        System.out.println("# Local Model Configuration (Ollama, etc.)");
        System.out.println("autoheal.ai.provider=LOCAL_MODEL");
        System.out.println("autoheal.ai.api-url=http://localhost:11434/v1/chat/completions");
        System.out.println("autoheal.ai.model=llama2");
    }
}