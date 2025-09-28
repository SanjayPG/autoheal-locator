package com.autoheal.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AutoHeal Reporter - Tracks and reports all selector usage and healing strategies
 */
public class AutoHealReporter {

    private final List<SelectorReport> reports = new ArrayList<>();
    private final String testRunId;
    private final LocalDateTime startTime;

    // AI Configuration details
    private final String aiProvider;
    private final String aiModel;
    private final String apiEndpoint;
    private final double domTemperature;
    private final double visualTemperature;
    private final int domMaxTokens;
    private final int visualMaxTokens;
    private final int maxRetries;

    public AutoHealReporter() {
        this.testRunId = "AutoHeal_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        this.startTime = LocalDateTime.now();

        // Default values when no configuration is provided
        this.aiProvider = "OpenAI";
        this.aiModel = "gpt-4o-mini";
        this.apiEndpoint = "https://api.openai.com/v1/chat/completions";
        this.domTemperature = 0.1;
        this.visualTemperature = 0.0;
        this.domMaxTokens = 500;
        this.visualMaxTokens = 1000;
        this.maxRetries = 3;
    }

    public AutoHealReporter(com.autoheal.config.AIConfig aiConfig) {
        this.testRunId = "AutoHeal_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        this.startTime = LocalDateTime.now();

        // Read from actual configuration - now supports user-specified models
        this.aiProvider = aiConfig.getProvider().toString();
        this.aiModel = aiConfig.getModel(); // Use configured model (user choice or smart default)
        this.apiEndpoint = aiConfig.getApiUrl();
        this.domTemperature = aiConfig.getTemperatureDOM(); // Read from configuration
        this.visualTemperature = aiConfig.getTemperatureVisual(); // Read from configuration
        this.domMaxTokens = aiConfig.getMaxTokensDOM(); // Read from configuration
        this.visualMaxTokens = aiConfig.getMaxTokensVisual(); // Read from configuration
        this.maxRetries = aiConfig.getMaxRetries();
    }

    /**
     * Record a selector usage event
     */
    public void recordSelectorUsage(String originalSelector, String description,
                                  SelectorStrategy strategy, long executionTimeMs,
                                  boolean success, String actualSelector,
                                  String elementDetails, String reasoning) {
        recordSelectorUsage(originalSelector, description, strategy, executionTimeMs,
                           success, actualSelector, elementDetails, reasoning, 0);
    }

    /**
     * Record a selector usage event with token usage information
     */
    public void recordSelectorUsage(String originalSelector, String description,
                                  SelectorStrategy strategy, long executionTimeMs,
                                  boolean success, String actualSelector,
                                  String elementDetails, String reasoning, long tokensUsed) {
        SelectorReport report = new SelectorReport();
        report.originalSelector = originalSelector;
        report.description = description;
        report.strategy = strategy;
        report.executionTimeMs = executionTimeMs;
        report.success = success;
        report.actualSelector = actualSelector;
        report.elementDetails = elementDetails;
        report.reasoning = reasoning;
        report.tokensUsed = tokensUsed;
        report.timestamp = LocalDateTime.now();

        // Set AI implementation details for AI-based strategies from configuration
        if (strategy == SelectorStrategy.DOM_ANALYSIS || strategy == SelectorStrategy.VISUAL_ANALYSIS) {
            report.aiProvider = this.aiProvider;
            report.aiModel = this.aiModel;
            report.apiEndpoint = this.apiEndpoint;
            report.maxTokens = strategy == SelectorStrategy.DOM_ANALYSIS ? this.domMaxTokens : this.visualMaxTokens;
            report.temperature = strategy == SelectorStrategy.DOM_ANALYSIS ? this.domTemperature : this.visualTemperature;
            report.retryCount = this.maxRetries;
            report.promptType = strategy == SelectorStrategy.DOM_ANALYSIS ? "DOM Analysis" : "Visual Analysis";
            // Token breakdown will be updated later if available
            report.promptTokens = 0;
            report.completionTokens = 0;
        }

        reports.add(report);

        // Also log to console for immediate visibility
        logToConsole(report);
    }

