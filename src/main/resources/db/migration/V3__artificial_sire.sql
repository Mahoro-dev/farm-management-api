ALTER TABLE animals
    ADD COLUMN artificial_sire_code VARCHAR(100) NULL;

ALTER TABLE calving_records
    ADD COLUMN artificial_sire_code VARCHAR(100) NULL;
