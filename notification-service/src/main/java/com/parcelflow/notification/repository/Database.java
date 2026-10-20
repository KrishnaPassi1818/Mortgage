package com.parcelflow.notification.repository;

import com.parcelflow.notification.config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {}
    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new ExceptionInInitializerError(e); }
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(Config.getJdbcUrl(), Config.getDbUser(), Config.getDbPassword());
    }
    public static String checkConnection() throws SQLException {
        try (Connection c = getConnection();
             var stmt = c.createStatement();
             var rs = stmt.executeQuery("SELECT VERSION()")) {
            if (rs.next()) return rs.getString(1);
            throw new SQLException("No version returned");
        }
    }
}
