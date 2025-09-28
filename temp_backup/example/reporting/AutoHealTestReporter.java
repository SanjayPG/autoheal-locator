package com.example.reporting;

import com.autoheal.monitoring.AutoHealMetrics;
import com.autoheal.monitoring.HealthStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive reporting for AutoHeal test execution and metrics
 */
public class AutoHealTestReporter {
    private static final Logger logger = LoggerFactory.getLogger(AutoHealTestReporter.class);
    private static final Logger metricsLogger = LoggerFactory.getLogger("AUTOHEAL_METRICS");
    
    private static final String REPORTS_DIR = "target/reports/autoheal";
    private static final ObjectMapper objectMapper = createObjectMapper();
    
    private static final Map<String, TestExecutionData> testExecutions = new ConcurrentHashMap<>();
    private static final List<AutoHealEvent> healingEvents = new ArrayList<>();
    private static final Map<String, Object> sessionMetrics = new ConcurrentHashMap<>();
    
    static {
        createReportsDirectory();
    }
    
    /**
     * Record test execution start
     */
    public static void recordTestStart(String testName, String testClass) {
        TestExecutionData data = new TestExecutionData();
        data.testName = testName;
        data.testClass = testClass;
        data.startTime = LocalDateTime.now();
        data.status = "RUNNING";
        
        testExecutions.put(testName, data);
        logger.info("AutoHeal test started: {} in {}", testName, testClass);
    }
    
    /**
     * Record test execution completion
     */
    public static void recordTestCompletion(String testName, boolean passed, String errorMessage, 
                                          AutoHealMetrics metrics, HealthStatus healthStatus) {
        TestExecutionData data = testExecutions.get(testName);
        if (data != null) {
            data.endTime = LocalDateTime.now();
            data.status = passed ? "PASSED" : "FAILED";
            data.errorMessage = errorMessage;
            data.autoHealMetrics = metrics;
            data.healthStatus = healthStatus;
            data.duration = java.time.Duration.between(data.startTime, data.endTime);
            
            // Log metrics
            logTestMetrics(testName, data);
            
            logger.info("AutoHeal test completed: {} - {} in {}ms", 
                testName, data.status, data.duration.toMillis());
        }
    }
    
    /**
     * Record AutoHeal healing event
     */
    public static void recordHealingEvent(String testName, String originalSelector, String healedSelector, 
                                        String strategy, boolean successful, String reason) {
        AutoHealEvent event = new AutoHealEvent();
        event.timestamp = LocalDateTime.now();
        event.testName = testName;
        event.originalSelector = originalSelector;
        event.healedSelector = healedSelector;
        event.strategy = strategy;
        event.successful = successful;
        event.reason = reason;
        
        synchronized (healingEvents) {
            healingEvents.add(event);
        }
        
        logger.info("AutoHeal event: {} - Original: '{}', Healed: '{}', Strategy: {}, Success: {}", 
            testName, originalSelector, healedSelector, strategy, successful);
        
        metricsLogger.info("HEALING_EVENT|{}|{}|{}|{}|{}|{}", 
            testName, originalSelector, healedSelector, strategy, successful, reason);
    }
    
    /**
     * Record session-level metrics
     */
    public static void recordSessionMetrics(String key, Object value) {
        sessionMetrics.put(key, value);
        sessionMetrics.put(key + "_timestamp", LocalDateTime.now().toString());
        
        metricsLogger.info("SESSION_METRIC|{}|{}", key, value);
    }
    
