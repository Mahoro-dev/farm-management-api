-- A vaccination takes a quantity from a store batch, the same way a treatment does.
-- Safe to run again: a failed attempt may already have added the new columns.
-- MariaDB names a column check after the column. DROP CONSTRAINT `movement_type`
-- then fails, so those checks are removed by redefining the column instead.

ALTER TABLE vaccinations
    ADD COLUMN IF NOT EXISTS medicine_product_id CHAR(36) NULL AFTER batch_number,
    ADD COLUMN IF NOT EXISTS medicine_batch_id CHAR(36) NULL AFTER medicine_product_id,
    ADD COLUMN IF NOT EXISTS quantity_used NUMERIC(14,3) NULL AFTER medicine_batch_id;

ALTER TABLE medicine_stock_movements
    ADD COLUMN IF NOT EXISTS vaccination_id CHAR(36) NULL AFTER treatment_medication_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_stock_movement_vaccination
    ON medicine_stock_movements (vaccination_id);

DROP PROCEDURE IF EXISTS run_ddl_if_constraint_missing;
DROP PROCEDURE IF EXISTS drop_named_constraint;
DROP PROCEDURE IF EXISTS drop_stock_checks;
DROP PROCEDURE IF EXISTS assert_stock_checks_replaced;

CREATE PROCEDURE run_ddl_if_constraint_missing(IN p_table VARCHAR(64), IN p_name VARCHAR(64), IN p_sql TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND CONSTRAINT_NAME = p_name
    ) THEN
        SET @ddl = p_sql;
        PREPARE ddl_stmt FROM @ddl;
        EXECUTE ddl_stmt;
        DEALLOCATE PREPARE ddl_stmt;
    END IF;
END;

CALL run_ddl_if_constraint_missing(
    'vaccinations', 'fk_vaccinations_product',
    'ALTER TABLE vaccinations ADD CONSTRAINT fk_vaccinations_product FOREIGN KEY (medicine_product_id) REFERENCES medicine_products(id)');
CALL run_ddl_if_constraint_missing(
    'vaccinations', 'fk_vaccinations_batch',
    'ALTER TABLE vaccinations ADD CONSTRAINT fk_vaccinations_batch FOREIGN KEY (medicine_batch_id) REFERENCES medicine_batches(id)');
CALL run_ddl_if_constraint_missing(
    'vaccinations', 'chk_vaccinations_quantity',
    'ALTER TABLE vaccinations ADD CONSTRAINT chk_vaccinations_quantity CHECK (quantity_used IS NULL OR quantity_used > 0)');
CALL run_ddl_if_constraint_missing(
    'medicine_stock_movements', 'fk_stock_movement_vaccination',
    'ALTER TABLE medicine_stock_movements ADD CONSTRAINT fk_stock_movement_vaccination FOREIGN KEY (vaccination_id) REFERENCES vaccinations(id)');

ALTER TABLE medicine_stock_movements
    MODIFY movement_type VARCHAR(30) NOT NULL,
    MODIFY quantity_change NUMERIC(14,3) NOT NULL;

DROP TABLE IF EXISTS check_drop;
CREATE TABLE check_drop (
    constraint_name VARCHAR(64) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO check_drop (constraint_name)
SELECT CONSTRAINT_NAME
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'medicine_stock_movements'
  AND CONSTRAINT_TYPE = 'CHECK'
  AND CONSTRAINT_NAME NOT IN ('movement_type', 'quantity_change');

CREATE PROCEDURE drop_named_constraint(IN p_name VARCHAR(64))
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    SET @drop_sql = CONCAT('ALTER TABLE medicine_stock_movements DROP CONSTRAINT `', p_name, '`');
    PREPARE drop_stmt FROM @drop_sql;
    EXECUTE drop_stmt;
    DEALLOCATE PREPARE drop_stmt;
END;

CREATE PROCEDURE drop_stock_checks()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_name VARCHAR(64);
    DECLARE cur CURSOR FOR SELECT constraint_name FROM check_drop;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_name;
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        CALL drop_named_constraint(v_name);
    END LOOP;
    CLOSE cur;
END;

CALL drop_stock_checks();

CALL run_ddl_if_constraint_missing(
    'medicine_stock_movements', 'chk_stock_movement_nonzero',
    'ALTER TABLE medicine_stock_movements ADD CONSTRAINT chk_stock_movement_nonzero CHECK (quantity_change <> 0)');
CALL run_ddl_if_constraint_missing(
    'medicine_stock_movements', 'chk_stock_movement_type',
    'ALTER TABLE medicine_stock_movements ADD CONSTRAINT chk_stock_movement_type CHECK (movement_type IN (''PURCHASE'', ''TREATMENT_USE'', ''VACCINATION_USE'', ''EXPIRED'', ''RETURN'', ''ADJUSTMENT'', ''TRANSFER_IN'', ''TRANSFER_OUT''))');
CALL run_ddl_if_constraint_missing(
    'medicine_stock_movements', 'chk_stock_movement_sign',
    'ALTER TABLE medicine_stock_movements ADD CONSTRAINT chk_stock_movement_sign CHECK ((movement_type IN (''PURCHASE'', ''RETURN'', ''TRANSFER_IN'') AND quantity_change > 0) OR (movement_type IN (''TREATMENT_USE'', ''VACCINATION_USE'', ''EXPIRED'', ''TRANSFER_OUT'') AND quantity_change < 0) OR movement_type = ''ADJUSTMENT'')');
CALL run_ddl_if_constraint_missing(
    'medicine_stock_movements', 'chk_stock_movement_link',
    'ALTER TABLE medicine_stock_movements ADD CONSTRAINT chk_stock_movement_link CHECK ((movement_type = ''TREATMENT_USE'' AND treatment_medication_id IS NOT NULL AND vaccination_id IS NULL) OR (movement_type = ''VACCINATION_USE'' AND vaccination_id IS NOT NULL AND treatment_medication_id IS NULL) OR (movement_type NOT IN (''TREATMENT_USE'', ''VACCINATION_USE'') AND treatment_medication_id IS NULL AND vaccination_id IS NULL))');

CREATE PROCEDURE assert_stock_checks_replaced()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.CHECK_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'medicine_stock_movements'
          AND CHECK_CLAUSE LIKE '%TREATMENT_USE%'
          AND CHECK_CLAUSE NOT LIKE '%VACCINATION_USE%'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Could not replace the old medicine stock check';
    END IF;
END;

CALL assert_stock_checks_replaced();

DROP PROCEDURE IF EXISTS assert_stock_checks_replaced;
DROP PROCEDURE IF EXISTS drop_stock_checks;
DROP PROCEDURE IF EXISTS drop_named_constraint;
DROP PROCEDURE IF EXISTS run_ddl_if_constraint_missing;
DROP TABLE IF EXISTS check_drop;
