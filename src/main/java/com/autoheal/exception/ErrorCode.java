package com.autoheal.exception;

/**
 * Error codes for different types of AutoHeal failures
 */
public enum ErrorCode {
    /**
     * Configuration validation failed
     */
    CONFIGURATION_INVALID,

    /**
     * Element could not be located using any strategy
     */
    ELEMENT_NOT_FOUND,

    /**
     * AI service is unavailable or failing
     */
    AI_SERVICE_UNAVAILABLE,

    /**
     * Operation timed out
     */
    TIMEOUT_EXCEEDED,

    /**
     * Cache operation failed
     */
    CACHE_ERROR,

    /**
     * Web automation adapter error
     */
    ADAPTER_ERROR,

    /**
     * Circuit breaker is open, blocking requests
     */
    CIRCUIT_BREAKER_OPEN,

    /**
     * Invalid locator format or unable to parse locator
     */
    INVALID_LOCATOR
}