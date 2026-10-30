-- Farm Management System (InnoDB, utf8mb4_unicode_ci)
-- One organization, multiple farms. Cattle-focused, species-extensible.
-- All monetary amounts are Rwandan francs (RWF). There is no currency column.
-- UUIDs are CHAR(36). Application timestamps are UTC.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- -----------------------------------------------------------------------------
-- 1. Organization, users, farms, roles and access
-- -----------------------------------------------------------------------------
CREATE TABLE organizations (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(160) NOT NULL,
    legal_name VARCHAR(200),
    registration_number VARCHAR(100),
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Kigali',
    singleton_key BOOLEAN NOT NULL DEFAULT TRUE UNIQUE CHECK (singleton_key),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    organization_id CHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(40),
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_users_org_email UNIQUE (organization_id, email),
    CHECK (CHAR_LENGTH(TRIM(full_name)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (code, name, description) VALUES
 ('ORG_ADMIN', 'Organization administrator', 'Full organization configuration and access management'),
 ('FARM_MANAGER', 'Farm manager', 'Manage assigned farms and farm operations'),
 ('VETERINARIAN', 'Veterinarian', 'Manage clinical, breeding and animal health records'),
 ('FARM_WORKER', 'Farm worker', 'Record routine farm and milk production data'),
 ('INVENTORY_MANAGER', 'Inventory manager', 'Manage medicine inventory and stock movements'),
 ('READ_ONLY', 'Read only', 'Read permitted farm records');

CREATE TABLE farms (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    organization_id CHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    name VARCHAR(160) NOT NULL,
    farm_code VARCHAR(40) NOT NULL,
    location_text VARCHAR(255),
    district VARCHAR(100),
    province VARCHAR(100),
    country_code CHAR(2) NOT NULL DEFAULT 'RW',
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Kigali',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (organization_id, farm_code),
    UNIQUE (id, organization_id),
    CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_farm_assignments (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    role_code VARCHAR(40) NOT NULL REFERENCES roles(code) ON DELETE RESTRICT,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at DATETIME(6),
    assigned_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    ended_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    CHECK (ended_at IS NULL OR ended_at >= assigned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE user_farm_assignments
    ADD COLUMN active_assignment_key VARCHAR(200)
    GENERATED ALWAYS AS (
      CASE WHEN ended_at IS NULL
      THEN CONCAT(user_id, ':', farm_id, ':', role_code) ELSE NULL END
    ) STORED;
CREATE UNIQUE INDEX uq_user_active_farm_role
    ON user_farm_assignments (active_assignment_key);
CREATE INDEX idx_user_farm_assignments_farm_active ON user_farm_assignments (farm_id, user_id, ended_at);

-- -----------------------------------------------------------------------------
-- 2. Animal catalogue, animals, identification and farm history
-- -----------------------------------------------------------------------------
CREATE TABLE animal_species (
    id SMALLINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO animal_species (code, name) VALUES ('CATTLE', 'Cattle');

CREATE TABLE cattle_breeds (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE animals (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    organization_id CHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    species_id SMALLINT NOT NULL REFERENCES animal_species(id) ON DELETE RESTRICT,
    breed_id CHAR(36) REFERENCES cattle_breeds(id) ON DELETE SET NULL,
    animal_number VARCHAR(60) NOT NULL,
    name VARCHAR(100),
    sex VARCHAR(10) NOT NULL CHECK (sex IN ('MALE', 'FEMALE', 'UNKNOWN')),
    date_of_birth DATE,
    birth_date_is_estimated BOOLEAN NOT NULL DEFAULT FALSE,
    color_markings TEXT,
    acquisition_type VARCHAR(20) NOT NULL DEFAULT 'BORN_ON_FARM'
        CHECK (acquisition_type IN ('BORN_ON_FARM', 'PURCHASED', 'TRANSFERRED_IN', 'OTHER')),
    acquired_on DATE,
    dam_id CHAR(36) REFERENCES animals(id) ON DELETE RESTRICT,
    sire_id CHAR(36) REFERENCES animals(id) ON DELETE RESTRICT,
    status VARCHAR(25) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SOLD', 'DECEASED', 'LOST', 'CULLED', 'TRANSFERRED_OUT')),
    status_changed_on DATE,
    notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (organization_id, animal_number),
    UNIQUE (id, organization_id),
    CHECK (dam_id IS NULL OR dam_id <> id),
    CHECK (sire_id IS NULL OR sire_id <> id),
    CHECK (dam_id IS NULL OR sire_id IS NULL OR dam_id <> sire_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_animals_org_status ON animals (organization_id, status);
CREATE INDEX idx_animals_dam ON animals (dam_id);
CREATE INDEX idx_animals_sire ON animals (sire_id);
CREATE INDEX idx_animals_breed ON animals (breed_id);
CREATE INDEX idx_animals_dob ON animals (date_of_birth);

CREATE TABLE animal_identifiers (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    identifier_type VARCHAR(30) NOT NULL CHECK (identifier_type IN
        ('VISUAL_EAR_TAG', 'RFID_EAR_TAG', 'INJECTABLE_RFID', 'TATTOO', 'OTHER')),
    identifier_value VARCHAR(120) NOT NULL,
    issuing_authority VARCHAR(120),
    issued_at DATE,
    retired_at DATE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    CHECK (retired_at IS NULL OR issued_at IS NULL OR retired_at >= issued_at),
    UNIQUE (identifier_type, identifier_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_animal_identifiers_animal ON animal_identifiers (animal_id);
ALTER TABLE animal_identifiers
    ADD COLUMN active_primary_animal_id CHAR(36)
    GENERATED ALWAYS AS (
      CASE WHEN is_primary = TRUE AND retired_at IS NULL THEN animal_id ELSE NULL END
    ) STORED;
CREATE UNIQUE INDEX uq_animal_one_active_primary_identifier
    ON animal_identifiers (active_primary_animal_id);
CREATE INDEX idx_animal_identifiers_active_value ON animal_identifiers (identifier_value, retired_at);

CREATE TABLE animal_farm_history (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    joined_on DATE NOT NULL DEFAULT (CURRENT_DATE),
    left_on DATE,
    transfer_reason VARCHAR(30) CHECK (transfer_reason IS NULL OR transfer_reason IN
        ('BIRTH', 'PURCHASE', 'TRANSFER_IN', 'TRANSFER_OUT', 'SALE', 'OTHER')),
    notes TEXT,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    transferred_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    CHECK (left_on IS NULL OR left_on >= joined_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE animal_farm_history
    ADD COLUMN current_animal_id CHAR(36)
    GENERATED ALWAYS AS (CASE WHEN left_on IS NULL THEN animal_id ELSE NULL END) STORED;
CREATE UNIQUE INDEX uq_animal_current_farm ON animal_farm_history (current_animal_id);
CREATE INDEX idx_animal_farm_history_farm_dates
    ON animal_farm_history (farm_id, joined_on, left_on);

-- -----------------------------------------------------------------------------
-- 3. Milk shifts and milk production
-- -----------------------------------------------------------------------------
CREATE TABLE milking_shifts (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    name VARCHAR(60) NOT NULL,
    start_time TIME,
    end_time TIME,
    display_order SMALLINT NOT NULL DEFAULT 1 CHECK (display_order > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (farm_id, name),
    UNIQUE (id, farm_id),
    CHECK (start_time IS NULL OR end_time IS NULL OR start_time <> end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE milk_records (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    shift_id CHAR(36) NOT NULL,
    milking_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    quantity_liters NUMERIC(10,3) NOT NULL CHECK (quantity_liters >= 0),
    fat_percent NUMERIC(5,2) CHECK (fat_percent BETWEEN 0 AND 100),
    protein_percent NUMERIC(5,2) CHECK (protein_percent BETWEEN 0 AND 100),
    somatic_cell_count INTEGER CHECK (somatic_cell_count IS NULL OR somatic_cell_count >= 0),
    notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (shift_id, farm_id) REFERENCES milking_shifts(id, farm_id) ON DELETE RESTRICT,
    UNIQUE (animal_id, farm_id, shift_id, milking_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_milk_farm_date ON milk_records (farm_id, milking_date DESC);
CREATE INDEX idx_milk_animal_date ON milk_records (animal_id, milking_date DESC);

-- -----------------------------------------------------------------------------
-- 4. Veterinary records, vaccinations and treatments
-- -----------------------------------------------------------------------------
CREATE TABLE veterinary_visits (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    visit_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    visit_type VARCHAR(30) NOT NULL DEFAULT 'EXAMINATION'
        CHECK (visit_type IN ('EXAMINATION', 'ILLNESS', 'INJURY', 'VACCINATION', 'FOLLOW_UP', 'OTHER')),
    symptoms TEXT,
    diagnosis TEXT,
    temperature_c NUMERIC(4,1) CHECK (temperature_c IS NULL OR temperature_c BETWEEN 25 AND 45),
    weight_kg NUMERIC(8,2) CHECK (weight_kg IS NULL OR weight_kg > 0),
    vet_notes TEXT,
    follow_up_at DATETIME(6),
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_vet_visits_animal_date ON veterinary_visits (animal_id, visit_at DESC);
CREATE INDEX idx_vet_visits_farm_date ON veterinary_visits (farm_id, visit_at DESC);

CREATE TABLE vaccinations (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    veterinary_visit_id CHAR(36) REFERENCES veterinary_visits(id) ON DELETE RESTRICT,
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    vaccine_name VARCHAR(160) NOT NULL,
    administered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    dose VARCHAR(80),
    next_due_on DATE,
    batch_number VARCHAR(100),
    administered_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (next_due_on IS NULL OR next_due_on >= DATE(administered_at))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_vaccinations_animal_date ON vaccinations (animal_id, administered_at DESC);
CREATE INDEX idx_vaccinations_due ON vaccinations (next_due_on);

CREATE TABLE treatments (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    veterinary_visit_id CHAR(36) REFERENCES veterinary_visits(id) ON DELETE RESTRICT,
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    treatment_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    diagnosis TEXT,
    treatment_plan TEXT,
    outcome VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (outcome IN ('IN_PROGRESS', 'RECOVERED', 'FOLLOW_UP_REQUIRED', 'REFERRED', 'OTHER')),
    milk_withdrawal_until DATETIME(6),
    meat_withdrawal_until DATETIME(6),
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_treatments_animal_date ON treatments (animal_id, treatment_date DESC);
CREATE INDEX idx_treatments_farm_date ON treatments (farm_id, treatment_date DESC);
CREATE INDEX idx_treatments_milk_withdrawal ON treatments (milk_withdrawal_until);

-- -----------------------------------------------------------------------------
-- 5. Medicine catalogue, batches, stock movements and treatment usage
-- unit_cost is always RWF.
-- -----------------------------------------------------------------------------
CREATE TABLE medicine_products (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    organization_id CHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    name VARCHAR(160) NOT NULL,
    active_ingredient VARCHAR(160),
    formulation VARCHAR(100),
    base_unit VARCHAR(30) NOT NULL,
    description TEXT,
    requires_prescription BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (organization_id, name, formulation),
    CHECK (CHAR_LENGTH(TRIM(base_unit)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medicine_batches (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    medicine_product_id CHAR(36) NOT NULL REFERENCES medicine_products(id) ON DELETE RESTRICT,
    batch_number VARCHAR(100) NOT NULL,
    expiry_date DATE NOT NULL,
    received_on DATE NOT NULL DEFAULT (CURRENT_DATE),
    supplier_name VARCHAR(160),
    unit_cost NUMERIC(14,2) COMMENT 'Cost per base unit, in RWF',
    quantity_on_hand NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (farm_id, medicine_product_id, batch_number),
    CHECK (unit_cost IS NULL OR unit_cost >= 0),
    CHECK (expiry_date >= received_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_medicine_batches_expiry ON medicine_batches (farm_id, expiry_date);
CREATE INDEX idx_medicine_batches_product ON medicine_batches (medicine_product_id, farm_id);

CREATE TABLE treatment_medications (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    treatment_id CHAR(36) NOT NULL REFERENCES treatments(id) ON DELETE RESTRICT,
    medicine_product_id CHAR(36) NOT NULL REFERENCES medicine_products(id) ON DELETE RESTRICT,
    batch_id CHAR(36) REFERENCES medicine_batches(id) ON DELETE RESTRICT,
    dose_amount NUMERIC(14,3) NOT NULL CHECK (dose_amount > 0),
    dose_unit VARCHAR(30) NOT NULL,
    route VARCHAR(30) NOT NULL CHECK (route IN
        ('ORAL', 'INTRAMUSCULAR', 'SUBCUTANEOUS', 'INTRAVENOUS', 'TOPICAL', 'OTHER')),
    administrations_count SMALLINT NOT NULL DEFAULT 1 CHECK (administrations_count > 0),
    quantity_used NUMERIC(14,3) NOT NULL CHECK (quantity_used > 0),
    frequency VARCHAR(100),
    duration_text VARCHAR(100),
    milk_withdrawal_hours INTEGER CHECK (milk_withdrawal_hours IS NULL OR milk_withdrawal_hours >= 0),
    meat_withdrawal_hours INTEGER CHECK (meat_withdrawal_hours IS NULL OR meat_withdrawal_hours >= 0),
    notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_treatment_medications_treatment ON treatment_medications (treatment_id);
CREATE INDEX idx_treatment_medications_batch ON treatment_medications (batch_id);

CREATE TABLE medicine_stock_movements (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    batch_id CHAR(36) NOT NULL REFERENCES medicine_batches(id) ON DELETE RESTRICT,
    movement_type VARCHAR(30) NOT NULL CHECK (movement_type IN
        ('PURCHASE', 'TREATMENT_USE', 'EXPIRED', 'RETURN', 'ADJUSTMENT', 'TRANSFER_IN', 'TRANSFER_OUT')),
    quantity_change NUMERIC(14,3) NOT NULL CHECK (quantity_change <> 0),
    movement_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    treatment_medication_id CHAR(36) REFERENCES treatment_medications(id) ON DELETE RESTRICT,
    reference_text VARCHAR(160),
    notes TEXT,
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (
      (movement_type IN ('PURCHASE', 'RETURN', 'TRANSFER_IN') AND quantity_change > 0)
      OR (movement_type IN ('TREATMENT_USE', 'EXPIRED', 'TRANSFER_OUT') AND quantity_change < 0)
      OR movement_type = 'ADJUSTMENT'
    ),
    CHECK ((movement_type = 'TREATMENT_USE' AND treatment_medication_id IS NOT NULL)
        OR (movement_type <> 'TREATMENT_USE' AND treatment_medication_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_stock_movements_batch_date ON medicine_stock_movements (batch_id, movement_at DESC);
CREATE INDEX idx_stock_movements_type_date ON medicine_stock_movements (movement_type, movement_at DESC);
CREATE UNIQUE INDEX uq_stock_movement_treatment_use
    ON medicine_stock_movements (treatment_medication_id);

-- -----------------------------------------------------------------------------
-- 6. Breeding, pregnancy and calving
-- -----------------------------------------------------------------------------
CREATE TABLE breeding_records (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    cow_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    sire_id CHAR(36) REFERENCES animals(id) ON DELETE RESTRICT,
    breeding_at DATETIME(6) NOT NULL,
    breeding_method VARCHAR(30) NOT NULL CHECK (breeding_method IN
        ('NATURAL', 'ARTIFICIAL_INSEMINATION', 'EMBRYO_TRANSFER', 'OTHER')),
    outcome VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (outcome IN
        ('PENDING', 'PREGNANT', 'NOT_PREGNANT', 'CANCELLED', 'UNKNOWN')),
    technician_name VARCHAR(160),
    semen_batch_number VARCHAR(100),
    ancestry_reviewed_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    ancestry_override_reason TEXT,
    notes TEXT,
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    CHECK (sire_id IS NULL OR sire_id <> cow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_breeding_cow_date ON breeding_records (cow_id, breeding_at DESC);
CREATE INDEX idx_breeding_sire_date ON breeding_records (sire_id, breeding_at DESC);
CREATE INDEX idx_breeding_farm_date ON breeding_records (farm_id, breeding_at DESC);

CREATE TABLE pregnancy_records (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    breeding_record_id CHAR(36) REFERENCES breeding_records(id) ON DELETE RESTRICT,
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    cow_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    confirmed_at DATETIME(6),
    confirmation_method VARCHAR(40),
    expected_calving_on DATE,
    status VARCHAR(25) NOT NULL DEFAULT 'SUSPECTED' CHECK (status IN
        ('SUSPECTED', 'CONFIRMED', 'LOST', 'CALVED', 'FALSE_PREGNANCY', 'UNKNOWN')),
    notes TEXT,
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    CHECK (expected_calving_on IS NULL OR confirmed_at IS NULL OR expected_calving_on >= DATE(confirmed_at))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_pregnancy_cow_status ON pregnancy_records (cow_id, status);
CREATE INDEX idx_pregnancy_expected_calving ON pregnancy_records (expected_calving_on, status);

CREATE TABLE calving_records (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    pregnancy_record_id CHAR(36) REFERENCES pregnancy_records(id) ON DELETE RESTRICT,
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    dam_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    sire_id CHAR(36) REFERENCES animals(id) ON DELETE RESTRICT,
    calf_id CHAR(36) REFERENCES animals(id) ON DELETE RESTRICT,
    calved_at DATETIME(6) NOT NULL,
    outcome VARCHAR(30) NOT NULL CHECK (outcome IN
        ('LIVE', 'STILLBORN', 'ABORTION', 'DYSTOCIA', 'OTHER')),
    assistance_required BOOLEAN NOT NULL DEFAULT FALSE,
    number_of_calves SMALLINT NOT NULL DEFAULT 1 CHECK (number_of_calves > 0),
    complications TEXT,
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (sire_id IS NULL OR sire_id <> dam_id),
    CHECK (calf_id IS NULL OR (calf_id <> dam_id AND (sire_id IS NULL OR calf_id <> sire_id)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_calving_dam_date ON calving_records (dam_id, calved_at DESC);
CREATE INDEX idx_calving_calf ON calving_records (calf_id);

-- -----------------------------------------------------------------------------
-- 7. Weight tracking and general animal events
-- -----------------------------------------------------------------------------
CREATE TABLE animal_weight_records (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    farm_id CHAR(36) NOT NULL REFERENCES farms(id) ON DELETE RESTRICT,
    measured_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    weight_kg NUMERIC(8,2) NOT NULL CHECK (weight_kg > 0),
    measurement_method VARCHAR(30),
    notes TEXT,
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_animal_weight_history ON animal_weight_records (animal_id, measured_at DESC);

CREATE TABLE animal_events (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    animal_id CHAR(36) NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    farm_id CHAR(36) REFERENCES farms(id) ON DELETE RESTRICT,
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN
        ('PURCHASE', 'BIRTH', 'TRANSFER', 'SALE', 'DEATH', 'LOSS', 'CALVING', 'OTHER')),
    event_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    details TEXT,
    recorded_by CHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_animal_events_history ON animal_events (animal_id, event_at DESC);

-- -----------------------------------------------------------------------------
-- 8. Audit log
-- -----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(128) NOT NULL,
    record_id CHAR(36),
    action VARCHAR(10) NOT NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    changed_by CHAR(36),
    transaction_id BIGINT,
    old_data JSON,
    new_data JSON,
    INDEX idx_audit_log_table_record_time (table_name, record_id, changed_at),
    INDEX idx_audit_log_changed_at (changed_at)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 9. Reporting views
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_current_animal_farm AS
SELECT afh.animal_id, afh.farm_id, afh.joined_on
FROM animal_farm_history afh
WHERE afh.left_on IS NULL;

CREATE OR REPLACE VIEW v_daily_milk_totals AS
SELECT farm_id, milking_date, SUM(quantity_liters) AS total_liters,
       COUNT(DISTINCT animal_id) AS animals_milked
FROM milk_records
GROUP BY farm_id, milking_date;

CREATE OR REPLACE VIEW v_medicine_expiry_watch AS
SELECT mb.id AS batch_id, mb.farm_id, mp.name AS medicine_name,
       mb.batch_number, mb.expiry_date, mb.quantity_on_hand
FROM medicine_batches mb
JOIN medicine_products mp ON mp.id = mb.medicine_product_id
WHERE mb.quantity_on_hand > 0
  AND mb.expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY);
