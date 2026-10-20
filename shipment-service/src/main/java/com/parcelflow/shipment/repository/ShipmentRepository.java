package com.parcelflow.shipment.repository;

import com.parcelflow.shipment.model.Shipment;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Demonstrates JDBC with PreparedStatement and try-with-resources.
 * Owns only the shipments table.
 */
public class ShipmentRepository {

    public void insert(Shipment s) throws SQLException {
        String sql = "INSERT INTO shipments (tracking_id, sender_name, recipient_name, origin, destination, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getTrackingId());
            ps.setString(2, s.getSenderName());
            ps.setString(3, s.getRecipientName());
            ps.setString(4, s.getOrigin());
            ps.setString(5, s.getDestination());
            ps.setTimestamp(6, Timestamp.valueOf(s.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getLong(1));
            }
        }
    }

    public Optional<Shipment> findByTrackingId(String trackingId) throws SQLException {
        String sql = "SELECT id, tracking_id, sender_name, recipient_name, origin, destination, created_at FROM shipments WHERE tracking_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Shipment s = new Shipment();
                    s.setId(rs.getLong("id"));
                    s.setTrackingId(rs.getString("tracking_id"));
                    s.setSenderName(rs.getString("sender_name"));
                    s.setRecipientName(rs.getString("recipient_name"));
                    s.setOrigin(rs.getString("origin"));
                    s.setDestination(rs.getString("destination"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    s.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
                    return Optional.of(s);
                }
                return Optional.empty();
            }
        }
    }

    /** Smoke check used in Phase 1 to verify table exists */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM shipments";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
