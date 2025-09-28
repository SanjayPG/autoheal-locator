# AutoHeal Reporting System

## Overview

AutoHeal automatically generates comprehensive reports of all healing activities, providing insights into test stability, AI usage, and element reliability. Reports are generated in **your test project** (not the AutoHeal source project).

---

## Report Configuration

### Basic Reporting Setup

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .reporting(ReportingConfig.builder()
        .enabled(true)                          // Enable reporting
        .generateHTML(true)                     // Interactive HTML reports
        .generateJSON(true)                     // Machine-readable data
        .generateText(true)                     // Human-readable summary
        .outputDirectory("./autoheal-reports")  // Output to your project
        .reportNamePrefix("MyApp_AutoHeal")     // Prefix for report files
        .consoleLogging(true)                   // Real-time console output
        .build())
    .build();
```

### Report Output Location

Reports are generated in **your test project directory**:

```
your-test-project/
├── src/test/java/
├── pom.xml
├── autoheal-reports/              ← Reports generated here
│   ├── MyApp_AutoHeal_2024-01-15_14-30-25.html
│   ├── MyApp_AutoHeal_2024-01-15_14-30-25.json
│   ├── MyApp_AutoHeal_2024-01-15_14-30-25.txt
│   └── screenshots/
│       ├── healing_001.png
│       └── healing_002.png
└── target/
```

---

## Sample Reports

### 1. HTML Report (Interactive Dashboard)

**File**: `autoheal-reports/MyApp_AutoHeal_2024-01-15_14-30-25.html`

```html
<!DOCTYPE html>
<html>
<head>
    <title>AutoHeal Test Report - MyApp</title>
    <style>
        .success { color: green; font-weight: bold; }
        .warning { color: orange; font-weight: bold; }
        .failure { color: red; font-weight: bold; }
        .metric-card { border: 1px solid #ddd; padding: 15px; margin: 10px; border-radius: 5px; }
    </style>
</head>
<body>
    <h1>🤖 AutoHeal Test Report</h1>
    <p><strong>Test Suite:</strong> MyApp Test Suite</p>
    <p><strong>Generated:</strong> 2024-01-15 14:30:25</p>
    <p><strong>Duration:</strong> 5 minutes 32 seconds</p>

    <h2>📊 Summary Metrics</h2>
    <div class="metric-card">
        <h3>Healing Success Rate</h3>
        <p class="success">92.3% (24/26 elements healed successfully)</p>
    </div>

    <div class="metric-card">
        <h3>AI Usage</h3>
        <p>DOM Analysis: 18 requests (avg: 1.2s)</p>
        <p>Visual Analysis: 6 requests (avg: 3.4s)</p>
        <p>Total Tokens Used: 15,420</p>
        <p>Estimated Cost: $0.23</p>
    </div>

    <div class="metric-card">
        <h3>Cache Performance</h3>
        <p class="success">Cache Hit Rate: 78.5% (51/65 requests)</p>
        <p>Cache Saved: $1.45 in AI costs</p>
    </div>

    <h2>🔍 Detailed Healing Activities</h2>
    <table border="1" style="width: 100%; border-collapse: collapse;">
        <thead>
            <tr>
                <th>Test Method</th>
                <th>Element Description</th>
                <th>Original Selector</th>
                <th>Status</th>
                <th>Healing Strategy</th>
                <th>New Selector</th>
                <th>Confidence</th>
                <th>Duration</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>testUserLogin()</td>
                <td>username input field on login page</td>
                <td>#username</td>
                <td><span class="success">✅ SUCCESS</span></td>
                <td>Original Selector</td>
                <td>#username</td>
                <td>100%</td>
                <td>45ms</td>
            </tr>
            <tr>
                <td>testUserLogin()</td>
                <td>login submit button</td>
                <td>#login-btn</td>
                <td><span class="warning">🔄 HEALED</span></td>
                <td>DOM Analysis</td>
                <td>button[type='submit'].btn-primary</td>
                <td>95.2%</td>
                <td>1,234ms</td>
            </tr>
            <tr>
                <td>testProductSearch()</td>
                <td>search results container</td>
                <td>.results-grid</td>
                <td><span class="warning">🔄 HEALED</span></td>
                <td>Visual Analysis</td>
                <td>.search-results-container</td>
                <td>89.7%</td>
                <td>3,456ms</td>
            </tr>
            <tr>
                <td>testCheckout()</td>
                <td>payment button</td>
                <td>#pay-now</td>
                <td><span class="failure">❌ FAILED</span></td>
                <td>All Strategies Attempted</td>
                <td>N/A</td>
                <td>0%</td>
                <td>8,901ms</td>
            </tr>
        </tbody>
    </table>

    <h2>📈 Performance Trends</h2>
    <div class="metric-card">
        <h3>Most Problematic Selectors</h3>
        <ul>
            <li><strong>#dynamic-content</strong> - Failed 3 times in different tests</li>
            <li><strong>.modal-close</strong> - Required healing 2 times (Visual Analysis needed)</li>
        </ul>
    </div>

    <h2>💡 Recommendations</h2>
    <div class="metric-card">
        <ul>
            <li>✅ <strong>Great cache performance</strong> - 78.5% hit rate saving costs</li>
            <li>⚠️ <strong>Consider data-testid attributes</strong> for #dynamic-content selector</li>
            <li>⚠️ <strong>Payment button needs attention</strong> - consistently failing</li>
            <li>💰 <strong>Cost optimization</strong> - Visual analysis used 26% of budget, consider DOM-only for some tests</li>
        </ul>
    </div>
</body>
</html>
```

### 2. JSON Report (Machine Readable)

**File**: `autoheal-reports/MyApp_AutoHeal_2024-01-15_14-30-25.json`

```json
{
  "reportMetadata": {
    "generatedAt": "2024-01-15T14:30:25Z",
    "testSuite": "MyApp Test Suite",
    "autoHealVersion": "1.0-SNAPSHOT",
    "duration": "PT5M32S",
    "totalTests": 8
  },
  "summary": {
    "totalElements": 26,
    "successfulHealing": 24,
    "failedHealing": 2,
    "healingSuccessRate": 92.3,
    "cacheHitRate": 78.5,
    "totalCost": 0.23,
    "savedCost": 1.45
  },
  "aiUsage": {
    "domAnalysisRequests": 18,
    "visualAnalysisRequests": 6,
    "totalTokensUsed": 15420,
    "avgDomResponseTime": 1200,
    "avgVisualResponseTime": 3400
  },
  "healingActivities": [
    {
      "testMethod": "testUserLogin()",
      "elementDescription": "username input field on login page",
      "originalSelector": "#username",
      "status": "SUCCESS",
      "strategy": "ORIGINAL_SELECTOR",
      "newSelector": "#username",
      "confidence": 100.0,
      "duration": 45,
      "timestamp": "2024-01-15T14:25:12Z"
    },
    {
      "testMethod": "testUserLogin()",
      "elementDescription": "login submit button",
      "originalSelector": "#login-btn",
      "status": "HEALED",
      "strategy": "DOM_ANALYSIS",
      "newSelector": "button[type='submit'].btn-primary",
      "confidence": 95.2,
      "duration": 1234,
      "timestamp": "2024-01-15T14:25:13Z",
      "aiReasoning": "Found button with matching text 'Sign In' and submit type",
      "alternatives": ["#submit-btn", ".login-button"]
    },
    {
      "testMethod": "testProductSearch()",
      "elementDescription": "search results container",
      "originalSelector": ".results-grid",
      "status": "HEALED",
      "strategy": "VISUAL_ANALYSIS",
      "newSelector": ".search-results-container",
      "confidence": 89.7,
      "duration": 3456,
      "timestamp": "2024-01-15T14:26:45Z",
      "aiReasoning": "Visual analysis identified grid layout with product cards",
      "screenshot": "screenshots/healing_001.png"
    },
    {
      "testMethod": "testCheckout()",
      "elementDescription": "payment button",
      "originalSelector": "#pay-now",
      "status": "FAILED",
      "strategy": "ALL_STRATEGIES_FAILED",
      "confidence": 0.0,
      "duration": 8901,
      "timestamp": "2024-01-15T14:28:30Z",
      "error": "Element not found after trying all healing strategies",
      "attemptedStrategies": ["CACHE", "DOM_ANALYSIS", "VISUAL_ANALYSIS"]
    }
  ],
  "recommendations": [
    {
      "type": "SELECTOR_IMPROVEMENT",
      "message": "Consider using data-testid attributes for #dynamic-content",
      "severity": "MEDIUM",
      "affectedElements": ["#dynamic-content", ".modal-close"]
    },
    {
      "type": "COST_OPTIMIZATION",
      "message": "Visual analysis used 26% of AI budget, consider DOM-only strategy",
      "severity": "LOW",
      "potentialSaving": "$0.15"
    }
  ]
}
```

### 3. Text Report (Human Readable Summary)

**File**: `autoheal-reports/MyApp_AutoHeal_2024-01-15_14-30-25.txt`

```
================================================================================
                            🤖 AutoHeal Test Report
================================================================================
Test Suite: MyApp Test Suite
Generated:  2024-01-15 14:30:25
Duration:   5 minutes 32 seconds
AutoHeal:   v1.0-SNAPSHOT

================================================================================
                                📊 SUMMARY
================================================================================
✅ Total Tests Executed:      8
✅ Elements Located:          26
✅ Successful Healing:        24  (92.3%)
❌ Failed Healing:             2  (7.7%)
🔄 Cache Hit Rate:            78.5% (51/65 requests)
💰 Total AI Cost:            $0.23
💰 Cache Saved:              $1.45

================================================================================
                            🔍 HEALING BREAKDOWN
================================================================================
Strategy             | Count | Avg Time | Success Rate
---------------------|-------|----------|-------------
Original Selector    |   14  |   52ms   |    100%
Cache Hit            |   51  |   12ms   |    100%
DOM Analysis         |   18  |  1.2s    |    94.4%
Visual Analysis      |    6  |  3.4s    |    83.3%
All Failed           |    2  |  8.9s    |      0%

================================================================================
                            ⚡ PERFORMANCE METRICS
================================================================================
🚀 Fastest Healing:          45ms  (#username - Original Selector)
🐌 Slowest Healing:          8.9s  (#pay-now - All Strategies Failed)
📈 Average Healing Time:     1.8s
🧠 AI Token Usage:          15,420 tokens
💰 Cost per Token:          $0.000015

================================================================================
                            🔧 DETAILED ACTIVITIES
================================================================================
[14:25:12] testUserLogin() → username input field on login page
           ✅ SUCCESS | #username | Original Selector | 45ms

[14:25:13] testUserLogin() → login submit button
           🔄 HEALED | #login-btn → button[type='submit'].btn-primary
           Strategy: DOM Analysis | Confidence: 95.2% | 1,234ms
           Reasoning: Found button with matching text 'Sign In' and submit type

[14:26:45] testProductSearch() → search results container
           🔄 HEALED | .results-grid → .search-results-container
           Strategy: Visual Analysis | Confidence: 89.7% | 3,456ms
           Reasoning: Visual analysis identified grid layout with product cards

[14:28:30] testCheckout() → payment button
           ❌ FAILED | #pay-now | All Strategies Failed | 8,901ms
           Error: Element not found after trying all healing strategies

================================================================================
                            💡 RECOMMENDATIONS
================================================================================
⚠️  ATTENTION NEEDED:
     • #pay-now selector failed in testCheckout() - investigate payment flow
     • #dynamic-content failed 3 times across tests

✅  WORKING WELL:
     • Cache performance excellent at 78.5% hit rate
     • DOM Analysis strategy very reliable at 94.4% success

🔧  IMPROVEMENTS:
     • Consider data-testid attributes for dynamic content selectors
     • Visual Analysis used 26% of AI budget - evaluate if DOM-only sufficient

💰  COST OPTIMIZATION:
     • Current spend: $0.23 per test run
     • Cache saved: $1.45 (86% cost reduction)
     • Potential saving with DOM-only strategy: $0.15

================================================================================
                            📁 GENERATED FILES
================================================================================
📄 HTML Report:     autoheal-reports/MyApp_AutoHeal_2024-01-15_14-30-25.html
📊 JSON Data:       autoheal-reports/MyApp_AutoHeal_2024-01-15_14-30-25.json
📝 Text Summary:    autoheal-reports/MyApp_AutoHeal_2024-01-15_14-30-25.txt
🖼️  Screenshots:    autoheal-reports/screenshots/ (2 files)

================================================================================
```

---

## Enabling Reporting in Your Tests

### Maven/TestNG Example

```java
@BeforeClass
public void setUpAutoHeal() {
    AutoHealConfiguration config = AutoHealConfiguration.builder()
        .ai(AIConfig.builder()
            .provider(AIProvider.OPENAI)
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .build())
        .reporting(ReportingConfig.builder()
            .enabled(true)
            .outputDirectory("./test-reports/autoheal")  // In your project
            .generateHTML(true)
            .reportNamePrefix("MyApp_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
            .build())
        .build();

    autoHeal = AutoHealLocator.builder()
        .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
        .withConfiguration(config)
        .build();
}

@AfterClass
public void tearDownAutoHeal() {
    if (autoHeal != null) {
        autoHeal.shutdown(); // 🎯 This generates the reports in YOUR project
    }
}
```

### JUnit 5 Example

```java
@AfterEach
void generateReports() {
    autoHeal.shutdown(); // Reports generated in test/reports/autoheal/

    // Reports are created in YOUR project directory:
    // your-project/test/reports/autoheal/YourApp_AutoHeal_2024-01-15.html
}
```

### Cucumber Integration

```java
@After
public void tearDown(Scenario scenario) {
    if (autoHeal != null) {
        // Reports include Cucumber scenario information
        autoHeal.shutdown();

        // Generated in: features/reports/autoheal/
        System.out.println("AutoHeal report generated for scenario: " + scenario.getName());
    }
}
```

---

## Report Integration

### CI/CD Pipeline Integration

```yaml
# GitHub Actions example
- name: Generate Test Reports
  run: mvn test

- name: Archive AutoHeal Reports
  uses: actions/upload-artifact@v3
  with:
    name: autoheal-reports
    path: test-reports/autoheal/

- name: Publish HTML Report
  uses: peaceiris/actions-gh-pages@v3
  with:
    github_token: ${{ secrets.GITHUB_TOKEN }}
    publish_dir: test-reports/autoheal/
```

### Email Reports

```java
// Send reports via email after test execution
@AfterSuite
public void emailReports() {
    File htmlReport = new File("./autoheal-reports/latest.html");
    EmailUtils.sendReport("team@company.com", "AutoHeal Test Report", htmlReport);
}
```

---

## Best Practices

1. **📁 Organize Reports**: Use meaningful prefixes and directories
2. **🔄 Archive Old Reports**: Set up rotation to avoid disk space issues
3. **📊 Monitor Trends**: Track healing success rates over time
4. **💰 Cost Tracking**: Monitor AI usage and optimize based on reports
5. **🚨 Set Alerts**: Alert on high failure rates or cost spikes

---

## Next Steps

1. [Performance Optimization](./performance.md) - Use report insights to optimize
2. [Troubleshooting Guide](./troubleshooting.md) - Debug issues found in reports
3. [AI Configuration](./ai-configuration.md) - Adjust AI settings based on usage