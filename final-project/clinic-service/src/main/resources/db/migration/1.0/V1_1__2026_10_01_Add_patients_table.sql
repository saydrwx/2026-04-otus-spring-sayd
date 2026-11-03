CREATE TABLE patients (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL,
  first_name VARCHAR(100) NOT NULL CHECK (LENGTH(TRIM(first_name)) > 0),
  last_name VARCHAR(100) NOT NULL CHECK (LENGTH(TRIM(last_name)) > 0),
  middle_name VARCHAR(100),
  birth_date DATE NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_patients_account UNIQUE (account_id)
);
