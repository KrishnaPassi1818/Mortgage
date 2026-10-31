-- ParcelFlow — Database Schema
-- Database: parcelflow
-- Ownership: service-owned tables, no cross-service foreign keys
-- Engine: InnoDB, timestamps UTC, idempotent

CREATE DATABASE IF NOT EXISTS parcelflow
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE parcelflow;

-- ==========================================================
-- Shipments — owned by shipment-service (port 8081)
-- ==========================================================
CREATE TABLE IF NOT EXISTS shipments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tracking_id VARCHAR(32) NOT NULL,
  sender_name VARCHAR(100) NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  origin VARCHAR(120) NOT NULL,
  destination VARCHAR(120) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_shipments_tracking_id (tracking_id),
  INDEX idx_shipments_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================================
-- Shipment Status — owned by tracking-service (port 8082)
-- ==========================================================
CREATE TABLE IF NOT EXISTS shipment_status (
  tracking_id VARCHAR(32) NOT NULL PRIMARY KEY,
  current_status VARCHAR(32) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_shipment_status_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================================
-- Tracking Events — owned by tracking-service (port 8082)
-- ==========================================================
CREATE TABLE IF NOT EXISTS tracking_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tracking_id VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  location VARCHAR(120) NULL,
  note VARCHAR(255) NULL,
  event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tracking_events_tracking_id (tracking_id),
  INDEX idx_tracking_events_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================================
-- Notifications — owned by notification-service (port 8083)
-- ==========================================================
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tracking_id VARCHAR(32) NOT NULL,
  title VARCHAR(120) NOT NULL,
  message VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delivery_state VARCHAR(20) NOT NULL DEFAULT 'RECORDED',
  INDEX idx_notifications_tracking_id (tracking_id),
  INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================================
-- Users & Roles — owned by shipment-service (port 8081)
-- ==========================================================
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  full_name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================================
-- Shipment Requests — user requests handled by Admin
-- ==========================================================
CREATE TABLE IF NOT EXISTS shipment_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  sender_name VARCHAR(100) NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  origin VARCHAR(120) NOT NULL,
  destination VARCHAR(120) NOT NULL,
  package_details VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  tracking_id VARCHAR(32) NULL,
  admin_notes VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_req_user_id (user_id),
  INDEX idx_req_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
