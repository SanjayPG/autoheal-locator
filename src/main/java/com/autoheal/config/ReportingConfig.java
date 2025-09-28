package com.autoheal.config;

/**
 * Configuration for AutoHeal reporting functionality
 */
public class ReportingConfig {
    private final boolean enabled;
    private final boolean generateHTML;
    private final boolean generateJSON;
    private final boolean generateText;
    private final boolean consoleLogging;
    private final String outputDirectory;
    private final String reportNamePrefix;

    public static Builder builder() {
        return new Builder();
    }

    public static ReportingConfig defaultConfig() {
        return builder().build();
    }

    public static ReportingConfig disabled() {
        return builder().enabled(false).build();
    }

    public static ReportingConfig enabledWithDefaults() {
        return builder().enabled(true).build();
    }

    public static class Builder {
        private boolean enabled = false;  // Disabled by default for backward compatibility
        private boolean generateHTML = true;
        private boolean generateJSON = true;
        private boolean generateText = true;
        private boolean consoleLogging = true;
        private String outputDirectory = System.getProperty("user.dir"); // Current directory
        private String reportNamePrefix = "AutoHeal_Report";

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder generateHTML(boolean generateHTML) {
            this.generateHTML = generateHTML;
            return this;
        }

        public Builder generateJSON(boolean generateJSON) {
            this.generateJSON = generateJSON;
            return this;
        }

        public Builder generateText(boolean generateText) {
            this.generateText = generateText;
            return this;
        }

        public Builder consoleLogging(boolean consoleLogging) {
            this.consoleLogging = consoleLogging;
            return this;
        }

        public Builder outputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public Builder reportNamePrefix(String reportNamePrefix) {
            this.reportNamePrefix = reportNamePrefix;
            return this;
        }

        public ReportingConfig build() {
            return new ReportingConfig(enabled, generateHTML, generateJSON, generateText,
                    consoleLogging, outputDirectory, reportNamePrefix);
        }
    }

    private ReportingConfig(boolean enabled, boolean generateHTML, boolean generateJSON,
                           boolean generateText, boolean consoleLogging, String outputDirectory,
                           String reportNamePrefix) {
        this.enabled = enabled;
        this.generateHTML = generateHTML;
        this.generateJSON = generateJSON;
        this.generateText = generateText;
        this.consoleLogging = consoleLogging;
        this.outputDirectory = outputDirectory;
        this.reportNamePrefix = reportNamePrefix;
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isGenerateHTML() { return generateHTML; }
    public boolean isGenerateJSON() { return generateJSON; }
    public boolean isGenerateText() { return generateText; }
    public boolean isConsoleLogging() { return consoleLogging; }
    public String getOutputDirectory() { return outputDirectory; }
    public String getReportNamePrefix() { return reportNamePrefix; }
}