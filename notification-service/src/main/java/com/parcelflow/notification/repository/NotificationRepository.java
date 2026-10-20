package com.parcelflow.notification.repository;

import com.parcelflow.notification.model.Notification;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public Notification insert(Notification n) throws SQLException {
        String sql = "INSERT INTO notifications (tracking_id, title, message, status, delivery_state, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getTrackingId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getStatus());
            ps.setString(5, n.getDeliveryState() != null ? n.getDeliveryState() : "RECORDED");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) n.setId(keys.getLong(1));
            }
            return n;
        }
    }

    public List<Notification> findByTrackingId(String trackingId) throws SQLException {
        String sql = "SELECT id, tracking_id, title, message, status, created_at, delivery_state FROM notifications WHERE tracking_id = ? ORDER BY created_at ASC, id ASC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setId(rs.getLong("id"));
                    n.setTrackingId(rs.getString("tracking_id"));
                    n.setTitle(rs.getString("title"));
                    n.setMessage(rs.getString("message"));
                    n.setStatus(rs.getString("status"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    n.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
                    n.setDeliveryState(rs.getString("delivery_state"));
                    list.add(n);
                }
            }
        }
        return list;
    }

    public int countByTrackingId(String trackingId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE tracking_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackingId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
