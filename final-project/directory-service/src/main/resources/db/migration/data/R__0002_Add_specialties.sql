INSERT INTO specialties (id, name)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Хирургия'),
       (2, 'Педиатрия'),
       (3, 'Кардиология'),
       (4, 'Неврология'),
       (5, 'Психиатрия'),
       (6, 'Дерматология'),
       (7, 'Офтальмология'),
       (8, 'Ортопедия'),
       (9, 'Гинекология'),
       (10, 'Отоларингология'),
       (11, 'Урология'),
       (12, 'Гастроэнтерология'),
       (13, 'Пульмонология'),
       (14, 'Эндокринология'),
       (15, 'Онкология')
ON CONFLICT (id) DO NOTHING;

SELECT setval(
  pg_get_serial_sequence('specialties', 'id'),
  COALESCE((SELECT MAX(id) FROM specialties), 0) + 1,
  false
);