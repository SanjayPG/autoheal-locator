# OpenAI Debugging Guide

## Issue: OpenAI fails where Gemini succeeds

### Symptoms
- Tests work fine with `GEMINI_API_KEY`
- Tests fail with `OPENAI_API_KEY`
- Error: `Could not find element even after healing`
- Timeout around 10-11 seconds

---

## Possible Causes

### 1. **API Key Issues**
- ❌ Invalid or expired OpenAI API key
- ❌ API key doesn't have sufficient credits
- ❌ Rate limiting on OpenAI account

### 2. **Timeout Issues**
- ❌ OpenAI taking longer than configured timeout
- ❌ Network latency to OpenAI servers

### 3. **Model Configuration**
- ❌ Using incompatible model
- ❌ Model doesn't support the request format

### 4. **Response Parsing Issues**
- ❌ OpenAI returning different JSON format than expected
- ❌ Response wrapped in unexpected format

---

## Debugging Steps

### Step 1: Enable Debug Logging

Add to `autoheal.properties`:
```properties
# Enable debug logging
logging.level.com.autoheal=DEBUG
autoheal.reporting.console-logging=true
```

Or add to `logback.xml`:
```xml
<logger name="com.autoheal.impl.ai.ResilientAIService" level="DEBUG"/>
```

### Step 2: Check OpenAI Configuration

Verify your configuration in `autoheal.properties`:
```properties
# OpenAI Configuration (RECOMMENDED settings)
autoheal.ai.provider=OPENAI
autoheal.ai.api-key=${OPENAI_API_KEY}
autoheal.ai.model=gpt-4o-mini          # RECOMMENDED: Explicitly specify model
autoheal.ai.timeout=60s                # CRITICAL: OpenAI needs more time (5-15s per request)
autoheal.ai.max-retries=5              # Increased for OpenAI rate limiting
```

**Important**:
- ✅ **Always use unit suffix** for timeout: `60s` (NOT just `60`)
- ✅ **Explicitly specify model** (even though library defaults to gpt-4o-mini)
- ✅ **Use 60s timeout minimum** for OpenAI (vs 30s for Gemini)

### Step 3: Verify API Key

Test your OpenAI API key manually:
```bash
curl https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "Say hello"}]
  }'
```

Expected response should include:
```json
{
  "choices": [{
    "message": {
      "content": "Hello!"
    }
  }]
}
```

### Step 4: Check OpenAI Account

1. Visit https://platform.openai.com/account/usage
2. Verify you have available credits
3. Check rate limits: https://platform.openai.com/account/rate-limits

### Step 5: Compare Timeouts

**Gemini:** Typically responds in 2-4 seconds
**OpenAI:** Can take 5-15 seconds depending on model

**Solution:** Increase timeout
```properties
autoheal.ai.timeout=60s  # Up from default 30s
```

### Step 6: Try Different Models

OpenAI models have different response times and costs:

```properties
# Fast and cheap (recommended for AutoHeal)
autoheal.ai.model=gpt-4o-mini

# More accurate but slower and more expensive
autoheal.ai.model=gpt-4o

# Legacy (not recommended)
autoheal.ai.model=gpt-3.5-turbo
```

### Step 7: Check Logs for Actual Error

Look in the test output for these log lines:

```
[DEBUG] ResilientAIService - OpenAI API response received (length: ...)
[DEBUG] ResilientAIService - Raw AI response content: ...
[ERROR] ResilientAIService - OpenAI API call failed with status: ...
[ERROR] ResilientAIService - Failed to parse OpenAI response...
```

The actual error will show:
- HTTP status code (401=auth, 429=rate limit, 500=server error)
- Response body content
- Parsing errors

---

## Common Fixes

### Fix 1: Increase Timeout (Most Common - REQUIRED)

**Critical**: OpenAI requires longer timeout than Gemini

```properties
autoheal.ai.timeout=60s                  # Use 60s with 's' suffix!
autoheal.performance.element-timeout=60s
```

**Common mistake**: Using `autoheal.ai.timeout=30` without the `s` suffix

### Fix 2: Explicitly Specify Model (Best Practice)

```properties
autoheal.ai.provider=OPENAI
autoheal.ai.model=gpt-4o-mini    # Explicitly specify (even though it's the default)
```

### Fix 3: Increase Retries for OpenAI

```properties
autoheal.ai.max-retries=5        # OpenAI may hit rate limits occasionally
```

### Fix 4: Verify API Key is Set

```bash
# Windows
echo %OPENAI_API_KEY%

# Mac/Linux
echo $OPENAI_API_KEY
```

Should show: `sk-proj-...` or `sk-...`

### Fix 4: Check OpenAI Status

Visit: https://status.openai.com/

OpenAI occasionally has outages or degraded performance.

### Fix 5: Add Retry Logic

```properties
autoheal.ai.max-retries=5  # Increase from default 3
```

---

## Still Not Working?

### Get Detailed Debug Info

Run your test with maximum logging:

```bash
# Windows
set OPENAI_API_KEY=your-key
mvn test -Dtest=SignUpPageAutohealTest#testWelcomeMessage -X

# Mac/Linux
export OPENAI_API_KEY=your-key
mvn test -Dtest=SignUpPageAutohealTest#testWelcomeMessage -X
```

### Send Debug Info

Please provide:
1. Full error stack trace
2. Log lines containing "OpenAI API"
3. Your `autoheal.properties` (remove API key!)
4. OpenAI model being used

---

## Comparison: Gemini vs OpenAI

| Aspect | Gemini | OpenAI |
|--------|--------|--------|
| **Speed** | ⚡⚡ 2-4s | ⚡ 5-15s |
| **Cost** | 💰 $0.001/1K tokens | 💰💰 $0.15-$2.50/1M tokens |
| **Rate Limits** | High (free tier) | Low (paid only) |
| **Availability** | Very high | Can have outages |
| **Accuracy** | High | Very high |

**Recommendation:** For AutoHeal, Gemini is often better due to speed and cost unless you need OpenAI's superior accuracy.

---

## Quick Test Script

Save as `test-openai.sh`:

```bash
#!/bin/bash
echo "Testing OpenAI API..."
echo "API Key: ${OPENAI_API_KEY:0:10}..."

curl -s https://api.openai.com/v1/chat/completions \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{
      "role": "user",
      "content": "Respond with valid JSON: {\"status\": \"ok\"}"
    }]
  }' | jq '.'

echo -e "\n✓ If you see JSON above, OpenAI is working!"
echo "✗ If you see an error, check your API key and credits"
```

Run: `chmod +x test-openai.sh && ./test-openai.sh`

---

## Next Steps

1. Try the fixes above
2. Enable debug logging
3. Check the actual error message
4. If still failing, please share the debug logs

Most likely causes:
- ⏱️ **60% chance:** Timeout too short for OpenAI
- 🔑 **20% chance:** API key or credits issue
- 🚦 **15% chance:** Rate limiting
- 🐛 **5% chance:** Actual parsing bug

