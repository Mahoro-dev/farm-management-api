-- One collation for the local server. Tables were utf8mb4_unicode_ci while
-- trigger variables followed the database default utf8mb4_general_ci, so
-- comparisons such as sex <> 'FEMALE' failed while saving an animal.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
ALTER DATABASE farm_management CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP PROCEDURE IF EXISTS convert_table_collation;
DROP PROCEDURE IF EXISTS drop_foreign_keys;
DROP PROCEDURE IF EXISTS restore_foreign_keys;

CREATE PROCEDURE convert_table_collation(IN p_table VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = p_table
    ) THEN
        SET @convert_sql = CONCAT(
            'ALTER TABLE `', p_table, '` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci');
        PREPARE convert_stmt FROM @convert_sql;
        EXECUTE convert_stmt;
        DEALLOCATE PREPARE convert_stmt;
    END IF;
END;

CREATE PROCEDURE drop_foreign_keys()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table VARCHAR(64);
    DECLARE v_name VARCHAR(64);
    DECLARE cur CURSOR FOR SELECT table_name, constraint_name FROM fk_rebuild;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_table, v_name;
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        SET @drop_sql = CONCAT('ALTER TABLE `', v_table, '` DROP FOREIGN KEY `', v_name, '`');
        PREPARE drop_stmt FROM @drop_sql;
        EXECUTE drop_stmt;
        DEALLOCATE PREPARE drop_stmt;
    END LOOP;
    CLOSE cur;
END;

CREATE PROCEDURE restore_foreign_keys()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table VARCHAR(64);
    DECLARE v_name VARCHAR(64);
    DECLARE v_columns TEXT;
    DECLARE v_ref_table VARCHAR(64);
    DECLARE v_ref_columns TEXT;
    DECLARE v_delete_rule VARCHAR(20);
    DECLARE v_update_rule VARCHAR(20);
    DECLARE cur CURSOR FOR
        SELECT table_name, constraint_name, columns_sql, ref_table, ref_columns_sql, delete_rule, update_rule
        FROM fk_rebuild;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_table, v_name, v_columns, v_ref_table, v_ref_columns, v_delete_rule, v_update_rule;
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        SET @add_sql = CONCAT(
            'ALTER TABLE `', v_table, '` ADD CONSTRAINT `', v_name,
            '` FOREIGN KEY (', v_columns, ') REFERENCES `', v_ref_table, '` (', v_ref_columns, ')',
            ' ON DELETE ', v_delete_rule, ' ON UPDATE ', v_update_rule);
        PREPARE add_stmt FROM @add_sql;
        EXECUTE add_stmt;
        DEALLOCATE PREPARE add_stmt;
    END LOOP;
    CLOSE cur;
END;

DROP TABLE IF EXISTS fk_rebuild;
CREATE TABLE fk_rebuild (
    table_name VARCHAR(64) NOT NULL,
    constraint_name VARCHAR(64) NOT NULL,
    columns_sql TEXT NOT NULL,
    ref_table VARCHAR(64) NOT NULL,
    ref_columns_sql TEXT NOT NULL,
    delete_rule VARCHAR(20) NOT NULL,
    update_rule VARCHAR(20) NOT NULL,
    PRIMARY KEY (table_name, constraint_name)
) ENGINE=InnoDB;

INSERT INTO fk_rebuild (table_name, constraint_name, columns_sql, ref_table, ref_columns_sql, delete_rule, update_rule)
SELECT k.TABLE_NAME,
       k.CONSTRAINT_NAME,
       GROUP_CONCAT(CONCAT('`', k.COLUMN_NAME, '`') ORDER BY k.ORDINAL_POSITION SEPARATOR ', '),
       k.REFERENCED_TABLE_NAME,
       GROUP_CONCAT(CONCAT('`', k.REFERENCED_COLUMN_NAME, '`') ORDER BY k.ORDINAL_POSITION SEPARATOR ', '),
       r.DELETE_RULE,
       r.UPDATE_RULE
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.REFERENTIAL_CONSTRAINTS r
  ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
 AND r.TABLE_NAME = k.TABLE_NAME
 AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
WHERE k.TABLE_SCHEMA = DATABASE()
  AND k.REFERENCED_TABLE_NAME IS NOT NULL
