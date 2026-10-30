ALTER TABLE breeding_records
    ADD COLUMN technician_id CHAR(36) NULL,
    ADD COLUMN semen_straw_id CHAR(36) NULL;

ALTER TABLE animals
    ADD COLUMN semen_straw_id CHAR(36) NULL;

ALTER TABLE calving_records
    ADD COLUMN semen_straw_id CHAR(36) NULL;

CREATE TABLE semen_straws (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (organization_id, code),
    CONSTRAINT fk_semen_straws_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE breeding_records
    ADD CONSTRAINT fk_breeding_technician FOREIGN KEY (technician_id) REFERENCES users(id);

ALTER TABLE breeding_records
    ADD CONSTRAINT fk_breeding_semen_straw FOREIGN KEY (semen_straw_id) REFERENCES semen_straws(id);

ALTER TABLE animals
    ADD CONSTRAINT fk_animals_semen_straw FOREIGN KEY (semen_straw_id) REFERENCES semen_straws(id);

ALTER TABLE calving_records
    ADD CONSTRAINT fk_calving_semen_straw FOREIGN KEY (semen_straw_id) REFERENCES semen_straws(id);
