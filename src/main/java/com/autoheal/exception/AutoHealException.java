package com.autoheal.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for AutoHeal locator errors
 */
public class AutoHealException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    public AutoHealException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, new HashMap<>());
    }

    public AutoHealException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, new HashMap<>());
    }

    public AutoHealException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = Collections.unmodifiableMap(new HashMap<>(context));
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("AutoHealException{errorCode=%s, message='%s', context=%s}",
                errorCode, getMessage(), context);
    }
}