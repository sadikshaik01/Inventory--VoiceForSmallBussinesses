# Phase 2 — PostgreSQL persistence

Verified on 19 September 2026 in the existing VoiceStock codebase.

## Result

**Phase 2 works with real local PostgreSQL 17.2.** Spring Boot connects, Flyway creates and validates the schema, Hibernate validates its entity mappings, repositories persist/read data, and committed data survives a complete Java application process restart.

Supabase-compatible JDBC configuration is ready, but the hosted Supabase connection is **not verified** because no project credentials were supplied. Local verification uses the same PostgreSQL driver, migration, schema, and repositories. Supabase remains the agreed database host; the local cluster is a development/verification environment.

Authentication, password hashing/login, inventory operations, voice, and AI have not been implemented. No public data-write endpoint or production seed data was added.

## Files added or changed in Phase 2

Backend source paths below are relative to `backend/src/main/java/com/voicestock/`.

- `entity/BaseEntity.java`, `AuditedEntity.java`: UUID identifiers and UTC audit lifecycle fields.
- `entity/User.java`, `Product.java`, `InventoryTransaction.java`: the three approved models and relationships.
- `entity/InventoryUnit.java`, `InventoryUnitConverter.java`, `TransactionType.java`, `TransactionSource.java`: approved units and transaction values.
- `entity/package-info.java`, `repository/package-info.java`: updated layer descriptions.
- `repository/UserRepository.java`, `ProductRepository.java`, `InventoryTransactionRepository.java`: JPA persistence and user-scoped product/transaction reads.
- `service/DatabaseHealthService.java`, `controller/HealthController.java`, `dto/HealthResponse.java`: actual database connection checks and safe health responses.
- `backend/src/main/resources/db/migration/V1__create_inventory_schema.sql`: initial schema and constraints.
- `backend/src/main/resources/application.properties`, `application-postgres.properties`, `application-bootstrap.properties`: PostgreSQL is now the normal profile; bootstrap is retained explicitly for no-database tests.
- `backend/.env.example`, `.env.test.example`: runtime and separate test-database templates.
- `backend/pom.xml`: opt-in Maven `database-tests` integration-test profile.
- `backend/src/test/java/com/voicestock/VoiceStockApplicationTests.java`: explicit bootstrap test profile.
- `backend/src/test/java/com/voicestock/database/TestDatabase.java`, `RepositoryIT.java`, `PersistenceProbe.java`, `DatabasePersistenceIT.java`: database fixtures, constraints, repositories, and separate-process restart verification.
- `scripts/start-local-postgres.ps1`, `stop-local-postgres.ps1`, `test-database.ps1`: reproducible local database/test helpers.
- `frontend/src/App.jsx`, `frontend/src/components/Brand.jsx`, `frontend/tests/health.spec.js`: Phase 2 label, actual database status, and browser assertions.
- `.gitignore`, `README.md`, `render.yaml`, this handoff: environment-file exclusions, current instructions, and PostgreSQL deployment variables.
- Local ignored files: `backend/.env`, `backend/.env.test`, and `.run/postgres/` hold generated local settings/data. Credentials were not hardcoded into source or printed.

The three approved Milestone 1 documents remain unchanged. No Git commit, push, or cloud deployment was performed.

## Tables created

Created in both the application database `voicestock` and separate test database `voicestock_test`:

1. `users`: id, name, email, password, business_name, preferred_language, created_at, updated_at.
2. `products`: id, user_id, name, category, unit, current_stock, minimum_stock, price, created_at, updated_at.
3. `inventory_transactions`: id, user_id, product_id, transaction_type, quantity, unit, source, created_at.
4. `flyway_schema_history`: Flyway's migration metadata, not an additional product feature.

Implementation choices fill in types and integrity rules omitted from the high-level schema: UUID IDs; decimal quantities/prices; timezone-aware timestamps; unique case-insensitive emails; nonnegative stock/minimums/prices; positive transaction quantities; supported unit/type/source checks; owner-consistent foreign keys; indexes for user/history lookups; no cascading deletion of history.

Row-level security is enabled on the three application tables without client policies to prevent direct Supabase browser/Data API access. The verified application role is a **non-superuser table owner**, so its direct backend connection can access the schema. This is infrastructure protection, not Phase 3 user authentication.

## Environment variables

Required for normal PostgreSQL operation:

- `DB_URL`: JDBC URL. For Supabase, use the direct or session pooler host with TLS, for example `jdbc:postgresql://YOUR_HOST:5432/postgres?sslmode=require`.
- `DB_USERNAME`: exact database user provided by your Supabase Connect panel.
- `DB_PASSWORD`: database password (not an API key).

Optional/defaulted: `SPRING_PROFILES_ACTIVE=postgres`, `DB_POOL_SIZE=5`, `PORT=8080`, `CORS_ALLOWED_ORIGINS`.

The local app is already configured on port 8081 with generated credentials. No manual credentials are needed for that local instance. To switch to Supabase, replace the three DB values privately in `backend/.env` and restart. The target should be a dedicated empty schema that the configured database role can migrate. Supabase's direct/session-pooler connection details are documented in its [official Spring Boot guide](https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot).

Database integration tests deliberately use separate `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` settings. Keep test fixtures off a production database. The local helper already created the ignored `.env.test` file.

## Tests and observed results

| Verification | Result |
| --- | --- |
| Java compilation and executable JAR packaging | Passed |
| Existing backend HTTP/context tests | 7 passed |
| Real PostgreSQL repository/constraint tests | 7 passed |
| Complete application process restart persistence test | 1 passed |
| Frontend lint and production build | Passed |
| Desktop/mobile browser integration tests | 4 passed |
| Flyway V1 in application and test databases | Applied successfully; migration history verified |
| Hibernate schema validation | Passed on each startup |
| Database outage | Stopping only the project-local PostgreSQL instance caused HTTP 503 and `database=DOWN` |
| Database recovery | Restarting PostgreSQL restored `database=UP` without restarting the application |
| Fixture cleanup | All three tables had zero rows in the application and test databases after checks |
| Credential hygiene | Runtime environment files and password files confirmed ignored by Git |

The restart test commits a user, product, and inventory transaction with 7.250 bags in process A, waits for that process to exit, and starts process B. Process B reads the same UUIDs and verifies quantity/unit/source. This cannot pass from an in-memory application cache. Process C then removes only those fixture IDs in foreign-key order. Other repository tests roll back.

Repository tests also check fractional precision, optional price, timestamps, case-insensitive email lookup/uniqueness, user-scoped access, cross-owner transaction rejection, negative-stock rejection, and history-preserving deletion rejection.

Commands used from the root (with JDK 21 configured):

```powershell
./scripts/start-local-postgres.ps1 -ConfigureBackend
./scripts/test-database.ps1
npm run lint
npm run build
npm run test:e2e
```

The database helper runs `mvnw.cmd -B -ntp -Pdatabase-tests verify`. The backend was launched from `backend/` with `java -jar target/voicestock-api-0.0.1-SNAPSHOT.jar`. Additional checks inspected table/row-security/migration metadata with psql and exercised health while stopping/restarting only the isolated database.

## Handoff

Local frontend: <http://127.0.0.1:5173>. Backend health: <http://localhost:8081/api/health>. The backend and isolated PostgreSQL instance are running. Follow the README to restart them after a system/session restart; the local database is not installed as a Windows auto-start service.

Continue with Phase 3 only when requested. Hosted Supabase verification remains the outstanding environment-specific step when its credentials become available.
