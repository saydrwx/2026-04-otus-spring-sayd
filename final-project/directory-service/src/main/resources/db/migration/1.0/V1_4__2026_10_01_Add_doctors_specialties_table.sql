CREATE TABLE doctors_specialties (
  doctor_id BIGINT REFERENCES doctors(id) ON DELETE CASCADE,
  specialty_id BIGINT REFERENCES specialties(id) ON DELETE CASCADE,
  PRIMARY KEY (doctor_id, specialty_id)
)