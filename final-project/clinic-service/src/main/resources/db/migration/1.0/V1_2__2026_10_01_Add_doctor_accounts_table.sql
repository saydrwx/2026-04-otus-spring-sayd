CREATE TABLE doctor_accounts (
  account_id UUID PRIMARY KEY,
  doctor_id BIGINT NOT NULL UNIQUE CHECK (doctor_id > 0)
);
