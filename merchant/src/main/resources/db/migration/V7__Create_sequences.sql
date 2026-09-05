-- Create Hibernate sequence if not yet present (shared across schemas via search_path)
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 100 INCREMENT BY 50;
