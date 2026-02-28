#!/bin/bash
set -e

# Defaults
POSTGRES_USER="${POSTGRES_USER:-fitbit}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-fitbit}"
POSTGRES_DB="${POSTGRES_DB:-fitbit}"

PGDATA="${PGDATA:-/var/lib/postgresql/data}"
PG_CTL="/usr/lib/postgresql/17/bin/pg_ctl"
INITDB="/usr/lib/postgresql/17/bin/initdb"
PSQL="/usr/bin/psql"

# Initialize database cluster if not already done
if [ ! -f "$PGDATA/PG_VERSION" ]; then
    echo "Initializing PostgreSQL data directory..."
    mkdir -p "$PGDATA"
    chown postgres:postgres "$PGDATA"
    chmod 700 "$PGDATA"
    sudo -u postgres "$INITDB" \
        --pgdata="$PGDATA" \
        --auth=trust \
        --encoding=UTF8 \
        --locale=en_US.UTF-8

    # Allow password auth from localhost (needed for the app datasource)
    echo "host all all 127.0.0.1/32 md5" >> "$PGDATA/pg_hba.conf"
fi

# Start PostgreSQL in the background
echo "Starting PostgreSQL..."
sudo -u postgres "$PG_CTL" -D "$PGDATA" -l /tmp/postgresql.log start

# Wait until PostgreSQL is ready
echo "Waiting for PostgreSQL to be ready..."
until /usr/lib/postgresql/17/bin/pg_isready -h 127.0.0.1 -U postgres -q; do
    sleep 1
done
echo "PostgreSQL is ready."

# Idempotently create role and database
echo "Ensuring database user and database exist..."
sudo -u postgres "$PSQL" -h 127.0.0.1 <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${POSTGRES_USER}') THEN
            CREATE ROLE "${POSTGRES_USER}" LOGIN PASSWORD '${POSTGRES_PASSWORD}';
        END IF;
    END
    \$\$;

    SELECT 'CREATE DATABASE "${POSTGRES_DB}" OWNER "${POSTGRES_USER}"'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${POSTGRES_DB}')
    \gexec
EOSQL

# Ensure the user has full privileges on the public schema
sudo -u postgres "$PSQL" -h 127.0.0.1 -d "${POSTGRES_DB}" <<-EOSQL
    GRANT ALL ON SCHEMA public TO "${POSTGRES_USER}";
    ALTER SCHEMA public OWNER TO "${POSTGRES_USER}";
EOSQL

echo "PostgreSQL is ready. Starting Spring Boot application..."
exec java ${JAVA_OPTS:-} -jar /app/server.jar
