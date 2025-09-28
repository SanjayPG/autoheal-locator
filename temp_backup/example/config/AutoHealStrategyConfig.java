package com.example.config;

import com.autoheal.model.ExecutionStrategy;
import com.autoheal.model.LocatorStrategy;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for AutoHeal strategy selection and execution
 */
public class AutoHealStrategyConfig {
    
    public enum HealingStrategy {
        DOM_ONLY,           // Fast - DOM analysis only
        VISUAL_ONLY,        // Slower - Visual/AI analysis only  
        HYBRID_SEQUENTIAL,  // DOM first, then Visual if DOM fails
        HYBRID_PARALLEL,    // DOM and Visual in parallel, fastest wins
        COST_OPTIMIZED     // Intelligent cost-based selection
    }
    
    public enum ElementPriority {
        CRITICAL,    // Use all strategies, maximum reliability
        HIGH,        // Use hybrid approach
        NORMAL,      // Use DOM first, fallback to visual
        LOW          // DOM only, fast execution
    }
    
    private final HealingStrategy healingStrategy;
    private final ExecutionStrategy executionStrategy;
    private final boolean enableVisualHealing;
    private final boolean enableDomAnalysis;
    private final boolean enableParallelExecution;
    private final int maxRetries;
    private final long timeoutMs;
    
    public AutoHealStrategyConfig(HealingStrategy healingStrategy, 
                                 ExecutionStrategy executionStrategy,
                                 boolean enableVisualHealing,
                                 boolean enableDomAnalysis,
                                 boolean enableParallelExecution,
                                 int maxRetries,
                                 long timeoutMs) {
        this.healingStrategy = healingStrategy;
        this.executionStrategy = executionStrategy;
        this.enableVisualHealing = enableVisualHealing;
        this.enableDomAnalysis = enableDomAnalysis;
        this.enableParallelExecution = enableParallelExecution;
        this.maxRetries = maxRetries;
        this.timeoutMs = timeoutMs;
    }
    
    public static AutoHealStrategyConfig forPriority(ElementPriority priority) {
        return switch (priority) {
            case CRITICAL -> new AutoHealStrategyConfig(
                HealingStrategy.HYBRID_PARALLEL,
                ExecutionStrategy.PARALLEL,
                true, true, true, 5, 30000
            );
            case HIGH -> new AutoHealStrategyConfig(
                HealingStrategy.HYBRID_SEQUENTIAL,
                ExecutionStrategy.SEQUENTIAL,
                true, true, false, 3, 20000
            );
            case NORMAL -> new AutoHealStrategyConfig(
                HealingStrategy.HYBRID_SEQUENTIAL,
                ExecutionStrategy.COST_OPTIMIZED,
                true, true, false, 2, 15000
            );
            case LOW -> new AutoHealStrategyConfig(
                HealingStrategy.DOM_ONLY,
                ExecutionStrategy.SEQUENTIAL,
                false, true, false, 1, 10000
            );
        };
    }
    
    public static AutoHealStrategyConfig defaultConfig() {
        return forPriority(ElementPriority.NORMAL);
    }
    
    public static AutoHealStrategyConfig fastConfig() {
        return forPriority(ElementPriority.LOW);
    }
    
    public static AutoHealStrategyConfig reliableConfig() {
        return forPriority(ElementPriority.CRITICAL);
    }
    
    public List<LocatorStrategy> getPreferredStrategies() {
        return switch (healingStrategy) {
            case DOM_ONLY -> Arrays.asList(LocatorStrategy.DOM_ANALYSIS);
            case VISUAL_ONLY -> Arrays.asList(LocatorStrategy.VISUAL_ANALYSIS);
            case HYBRID_SEQUENTIAL -> Arrays.asList(
                LocatorStrategy.DOM_ANALYSIS, 
                LocatorStrategy.VISUAL_ANALYSIS
            );
            case HYBRID_PARALLEL -> Arrays.asList(
                LocatorStrategy.DOM_ANALYSIS, 
                LocatorStrategy.VISUAL_ANALYSIS
            );
            case COST_OPTIMIZED -> Arrays.asList(
                LocatorStrategy.DOM_ANALYSIS,
                LocatorStrategy.CACHED,
                LocatorStrategy.VISUAL_ANALYSIS
            );
        };
    }
    
    // Getters
    public HealingStrategy getHealingStrategy() { return healingStrategy; }
    public ExecutionStrategy getExecutionStrategy() { return executionStrategy; }
    public boolean isVisualHealingEnabled() { return enableVisualHealing; }
    public boolean isDomAnalysisEnabled() { return enableDomAnalysis; }
    public boolean isParallelExecutionEnabled() { return enableParallelExecution; }
    public int getMaxRetries() { return maxRetries; }
    public long getTimeoutMs() { return timeoutMs; }
    
    @Override
    public String toString() {
        return String.format("AutoHealStrategy{strategy=%s, execution=%s, visual=%s, dom=%s, parallel=%s, retries=%d, timeout=%dms}",
            healingStrategy, executionStrategy, enableVisualHealing, enableDomAnalysis, 
            enableParallelExecution, maxRetries, timeoutMs);
    }
}