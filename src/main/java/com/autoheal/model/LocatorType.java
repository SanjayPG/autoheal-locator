package com.autoheal.model;

/**
 * Enum representing different locator strategies supported by AutoHeal
 */
public enum LocatorType {

    /**
     * CSS Selector strategy (e.g., "#id", ".class", "input[name='username']")
     */
    CSS_SELECTOR("CSS Selector"),

    /**
     * XPath strategy (e.g., "//input[@name='username']", "//*[@id='login']")
     */
    XPATH("XPath"),

    /**
     * ID attribute strategy (e.g., "username", "login-button")
     */
    ID("ID"),

    /**
     * Name attribute strategy (e.g., "username", "password")
     */
    NAME("Name"),

    /**
     * Class name strategy (e.g., "btn-primary", "form-control")
     */
    CLASS_NAME("Class Name"),

    /**
     * Tag name strategy (e.g., "input", "button", "div")
     */
    TAG_NAME("Tag Name"),

    /**
     * Link text strategy for anchor tags (exact match)
     */
    LINK_TEXT("Link Text"),

    /**
     * Partial link text strategy for anchor tags (partial match)
     */
    PARTIAL_LINK_TEXT("Partial Link Text");

    private final String displayName;

    LocatorType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}