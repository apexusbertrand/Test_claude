#!/usr/bin/env bash
# Teste schema.sql + seed.sql + droits sur un Postgres local jetable.
# Usage : PGHOST=... PGPORT=... bash supabase/tests/run.sh
set -euo pipefail
cd "$(dirname "$0")/.."
P="psql -U ${PGUSER:-postgres} -v ON_ERROR_STOP=1 -q"
$P -c "drop database if exists nr_test" -c "create database nr_test"
$P -d nr_test -f tests/stub-supabase.sql
$P -d nr_test -f schema.sql 2>/dev/null
$P -d nr_test -f schema.sql 2>/dev/null   # relançable
$P -d nr_test -f seed.sql
$P -d nr_test -f seed.sql                 # relançable
$P -d nr_test -At -f tests/rls.sql
