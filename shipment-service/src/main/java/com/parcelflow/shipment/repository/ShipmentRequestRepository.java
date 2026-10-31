package com.parcelflow.shipment.repository;

import com.parcelflow.shipment.model.ShipmentRequest;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShipmentRequestRepository {

    public ShipmentRequest insert(ShipmentRequest req) throws SQLException {
        String sql = "INSERT INTO shipment_requests (user_id, sender_name, recipient_name, origin, destination, package_details, status, tracking_id, admin_notes, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, req.getUserId());
            ps.setString(2, req.getSenderName());
            ps.setString(3, req.getRecipientName());
            ps.setString(4, req.getOrigin());
            ps.setString(5, req.getDestination());
            ps.setString(6, req.getPackageDetails());
            ps.setString(7, req.getStatus() != null ? req.getStatus() : "PENDING");
            ps.setString(8, req.getTrackingId());
            ps.setString(9, req.getAdminNotes());
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(10, now);
            ps.setTimestamp(11, now);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    req.setId(rs.getLong(1));
                }
            }
            req.setCreatedAt(now.toLocalDateTime());
            req.setUpdatedAt(now.toLocalDateTime());
            return req;
        }
    }

    public Optional<ShipmentRequest> findById(Long id) throws SQLException {
        String sql = "SELECT id, user_id, sender_name, recipient_name, origin, destination, package_details, status, tracking_id, admin_notes, created_at, updated_at FROM shipment_requests WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ShipmentRequest> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT id, user_id, sender_name, recipient_name, origin, destination, package_details, status, tracking_id, admin_notes, created_at, updated_at FROM shipment_requests WHERE user_id = ? ORDER BY created_at DESC";
        List<ShipmentRequest> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<ShipmentRequest> findAll() throws SQLException {
        String sql = "SELECT id, user_id, sender_name, recipient_name, origin, destination, package_details, status, tracking_id, admin_notes, created_at, updated_at FROM shipment_requests ORDER BY created_at DESC";
        List<ShipmentRequest> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void update(ShipmentRequest req) throws SQLException {
        String sql = "UPDATE shipment_requests SET status = ?, tracking_id = ?, admin_notes = ?, destination = ?, package_details = ?, updated_at = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, req.getStatus());
            ps.setString(2, req.getTrackingId());
            ps.setString(3, req.getAdminNotes());
            ps.setString(4, req.getDestination());
            ps.setString(5, req.getPackageDetails());
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(6, now);
            ps.setLong(7, req.getId());

            ps.executeUpdate();
            req.setUpdatedAt(now.toLocalDateTime());
        }
    }

    private ShipmentRequest mapRow(ResultSet rs) throws SQLException {
        ShipmentRequest r = new ShipmentRequest();
        r.setId(rs.getLong("id"));
        r.setUserId(rs.getLong("user_id"));
        r.setSenderName(rs.getString("sender_name"));
        r.setRecipientName(rs.getString("recipient_name"));
        r.setOrigin(rs.getString("origin"));
        r.setDestination(rs.getString("destination"));
        r.setPackageDetails(rs.getString("package_details"));
        r.setStatus(rs.getString("status"));
        r.setTrackingId(rs.getString("tracking_id"));
        r.setAdminNotes(rs.getString("admin_notes"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) r.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) r.setUpdatedAt(uat.toLocalDateTime());
        return r;
    }
}
