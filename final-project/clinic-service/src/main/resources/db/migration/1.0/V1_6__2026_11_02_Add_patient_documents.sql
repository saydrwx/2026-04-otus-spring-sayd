CREATE TABLE patient_documents (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  filename VARCHAR(200) NOT NULL,
  media_type VARCHAR(100) NOT NULL CHECK (media_type IN ('application/pdf', 'image/png', 'image/jpeg')),
  size_bytes BIGINT NOT NULL CHECK (size_bytes BETWEEN 1 AND 5242880),
  sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
  status VARCHAR(20) NOT NULL CHECK (status IN ('STAGED', 'READY', 'DELETE_PENDING')),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  delete_requested_at TIMESTAMP WITH TIME ZONE,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_patient_documents_deletion CHECK (
    (status = 'DELETE_PENDING' AND delete_requested_at IS NOT NULL)
    OR (status <> 'DELETE_PENDING' AND delete_requested_at IS NULL)
  )
);
CREATE INDEX ix_patient_documents_patient ON patient_documents(patient_id,status,created_at,id);
CREATE INDEX ix_patient_documents_cleanup ON patient_documents(status,created_at);
