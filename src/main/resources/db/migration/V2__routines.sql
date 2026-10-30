-- Stock balance, parentage, breeding compatibility, and animal audit.
-- The breeding check includes each animal (depth 0) so a direct parent/offspring
-- mating is rejected even when the older animal has no recorded parents.
-- The draft procedure compared an undeclared v_cow_id and omitted self, so both are corrected here.
-- Flyway's MySQL parser understands BEGIN/END, so client DELIMITER commands are omitted.

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
    DECLARE v_sex VARCHAR(10);
    DECLARE v_org CHAR(36);
    DECLARE v_dob DATE;
    IF NEW.dam_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.dam_id;
        IF v_sex IS NULL OR v_sex <> 'FEMALE' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must exist and be female';
        END IF;
        IF v_org <> NEW.organization_id THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must belong to the same organization';
        END IF;
        IF NEW.date_of_birth IS NOT NULL AND v_dob IS NOT NULL AND v_dob >= NEW.date_of_birth THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must be older than offspring';
        END IF;
    END IF;
    IF NEW.sire_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.sire_id;
        IF v_sex IS NULL OR v_sex <> 'MALE' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sire must exist and be male';
        END IF;
        IF v_org <> NEW.organization_id THEN
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
    DECLARE v_sex VARCHAR(10);
    DECLARE v_org CHAR(36);
    DECLARE v_dob DATE;
    IF NEW.dam_id = NEW.id OR NEW.sire_id = NEW.id OR
       (NEW.dam_id IS NOT NULL AND NEW.dam_id = NEW.sire_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid parentage relationship';
    END IF;
    IF NEW.dam_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.dam_id;
        IF v_sex <> 'FEMALE' OR v_org <> NEW.organization_id THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must be female in same organization';
        END IF;
        IF NEW.date_of_birth IS NOT NULL AND v_dob IS NOT NULL AND v_dob >= NEW.date_of_birth THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dam must be older than offspring';
        END IF;
    END IF;
    IF NEW.sire_id IS NOT NULL THEN
        SELECT sex, organization_id, date_of_birth INTO v_sex, v_org, v_dob
          FROM animals WHERE id = NEW.sire_id;
        IF v_sex <> 'MALE' OR v_org <> NEW.organization_id THEN
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
    DECLARE v_cow_sex VARCHAR(10);
    DECLARE v_sire_sex VARCHAR(10);
    DECLARE v_cow_org CHAR(36);
    DECLARE v_sire_org CHAR(36);
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
    IF v_cow_sex <> 'FEMALE' OR v_sire_sex <> 'MALE' THEN
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
