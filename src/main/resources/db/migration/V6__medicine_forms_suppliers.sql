-- Medicine kinds, countable units, and suppliers/pharmacies.
-- New tables use utf8mb4_general_ci so they match the database after V5.

ALTER TABLE medicine_products
    ADD COLUMN form VARCHAR(30) NOT NULL DEFAULT 'OTHER' AFTER active_ingredient;

UPDATE medicine_products
SET form = CASE
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%inject%' THEN 'INJECTION'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%tablet%' OR LOWER(base_unit) IN ('tablet', 'tablets') THEN 'TABLET'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%capsule%' OR LOWER(base_unit) IN ('capsule', 'capsules') THEN 'CAPSULE'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%bolus%' OR LOWER(base_unit) = 'bolus' THEN 'BOLUS'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%ointment%' OR LOWER(COALESCE(formulation, '')) LIKE '%cream%' THEN 'OINTMENT'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%powder%' THEN 'POWDER'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%pour%' THEN 'POUR_ON'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%intramammary%' OR LOWER(COALESCE(formulation, '')) LIKE '%intramam%' THEN 'INTRAMAMMARY'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%suspension%' THEN 'SUSPENSION'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%spray%' THEN 'SPRAY'
    WHEN LOWER(COALESCE(formulation, '')) LIKE '%liquid%' OR LOWER(base_unit) IN ('ml', 'millilitre', 'milliliter', 'l', 'litre', 'liter') THEN 'LIQUID'
    ELSE 'OTHER'
END;

UPDATE medicine_products
SET base_unit = CASE LOWER(TRIM(base_unit))
    WHEN 'ml' THEN 'ML'
    WHEN 'millilitre' THEN 'ML'
    WHEN 'milliliter' THEN 'ML'
    WHEN 'l' THEN 'LITER'
    WHEN 'litre' THEN 'LITER'
    WHEN 'liter' THEN 'LITER'
    WHEN 'tablet' THEN 'TABLET'
    WHEN 'tablets' THEN 'TABLET'
    WHEN 'capsule' THEN 'CAPSULE'
    WHEN 'capsules' THEN 'CAPSULE'
    WHEN 'bolus' THEN 'BOLUS'
    WHEN 'dose' THEN 'DOSE'
    WHEN 'doses' THEN 'DOSE'
    WHEN 'vial' THEN 'VIAL'
    WHEN 'vials' THEN 'VIAL'
    WHEN 'piece' THEN 'PIECE'
    WHEN 'pieces' THEN 'PIECE'
    WHEN 'g' THEN 'GRAM'
    WHEN 'gram' THEN 'GRAM'
    WHEN 'grams' THEN 'GRAM'
    WHEN 'sachet' THEN 'SACHET'
    WHEN 'sachets' THEN 'SACHET'
    WHEN 'tube' THEN 'TUBE'
    WHEN 'tubes' THEN 'TUBE'
    ELSE UPPER(TRIM(base_unit))
END;

UPDATE medicine_products
SET description = TRIM(CONCAT(COALESCE(description, ''), ' Previous unit: ', base_unit)),
    base_unit = 'PIECE'
WHERE base_unit NOT IN (
    'ML', 'LITER', 'TABLET', 'CAPSULE', 'BOLUS', 'DOSE', 'VIAL', 'PIECE', 'GRAM', 'SACHET', 'TUBE'
);

ALTER TABLE medicine_products
    MODIFY form VARCHAR(30) NOT NULL,
    ADD CONSTRAINT chk_medicine_products_form CHECK (form IN (
        'LIQUID', 'TABLET', 'INJECTION', 'CAPSULE', 'BOLUS', 'POWDER', 'OINTMENT',
        'SUSPENSION', 'POUR_ON', 'INTRAMAMMARY', 'SPRAY', 'OTHER'
    )),
    ADD CONSTRAINT chk_medicine_products_unit CHECK (base_unit IN (
        'ML', 'LITER', 'TABLET', 'CAPSULE', 'BOLUS', 'DOSE', 'VIAL', 'PIECE', 'GRAM', 'SACHET', 'TUBE'
    ));

CREATE TABLE suppliers (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    phone VARCHAR(40),
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (organization_id, name),
    CONSTRAINT fk_suppliers_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT chk_suppliers_kind CHECK (kind IN ('SUPPLIER', 'PHARMACY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE INDEX idx_suppliers_organization ON suppliers (organization_id, name);

ALTER TABLE medicine_batches
    ADD COLUMN supplier_id CHAR(36) NULL AFTER supplier_name,
    ADD CONSTRAINT fk_medicine_batches_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id);
