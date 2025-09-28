package com.autoheal.monitoring;

import com.autoheal.AutoHealLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performance monitoring and alerting for AutoHeal
 */
public class AutoHealMonitor {
    private static final Logger logger = LoggerFactory.getLogger(AutoHealMonitor.class);

    private final AutoHealLocator autoHeal;
    private final ScheduledExecutorService scheduler;
    private final List<MetricsListener> listeners;

    public AutoHealMonitor(AutoHealLocator autoHeal) {
        this.autoHeal = autoHeal;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.listeners = new CopyOnWriteArrayList<>();
        logger.info("AutoHealMonitor initialized");
    }

    /**
     * Start monitoring with specified interval
     *
     * @param interval monitoring interval
     */
    public void startMonitoring(Duration interval) {
        logger.info("Starting monitoring with interval: {}", interval);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                AutoHealMetrics metrics = autoHeal.getMetrics();
                HealthStatus health = autoHeal.getHealthStatus();

                // Check for alerts
                checkAlerts(metrics, health);

                // Notify listeners
                notifyListeners(metrics, health);

            } catch (Exception e) {
                logger.error("Monitoring error: {}", e.getMessage(), e);
            }
        }, 0, interval.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Add metrics listener
     *
     * @param listener the listener to add
     */
    public void addListener(MetricsListener listener) {
        listeners.add(listener);
        logger.debug("Added metrics listener: {}", listener.getClass().getSimpleName());
    }

    /**
     * Remove metrics listener
     *
     * @param listener the listener to remove
     */
    public void removeListener(MetricsListener listener) {
        listeners.remove(listener);
        logger.debug("Removed metrics listener: {}", listener.getClass().getSimpleName());
    }

    private void checkAlerts(AutoHealMetrics metrics, HealthStatus health) {
        // Success rate too low
        if (metrics.getLocatorMetrics().getSuccessRate() < 0.8) {
            triggerAlert("LOW_SUCCESS_RATE",
                    "AutoHeal success rate below 80%: " + metrics.getLocatorMetrics().getSuccessRate());
        }

        // Cache hit rate too low
        if (metrics.getCacheMetrics().getHitRate() < 0.5) {
            triggerAlert("LOW_CACHE_HIT_RATE",
                    "Cache hit rate below 50%: " + metrics.getCacheMetrics().getHitRate());
        }

        // Overall health check
        if (!health.isOverall()) {
            triggerAlert("SYSTEM_UNHEALTHY", "AutoHeal system health check failed");
        }

        // High response times
        if (metrics.getAiServiceMetrics() != null &&
                metrics.getAiServiceMetrics().getAverageResponseTime() > 5000) {
            triggerAlert("HIGH_RESPONSE_TIME",
                    "AI service response time above 5s: " + metrics.getAiServiceMetrics().getAverageResponseTime());
        }
    }

    private void triggerAlert(String alertType, String message) {
        logger.warn("ALERT [{}]: {}", alertType, message);
        // In real implementation, send to monitoring system
        // Example: sendToPrometheus(alertType, message);
    }

    private void notifyListeners(AutoHealMetrics metrics, HealthStatus health) {
        for (MetricsListener listener : listeners) {
            try {
                listener.onMetricsUpdate(metrics, health);
            } catch (Exception e) {
                logger.error("Listener notification failed: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Shutdown the monitor
     */
    public void shutdown() {
        logger.info("Shutting down AutoHealMonitor");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("AutoHealMonitor shutdown completed");
    }
}