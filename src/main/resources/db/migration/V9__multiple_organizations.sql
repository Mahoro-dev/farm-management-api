-- More than one organization. singleton_key was unique and had to be true,
-- so a second row was rejected as a duplicate.

ALTER TABLE organizations DROP INDEX IF EXISTS singleton_key;
ALTER TABLE organizations DROP CONSTRAINT IF EXISTS singleton_key;
ALTER TABLE organizations DROP COLUMN IF EXISTS singleton_key;
