# AutoHeal Cost Optimization Examples

## 🎯 **Execution Strategy Comparison**

| Strategy | Cost | Speed | Use Case |
|----------|------|-------|----------|
| `DOM_ONLY` | $0.02/request | Fast | Budget-conscious, DOM-heavy sites |
| `SMART_SEQUENTIAL` | $0.02-0.12/request | Medium | **Recommended default** |
| `SEQUENTIAL` | $0.02-0.12/request | Slow | Predictable costs |
| `VISUAL_FIRST` | $0.10-0.12/request | Medium | Visual-heavy sites |
| `PARALLEL` | $0.12/request | Fastest | Performance-critical, cost no object |

## 💰 **Cost Savings Examples**

### Example 1: Budget-Conscious Setup
```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .performance(PerformanceConfig.builder()
        .executionStrategy(ExecutionStrategy.DOM_ONLY)  // 83% cost savings!
        .build())
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .visualAnalysisEnabled(false)  // Disable visual entirely
        .build())
    .build();

// Cost: $0.02 per healing attempt (vs $0.12 parallel)
// Savings: $0.10 per attempt (83% reduction)
```

### Example 2: Sequential (Default)
```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .performance(PerformanceConfig.builder()
        .executionStrategy(ExecutionStrategy.SEQUENTIAL)  // Default
        .build())
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .visualAnalysisEnabled(true)
        .build())
    .build();

// Cost: $0.02 if DOM succeeds, $0.12 if visual needed
// Average cost: ~$0.04-0.06 per healing attempt
// Savings: ~50% vs parallel execution
```

### Example 3: Performance-Critical Setup
```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .performance(PerformanceConfig.builder()
        .executionStrategy(ExecutionStrategy.PARALLEL)  // Fastest
        .build())
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .visualAnalysisEnabled(true)
        .build())
    .build();

// Cost: $0.12 per healing attempt (both DOM + Visual)
// Speed: Fastest possible
// Use case: Critical production systems
```

## 📊 **Cost Monitoring**

```java
// Get cost metrics
ResilientAIService aiService = (ResilientAIService) autoHeal.getAIService();
CostMetrics costs = aiService.getCostMetrics();

System.out.println("Total AI cost: $" + costs.getTotalCost());
System.out.println("DOM requests: " + costs.getDomRequests() + " ($" + costs.getDomCost() + ")");
System.out.println("Visual requests: " + costs.getVisualRequests() + " ($" + costs.getVisualCost() + ")");
System.out.println("Cost savings vs parallel: $" + costs.getCostSavingsVsParallel());
System.out.println("Average cost per request: $" + costs.getAverageCostPerRequest());
```

## 🎛️ **Spring Boot Configuration**

```yaml
autoheal:
  performance:
    execution-strategy: SEQUENTIAL  # Cost-optimized default
    thread-pool-size: 4
    element-timeout: 10s
    
  ai:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    visual-analysis-enabled: true
    timeout: 30s

# Alternative configurations:

# Budget mode (DOM only)
autoheal:
  performance:
    execution-strategy: DOM_ONLY
  ai:
    visual-analysis-enabled: false

# Performance mode (parallel)
autoheal:
  performance:
    execution-strategy: PARALLEL
  ai:
    visual-analysis-enabled: true
```

## 📈 **Cost Analysis by Strategy**

### 1000 Healing Attempts Cost Comparison:

| Strategy | DOM Success Rate | Total Cost | Savings vs Parallel |
|----------|------------------|------------|---------------------|
| `DOM_ONLY` | 70% | $20 | $100 (83%) |
| `SMART_SEQUENTIAL` | 70% | $50 | $70 (58%) |
| `SEQUENTIAL` | 70% | $50 | $70 (58%) |
| `VISUAL_FIRST` | 70% | $110 | $10 (8%) |
| `PARALLEL` | 70% | $120 | $0 (0%) |

### Assumptions:
- DOM analysis succeeds 70% of the time
- DOM cost: $0.02 per request
- Visual cost: $0.10 per request
- 1000 healing attempts

## 🚀 **Recommendations**

### For Most Users (Default):
```java
.executionStrategy(ExecutionStrategy.SEQUENTIAL)
```
- **Best balance** of cost and reliability
- **50-60% cost savings** vs parallel
- DOM first (cheaper), visual as fallback

### For Budget-Conscious Users:
```java
.executionStrategy(ExecutionStrategy.DOM_ONLY)
```
- **83% cost savings**
- Still very effective for most websites
- Disable visual analysis entirely

### For Performance-Critical Systems:
```java
.executionStrategy(ExecutionStrategy.PARALLEL)
```
- **Fastest healing** (parallel execution)
- **Highest reliability** (multiple strategies)
- Higher cost but maximum success rate

### For Visual-Heavy Applications:
```java
.executionStrategy(ExecutionStrategy.VISUAL_FIRST)
```
- Visual analysis first
- Good for image-heavy or canvas-based apps
- Higher cost but better for visual elements