GROUP BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.REFERENCED_TABLE_NAME, r.DELETE_RULE, r.UPDATE_RULE;

CALL drop_foreign_keys();
SET FOREIGN_KEY_CHECKS = 0;
CALL convert_table_collation('organizations');
CALL convert_table_collation('users');
CALL convert_table_collation('roles');
CALL convert_table_collation('farms');
CALL convert_table_collation('user_farm_assignments');
CALL convert_table_collation('animal_species');
CALL convert_table_collation('cattle_breeds');
CALL convert_table_collation('animals');
CALL convert_table_collation('animal_identifiers');
CALL convert_table_collation('animal_farm_history');
CALL convert_table_collation('milking_shifts');
CALL convert_table_collation('milk_records');
CALL convert_table_collation('veterinary_visits');
CALL convert_table_collation('vaccinations');
CALL convert_table_collation('treatments');
CALL convert_table_collation('medicine_products');
CALL convert_table_collation('medicine_batches');
CALL convert_table_collation('treatment_medications');
CALL convert_table_collation('medicine_stock_movements');
CALL convert_table_collation('breeding_records');
CALL convert_table_collation('pregnancy_records');
CALL convert_table_collation('calving_records');
CALL convert_table_collation('animal_weight_records');
CALL convert_table_collation('animal_events');
CALL convert_table_collation('audit_log');
CALL convert_table_collation('semen_straws');
CALL convert_table_collation('flyway_schema_history');
SET FOREIGN_KEY_CHECKS = 1;
CALL restore_foreign_keys();
DROP TABLE IF EXISTS fk_rebuild;
DROP PROCEDURE IF EXISTS convert_table_collation;
DROP PROCEDURE IF EXISTS drop_foreign_keys;
DROP PROCEDURE IF EXISTS restore_foreign_keys;

DROP TRIGGER IF EXISTS trg_apply_medicine_stock_movement;
DROP TRIGGER IF EXISTS trg_stock_movement_immutable_update;
DROP TRIGGER IF EXISTS trg_stock_movement_immutable_delete;
DROP TRIGGER IF EXISTS trg_validate_animal_parentage_insert;
DROP TRIGGER IF EXISTS trg_validate_animal_parentage_update;
DROP TRIGGER IF EXISTS trg_animals_audit_insert;
DROP TRIGGER IF EXISTS trg_animals_audit_update;
DROP PROCEDURE IF EXISTS check_breeding_compatibility;

CREATE TRIGGER trg_apply_medicine_stock_movement
BEFORE INSERT ON medicine_stock_movements
FOR EACH ROW
BEGIN
    DECLARE v_stock DECIMAL(14,3);
    SELECT quantity_on_hand INTO v_stock
      FROM medicine_batches WHERE id = NEW.batch_id FOR UPDATE;
    IF v_stock IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medicine batch not found';
    END IF;
    IF v_stock + NEW.quantity_change < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient medicine stock';
    END IF;
    UPDATE medicine_batches
       SET quantity_on_hand = quantity_on_hand + NEW.quantity_change,
           updated_at = CURRENT_TIMESTAMP(6)
     WHERE id = NEW.batch_id;
END;

CREATE TRIGGER trg_stock_movement_immutable_update
BEFORE UPDATE ON medicine_stock_movements
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Stock movements cannot be updated; post a correcting movement';
END;

CREATE TRIGGER trg_stock_movement_immutable_delete
BEFORE DELETE ON medicine_stock_movements
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Stock movements cannot be deleted; post a correcting movement';
END;

CREATE TRIGGER trg_validate_animal_parentage_insert
BEFORE INSERT ON animals
FOR EACH ROW
BEGIN
    DECLARE v_sex VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_org CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_dob DATE;
    IF NEW.dam_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.dam_id;
        IF v_sex IS NULL OR v_sex <> 'FEMALE' COLLATE utf8mb4_general_ci THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must exist and be female';
        END IF;
        IF v_org <> NEW.organization_id COLLATE utf8mb4_general_ci THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must belong to the same organization';
        END IF;
        IF NEW.date_of_birth IS NOT NULL AND v_dob IS NOT NULL AND v_dob >= NEW.date_of_birth THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must be older than offspring';
        END IF;
    END IF;
    IF NEW.sire_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.sire_id;
        IF v_sex IS NULL OR v_sex <> 'MALE' COLLATE utf8mb4_general_ci THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sire must exist and be male';
        END IF;
        IF v_org <> NEW.organization_id COLLATE utf8mb4_general_ci THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sire must belong to the same organization';
        END IF;
        IF NEW.date_of_birth IS NOT NULL AND v_dob IS NOT NULL AND v_dob >= NEW.date_of_birth THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sire must be older than offspring';
        END IF;
    END IF;
