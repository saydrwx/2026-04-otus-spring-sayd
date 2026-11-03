INSERT INTO doctors_specialties (doctor_id, specialty_id)
VALUES (1, 1),
       (1, 3),
       (2, 2),
       (2, 12)
ON CONFLICT (doctor_id, specialty_id) DO NOTHING;