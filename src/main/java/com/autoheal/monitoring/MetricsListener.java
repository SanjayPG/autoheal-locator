package com.autoheal.monitoring;

/**
 * Interface for metrics listeners
 */
public interface MetricsListener {
    void onMetricsUpdate(AutoHealMetrics metrics, HealthStatus health);
}