    /**
     * Generate comprehensive HTML report
     */
    public static void generateReport() {
        try {
            generateJsonReport();
            generateHtmlReport();
            generateCsvReport();
            
            logger.info("AutoHeal reports generated in: {}", REPORTS_DIR);
        } catch (Exception e) {
            logger.error("Failed to generate AutoHeal reports: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Generate JSON report
     */
    private static void generateJsonReport() throws IOException {
        TestSuiteReport report = createTestSuiteReport();
        
        String jsonReport = objectMapper.writeValueAsString(report);
        String filename = String.format("autoheal-report-%s.json", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        
        try (FileWriter writer = new FileWriter(new File(REPORTS_DIR, filename))) {
            writer.write(jsonReport);
        }
        
        // Also create a latest report
        try (FileWriter writer = new FileWriter(new File(REPORTS_DIR, "latest-report.json"))) {
            writer.write(jsonReport);
        }
        
        logger.info("JSON report generated: {}", filename);
    }
    
    /**
     * Generate HTML report
     */
    private static void generateHtmlReport() throws IOException {
        TestSuiteReport report = createTestSuiteReport();
        String html = generateHtmlContent(report);
        
        String filename = String.format("autoheal-report-%s.html", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        
        try (FileWriter writer = new FileWriter(new File(REPORTS_DIR, filename))) {
            writer.write(html);
        }
        
        // Also create a latest report
        try (FileWriter writer = new FileWriter(new File(REPORTS_DIR, "latest-report.html"))) {
            writer.write(html);
        }
        
        logger.info("HTML report generated: {}", filename);
    }
    
    /**
     * Generate CSV report for metrics analysis
     */
    private static void generateCsvReport() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("TestName,TestClass,Status,Duration(ms),AutoHealSuccessRate,CacheHitRate,HealingEvents,StartTime,EndTime\n");
        
        for (TestExecutionData data : testExecutions.values()) {
            csv.append(String.format("%s,%s,%s,%d,%.2f,%.2f,%d,%s,%s\n",
                data.testName,
                data.testClass,
                data.status,
                data.duration != null ? data.duration.toMillis() : 0,
                data.autoHealMetrics != null ? data.autoHealMetrics.getLocatorMetrics().getSuccessRate() : 0.0,
                data.autoHealMetrics != null ? data.autoHealMetrics.getCacheMetrics().getHitRate() : 0.0,
                countHealingEventsForTest(data.testName),
                data.startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                data.endTime != null ? data.endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : ""
            ));
        }
        
        String filename = String.format("autoheal-metrics-%s.csv", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        
        try (FileWriter writer = new FileWriter(new File(REPORTS_DIR, filename))) {
            writer.write(csv.toString());
        }
        
        logger.info("CSV report generated: {}", filename);
    }
    
    /**
     * Create comprehensive test suite report
     */
    private static TestSuiteReport createTestSuiteReport() {
        TestSuiteReport report = new TestSuiteReport();
        report.timestamp = LocalDateTime.now();
        report.totalTests = testExecutions.size();
        report.passedTests = (int) testExecutions.values().stream().filter(t -> "PASSED".equals(t.status)).count();
        report.failedTests = (int) testExecutions.values().stream().filter(t -> "FAILED".equals(t.status)).count();
        report.totalHealingEvents = healingEvents.size();
        report.successfulHealingEvents = (int) healingEvents.stream().filter(e -> e.successful).count();
        
        // Calculate aggregated metrics
        double avgSuccessRate = testExecutions.values().stream()
            .filter(t -> t.autoHealMetrics != null)
            .mapToDouble(t -> t.autoHealMetrics.getLocatorMetrics().getSuccessRate())
            .average().orElse(0.0);
        
        double avgCacheHitRate = testExecutions.values().stream()
            .filter(t -> t.autoHealMetrics != null)
            .mapToDouble(t -> t.autoHealMetrics.getCacheMetrics().getHitRate())
            .average().orElse(0.0);
        
        report.averageAutoHealSuccessRate = avgSuccessRate;
        report.averageCacheHitRate = avgCacheHitRate;
        report.testExecutions = new ArrayList<>(testExecutions.values());
        report.healingEvents = new ArrayList<>(healingEvents);
        report.sessionMetrics = new HashMap<>(sessionMetrics);
        
        return report;
    }
    
    /**
     * Generate HTML content for the report
     */
    private static String generateHtmlContent(TestSuiteReport report) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n<head>\n")
            .append("<title>AutoHeal Test Report</title>\n")
            .append("<style>\n")
            .append("body { font-family: Arial, sans-serif; margin: 20px; }\n")
            .append("h1, h2 { color: #333; }\n")
            .append(".summary { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n")
            .append(".metric { display: inline-block; margin-right: 30px; }\n")
            .append(".metric-value { font-weight: bold; font-size: 1.2em; }\n")
            .append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n")
            .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            .append("th { background-color: #f2f2f2; }\n")
            .append(".passed { color: green; }\n")
            .append(".failed { color: red; }\n")
            .append(".success { color: green; }\n")
            .append(".failure { color: red; }\n")
            .append("</style>\n")
            .append("</head>\n<body>\n");
        
        // Header
        html.append("<h1>AutoHeal Test Execution Report</h1>\n")
            .append("<p>Generated: ").append(report.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
        
        // Summary
        html.append("<div class='summary'>\n")
            .append("<h2>Summary</h2>\n")
            .append("<div class='metric'>Total Tests: <span class='metric-value'>").append(report.totalTests).append("</span></div>\n")
            .append("<div class='metric'>Passed: <span class='metric-value passed'>").append(report.passedTests).append("</span></div>\n")
            .append("<div class='metric'>Failed: <span class='metric-value failed'>").append(report.failedTests).append("</span></div>\n")
            .append("<br><br>\n")
            .append("<div class='metric'>Healing Events: <span class='metric-value'>").append(report.totalHealingEvents).append("</span></div>\n")
            .append("<div class='metric'>Successful Healings: <span class='metric-value success'>").append(report.successfulHealingEvents).append("</span></div>\n")
            .append("<br><br>\n")
            .append("<div class='metric'>Avg Success Rate: <span class='metric-value'>").append(String.format("%.2f%%", report.averageAutoHealSuccessRate * 100)).append("</span></div>\n")
            .append("<div class='metric'>Avg Cache Hit Rate: <span class='metric-value'>").append(String.format("%.2f%%", report.averageCacheHitRate * 100)).append("</span></div>\n")
            .append("</div>\n");
        
        // Test Results Table
        html.append("<h2>Test Results</h2>\n")
            .append("<table>\n")
            .append("<tr><th>Test Name</th><th>Class</th><th>Status</th><th>Duration</th><th>AutoHeal Success Rate</th><th>Cache Hit Rate</th></tr>\n");
        
        for (TestExecutionData test : report.testExecutions) {
            html.append("<tr>\n")
                .append("<td>").append(test.testName).append("</td>\n")
                .append("<td>").append(test.testClass).append("</td>\n")
                .append("<td class='").append(test.status.toLowerCase()).append("'>").append(test.status).append("</td>\n")
                .append("<td>").append(test.duration != null ? test.duration.toMillis() + "ms" : "N/A").append("</td>\n")
                .append("<td>").append(test.autoHealMetrics != null ? String.format("%.2f%%", test.autoHealMetrics.getLocatorMetrics().getSuccessRate() * 100) : "N/A").append("</td>\n")
                .append("<td>").append(test.autoHealMetrics != null ? String.format("%.2f%%", test.autoHealMetrics.getCacheMetrics().getHitRate() * 100) : "N/A").append("</td>\n")
                .append("</tr>\n");
        }
        html.append("</table>\n");
        
        // Healing Events Table
        if (!report.healingEvents.isEmpty()) {
            html.append("<h2>AutoHeal Events</h2>\n")
                .append("<table>\n")
                .append("<tr><th>Timestamp</th><th>Test</th><th>Original Selector</th><th>Healed Selector</th><th>Strategy</th><th>Result</th></tr>\n");
            
            for (AutoHealEvent event : report.healingEvents) {
                html.append("<tr>\n")
                    .append("<td>").append(event.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</td>\n")
                    .append("<td>").append(event.testName).append("</td>\n")
                    .append("<td>").append(event.originalSelector).append("</td>\n")
                    .append("<td>").append(event.healedSelector != null ? event.healedSelector : "N/A").append("</td>\n")
                    .append("<td>").append(event.strategy).append("</td>\n")
                    .append("<td class='").append(event.successful ? "success" : "failure").append("'>").append(event.successful ? "Success" : "Failed").append("</td>\n")
                    .append("</tr>\n");
            }
            html.append("</table>\n");
        }
        
        html.append("</body>\n</html>");
        return html.toString();
    }
    
    /**
     * Log test metrics to the metrics logger
     */
    private static void logTestMetrics(String testName, TestExecutionData data) {
        if (data.autoHealMetrics != null) {
            metricsLogger.info("TEST_METRICS|{}|{}|{}|{}|{}", 
                testName, 
                data.status,
                data.duration != null ? data.duration.toMillis() : 0,
                data.autoHealMetrics.getLocatorMetrics().getSuccessRate(),
                data.autoHealMetrics.getCacheMetrics().getHitRate()
            );
        }
    }
    
    /**
     * Count healing events for a specific test
     */
    private static long countHealingEventsForTest(String testName) {
        return healingEvents.stream().filter(e -> testName.equals(e.testName)).count();
    }
    
    /**
     * Create reports directory
     */
    private static void createReportsDirectory() {
        try {
            Path reportsPath = Paths.get(REPORTS_DIR);
            Files.createDirectories(reportsPath);
            Files.createDirectories(reportsPath.resolve("../logs"));
        } catch (IOException e) {
            logger.error("Failed to create reports directory: {}", e.getMessage());
        }
    }
    
    /**
     * Create and configure ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }
    
    // Data classes for reporting
    public static class TestExecutionData {
        public String testName;
        public String testClass;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public java.time.Duration duration;
        public String status;
        public String errorMessage;
        public AutoHealMetrics autoHealMetrics;
        public HealthStatus healthStatus;
    }
    
    public static class AutoHealEvent {
        public LocalDateTime timestamp;
        public String testName;
        public String originalSelector;
        public String healedSelector;
        public String strategy;
        public boolean successful;
        public String reason;
    }
    
    public static class TestSuiteReport {
        public LocalDateTime timestamp;
        public int totalTests;
        public int passedTests;
        public int failedTests;
        public int totalHealingEvents;
        public int successfulHealingEvents;
        public double averageAutoHealSuccessRate;
        public double averageCacheHitRate;
        public List<TestExecutionData> testExecutions;
        public List<AutoHealEvent> healingEvents;
        public Map<String, Object> sessionMetrics;
    }
}