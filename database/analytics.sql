USE telecom_dashboard;

-- Total data usage by region.
SELECT r.name AS region,
       SUM(u.data_usage) AS total_usage_mb
FROM usage_data u
JOIN subscribers s ON s.id = u.subscriber_id
JOIN regions r ON r.id = s.region_id
GROUP BY r.id, r.name
ORDER BY total_usage_mb DESC;

-- Total data usage for each day.
SELECT DATE(u.`timestamp`) AS usage_date,
       SUM(u.data_usage) AS total_usage_mb
FROM usage_data u
GROUP BY DATE(u.`timestamp`)
ORDER BY usage_date;

-- Average data usage per usage record.
SELECT AVG(data_usage) AS average_usage_mb
FROM usage_data;

-- Region with the highest total data usage.
SELECT r.name AS region,
       SUM(u.data_usage) AS total_usage_mb
FROM usage_data u
JOIN subscribers s ON s.id = u.subscriber_id
JOIN regions r ON r.id = s.region_id
GROUP BY r.id, r.name
ORDER BY total_usage_mb DESC
LIMIT 1;

-- Subscriber count by region and status.
SELECT r.name AS region,
       s.status,
       COUNT(*) AS subscriber_count
FROM subscribers s
JOIN regions r ON r.id = s.region_id
GROUP BY r.id, r.name, s.status
ORDER BY r.name, s.status;

-- Outage count and average resolved duration by region.
SELECT r.name AS region,
       COUNT(o.id) AS outage_count,
       AVG(o.duration_minutes) AS average_duration_minutes
FROM network_outages o
JOIN regions r ON r.id = o.region_id
GROUP BY r.id, r.name
ORDER BY outage_count DESC;
