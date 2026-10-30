-- Lactation stages are reference data. A milk record points at one of them.
-- New tables use the database collation so later comparisons stay consistent.

CREATE TABLE IF NOT EXISTS lactation_stages (
    code VARCHAR(40) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    meaning VARCHAR(200) NOT NULL,
    display_order SMALLINT NOT NULL,
    UNIQUE KEY uq_lactation_stage_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO lactation_stages (code, name, meaning, display_order) VALUES
    ('COLOSTRUM', 'Colostrum', 'First milk after calving', 1),
    ('EARLY_LACTATION', 'Early lactation', 'Beginning of milk production', 2),
    ('MID_LACTATION', 'Mid lactation', 'Middle of the milk-producing period', 3),
    ('LATE_LACTATION', 'Late lactation', 'End of the milk-producing period', 4),
    ('DRY_PERIOD', 'Dry period', 'Rest period before the next calving', 5)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    meaning = VALUES(meaning),
    display_order = VALUES(display_order);

ALTER TABLE milk_records
    ADD COLUMN IF NOT EXISTS lactation_stage VARCHAR(40) NULL AFTER quantity_liters;

DROP PROCEDURE IF EXISTS add_milk_stage_fk;

CREATE PROCEDURE add_milk_stage_fk()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'milk_records'
          AND CONSTRAINT_NAME = 'fk_milk_lactation_stage'
    ) THEN
        ALTER TABLE milk_records
            ADD CONSTRAINT fk_milk_lactation_stage
            FOREIGN KEY (lactation_stage) REFERENCES lactation_stages (code);
    END IF;
END;

CALL add_milk_stage_fk();

DROP PROCEDURE IF EXISTS add_milk_stage_fk;
