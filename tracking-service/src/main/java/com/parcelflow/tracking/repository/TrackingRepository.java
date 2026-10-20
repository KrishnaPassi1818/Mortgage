package com.parcelflow.tracking.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.parcelflow.tracking.model.TrackingEvent;

/**
 * Owns shipment_status and tracking_events tables.
 * Demonstrates transaction: update status + insert event atomically.
 */
public class TrackingRepository {

    public Optional<String> getCurrentStatus(String trackingId) throws SQLException {
        String sql = "SELECT current_status FROM shipment_status WHERE tracking_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString("current_status"));
                return Optional.empty();
            }
        }
    }

    public List<TrackingEvent> findEvents(String trackingId) throws SQLException {
        String sql = "SELECT id, tracking_id, status, location, note, event_time FROM tracking_events WHERE tracking_id = ? ORDER BY event_time ASC, id ASC";
        List<TrackingEvent> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TrackingEvent e = new TrackingEvent();
                    e.setId(rs.getLong("id"));
                    e.setTrackingId(rs.getString("tracking_id"));
                    e.setStatus(rs.getString("status"));
                    e.setLocation(rs.getString("location"));
                    e.setNote(rs.getString("note"));
                    Timestamp ts = rs.getTimestamp("event_time");
                    e.setEventTime(ts != null ? ts.toLocalDateTime() : null);
                    list.add(e);
                }
            }
        }
        return list;
    }

    // Example transactional method (Phase 4 will implement fully):
    // Do NOT hold transaction open during HTTP calls — commit status+event together, then call notification.
    public void updateStatusAtomically(String trackingId, String newStatus, String location, String note) throws SQLException {
        String upsertStatus = "INSERT INTO shipment_status (tracking_id, current_status, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE current_status = VALUES(current_status), updated_at = CURRENT_TIMESTAMP";
        String insertEvent = "INSERT INTO tracking_events (tracking_id, status, location, note, event_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(upsertStatus)) {
                    ps.setString(1, trackingId);
                    ps.setString(2, newStatus);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(insertEvent)) {
                    ps.setString(1, trackingId);
                    ps.setString(2, newStatus);
                    ps.setString(3, location);
                    ps.setString(4, note);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
