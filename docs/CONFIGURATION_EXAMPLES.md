# AutoHeal Configuration Examples

This document provides **copy-paste ready** configuration examples for different AI providers.

---

## Quick Start Configurations

### Google Gemini (Recommended - Fast & Free)

**Best for**: Fast performance, free tier available, excellent for most use cases

```properties
# ============================================================================
# Google Gemini Configuration
# ============================================================================
autoheal.ai.provider=GOOGLE_GEMINI
autoheal.ai.model=gemini-2.0-flash
autoheal.ai.api-key=${GEMINI_API_KEY}
autoheal.ai.timeout=30s
autoheal.ai.max-retries=3
autoheal.ai.visual-analysis-enabled=true

# Cache Configuration
autoheal.cache.type=PERSISTENT_FILE
autoheal.cache.maximum-size=10000
autoheal.cache.expire-after-write-hours=24
autoheal.cache.expire-after-access-hours=2

# Performance
autoheal.performance.element-timeout=30s
autoheal.performance.execution-strategy=SMART_SEQUENTIAL

# Reporting
autoheal.reporting.enabled=true
autoheal.reporting.generate-html=true
```

**Performance**: 2-4 seconds per request
**Cost**: Free tier available
**Visual Analysis**: ✅ Supported

---

### OpenAI (Accurate & Reliable)

**Best for**: High accuracy requirements, production environments

```properties
# ============================================================================
# OpenAI Configuration
# ============================================================================
autoheal.ai.provider=OPENAI
autoheal.ai.model=gpt-4o-mini
autoheal.ai.api-key=${OPENAI_API_KEY}
autoheal.ai.timeout=60s                  # CRITICAL: OpenAI needs more time!
autoheal.ai.max-retries=5                # Handle occasional rate limiting
autoheal.ai.visual-analysis-enabled=true

# Cache Configuration (same as Gemini)
autoheal.cache.type=PERSISTENT_FILE
autoheal.cache.maximum-size=10000
autoheal.cache.expire-after-write-hours=24
autoheal.cache.expire-after-access-hours=2

# Performance
autoheal.performance.element-timeout=60s  # Match AI timeout
autoheal.performance.execution-strategy=SMART_SEQUENTIAL

# Reporting
autoheal.reporting.enabled=true
autoheal.reporting.generate-html=true
```

**Performance**: 5-15 seconds per request
**Cost**: $0.15 per 1M input tokens, $0.60 per 1M output tokens
**Visual Analysis**: ✅ Supported

**Important Notes**:
- ⚠️ **Must use `60s` timeout** (not 30s like Gemini)
- ⚠️ **Always use `s` suffix** for timeout values
- ⚠️ **Increase max-retries to 5** for rate limiting
- ✅ API key required (no free tier)

---

### Anthropic Claude (High Quality)

**Best for**: Complex reasoning, high-quality text analysis

```properties
# ============================================================================
# Anthropic Claude Configuration
# ============================================================================
autoheal.ai.provider=ANTHROPIC_CLAUDE
autoheal.ai.model=claude-3-5-sonnet-20241022
autoheal.ai.api-key=${ANTHROPIC_API_KEY}
autoheal.ai.timeout=45s
autoheal.ai.max-retries=3
autoheal.ai.visual-analysis-enabled=false  # Claude doesn't support vision yet

# Cache Configuration
autoheal.cache.type=PERSISTENT_FILE
autoheal.cache.maximum-size=10000
autoheal.cache.expire-after-write-hours=24

# Performance
autoheal.performance.element-timeout=45s
autoheal.performance.execution-strategy=DOM_ONLY  # No visual analysis

# Reporting
autoheal.reporting.enabled=true
autoheal.reporting.generate-html=true
```

**Performance**: 3-8 seconds per request
**Cost**: $3 per 1M input tokens, $15 per 1M output tokens
**Visual Analysis**: ❌ Not supported (DOM analysis only)

---

### Local Model (Ollama/LM Studio)

**Best for**: Privacy, cost savings, offline usage, no API dependencies

