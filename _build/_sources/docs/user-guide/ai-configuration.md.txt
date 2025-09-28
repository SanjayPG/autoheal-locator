# AI Configuration Guide

## Overview

AutoHeal supports multiple AI providers for intelligent element location. Choose the provider that best fits your needs in terms of cost, performance, and features.

## Supported AI Providers

| Provider | DOM Analysis | Visual Analysis | Cost | Setup Complexity |
|----------|-------------|-----------------|------|------------------|
| OpenAI   | ✅ Excellent | ✅ Excellent    | $$   | Easy            |
| Gemini   | ✅ Good     | ❌ Not Yet      | $    | Easy            |
| Local LLM| ✅ Good     | ❌ Limited      | Free | Medium          |
| Mock     | ✅ Testing  | ❌ Testing      | Free | None            |

---

## OpenAI Configuration

### 1. Get API Key

1. Visit [OpenAI API Keys](https://platform.openai.com/api-keys)
2. Create an account or sign in
3. Click **"Create new secret key"**
4. Copy the key (starts with `sk-proj-` or `sk-`)
5. Set billing information (required for API access)

### 2. Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .apiKey(System.getenv("OPENAI_API_KEY"))  // From environment variable
        .model("gpt-4o-mini")           // Recommended: cost-effective
        .timeout(Duration.ofSeconds(30))
        .maxRetries(3)
        .visualAnalysisEnabled(true)    // Enable screenshot analysis
        .build())
    .build();
```

### 3. Model Options

| Model | DOM Analysis | Visual Analysis | Cost/1K tokens | Best For |
|-------|-------------|-----------------|----------------|----------|
| `gpt-4o-mini` | ✅ | ✅ | $0.00015 | **Recommended** - Best balance |
| `gpt-4o` | ✅ | ✅ | $0.005 | Complex scenarios |
| `gpt-3.5-turbo` | ✅ | ❌ | $0.0005 | DOM-only, budget |

### 4. Environment Setup

**Option 1: Environment Variable (Recommended)**
```bash
# Linux/Mac
export OPENAI_API_KEY="sk-proj-your-key-here"

# Windows
set OPENAI_API_KEY=sk-proj-your-key-here
```

**Option 2: System Property**
```bash
-DOPENAI_API_KEY="sk-proj-your-key-here"
```

**Option 3: Configuration File (.env)**
```bash
# Create .env file in project root
echo "OPENAI_API_KEY=sk-proj-your-key-here" > .env
```

**Option 4: Properties File**
```java
// Load from application.properties or config.properties
Properties props = new Properties();
props.load(new FileInputStream("config.properties"));

AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .apiKey(props.getProperty("openai.api.key"))
        .build())
    .build();
```

---

## Google Gemini Configuration

### 1. Get API Key

1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with Google account
3. Click **"Create API key"**
4. Select existing project or create new one
5. Copy the generated key

### 2. Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.GEMINI)
        .apiKey(System.getenv("GEMINI_API_KEY"))  // From environment variable
        .model("gemini-1.5-pro")
        .timeout(Duration.ofSeconds(45))
        .maxRetries(2)
        .visualAnalysisEnabled(false)   // Not supported yet
        .build())
    .build();
```

### 3. Model Options

| Model | Features | Cost/1K tokens | Notes |
|-------|----------|----------------|--------|
| `gemini-1.5-pro` | Advanced reasoning | $0.00125 | Best performance |
| `gemini-1.5-flash` | Fast responses | $0.000075 | Budget option |

### 4. Environment Setup

**Environment Variable (Recommended)**
```bash
# Linux/Mac
export GEMINI_API_KEY="your-gemini-key-here"

# Windows
set GEMINI_API_KEY=your-gemini-key-here
```

**Properties File**
```java
// config.properties
gemini.api.key=your-gemini-key-here

// Load in code
Properties props = new Properties();
props.load(new FileInputStream("config.properties"));

AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.GEMINI)
        .apiKey(props.getProperty("gemini.api.key"))
        .build())
    .build();
```

### 5. Limitations

- ❌ Visual analysis not yet supported
- ⚠️ Longer response times for complex DOM analysis
- ✅ Excellent for simple selector healing

---

## Local LLM Configuration

Run AI models locally for privacy and cost control.

### 1. Setup Ollama

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull a model
ollama pull llama3.1:8b

# Start the server
ollama serve
```

### 2. Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.LOCAL_LLM)
        .apiUrl("http://localhost:11434/v1/chat/completions")
        .model("llama3.1:8b")
        .timeout(Duration.ofSeconds(60))
        .maxRetries(1)
        .visualAnalysisEnabled(false)
        .build())
    .build();
```

### 3. Recommended Models

| Model | Size | Performance | Memory Required |
|-------|------|-------------|-----------------|
| `llama3.1:8b` | 4.7GB | Good | 8GB RAM |
| `llama3.1:70b` | 40GB | Excellent | 64GB RAM |
| `codellama:7b` | 3.8GB | Code-focused | 8GB RAM |

