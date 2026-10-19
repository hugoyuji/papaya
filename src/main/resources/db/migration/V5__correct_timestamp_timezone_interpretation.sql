-- V2 interpreted the original timestamp-without-time-zone values as Sao Paulo time.
-- Hibernate was configured with JDBC time zone UTC, so reinterpret the original
-- wall-clock values as UTC to preserve the instants that were originally stored.
UPDATE products
SET created_at = (created_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    updated_at = (updated_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC';
