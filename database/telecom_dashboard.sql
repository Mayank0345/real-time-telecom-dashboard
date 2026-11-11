CREATE DATABASE IF NOT EXISTS telecom_dashboard
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE telecom_dashboard;

CREATE TABLE IF NOT EXISTS regions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_regions_name (name),
    UNIQUE KEY uk_regions_code (code)
);

CREATE TABLE IF NOT EXISTS subscribers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    region_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subscribers_phone_number (phone_number),
    KEY idx_subscribers_region_id (region_id),
    CONSTRAINT fk_subscribers_region
        FOREIGN KEY (region_id) REFERENCES regions (id)
);

CREATE TABLE IF NOT EXISTS usage_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subscriber_id BIGINT NOT NULL,
    data_usage DOUBLE NOT NULL,
    call_duration INT NOT NULL,
    sms_count INT NOT NULL,
    `timestamp` DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_usage_data_timestamp (`timestamp`),
    KEY idx_usage_data_subscriber_id (subscriber_id),
    CONSTRAINT fk_usage_data_subscriber
        FOREIGN KEY (subscriber_id) REFERENCES subscribers (id)
);

CREATE TABLE IF NOT EXISTS network_outages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    region_id BIGINT NOT NULL,
    duration_minutes BIGINT NULL,
    reason VARCHAR(255) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_network_outages_start_time (start_time),
    KEY idx_network_outages_region_id (region_id),
    KEY idx_network_outages_end_time (end_time),
    CONSTRAINT fk_network_outages_region
        FOREIGN KEY (region_id) REFERENCES regions (id)
);
