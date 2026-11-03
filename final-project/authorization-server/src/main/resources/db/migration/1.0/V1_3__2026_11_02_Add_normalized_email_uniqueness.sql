CREATE UNIQUE INDEX uk_accounts_normalized_email
  ON accounts ((lower(btrim(email) COLLATE "C")));