    private void logToConsole(SelectorReport report) {
        String icon = getStrategyIcon(report.strategy);
        String status = report.success ? "SUCCESS" : "FAILED";
        String statusIcon = report.success ? "[SUCCESS]" : "[FAILED]";

        // Include token usage if available and strategy uses AI
        String tokenInfo = "";
        if (report.tokensUsed > 0 && (report.strategy == SelectorStrategy.DOM_ANALYSIS || report.strategy == SelectorStrategy.VISUAL_ANALYSIS)) {
            tokenInfo = String.format(" [%d tokens]", report.tokensUsed);
        }

        System.out.printf("%s %s [%dms]%s %s → %s%n",
            statusIcon, icon, report.executionTimeMs, tokenInfo,
            report.originalSelector,
            report.success ? report.actualSelector : "FAILED");

        if (!report.originalSelector.equals(report.actualSelector) && report.success) {
            System.out.println("   [HEALED] " + report.reasoning);
        }
    }

    /**
     * Generate comprehensive HTML report
     */
    public void generateHTMLReport() {
        try {
            String filename = testRunId + "_AutoHeal_Report.html";
            File file = new File(filename);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(generateHTMLContent());
            }

            System.out.println("HTML Report generated: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to generate HTML report: " + e.getMessage());
        }
    }

    /**
     * Generate JSON report for programmatic consumption
     */
    public void generateJSONReport() {
        try {
            String filename = testRunId + "_AutoHeal_Report.json";
            File file = new File(filename);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();

            root.put("testRunId", testRunId);
            root.put("startTime", startTime.toString());
            root.put("endTime", LocalDateTime.now().toString());
            root.put("totalSelectors", reports.size());

            // Statistics
            ObjectNode stats = mapper.createObjectNode();
            long successful = reports.stream().mapToLong(r -> r.success ? 1 : 0).sum();
            long originalStrategy = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.ORIGINAL_SELECTOR ? 1 : 0).sum();
            long domHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS ? 1 : 0).sum();
            long visualHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS ? 1 : 0).sum();
            long cached = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.CACHED ? 1 : 0).sum();

            stats.put("successful", successful);
            stats.put("failed", reports.size() - successful);
            stats.put("originalSelector", originalStrategy);
            stats.put("domHealed", domHealed);
            stats.put("visualHealed", visualHealed);
            stats.put("cached", cached);
            stats.put("successRate", (double)successful / reports.size() * 100);

            root.set("statistics", stats);

            // AI Implementation Details
            boolean hasAIStrategies = reports.stream().anyMatch(r ->
                r.strategy == SelectorStrategy.DOM_ANALYSIS || r.strategy == SelectorStrategy.VISUAL_ANALYSIS);

            if (hasAIStrategies) {
                ObjectNode aiDetails = mapper.createObjectNode();

                // Get first AI report for configuration details
                SelectorReport aiReport = reports.stream()
                    .filter(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS || r.strategy == SelectorStrategy.VISUAL_ANALYSIS)
                    .findFirst()
                    .orElse(null);

                if (aiReport != null) {
                    // Configuration
                    ObjectNode config = mapper.createObjectNode();
                    config.put("provider", aiReport.aiProvider);
                    config.put("model", aiReport.aiModel);
                    config.put("apiEndpoint", aiReport.apiEndpoint);
                    config.put("maxTokensDOM", this.domMaxTokens);
                    config.put("maxTokensVisual", this.visualMaxTokens);
                    config.put("temperatureDOM", this.domTemperature);
                    config.put("temperatureVisual", this.visualTemperature);
                    config.put("maxRetries", aiReport.retryCount);
                    aiDetails.set("configuration", config);

                    // Usage Statistics
                    ObjectNode usage = mapper.createObjectNode();
                    usage.put("domAnalysisRequests", domHealed);
                    usage.put("visualAnalysisRequests", visualHealed);

                    // Token usage
                    long totalTokens = reports.stream().mapToLong(r -> r.tokensUsed).sum();
                    long domTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();
                    long visualTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();

                    usage.put("totalTokens", totalTokens);
                    usage.put("domTokens", domTokens);
                    usage.put("visualTokens", visualTokens);

                    if (totalTokens > 0) {
                        double estimatedCost = (totalTokens * 0.375) / 1000000.0;
                        usage.put("estimatedCostUSD", Double.parseDouble(String.format("%.4f", estimatedCost)));
                    }

                    aiDetails.set("usage", usage);
                }

                root.set("aiImplementation", aiDetails);
            }

            // Detailed reports
            ArrayNode reportsArray = mapper.createArrayNode();
            for (SelectorReport report : reports) {
                ObjectNode reportNode = mapper.createObjectNode();
                reportNode.put("originalSelector", report.originalSelector);
                reportNode.put("actualSelector", report.actualSelector);
                reportNode.put("description", report.description);
                reportNode.put("strategy", report.strategy.toString());
                reportNode.put("executionTimeMs", report.executionTimeMs);
                reportNode.put("success", report.success);
                reportNode.put("elementDetails", report.elementDetails);
                reportNode.put("tokensUsed", report.tokensUsed);
                reportNode.put("reasoning", report.reasoning);
                reportNode.put("timestamp", report.timestamp.toString());

                // Add AI implementation details for AI-based strategies
                if (report.strategy == SelectorStrategy.DOM_ANALYSIS || report.strategy == SelectorStrategy.VISUAL_ANALYSIS) {
                    ObjectNode aiInfo = mapper.createObjectNode();
                    aiInfo.put("provider", report.aiProvider);
                    aiInfo.put("model", report.aiModel);
                    aiInfo.put("promptType", report.promptType);
                    aiInfo.put("temperature", report.temperature);
                    aiInfo.put("maxTokens", report.maxTokens);
                    reportNode.set("aiImplementation", aiInfo);
                }
                reportsArray.add(reportNode);
            }
            root.set("selectorReports", reportsArray);

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            System.out.println("JSON Report generated: " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to generate JSON report: " + e.getMessage());
        }
    }

    /**
     * Generate text report for easy reading
     */
    public void generateTextReport() {
        try {
            String filename = testRunId + "_AutoHeal_Report.txt";
            File file = new File(filename);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(generateTextContent());
            }

            System.out.println("Text Report generated: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to generate text report: " + e.getMessage());
        }
    }

    private String generateHTMLContent() {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<title>AutoHeal Test Report - ").append(testRunId).append("</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
        html.append(".container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }");
        html.append(".stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0; }");
        html.append(".stat-box { background: #ecf0f1; padding: 15px; border-radius: 5px; text-align: center; }");
        html.append(".stat-value { font-size: 2em; font-weight: bold; color: #2980b9; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }");
        html.append("th { background-color: #34495e; color: white; }");
        html.append("tr:nth-child(even) { background-color: #f2f2f2; }");
        html.append(".original { background-color: #d5edd0 !important; }");
        html.append(".dom-healed { background-color: #fff2cc !important; }");
        html.append(".visual-healed { background-color: #ffe6e6 !important; }");
        html.append(".cached { background-color: #e1f5fe !important; }");
        html.append(".failed { background-color: #ffebee !important; }");
        html.append(".success { color: #27ae60; font-weight: bold; }");
        html.append(".failure { color: #e74c3c; font-weight: bold; }");
        // Filter section styles
        html.append(".filter-section { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #3498db; }");
        html.append(".filter-section h3 { margin-top: 0; color: #2c3e50; }");
        html.append(".filters { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 15px 0; }");
        html.append(".filter-group { }");
        html.append(".filter-group label { display: block; margin-bottom: 5px; font-weight: 600; color: #2c3e50; }");
        html.append(".filter-select { width: 100%; padding: 8px 12px; border: 2px solid #bdc3c7; border-radius: 4px; background: white; }");
        html.append(".filter-select:focus { border-color: #3498db; outline: none; }");
        html.append(".filter-stats { text-align: center; margin: 15px 0; padding: 10px; background: #ecf0f1; border-radius: 4px; }");
        html.append(".reset-btn { background: #e74c3c; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; font-weight: 600; }");
        html.append(".reset-btn:hover { background: #c0392b; }");
        html.append(".search-box { width: 100%; padding: 8px 12px; border: 2px solid #bdc3c7; border-radius: 4px; }");
        html.append(".search-box:focus { border-color: #3498db; outline: none; }");
        html.append("</style>");
        html.append("</head><body>");

        html.append("<div class='container'>");
        html.append("<h1>[SEARCH] AutoHeal Test Report</h1>");
        html.append("<p><strong>Test Run:</strong> ").append(testRunId).append("</p>");
        html.append("<p><strong>Generated:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");

        // Statistics
        long successful = reports.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        long originalStrategy = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.ORIGINAL_SELECTOR ? 1 : 0).sum();
        long domHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS ? 1 : 0).sum();
        long visualHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS ? 1 : 0).sum();
        long cached = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.CACHED ? 1 : 0).sum();

        html.append("<div class='stats'>");
        html.append("<div class='stat-box'><div class='stat-value'>").append(reports.size()).append("</div><div>Total Selectors</div></div>");
        html.append("<div class='stat-box'><div class='stat-value'>").append(successful).append("</div><div>Successful</div></div>");
        html.append("<div class='stat-box'><div class='stat-value'>").append(originalStrategy).append("</div><div>Original Selectors</div></div>");
        html.append("<div class='stat-box'><div class='stat-value'>").append(domHealed).append("</div><div>DOM Healed</div></div>");
        html.append("<div class='stat-box'><div class='stat-value'>").append(visualHealed).append("</div><div>Visual Healed</div></div>");
        html.append("<div class='stat-box'><div class='stat-value'>").append(cached).append("</div><div>Cached Results</div></div>");
        html.append("</div>");

        // Filter Section
        html.append("<div class='filter-section'>");
        html.append("<h3>Filter Results</h3>");
        html.append("<div class='filters'>");
        html.append("  <div class='filter-group'>");
        html.append("    <label for='strategyFilter'>Strategy:</label>");
        html.append("    <select id='strategyFilter' class='filter-select'>");
        html.append("      <option value=''>All Strategies</option>");
        html.append("    </select>");
        html.append("  </div>");
        html.append("  <div class='filter-group'>");
        html.append("    <label for='statusFilter'>Status:</label>");
        html.append("    <select id='statusFilter' class='filter-select'>");
        html.append("      <option value=''>All Status</option>");
        html.append("    </select>");
        html.append("  </div>");
        html.append("  <div class='filter-group'>");
        html.append("    <label for='performanceFilter'>Performance:</label>");
        html.append("    <select id='performanceFilter' class='filter-select'>");
        html.append("      <option value=''>All Performance</option>");
        html.append("    </select>");
        html.append("  </div>");
        html.append("  <div class='filter-group'>");
        html.append("    <label for='searchBox'>Search:</label>");
        html.append("    <input type='text' id='searchBox' class='search-box' placeholder='Search all columns...'>");
        html.append("  </div>");
        html.append("</div>");
        html.append("<div class='filter-stats'>");
        html.append("  <span id='resultCount'>Showing ").append(reports.size()).append(" of ").append(reports.size()).append(" results</span>");
        html.append("  <button id='resetFilters' class='reset-btn'>Reset Filters</button>");
        html.append("</div>");
        html.append("</div>");

        // AI Implementation Details section
        boolean hasAIStrategies = reports.stream().anyMatch(r ->
            r.strategy == SelectorStrategy.DOM_ANALYSIS || r.strategy == SelectorStrategy.VISUAL_ANALYSIS);

        if (hasAIStrategies) {
            html.append("<h2>[AI] AI Implementation Details</h2>");
            html.append("<div style='background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>");

            // Get first AI report for configuration details
            SelectorReport aiReport = reports.stream()
                .filter(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS || r.strategy == SelectorStrategy.VISUAL_ANALYSIS)
                .findFirst()
                .orElse(null);

            if (aiReport != null) {
                html.append("<div style='display: grid; grid-template-columns: 1fr 1fr; gap: 20px;'>");

                // Configuration Details
                html.append("<div>");
                html.append("<h3>Configuration</h3>");
                html.append("<ul>");
                html.append("<li><strong>Provider:</strong> ").append(aiReport.aiProvider).append("</li>");
                html.append("<li><strong>Model:</strong> ").append(aiReport.aiModel).append("</li>");
                html.append("<li><strong>API Endpoint:</strong> ").append(aiReport.apiEndpoint).append("</li>");
                html.append("<li><strong>Max Tokens:</strong> ").append(this.domMaxTokens).append(" (DOM), ").append(this.visualMaxTokens).append(" (Visual)</li>");
                html.append("<li><strong>Temperature:</strong> ").append(this.domTemperature).append(" (DOM), ").append(this.visualTemperature).append(" (Visual)</li>");
                html.append("<li><strong>Max Retries:</strong> ").append(aiReport.retryCount).append("</li>");
                html.append("</ul>");
                html.append("</div>");

                // Statistics
                html.append("<div>");
                html.append("<h3>AI Usage Statistics</h3>");
                html.append("<ul>");
                html.append("<li><strong>DOM Analysis Requests:</strong> ").append(domHealed).append("</li>");
                html.append("<li><strong>Visual Analysis Requests:</strong> ").append(visualHealed).append("</li>");

                // Token usage statistics
                long totalTokens = reports.stream().mapToLong(r -> r.tokensUsed).sum();
                long domTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();
                long visualTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();

                html.append("<li><strong>Total Tokens:</strong> ").append(totalTokens).append("</li>");
                html.append("<li><strong>DOM Tokens:</strong> ").append(domTokens).append("</li>");
                html.append("<li><strong>Visual Tokens:</strong> ").append(visualTokens).append("</li>");

                // Cost estimation (GPT-4o-mini pricing: $0.15/1M input, $0.60/1M output)
                if (totalTokens > 0) {
                    double estimatedCost = (totalTokens * 0.375) / 1000000.0; // Average of input/output pricing
                    html.append("<li><strong>Estimated Cost:</strong> $").append(String.format("%.4f", estimatedCost)).append("</li>");
                }

                html.append("</ul>");
                html.append("</div>");
                html.append("</div>");
            }
            html.append("</div>");
        }

        // Detailed table
        html.append("<h2>[REPORT] Detailed Selector Report</h2>");
        html.append("<table id='reportTable'>");
        html.append("<tr><th>Original Selector</th><th>Strategy</th><th>Time (ms)</th><th>Status</th><th>Actual Selector</th><th>Element</th><th>Tokens</th><th>Reasoning</th></tr>");

        for (SelectorReport report : reports) {
            String rowClass = getRowClass(report.strategy, report.success);
            String statusClass = report.success ? "success" : "failure";
            String status = report.success ? "[SUCCESS] SUCCESS" : "[FAILED] FAILED";

            html.append("<tr class='").append(rowClass).append("'>");
            html.append("<td><code>").append(report.originalSelector).append("</code></td>");
            html.append("<td>").append(getStrategyIcon(report.strategy)).append(" ").append(report.strategy.getDisplayName()).append("</td>");
            html.append("<td>").append(report.executionTimeMs).append("</td>");
            html.append("<td class='").append(statusClass).append("'>").append(status).append("</td>");
            html.append("<td><code>").append(report.actualSelector != null ? report.actualSelector : "-").append("</code></td>");
            html.append("<td>").append(report.elementDetails != null ? report.elementDetails : "-").append("</td>");

            // Add tokens column - show tokens only for AI strategies
            if (report.tokensUsed > 0 && (report.strategy == SelectorStrategy.DOM_ANALYSIS || report.strategy == SelectorStrategy.VISUAL_ANALYSIS)) {
                html.append("<td>").append(report.tokensUsed).append("</td>");
            } else {
                html.append("<td>-</td>");
            }

            html.append("<td>").append(report.reasoning != null ? report.reasoning : "-").append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");

        // Add JavaScript for filtering functionality
        html.append("<script>");
        html.append("// Filter functionality");
        html.append("document.addEventListener('DOMContentLoaded', function() {");
        html.append("    const table = document.getElementById('reportTable');");
        html.append("    const rows = Array.from(table.querySelectorAll('tr')).slice(1); // Skip header");
        html.append("    const strategyFilter = document.getElementById('strategyFilter');");
        html.append("    const statusFilter = document.getElementById('statusFilter');");
        html.append("    const performanceFilter = document.getElementById('performanceFilter');");
        html.append("    const searchBox = document.getElementById('searchBox');");
        html.append("    const resetBtn = document.getElementById('resetFilters');");
        html.append("    const resultCount = document.getElementById('resultCount');");
        html.append("");
        html.append("    // Populate filter options dynamically");
        html.append("    const strategies = new Set();");
        html.append("    const statuses = new Set();");
        html.append("    const performances = new Set();");
        html.append("");
        html.append("    rows.forEach(row => {");
        html.append("        const cells = row.querySelectorAll('td');");
        html.append("        if (cells.length >= 4) {");
        html.append("            strategies.add(cells[1].textContent.trim());");
        html.append("            statuses.add(cells[3].textContent.trim());");
        html.append("            const time = parseInt(cells[2].textContent.trim());");
        html.append("            if (time < 100) performances.add('Fast (<100ms)');");
        html.append("            else if (time < 500) performances.add('Medium (100-500ms)');");
        html.append("            else performances.add('Slow (>500ms)');");
        html.append("        }");
        html.append("    });");
        html.append("");
        html.append("    // Add options to dropdowns");
        html.append("    strategies.forEach(s => {");
        html.append("        const opt = document.createElement('option');");
        html.append("        opt.value = s; opt.textContent = s;");
        html.append("        strategyFilter.appendChild(opt);");
        html.append("    });");
        html.append("    statuses.forEach(s => {");
        html.append("        const opt = document.createElement('option');");
        html.append("        opt.value = s; opt.textContent = s;");
        html.append("        statusFilter.appendChild(opt);");
        html.append("    });");
        html.append("    performances.forEach(p => {");
        html.append("        const opt = document.createElement('option');");
        html.append("        opt.value = p; opt.textContent = p;");
        html.append("        performanceFilter.appendChild(opt);");
        html.append("    });");
        html.append("");
        html.append("    // Filter function");
        html.append("    function applyFilters() {");
        html.append("        const strategyValue = strategyFilter.value;");
        html.append("        const statusValue = statusFilter.value;");
        html.append("        const performanceValue = performanceFilter.value;");
        html.append("        const searchValue = searchBox.value.toLowerCase();");
        html.append("        let visibleCount = 0;");
        html.append("");
        html.append("        rows.forEach(row => {");
        html.append("            const cells = row.querySelectorAll('td');");
        html.append("            let show = true;");
        html.append("");
        html.append("            if (cells.length >= 4) {");
        html.append("                // Strategy filter");
        html.append("                if (strategyValue && cells[1].textContent.trim() !== strategyValue) {");
        html.append("                    show = false;");
        html.append("                }");
        html.append("                // Status filter");
        html.append("                if (statusValue && cells[3].textContent.trim() !== statusValue) {");
        html.append("                    show = false;");
        html.append("                }");
        html.append("                // Performance filter");
        html.append("                if (performanceValue) {");
        html.append("                    const time = parseInt(cells[2].textContent.trim());");
        html.append("                    let perfCategory = '';");
        html.append("                    if (time < 100) perfCategory = 'Fast (<100ms)';");
        html.append("                    else if (time < 500) perfCategory = 'Medium (100-500ms)';");
        html.append("                    else perfCategory = 'Slow (>500ms)';");
        html.append("                    if (perfCategory !== performanceValue) show = false;");
        html.append("                }");
        html.append("                // Search filter");
        html.append("                if (searchValue) {");
        html.append("                    const rowText = Array.from(cells).map(c => c.textContent.toLowerCase()).join(' ');");
        html.append("                    if (!rowText.includes(searchValue)) show = false;");
        html.append("                }");
        html.append("            }");
        html.append("");
        html.append("            row.style.display = show ? '' : 'none';");
        html.append("            if (show) visibleCount++;");
        html.append("        });");
        html.append("");
        html.append("        resultCount.textContent = `Showing ${visibleCount} of ${rows.length} results`;");
        html.append("    }");
        html.append("");
        html.append("    // Reset function");
        html.append("    function resetFilters() {");
        html.append("        strategyFilter.value = '';");
        html.append("        statusFilter.value = '';");
        html.append("        performanceFilter.value = '';");
        html.append("        searchBox.value = '';");
        html.append("        applyFilters();");
        html.append("    }");
        html.append("");
        html.append("    // Event listeners");
        html.append("    strategyFilter.addEventListener('change', applyFilters);");
        html.append("    statusFilter.addEventListener('change', applyFilters);");
        html.append("    performanceFilter.addEventListener('change', applyFilters);");
        html.append("    searchBox.addEventListener('input', applyFilters);");
        html.append("    resetBtn.addEventListener('click', resetFilters);");
        html.append("");
        html.append("    // Initial count");
        html.append("    resultCount.textContent = `Showing ${rows.length} of ${rows.length} results`;");
        html.append("});");
        html.append("</script>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private String generateTextContent() {
        StringBuilder text = new StringBuilder();

        text.append("===============================================\n");
        text.append("         AutoHeal Test Report\n");
        text.append("===============================================\n");
        text.append("Test Run ID: ").append(testRunId).append("\n");
        text.append("Start Time: ").append(startTime).append("\n");
        text.append("End Time: ").append(LocalDateTime.now()).append("\n");
        text.append("Total Selectors Tested: ").append(reports.size()).append("\n");
        text.append("===============================================\n\n");

        // Statistics
        long successful = reports.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        long originalStrategy = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.ORIGINAL_SELECTOR ? 1 : 0).sum();
        long domHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS ? 1 : 0).sum();
        long visualHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS ? 1 : 0).sum();
        long cached = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.CACHED ? 1 : 0).sum();

        // Token usage statistics
        long totalTokens = reports.stream().mapToLong(r -> r.tokensUsed).sum();
        long domTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();
        long visualTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();

        text.append("SUMMARY STATISTICS:\n");
        text.append("- Successful: ").append(successful).append(" (").append(String.format("%.1f%%", (double)successful / reports.size() * 100)).append(")\n");
        text.append("- Failed: ").append(reports.size() - successful).append("\n");
        text.append("- Original Selectors (no healing): ").append(originalStrategy).append("\n");
        text.append("- DOM Healed: ").append(domHealed).append("\n");
        text.append("- Visual Healed: ").append(visualHealed).append("\n");
        text.append("- Cached Results: ").append(cached).append("\n");
        if (totalTokens > 0) {
            text.append("- Token Usage - Total: ").append(totalTokens).append(" | DOM: ").append(domTokens).append(" | Visual: ").append(visualTokens).append("\n");
        }
        text.append("\n");

        // AI Implementation Details section
        boolean hasAIStrategies = reports.stream().anyMatch(r ->
            r.strategy == SelectorStrategy.DOM_ANALYSIS || r.strategy == SelectorStrategy.VISUAL_ANALYSIS);

        if (hasAIStrategies) {
            text.append("AI IMPLEMENTATION DETAILS:\n");
            text.append("===============================================\n");

            // Get first AI report for configuration details
            SelectorReport aiReport = reports.stream()
                .filter(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS || r.strategy == SelectorStrategy.VISUAL_ANALYSIS)
                .findFirst()
                .orElse(null);

            if (aiReport != null) {
                text.append("Configuration:\n");
                text.append("- Provider: ").append(aiReport.aiProvider).append("\n");
                text.append("- Model: ").append(aiReport.aiModel).append("\n");
                text.append("- API Endpoint: ").append(aiReport.apiEndpoint).append("\n");
                text.append("- Max Tokens: ").append(this.domMaxTokens).append(" (DOM), ").append(this.visualMaxTokens).append(" (Visual)\n");
                text.append("- Temperature: ").append(this.domTemperature).append(" (DOM), ").append(this.visualTemperature).append(" (Visual)\n");
                text.append("- Max Retries: ").append(aiReport.retryCount).append("\n");
                text.append("\n");

                text.append("AI Usage Statistics:\n");
                text.append("- DOM Analysis Requests: ").append(domHealed).append("\n");
                text.append("- Visual Analysis Requests: ").append(visualHealed).append("\n");

                // Token usage statistics (reuse variables from earlier calculation)
                text.append("- Total Tokens: ").append(totalTokens).append("\n");
                text.append("- DOM Tokens: ").append(domTokens).append("\n");
                text.append("- Visual Tokens: ").append(visualTokens).append("\n");

                // Cost estimation
                if (totalTokens > 0) {
                    double estimatedCost = (totalTokens * 0.375) / 1000000.0;
                    text.append("- Estimated Cost: $").append(String.format("%.4f", estimatedCost)).append("\n");
                }
                text.append("\n");
            }
        }

        text.append("DETAILED SELECTOR REPORT:\n");
        text.append("===============================================\n");

        for (int i = 0; i < reports.size(); i++) {
            SelectorReport report = reports.get(i);
            text.append(String.format("%d. %s\n", i + 1, report.originalSelector));
            text.append("   Strategy: ").append(getStrategyIcon(report.strategy)).append(" ").append(report.strategy.getDisplayName()).append("\n");
            text.append("   Time: ").append(report.executionTimeMs).append("ms\n");
            text.append("   Status: ").append(report.success ? "SUCCESS" : "FAILED").append("\n");

            // Add tokens if available for AI strategies
            if (report.tokensUsed > 0 && (report.strategy == SelectorStrategy.DOM_ANALYSIS || report.strategy == SelectorStrategy.VISUAL_ANALYSIS)) {
                text.append("   Tokens: ").append(report.tokensUsed).append("\n");
            }

            if (report.success) {
                text.append("   Actual Selector: ").append(report.actualSelector).append("\n");
                text.append("   Element: ").append(report.elementDetails).append("\n");
                if (report.reasoning != null) {
                    text.append("   Reasoning: ").append(report.reasoning).append("\n");
                }
            }
            text.append("   Description: ").append(report.description).append("\n");
            text.append("   Timestamp: ").append(report.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n");
            text.append("-----------------------------------------------\n");
        }

        return text.toString();
    }

    private String getStrategyIcon(SelectorStrategy strategy) {
        return switch (strategy) {
            case ORIGINAL_SELECTOR -> "[ORIG]";
            case DOM_ANALYSIS -> "[DOM]";
            case VISUAL_ANALYSIS -> "[VIS]";
            case CACHED -> "[CACHE]";
            case FAILED -> "[FAIL]";
        };
    }

    private String getRowClass(SelectorStrategy strategy, boolean success) {
        if (!success) return "failed";
        return switch (strategy) {
            case ORIGINAL_SELECTOR -> "original";
            case DOM_ANALYSIS -> "dom-healed";
            case VISUAL_ANALYSIS -> "visual-healed";
            case CACHED -> "cached";
            case FAILED -> "failed";
        };
    }

    public void printSummary() {
        long successful = reports.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        long originalStrategy = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.ORIGINAL_SELECTOR ? 1 : 0).sum();
        long domHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS ? 1 : 0).sum();
        long visualHealed = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS ? 1 : 0).sum();
        long cached = reports.stream().mapToLong(r -> r.strategy == SelectorStrategy.CACHED ? 1 : 0).sum();

        long totalTokens = reports.stream().mapToLong(r -> r.tokensUsed).sum();
        long domTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.DOM_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();
        long visualTokens = reports.stream().filter(r -> r.strategy == SelectorStrategy.VISUAL_ANALYSIS).mapToLong(r -> r.tokensUsed).sum();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("AUTOHEAL TEST SUMMARY");
        System.out.println("=".repeat(60));
        System.out.printf("Total: %d | Success: %d | Failed: %d%n", reports.size(), successful, reports.size() - successful);
        System.out.printf("Original: %d | DOM Healed: %d | Visual: %d | Cached: %d%n",
            originalStrategy, domHealed, visualHealed, cached);
        if (totalTokens > 0) {
            System.out.printf("Token Usage - Total: %d | DOM: %d | Visual: %d%n", totalTokens, domTokens, visualTokens);
        }
        System.out.println("=".repeat(60));
    }

    public static class SelectorReport {
        public String originalSelector;
        public String actualSelector;
        public String description;
        public SelectorStrategy strategy;
        public long executionTimeMs;
        public boolean success;
        public String elementDetails;
        public String reasoning;
        public long tokensUsed;
        public LocalDateTime timestamp;

        // AI Implementation Details
        public String aiProvider;
        public String aiModel;
        public String apiEndpoint;
        public int maxTokens;
        public double temperature;
        public int retryCount;
        public String promptType;
        public long promptTokens;
        public long completionTokens;
    }

    public enum SelectorStrategy {
        ORIGINAL_SELECTOR("Original Selector"),
        DOM_ANALYSIS("DOM Analysis (AI)"),
        VISUAL_ANALYSIS("Visual Analysis (AI)"),
        CACHED("Cached Result"),
        FAILED("Failed");

        private final String displayName;

        SelectorStrategy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}