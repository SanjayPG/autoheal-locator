package com.autoheal.model;

/**
 * Enum representing different automation frameworks supported by AutoHeal
 */
public enum AutomationFramework {
    /**
     * Selenium WebDriver framework
     */
    SELENIUM,

    /**
     * Playwright framework
     */
    PLAYWRIGHT;

    /**
     * Check if this framework supports visual analysis
     *
     * @return true if visual analysis is supported
     */
    public boolean supportsVisualAnalysis() {
        // Both frameworks support visual analysis through screenshots
        return true;
    }

    /**
     * Get user-friendly display name
     *
     * @return Display name of the framework
     */
    public String getDisplayName() {
        return switch (this) {
            case SELENIUM -> "Selenium WebDriver";
            case PLAYWRIGHT -> "Playwright";
        };
    }
}