```properties
# ============================================================================
# Local Model Configuration (Ollama)
# ============================================================================
autoheal.ai.provider=LOCAL_MODEL
autoheal.ai.model=llama3.2:3b             # Or your local model name
autoheal.ai.api-key=not-required          # No API key needed
autoheal.ai.api-url=http://localhost:11434/v1
autoheal.ai.timeout=90s                   # Local models may be slower
autoheal.ai.max-retries=2
autoheal.ai.visual-analysis-enabled=false # Most local models don't support vision

# Cache Configuration (more aggressive for slower local models)
autoheal.cache.type=PERSISTENT_FILE
autoheal.cache.maximum-size=10000
autoheal.cache.expire-after-write-hours=48  # Cache longer for local models
autoheal.cache.expire-after-access-hours=4

# Performance
autoheal.performance.element-timeout=90s
autoheal.performance.execution-strategy=DOM_ONLY

# Reporting
autoheal.reporting.enabled=true
autoheal.reporting.generate-html=true
```

**Performance**: 10-60 seconds per request (depends on hardware)
**Cost**: Free (uses your local resources)
**Visual Analysis**: ❌ Usually not supported

**Setup Required**:
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull a model
ollama pull llama3.2:3b

# Verify it's running
curl http://localhost:11434/v1/models
```

---

## Performance Comparison

| Provider | Speed | Cost | Visual Analysis | Free Tier | Recommended Timeout |
|----------|-------|------|-----------------|-----------|---------------------|
| **Gemini** | ⚡⚡⚡ 2-4s | 💰 Free | ✅ Yes | ✅ Yes | 30s |
| **OpenAI** | ⚡⚡ 5-15s | 💰💰 $0.15/1M | ✅ Yes | ❌ No | **60s** |
| **Claude** | ⚡⚡ 3-8s | 💰💰💰 $3/1M | ❌ No | ❌ No | 45s |
| **Local** | ⚡ 10-60s | 🆓 Free | ❌ No | ✅ Free | 90s |

---

## Common Configuration Mistakes

### ❌ Mistake 1: Missing Unit Suffix

**Wrong**:
```properties
autoheal.ai.timeout=30    # Missing 's' suffix - may cause issues!
```

**Correct**:
```properties
autoheal.ai.timeout=30s   # Always use 's' suffix for seconds
```

---

### ❌ Mistake 2: Using Gemini Timeout for OpenAI

**Wrong**:
```properties
autoheal.ai.provider=OPENAI
autoheal.ai.timeout=30s   # Too short for OpenAI!
```

**Correct**:
```properties
autoheal.ai.provider=OPENAI
autoheal.ai.timeout=60s   # OpenAI needs 60s minimum
```

---

### ❌ Mistake 3: Not Specifying Model

**Less Ideal**:
```properties
autoheal.ai.provider=OPENAI
# autoheal.ai.model=        # Commented out - relies on default
```

**Better**:
```properties
autoheal.ai.provider=OPENAI
autoheal.ai.model=gpt-4o-mini  # Explicitly specified
```

While the library defaults to the correct model, **explicitly specifying it is best practice**.

---

### ❌ Mistake 4: Low Retries for OpenAI

**Wrong**:
```properties
autoheal.ai.provider=OPENAI
autoheal.ai.max-retries=3   # May not be enough for rate limiting
```

**Correct**:
```properties
autoheal.ai.provider=OPENAI
autoheal.ai.max-retries=5   # Better for handling rate limits
```

---

## Environment Variables

### Setting API Keys

**Windows (Command Prompt)**:
```cmd
set OPENAI_API_KEY=sk-proj-your-key-here
set GEMINI_API_KEY=your-gemini-key-here
set ANTHROPIC_API_KEY=sk-ant-your-key-here
```

**Windows (PowerShell)**:
```powershell
$env:OPENAI_API_KEY="sk-proj-your-key-here"
$env:GEMINI_API_KEY="your-gemini-key-here"
$env:ANTHROPIC_API_KEY="sk-ant-your-key-here"
```

**Mac/Linux**:
```bash
export OPENAI_API_KEY="sk-proj-your-key-here"
export GEMINI_API_KEY="your-gemini-key-here"
export ANTHROPIC_API_KEY="sk-ant-your-key-here"
```

### Using Direct Values (Not Recommended for Production)

```properties
# For testing only - don't commit to git!
autoheal.ai.api-key=sk-proj-your-actual-key-here
```

---

## Switching Between Providers

You can easily switch between providers by changing 3 settings:

### From Gemini to OpenAI

**Change**:
```properties
# From:
autoheal.ai.provider=GOOGLE_GEMINI
autoheal.ai.model=gemini-2.0-flash
autoheal.ai.api-key=${GEMINI_API_KEY}
autoheal.ai.timeout=30s
autoheal.ai.max-retries=3

