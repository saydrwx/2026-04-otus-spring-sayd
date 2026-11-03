CREATE TABLE doctors (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  education VARCHAR(1000),
  experience_years INTEGER NOT NULL CHECK(experience_years >= 0),
  email VARCHAR(255),
  hospital_id BIGINT NOT NULL REFERENCES hospitals(id) ON DELETE RESTRICT
)