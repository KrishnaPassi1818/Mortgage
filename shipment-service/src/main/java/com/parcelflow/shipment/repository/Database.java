package com.parcelflow.shipment.repository;

import com.parcelflow.shipment.config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Minimal JDBC connection provider. No pooling in baseline — each request opens a short-lived connection.
 * Uses try-with-resources in repositories.
 */
public final class Database {
    private Database() {}

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = Config.getJdbcUrl();
        String user = Config.getDbUser();
        String pass = Config.getDbPassword();
        return DriverManager.getConnection(url, user, pass);
    }

    /** Smoke-check: attempts a connection and returns DB version, or throws. */
    public static String checkConnection() throws SQLException {
        try (Connection c = getConnection();
             var stmt = c.createStatement();
             var rs = stmt.executeQuery("SELECT VERSION()")) {
            if (rs.next()) return rs.getString(1);
            throw new SQLException("No version returned");
        }
    }
}
