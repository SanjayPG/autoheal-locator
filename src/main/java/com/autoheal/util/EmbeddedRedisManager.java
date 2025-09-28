package com.autoheal.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages embedded Redis server for development and testing
 * Falls back gracefully when embedded Redis is not available
 */
public class EmbeddedRedisManager {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedRedisManager.class);
    private static Object redisServer = null;
    private static boolean redisStarted = false;

    /**
     * Try to start embedded Redis server if available
     * @param port Redis port
     * @return true if started successfully, false otherwise
     */
    public static boolean tryStartEmbeddedRedis(int port) {
        if (redisStarted) {
            return true;
        }

        // First check if Redis is already running on this port (from previous run)
        if (isRedisAvailable("localhost", port)) {
            System.out.println("[EMBEDDED-REDIS] Found existing Redis on port " + port + " (from previous run)");
            // Don't start new server, just mark as available
            redisStarted = false; // We didn't start it in this process
            return true;
        }

        try {
            // Try to load embedded Redis class using reflection
            Class<?> redisServerClass = Class.forName("redis.embedded.RedisServer");

            // Create RedisServer instance with persistent data directory
            Object serverBuilder = redisServerClass.getMethod("builder").invoke(null);
            Object builderWithPort = serverBuilder.getClass().getMethod("port", int.class).invoke(serverBuilder, port);

            // Add persistent data directory
            String dataDir = System.getProperty("user.dir") + "/redis-data";
            java.io.File dataDirFile = new java.io.File(dataDir);
            if (!dataDirFile.exists()) {
                dataDirFile.mkdirs();
            }

            // Try to set setting - some versions support this
            try {
                Object builderWithSetting = builderWithPort.getClass()
                    .getMethod("setting", String.class)
                    .invoke(builderWithPort, "dir " + dataDir);
                redisServer = builderWithSetting.getClass().getMethod("build").invoke(builderWithSetting);
            } catch (Exception e) {
                // Fallback to basic builder if setting method not available
                redisServer = builderWithPort.getClass().getMethod("build").invoke(builderWithPort);
            }

            // Start the server
            redisServer.getClass().getMethod("start").invoke(redisServer);

            redisStarted = true;
            System.out.println("[EMBEDDED-REDIS] Started NEW embedded Redis on port " + port + " with data dir: " + dataDir);
            logger.info("Embedded Redis started on port {} with data dir: {}", port, dataDir);
            return true;

        } catch (ClassNotFoundException e) {
            System.out.println("[EMBEDDED-REDIS] Embedded Redis not available (dependency not found)");
            logger.debug("Embedded Redis dependency not found", e);
            return false;
        } catch (Exception e) {
            // If we get "Address already in use", it means Redis is running from previous run
            if (e.getMessage() != null && e.getMessage().contains("Address already in use")) {
                System.out.println("[EMBEDDED-REDIS] Redis already running on port " + port + " (from previous run)");
                return true;
            }
            System.err.println("[EMBEDDED-REDIS] Failed to start embedded Redis: " + e.getMessage());
            logger.error("Failed to start embedded Redis", e);
            return false;
        }
    }

    /**
     * Stop embedded Redis server
     */
    public static void stopEmbeddedRedis() {
        if (redisServer != null && redisStarted) {
            try {
                redisServer.getClass().getMethod("stop").invoke(redisServer);
                System.out.println("[EMBEDDED-REDIS] Stopped embedded Redis");
                logger.info("Embedded Redis stopped");
            } catch (Exception e) {
                System.err.println("[EMBEDDED-REDIS] Error stopping Redis: " + e.getMessage());
                logger.error("Error stopping embedded Redis", e);
            }
            redisStarted = false;
            redisServer = null;
        }
    }

    /**
     * Check if Redis is available at given host and port
     */
    public static boolean isRedisAvailable(String host, int port) {
        try {
            // Try to create a simple connection
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 1000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get optimal cache configuration based on environment
     */
    public static com.autoheal.config.CacheConfig.CacheType getOptimalCacheType(String redisHost, int redisPort) {
        // First priority: Check if any Redis is available (external or embedded from any previous run)
        if (isRedisAvailable(redisHost, redisPort)) {
            System.out.println("[CACHE-CONFIG] Redis available at " + redisHost + ":" + redisPort + " - using persistent cache");
            return com.autoheal.config.CacheConfig.CacheType.REDIS;
        }

        // Second priority: Try to start embedded Redis for persistence
        if (tryStartEmbeddedRedis(redisPort)) {
            System.out.println("[CACHE-CONFIG] Started embedded Redis for persistent cache");
            return com.autoheal.config.CacheConfig.CacheType.REDIS;
        }

        // Last resort: Fall back to Caffeine (in-memory only)
        System.out.println("[CACHE-CONFIG] No Redis available, using in-memory cache (no persistence)");
        return com.autoheal.config.CacheConfig.CacheType.CAFFEINE;
    }

    /**
     * Add shutdown hook to clean up embedded Redis
     * Only stops Redis on JVM shutdown, not on program end
     */
    public static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Only log, don't actually stop Redis to allow persistence
            if (redisStarted) {
                System.out.println("[EMBEDDED-REDIS] Leaving Redis running for persistence (PID: " + getProcessId() + ")");
                System.out.println("[EMBEDDED-REDIS] Redis will persist data for future runs");
            }
        }));
    }

    /**
     * Force stop embedded Redis (only use for cleanup)
     */
    public static void forceStopEmbeddedRedis() {
        stopEmbeddedRedis();
    }

    /**
     * Get process ID for debugging
     */
    private static String getProcessId() {
        try {
            return java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}