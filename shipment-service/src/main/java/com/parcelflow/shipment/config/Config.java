package com.parcelflow.shipment.config;

/**
 * Centralized configuration loaded from environment variables with safe defaults.
 * Secrets (DB_PASSWORD) have no default and must be supplied.
 */
public final class Config {

    private Config() {}

    public static int getPort() {
        return envInt("SHIPMENT_SERVICE_PORT", 8081);
    }

    public static String getDbHost() {
        return env("DB_HOST", "localhost");
    }

    public static int getDbPort() {
        return envInt("DB_PORT", 3306);
    }

    public static String getDbName() {
        return env("DB_NAME", "parcelflow");
    }

    public static String getDbUser() {
        return env("DB_USER", "parcelflow_app");
    }

    public static String getDbPassword() {
        // No default for secret — return empty if not set, caller must handle
        return System.getenv().getOrDefault("DB_PASSWORD", "");
    }

    public static String getTrackingServiceUrl() {
        return env("TRACKING_SERVICE_URL", "http://localhost:8082");
    }

    public static String getFrontendOrigin() {
        return env("FRONTEND_ORIGIN", "http://localhost:5173");
    }

    public static String getJdbcUrl() {
        // Use UTC timezone handling
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                getDbHost(), getDbPort(), getDbName());
    }

    private static String env(String key, String defaultVal) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultVal : v.trim();
    }

    private static int envInt(String key, int defaultVal) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