### 4. Alternative Local Setups

**LM Studio:**
```java
.apiUrl("http://localhost:1234/v1/chat/completions")
.model("local-model")
```

**GPT4All:**
```java
.apiUrl("http://localhost:4891/v1/chat/completions")
.model("gpt4all-model")
```

---

## Mock AI (Testing)

For unit tests and development without AI costs.

### Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.MOCK)
        .build())
    .build();

// Add predefined responses
AutoHealLocator autoHeal = AutoHealLocator.builder()
    .withConfiguration(config)
    .build();

MockAIService mockAI = (MockAIService) autoHeal.getAIService();
mockAI.addMockResponse("login button", "#signin-btn", 0.95);
mockAI.addMockResponse("username field", "#user-input", 0.90);
```

---

## Advanced Configuration

### Circuit Breaker Settings

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .resilience(ResilienceConfig.builder()
        .circuitBreakerFailureThreshold(5)     // Open after 5 failures
        .circuitBreakerTimeout(Duration.ofMinutes(2))  // Stay open for 2 minutes
        .build())
    .build();
```

### Cost Optimization

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .model("gpt-4o-mini")                   // Cheapest option
        .timeout(Duration.ofSeconds(15))        // Shorter timeout
        .maxRetries(1)                         // Fewer retries
        .visualAnalysisEnabled(false)          // Disable expensive visual analysis
        .build())
    .cache(CacheConfig.builder()
        .enabled(true)                         // Aggressive caching
        .maxSize(10000)
        .expireAfterWrite(Duration.ofHours(24))
        .build())
    .performance(PerformanceConfig.builder()
        .executionStrategy(ExecutionStrategy.DOM_ONLY)  // Skip visual fallback
        .build())
    .build();
```

### High-Performance Setup

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .model("gpt-4o")                       // Fastest, most accurate
        .timeout(Duration.ofSeconds(45))
        .maxRetries(3)
        .visualAnalysisEnabled(true)
        .build())
    .performance(PerformanceConfig.builder()
        .threadPoolSize(8)                     // More parallel processing
        .executionStrategy(ExecutionStrategy.PARALLEL)  // Try all strategies simultaneously
        .build())
    .build();
```

---

## Configuration Best Practices

### 1. Environment-Specific Configs

```java
public class ConfigFactory {
    public static AutoHealConfiguration getConfig(Environment env) {
        return switch (env) {
            case DEVELOPMENT -> createDevelopmentConfig();
            case STAGING -> createStagingConfig();
            case PRODUCTION -> createProductionConfig();
        };
    }

    private static AutoHealConfiguration createDevelopmentConfig() {
        return AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .provider(AIProvider.MOCK)  // No costs in dev
                .build())
            .build();
    }

    private static AutoHealConfiguration createProductionConfig() {
        return AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .provider(AIProvider.OPENAI)
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .model("gpt-4o-mini")
                .build())
            .cache(CacheConfig.builder()
                .enabled(true)  // Production needs caching
                .build())
            .build();
    }
}
```

### 2. Configuration Validation

```java
@Test
void validateConfiguration() {
    AutoHealConfiguration config = ConfigFactory.getConfig(Environment.PRODUCTION);

    // Verify AI provider is configured
    assertNotNull(config.getAiConfig().getApiKey());
    assertTrue(config.getAiConfig().getTimeout().getSeconds() > 0);

    // Verify caching is enabled for production
    assertTrue(config.getCacheConfig().isEnabled());
}
```

### 3. Monitoring and Alerts

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .build())
    .monitoring(MonitoringConfig.builder()
        .metricsEnabled(true)
        .alertOnHighCosts(100.0)  // Alert if daily cost > $100
        .alertOnLowSuccessRate(0.8)  // Alert if success rate < 80%
        .build())
    .build();
```

---

## Troubleshooting

### Common Issues

**❌ API Key Invalid**
```
Error: OpenAI API returned 401 Unauthorized
Solution: Verify API key is correct and has billing enabled
```

**❌ Rate Limiting**
```
Error: OpenAI API returned 429 Too Many Requests
Solution: Reduce parallel requests or upgrade API plan
```

**❌ Model Not Found**
```
Error: Model 'gpt-4o-mini' not found
Solution: Check model name spelling and availability
```

**❌ Timeout Issues**
```
Error: AI request timed out after 30 seconds
Solution: Increase timeout or simplify prompts
```

### Debug Configuration

Enable debug logging to troubleshoot issues:

```java
System.setProperty("org.slf4j.simpleLogger.log.com.autoheal", "DEBUG");

AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .apiKey("your-key")
        .build())
    .logging(LoggingConfig.builder()
        .logAIRequests(true)     // Log all AI interactions
        .logAIResponses(true)    // Log AI responses
        .logPerformance(true)    // Log timing information
        .build())
    .build();
```

---

## Next Steps

1. [Quick Start Guide](./quick-start.md)
2. [Usage Examples](./examples/)
3. [Performance Optimization](./performance.md)