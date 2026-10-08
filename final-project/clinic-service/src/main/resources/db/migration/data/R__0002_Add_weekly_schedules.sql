INSERT INTO weekly_schedules (doctor_id, day_of_week, start_time, end_time)
SELECT doctor_id, day_of_week, TIME '09:00', TIME '17:00'
FROM (VALUES (1), (2)) AS doctors(doctor_id)
CROSS JOIN (VALUES (1), (2), (3), (4), (5)) AS weekdays(day_of_week)
ON CONFLICT (doctor_id, day_of_week) DO NOTHING;
