#!/bin/bash
set -e
set -u

function create_user_and_database() {
  local database=$1
  local username=$2
  echo "  Creating user '$username' and database '$database'"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER $username WITH PASSWORD '$username';
    CREATE DATABASE $database OWNER $username;
    GRANT ALL PRIVILEGES ON DATABASE $database TO $username;
EOSQL
}

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
  echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"

  IFS=',' read -ra PAIRS <<< "$POSTGRES_MULTIPLE_DATABASES"

  for ((i=0; i<${#PAIRS[@]}; i+=2)); do
    database="${PAIRS[i]}"
    username="${PAIRS[i+1]}"

    if [ -n "$database" ] && [ -n "$username" ]; then
      create_user_and_database "$database" "$username"
    else
      echo "Warning: Incomplete pair at index $i - database: '$database', username: '$username'"
    fi
  done

  echo "Multiple databases created"
fi