END;

CREATE TRIGGER trg_validate_animal_parentage_update
BEFORE UPDATE ON animals
FOR EACH ROW
BEGIN
    DECLARE v_sex VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_org CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_dob DATE;
    IF NEW.dam_id = NEW.id OR NEW.sire_id = NEW.id OR
       (NEW.dam_id IS NOT NULL AND NEW.dam_id = NEW.sire_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid parentage relationship';
    END IF;
    IF NEW.dam_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.dam_id;
        IF v_sex <> 'FEMALE' COLLATE utf8mb4_general_ci OR v_org <> NEW.organization_id COLLATE utf8mb4_general_ci THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must be female in same organization';
        END IF;
        IF NEW.date_of_birth IS NOT NULL AND v_dob IS NOT NULL AND v_dob >= NEW.date_of_birth THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must be older than offspring';
        END IF;
    END IF;
    IF NEW.sire_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.sire_id;
        IF v_sex <> 'MALE' COLLATE utf8mb4_general_ci OR v_org <> NEW.organization_id COLLATE utf8mb4_general_ci THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sire must be male in same organization';
        END IF;
        IF NEW.date_of_birth IS NOT NULL AND v_dob IS NOT NULL AND v_dob >= NEW.date_of_birth THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sire must be older than offspring';
        END IF;
    END IF;
END;

CREATE PROCEDURE check_breeding_compatibility(
    IN p_cow_id CHAR(36),
    IN p_sire_id CHAR(36),
    IN p_max_generations INT
)
BEGIN
    DECLARE v_cow_sex VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_sire_sex VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_cow_org CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_sire_org CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    DECLARE v_common INT DEFAULT 0;
    DECLARE v_depth INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        DROP TEMPORARY TABLE IF EXISTS tmp_cow_anc;
        DROP TEMPORARY TABLE IF EXISTS tmp_sire_anc;
        DROP TEMPORARY TABLE IF EXISTS tmp_frontier;
        DROP TEMPORARY TABLE IF EXISTS tmp_next;
        RESIGNAL;
    END;

    IF p_max_generations IS NULL OR p_max_generations < 1 OR p_max_generations > 20 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Generation limit must be between 1 and 20';
    END IF;

    SELECT sex, organization_id INTO v_cow_sex, v_cow_org FROM animals WHERE id = p_cow_id;
    SELECT sex, organization_id INTO v_sire_sex, v_sire_org FROM animals WHERE id = p_sire_id;

    IF v_cow_sex IS NULL OR v_sire_sex IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cow or sire not found';
    END IF;
    IF v_cow_sex <> 'FEMALE' COLLATE utf8mb4_general_ci OR v_sire_sex <> 'MALE' COLLATE utf8mb4_general_ci THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Breeding requires a female cow and a male sire';
    END IF;
    IF p_cow_id = p_sire_id OR v_cow_org <> v_sire_org THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Animals must be distinct and belong to the same organization';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_cow_anc;
    DROP TEMPORARY TABLE IF EXISTS tmp_sire_anc;
    DROP TEMPORARY TABLE IF EXISTS tmp_frontier;
    DROP TEMPORARY TABLE IF EXISTS tmp_next;

    CREATE TEMPORARY TABLE tmp_cow_anc (id CHAR(36) PRIMARY KEY, depth INT NOT NULL) ENGINE=MEMORY;
    CREATE TEMPORARY TABLE tmp_sire_anc (id CHAR(36) PRIMARY KEY, depth INT NOT NULL) ENGINE=MEMORY;
    CREATE TEMPORARY TABLE tmp_frontier (id CHAR(36) PRIMARY KEY) ENGINE=MEMORY;
    CREATE TEMPORARY TABLE tmp_next (id CHAR(36) PRIMARY KEY) ENGINE=MEMORY;

    INSERT INTO tmp_cow_anc (id, depth) VALUES (p_cow_id, 0);
    INSERT INTO tmp_frontier (id) VALUES (p_cow_id);
    SET v_depth = 0;
    WHILE v_depth < p_max_generations DO
        INSERT IGNORE INTO tmp_next (id)
            SELECT a.dam_id FROM animals a JOIN tmp_frontier f ON a.id = f.id WHERE a.dam_id IS NOT NULL;
        INSERT IGNORE INTO tmp_next (id)
            SELECT a.sire_id FROM animals a JOIN tmp_frontier f ON a.id = f.id WHERE a.sire_id IS NOT NULL;
        INSERT IGNORE INTO tmp_cow_anc (id, depth)
            SELECT id, v_depth + 1 FROM tmp_next;
        DELETE FROM tmp_frontier;
        INSERT INTO tmp_frontier (id) SELECT id FROM tmp_next;
        DELETE FROM tmp_next;
        SET v_depth = v_depth + 1;
    END WHILE;

    DELETE FROM tmp_frontier;
    INSERT INTO tmp_sire_anc (id, depth) VALUES (p_sire_id, 0);
    INSERT INTO tmp_frontier (id) VALUES (p_sire_id);
    SET v_depth = 0;
    WHILE v_depth < p_max_generations DO
        INSERT IGNORE INTO tmp_next (id)
            SELECT a.dam_id FROM animals a JOIN tmp_frontier f ON a.id = f.id WHERE a.dam_id IS NOT NULL;
        INSERT IGNORE INTO tmp_next (id)
            SELECT a.sire_id FROM animals a JOIN tmp_frontier f ON a.id = f.id WHERE a.sire_id IS NOT NULL;
        INSERT IGNORE INTO tmp_sire_anc (id, depth)
            SELECT id, v_depth + 1 FROM tmp_next;
        DELETE FROM tmp_frontier;
        INSERT INTO tmp_frontier (id) SELECT id FROM tmp_next;
        DELETE FROM tmp_next;
        SET v_depth = v_depth + 1;
    END WHILE;

    SELECT COUNT(*) INTO v_common
      FROM tmp_cow_anc c JOIN tmp_sire_anc s ON s.id = c.id;

    DROP TEMPORARY TABLE IF EXISTS tmp_cow_anc;
    DROP TEMPORARY TABLE IF EXISTS tmp_sire_anc;
    DROP TEMPORARY TABLE IF EXISTS tmp_frontier;
    DROP TEMPORARY TABLE IF EXISTS tmp_next;

    IF v_common > 0 THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT = 'Breeding rejected: shared ancestor found within configured generations';
    END IF;
END;

CREATE TRIGGER trg_animals_audit_insert AFTER INSERT ON animals
FOR EACH ROW
BEGIN
    INSERT INTO audit_log(table_name, record_id, action, changed_by, new_data)
    VALUES ('animals', NEW.id, 'INSERT', @app_user_id, JSON_OBJECT(
        'animal_number', NEW.animal_number, 'name', NEW.name, 'sex', NEW.sex,
        'date_of_birth', NEW.date_of_birth, 'dam_id', NEW.dam_id, 'sire_id', NEW.sire_id,
        'status', NEW.status));
END;

CREATE TRIGGER trg_animals_audit_update AFTER UPDATE ON animals
FOR EACH ROW
BEGIN
    INSERT INTO audit_log(table_name, record_id, action, changed_by, old_data, new_data)
    VALUES ('animals', NEW.id, 'UPDATE', @app_user_id,
        JSON_OBJECT('animal_number', OLD.animal_number, 'name', OLD.name, 'sex', OLD.sex,
                    'date_of_birth', OLD.date_of_birth, 'dam_id', OLD.dam_id, 'sire_id', OLD.sire_id,
                    'status', OLD.status),
        JSON_OBJECT('animal_number', NEW.animal_number, 'name', NEW.name, 'sex', NEW.sex,
                    'date_of_birth', NEW.date_of_birth, 'dam_id', NEW.dam_id, 'sire_id', NEW.sire_id,
                    'status', NEW.status));
END;
