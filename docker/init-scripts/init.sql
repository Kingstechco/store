-- Initialize the Store database for Docker environment
-- This script runs automatically when the PostgreSQL container starts

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create additional users/roles if needed
-- CREATE ROLE store_readonly;
-- GRANT CONNECT ON DATABASE store_db TO store_readonly;
-- GRANT USAGE ON SCHEMA public TO store_readonly;
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO store_readonly;

-- Set default search path
ALTER DATABASE store_db SET search_path TO public;

-- Create indexes for better performance (Liquibase will handle table creation)
-- These will be created after Liquibase runs, so they're conditional

-- Performance optimization settings for Docker environment
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
ALTER SYSTEM SET log_statement = 'none';
ALTER SYSTEM SET log_min_duration_statement = 1000;
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;

-- Reload configuration
SELECT pg_reload_conf();

-- Log initialization completion
DO $$
BEGIN
    RAISE NOTICE 'Store database initialization completed for Docker environment';
END $$;