# To:
autoheal.ai.provider=OPENAI
autoheal.ai.model=gpt-4o-mini
autoheal.ai.api-key=${OPENAI_API_KEY}
autoheal.ai.timeout=60s           # IMPORTANT: Increase timeout!
autoheal.ai.max-retries=5         # IMPORTANT: More retries!
```

---

## Testing Your Configuration

### Quick API Test

```bash
# Test OpenAI
mvn test -Dtest=OpenAIConnectionTest

# Test your actual application
mvn test -Dtest=YourTestClass
```

### Expected Behavior

**Gemini (working correctly)**:
```
[SUCCESS] [DOM] [2500ms] [1200 tokens] page.getByText("WRONG") → page.getByText("CORRECT")
```

**OpenAI (working correctly)**:
```
[SUCCESS] [DOM] [8000ms] [1500 tokens] page.getByText("WRONG") → page.getByText("CORRECT")
```

Notice OpenAI takes **3-4x longer** than Gemini, hence the need for 60s timeout.

---

## Complete Example: Production Configuration

For a production environment using OpenAI:

```properties
# ============================================================================
# AutoHeal Production Configuration - OpenAI
# ============================================================================

# AI Provider
autoheal.ai.provider=OPENAI
autoheal.ai.model=gpt-4o-mini
autoheal.ai.api-key=${OPENAI_API_KEY}
autoheal.ai.timeout=60s
autoheal.ai.max-retries=5
autoheal.ai.visual-analysis-enabled=true
autoheal.ai.max-tokens-dom=500
autoheal.ai.max-tokens-visual=1000
autoheal.ai.temperature-dom=0.1
autoheal.ai.temperature-visual=0.0

# Cache (aggressive caching for production)
autoheal.cache.type=PERSISTENT_FILE
autoheal.cache.file-directory=.autoheal-cache
autoheal.cache.maximum-size=50000
autoheal.cache.expire-after-write-hours=72
autoheal.cache.expire-after-access-hours=24
autoheal.cache.record-stats=true

# Performance
autoheal.performance.thread-pool-size=8
autoheal.performance.element-timeout=60s
autoheal.performance.enable-metrics=true
autoheal.performance.execution-strategy=SMART_SEQUENTIAL
autoheal.performance.max-concurrent-requests=50

# Resilience
autoheal.resilience.circuit-breaker-failure-threshold=5
autoheal.resilience.circuit-breaker-timeout=5m
autoheal.resilience.retry-max-attempts=5
autoheal.resilience.retry-delay=2s

# Reporting
autoheal.reporting.enabled=true
autoheal.reporting.generate-html=true
autoheal.reporting.generate-json=true
autoheal.reporting.generate-text=false
autoheal.reporting.console-logging=false
autoheal.reporting.output-directory=test-reports
autoheal.reporting.include-screenshots=true
autoheal.reporting.include-cache-stats=true

# Logging (production - less verbose)
logging.level.com.autoheal=INFO
```

---

## Need Help?

- **OpenAI not working?** See [OPENAI_DEBUG_GUIDE.md](OPENAI_DEBUG_GUIDE.md)
- **Cache issues?** See [CACHING_MECHANISM.md](CACHING_MECHANISM.md)
- **Playwright setup?** See [PLAYWRIGHT_GUIDE.md](PLAYWRIGHT_GUIDE.md)
- **Selenium setup?** See [selenium-usage-guide.md](selenium-usage-guide.md)
