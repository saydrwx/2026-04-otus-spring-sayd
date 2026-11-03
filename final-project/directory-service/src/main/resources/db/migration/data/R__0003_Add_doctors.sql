INSERT INTO doctors (id, first_name, last_name, education, experience_years, email, hospital_id)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Иван', 'Иванов', 'Самарский государственный медицинский университет', 5, 'ivanov@yandex.ru', 2),
       (2, 'Петр', 'Петров', 'Академия медицины им. Пирогова', 20, 'petrov@gmail.com', 1)
ON CONFLICT (id) DO NOTHING;

SELECT setval(
  pg_get_serial_sequence('doctors', 'id'),
  COALESCE((SELECT MAX(id) FROM doctors), 0) + 1,
  false
);
