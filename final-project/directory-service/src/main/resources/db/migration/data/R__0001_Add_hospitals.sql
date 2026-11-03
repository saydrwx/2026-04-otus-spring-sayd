INSERT INTO hospitals (id, name, address)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Городская поликлиника №1', 'г. Москва, Ленинский проспект, д. 1'),
       (2, 'Межрегиональный хирургический центр', 'г. Самара, Московское шоссе, д. 42')
ON CONFLICT (id) DO NOTHING;

SELECT setval(
  pg_get_serial_sequence('hospitals', 'id'),
  COALESCE((SELECT MAX(id) FROM hospitals), 0) + 1,
  false
);