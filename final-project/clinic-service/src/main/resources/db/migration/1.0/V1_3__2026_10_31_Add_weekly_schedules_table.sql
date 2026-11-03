CREATE TABLE weekly_schedules (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  doctor_id BIGINT NOT NULL CHECK (doctor_id > 0),
  day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  CONSTRAINT uk_weekly_schedules_doctor_day UNIQUE (doctor_id, day_of_week),
  CONSTRAINT ck_weekly_schedules_hours CHECK (
    start_time >= TIME '08:00' AND end_time <= TIME '18:00'
    AND end_time - start_time BETWEEN INTERVAL '6 hours' AND INTERVAL '8 hours'
  ),
  CONSTRAINT ck_weekly_schedules_alignment CHECK (
    EXTRACT(MINUTE FROM start_time) IN (0, 30)
    AND EXTRACT(MINUTE FROM end_time) IN (0, 30)
    AND EXTRACT(SECOND FROM start_time) = 0
    AND EXTRACT(SECOND FROM end_time) = 0
  )
);
