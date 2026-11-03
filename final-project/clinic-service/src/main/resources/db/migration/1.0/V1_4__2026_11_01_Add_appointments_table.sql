CREATE TABLE appointments (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  doctor_id BIGINT NOT NULL CHECK (doctor_id > 0),
  hospital_id BIGINT NOT NULL CHECK (hospital_id > 0),
  start_at TIMESTAMP WITH TIME ZONE NOT NULL,
  end_at TIMESTAMP WITH TIME ZONE NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
  booked_at TIMESTAMP WITH TIME ZONE NOT NULL,
  cancelled_at TIMESTAMP WITH TIME ZONE,
  cancelled_by UUID,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_appointments_duration CHECK (end_at = start_at + INTERVAL '30 minutes'),
  CONSTRAINT ck_appointments_start_alignment CHECK (
    EXTRACT(MINUTE FROM start_at AT TIME ZONE 'UTC') IN (0, 30)
    AND EXTRACT(SECOND FROM start_at AT TIME ZONE 'UTC') = 0
  ),
  CONSTRAINT ck_appointments_cancellation CHECK (
    (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
    OR (status <> 'CANCELLED' AND cancelled_at IS NULL AND cancelled_by IS NULL)
  )
);

CREATE UNIQUE INDEX uk_appointments_reserved_slot
  ON appointments (doctor_id, start_at) WHERE status <> 'CANCELLED';
CREATE INDEX ix_appointments_patient_start ON appointments (patient_id, start_at, id);
CREATE INDEX ix_appointments_doctor_start ON appointments (doctor_id, start_at, id);
