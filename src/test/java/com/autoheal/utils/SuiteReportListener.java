package com.autoheal.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

public class SuiteReportListener implements ISuiteListener {
    private static final Logger logger = LoggerFactory.getLogger(SuiteReportListener.class);

    @Override
    public void onStart(ISuite suite) {
        logger.info("🚀 Starting AutoHeal Test Suite: {}", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("🏁 Finishing AutoHeal Test Suite: {}", suite.getName());
        logger.info("Total tests executed: {}", SuiteReportManager.getTotalTests());

        // Generate consolidated suite report
        SuiteReportManager.generateSuiteReport();

        // Cleanup all AutoHeal instances
        SuiteReportManager.cleanup();

        logger.info("✅ Suite report generation and cleanup completed");
    }
}