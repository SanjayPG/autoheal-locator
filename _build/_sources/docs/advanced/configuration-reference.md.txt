# AutoHeal AI Configuration Examples

The AutoHeal framework now supports comprehensive properties file configuration with multiple AI providers, smart defaults, and environment variable support.

## Default Configuration (autoheal-default.properties)

```properties
# AI Service Configuration
autoheal.ai.provider=OPENAI
autoheal.ai.model=${AI_MODEL:}
autoheal.ai.api-key=${OPENAI_API_KEY:}
autoheal.ai.api-url=${AI_API_URL:}
autoheal.ai.timeout=30s
autoheal.ai.max-retries=3
autoheal.ai.visual-analysis-enabled=true

# AI Token Configuration
autoheal.ai.max-tokens-dom=500
autoheal.ai.max-tokens-visual=1000
autoheal.ai.temperature-dom=0.1
autoheal.ai.temperature-visual=0.0
```

## Configuration Examples for Different AI Providers

### OpenAI Configuration
```properties
autoheal.ai.provider=OPENAI
# Optional: Override default model (gpt-4o-mini)
autoheal.ai.model=gpt-4o-mini
# API key from environment variable
autoheal.ai.api-key=${OPENAI_API_KEY}
```

### Google Gemini Configuration
```properties
autoheal.ai.provider=GOOGLE_GEMINI
# Optional: Override default model (gemini-2.0-flash)
autoheal.ai.model=gemini-1.5-pro
# API key from environment variable
autoheal.ai.api-key=${GEMINI_API_KEY}
```

### Anthropic Claude Configuration
```properties
autoheal.ai.provider=ANTHROPIC_CLAUDE
# Optional: Override default model (claude-3-sonnet)
autoheal.ai.model=claude-3-5-sonnet-20241022
# API key from environment variable
autoheal.ai.api-key=${ANTHROPIC_API_KEY}
# Note: Claude doesn't support visual analysis, only text
autoheal.ai.visual-analysis-enabled=false
```

### DeepSeek Configuration
```properties
autoheal.ai.provider=DEEPSEEK
# Optional: Override default model (deepseek-chat)
autoheal.ai.model=deepseek-chat
# API key from environment variable
autoheal.ai.api-key=${DEEPSEEK_API_KEY}
# DeepSeek only supports text analysis
autoheal.ai.visual-analysis-enabled=false
```

### Grok Configuration
```properties
autoheal.ai.provider=GROK
# Optional: Override default model (grok-beta)
autoheal.ai.model=grok-beta
# API key from environment variable
autoheal.ai.api-key=${GROK_API_KEY}
# Grok only supports text analysis
autoheal.ai.visual-analysis-enabled=false
```

### Local Model Configuration
```properties
autoheal.ai.provider=LOCAL_MODEL
# Custom model name for your local deployment
autoheal.ai.model=llama-3.1-8b
# Custom API endpoint for your local server
autoheal.ai.api-url=http://localhost:11434/v1/chat/completions
# No API key required for local models
autoheal.ai.visual-analysis-enabled=false
```

## Environment Variables

Set these environment variables for API keys:
- `OPENAI_API_KEY` - OpenAI API key
- `GEMINI_API_KEY` - Google Gemini API key
- `ANTHROPIC_API_KEY` - Anthropic Claude API key
- `DEEPSEEK_API_KEY` - DeepSeek API key
- `GROK_API_KEY` - Grok API key

## Smart Defaults

The system provides smart defaults:
1. **Provider**: If not specified or invalid, defaults to OPENAI
2. **Model**: If not specified, uses the provider's default model
3. **API URL**: If not specified, uses the provider's default endpoint
4. **API Key**: Automatically selects the correct environment variable based on provider
5. **Configuration Validation**: Ensures valid provider/model combinations with helpful error messages

## Visual Analysis Support

Only these providers support visual analysis:
- OpenAI (GPT-4o models with vision)
- Google Gemini (models with vision)
- Mock (for testing)

The system automatically detects provider capabilities and gracefully handles unsupported visual analysis requests.