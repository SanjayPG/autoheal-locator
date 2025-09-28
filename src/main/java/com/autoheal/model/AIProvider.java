package com.autoheal.model;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration of supported AI service providers with capability information
 */
public enum AIProvider {
    /**
     * OpenAI's GPT models for element analysis
     * Supports both text analysis and visual analysis (GPT-4o-mini with vision)
     */
    OPENAI("gpt-4o-mini", true, true),

    /**
     * Google's Gemini models for element analysis
     * Supports both text analysis and visual analysis
     */
    GOOGLE_GEMINI("gemini-2.0-flash", true, true),

    /**
     * Anthropic's Claude models for element analysis
     * Supports text analysis only (no vision capabilities)
     */
    ANTHROPIC_CLAUDE("claude-3-sonnet", true, false),

    /**
     * DeepSeek AI models for element analysis
     * Supports text analysis only
     */
    DEEPSEEK("deepseek-chat", true, false),

    /**
     * Grok AI models for element analysis
     * Supports text analysis only
     */
    GROK("grok-beta", true, false),

    /**
     * Local AI model deployment
     * Typically supports text analysis, visual support depends on model
     */
    LOCAL_MODEL("local-model", true, false),

    /**
     * Mock implementation for testing
     * Supports both for testing purposes
     */
    MOCK("mock-model", true, true);

    private final String defaultModel;
    private final boolean supportsTextAnalysis;
    private final boolean supportsVisualAnalysis;

    AIProvider(String defaultModel, boolean supportsTextAnalysis, boolean supportsVisualAnalysis) {
        this.defaultModel = defaultModel;
        this.supportsTextAnalysis = supportsTextAnalysis;
        this.supportsVisualAnalysis = supportsVisualAnalysis;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public boolean supportsTextAnalysis() {
        return supportsTextAnalysis;
    }

    public boolean supportsVisualAnalysis() {
        return supportsVisualAnalysis;
    }

    public static List<AIProvider> getVisualAnalysisCapableProviders() {
        return Arrays.asList(OPENAI, GOOGLE_GEMINI, MOCK);
    }
}