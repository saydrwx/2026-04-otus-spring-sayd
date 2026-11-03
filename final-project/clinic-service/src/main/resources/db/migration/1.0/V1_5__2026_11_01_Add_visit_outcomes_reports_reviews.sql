ALTER TABLE appointments ADD COLUMN outcome_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE appointments ADD COLUMN outcome_by UUID;
ALTER TABLE appointments ADD CONSTRAINT ck_appointments_outcome CHECK (
  (status IN ('COMPLETED', 'NO_SHOW') AND outcome_at IS NOT NULL AND outcome_by IS NOT NULL
    AND outcome_at >= end_at)
  OR (status IN ('BOOKED', 'CANCELLED') AND outcome_at IS NULL AND outcome_by IS NULL)
);

CREATE TABLE medical_reports (
  id UUID PRIMARY KEY,
  appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
  findings VARCHAR(10000) NOT NULL CHECK (LENGTH(TRIM(findings)) > 0),
  diagnosis VARCHAR(10000) NOT NULL CHECK (LENGTH(TRIM(diagnosis)) > 0),
  recommendations VARCHAR(10000) NOT NULL CHECK (LENGTH(TRIM(recommendations)) > 0),
  submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
  submitted_by UUID NOT NULL
);

CREATE TABLE reviews (
  id UUID PRIMARY KEY,
  appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
  rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  text VARCHAR(2000),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_appointments_record_access
  ON appointments (patient_id, doctor_id, hospital_id, status, start_at);
