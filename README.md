# VoiceStock

**Manage inventory as naturally as you speak — and know what to do next.** A voice-first multilingual inventory assistant for small businesses, built in the original React + Spring Boot + PostgreSQL codebase.

## Problem, solution and unique value

Shop owners should not need to navigate forms for every stock movement. VoiceStock turns English and Telugu/Hindi mixed commands into structured, validated inventory actions. Every assistant change requires human confirmation; every inventory answer comes from the owner's database.

## Core features and workflow

**Speak/type → understand → structure → validate → confirm → update → track → alert → answer.**

- Registration/login, BCrypt passwords, JWT, owner isolation, profile and logout.
- Product CRUD/archive, manual stock changes, transaction history, dashboard and alerts.
- Browser microphone and typed fallback share one command pipeline.
- Structured intents: `ADD_STOCK`, `REMOVE_STOCK`, `CHECK_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`, `REORDER`, `UNKNOWN`, plus attention, run-out, duration, reason and saved-action queries.
- Deterministic multilingual NLP: no external AI key or service is required. This release does not call an external LLM. Flexible LLM fallback is future scope, not a claimed feature.
- Confirmation shows actual current stock, change and projected stock. Edit revalidates; Cancel changes nothing. Mentioned prices are extracted but do not modify the product price.
- Confirmed assistant changes (spoken or typed) record `VOICE`; manual forms record `MANUAL`.
- Database-grounded stock answers and reorder cards. No generated inventory quantities.

### Regional/mixed-language examples

| English | Telugu mixed | Hindi mixed |
| --- | --- | --- |
| Add 20 bags of rice | Rice 5 bags add cheyyi | Rice mein 5 bags add karo |
| Remove 5 bags of rice | Rice stock 2 bags tagginchu | Rice mein se 2 bags hatao |
| How much rice do I have? | Rice entha undi? | Mere paas kitna rice hai? |

Also supports `penchu`, `remove cheyyi`, `badhao`, `kam karo`, English `take out`, optional `at 45 rupees`, low/out-of-stock and reorder questions. These regional examples use Latin transliteration. Common Devanagari Hindi speech is also normalized, for example `राइस में 5 बैग ऐड करो` and `राइस स्टॉक कितना है`. This is a limited vocabulary and phonetic normalization layer, not a full translation engine. Responses use English for accuracy.

Trade units: **PACKETS, PIECES, KG, GRAMS, LITRES, MILLILITRES, BAGS, CARTONS, BOXES, DOZENS, QUINTALS**. Singular/plural/common variants normalize to these units; incompatible units are never converted.

### Safe confirmation and grounding

The interpreter returns intent, product, quantity, unit, optional price, language, confidence, missing fields and an owner-scoped product ID when resolved. DTO validation and the existing inventory service enforce business rules. Names resolve only within the signed-in owner's active catalog: normalized exact match, unique whole-word partial match, then product selection when ambiguous. Multi-word names are preserved. `10 packets of chips` retains quantity/unit/product and asks Add or Remove; `Add chips` asks for quantity/unit. No missing business-critical value is guessed. `Received`, `restock`, `sold`, common Telugu/Hindi variants and unit aliases share the same safe workflow. Invalid quantities, unit mismatches and over-removal cannot create a confirmation.

Owner-bound confirmation IDs last 10 minutes. The server reuses a completed result for repeated confirmation requests. Stock is locked and compared with the preview before committing. A stale preview must be processed again. Drafts are held in memory: run **one backend instance** for this MVP. Restarting the backend expires pending confirmations, while committed stock/history remain in PostgreSQL.

Low stock means `0 < stock <= minimum`; out of stock means `stock == 0`. Reorder uses the existing formula `max(0, max(minimum * 2, minimum + 1) - stock)`. Dashboard totals, stock answers and reorder quantities query the real database.

## Business Action Intelligence

**Detect → Explain → Recommend → Approve → Track.** The Action Center and dashboard business card use real inventory and recorded removals. No external purchasing, supplier data or invented metrics are involved.

- Detection priority: out of stock (`stock == 0`), low stock (`stock <= minimum`), running out soon (estimated remaining at most three days), otherwise healthy. Only attention items appear in the Action Center.
- Usage: sum REMOVE quantities within the last seven days, divided by the observed product age in days, capped at seven. Partial days stay in the denominator. At least two elapsed days and two distinct UTC removal dates are required; otherwise usage/run-out estimates are unavailable. An ADD does not count as usage. Missing history is not filled with synthetic data.
- Days remaining = current stock / average daily usage, displayed to one decimal as an estimate based on recorded removals, not a guarantee or sales forecast.
- Low/out-of-stock recommendations preserve the original reorder formula. Running-out-soon items target the greater of that baseline and seven days of recent usage. Subtract current stock and round up to 0.001 units. Example: stock12/min10, removed28 over7days →4/day, ~3days, suggested16 to reach28.
- Approve saves one active `APPROVED` reorder task per owner/product, with quantity/unit/reason snapshots and timestamps. Repeated approvals return the existing approved task. Complete/cancel update only task status. Completed records survive refresh/restart; **neither action changes inventory or creates a stock transaction**.
- “Add received stock” opens the existing stock form with product/quantity prefilled and still requires explicit stock confirmation. History displays the most recent 100 actions. Suppliers are shown as not configured.

Business questions: `What needs my attention?`, `What should I reorder?`, `What is running out soon?`, `How long will rice last?`, `Why should I reorder rice?`, `What actions are pending?`, `What reorders did I approve?`. Answers use backend calculations and saved action records.

## Architecture and technology

React 19 / Vite / Tailwind → authenticated REST API → Spring Boot 4 / Java 21 → Spring Data JPA / Flyway → PostgreSQL (Supabase in production). Browser Web Speech recognition produces text before the same backend NLP pipeline used by typed input. No speech/AI secrets are bundled in the frontend.

[Approved requirements](docs/requirements.md), [functional requirements](docs/functional-requirements.md) and [architecture](docs/architecture.md) remain unchanged. Earlier phase handoffs are historical records.

### Database

Existing `users`, `products`, `inventory_transactions` tables use UUIDs, UTC timestamps, exact decimal quantities/prices, ownership foreign keys and validation constraints. Flyway V1 creates the approved schema; V2 adds product archiving. V3 safely extends unit constraints for PACKETS and adds `business_actions`, including ownership foreign keys, RLS and an index preventing duplicate active approvals. `flyway_schema_history` tracks migrations. Hibernate validates rather than recreating tables. There is no production seeder.

Opening stock records an ADD/MANUAL transaction. Stock updates and ledger entries commit together under row locks. Archives preserve history. Supabase client access is blocked by RLS; backend connections must use the migration/table-owner role. Application APIs enforce owner scoping independently.

### API overview

| Routes (prefix `/api`) | Purpose |
| --- | --- |
| POST `/auth/register`, `/auth/login` | Public authentication |
| GET/PUT `/users/profile` | Profile |
| GET/POST `/products`; GET/PUT/DELETE `/products/{id}` | Inventory; DELETE archives |
| POST `/inventory/add`, `/inventory/remove` | Manual stock |
| GET `/dashboard`, `/transactions`, `/inventory/low-stock` | Database-backed reports |
| POST `/assistant/interpret` | `{ "text": "Add 20 bags of rice" }`; preview/question only |
| POST `/assistant/preview` | Validate an edited structured command; no mutation |
| POST/DELETE `/assistant/confirm/{id}` | Confirm once / cancel |
| GET `/business`; POST `/business/approve/{productId}` | Calculated attention items/history; approve a reorder task |
| POST `/business/actions/{id}/complete` or `/cancel` | Track task outcome without changing stock |
| GET `/api/health`, `/actuator/health` (absolute paths) | Public database health |

Except authentication/health, endpoints require `Authorization: Bearer <token>`. Tokens are stored in tab-scoped sessionStorage; logout clears that tab and tokens expire normally.

## Run locally

Requires JDK 21, Node 22.12+ (24 LTS recommended), PostgreSQL. Keep the existing ignored environment files.

```powershell
# Repository root: existing isolated PostgreSQL on 127.0.0.1:55432
./scripts/start-local-postgres.ps1
npm ci
npm run dev
```

In another terminal:

```powershell
cd backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd spring-boot:run
```

Open **http://127.0.0.1:5173** and register. This workspace uses backend **8081**. On a new Windows checkout, `./scripts/start-local-postgres.ps1 -ConfigureBackend` generates ignored local database credentials/JWT secret and separate app/test databases. It refuses to overwrite an existing database URL. PostgreSQL binaries default to `C:\Program Files\PostgreSQL\17\bin`; the helper accepts `-PostgresBin`. Data stays in `.run/postgres/data`; `stop-local-postgres.ps1` stops only this project's cluster.

On Linux/macOS, configure an existing PostgreSQL instance and use `./mvnw`. Start a packaged backend from `backend/` with `java -jar target/voicestock-api-0.0.1-SNAPSHOT.jar`. Stop a running JAR before rebuilding on Windows.

## Environment variables

Templates: `backend/.env.example`, `backend/.env.test.example`, `frontend/.env.example`. Backend `.env` uses unquoted Java-properties syntax; process environment variables override it. Use process variables for passwords containing property escape characters. Never put secrets in `VITE_*` variables.

| Variable | Value needed |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<Supabase host>:5432/postgres?sslmode=require` |
| `DB_USERNAME` | Exact database username from Supabase Connect |
| `DB_PASSWORD` | Database password, not Supabase API key |
| `JWT_SECRET` | Stable base64-encoded random value of at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Exact frontend origin(s), comma-separated |
| `VITE_API_BASE_URL` | Public backend HTTPS origin; local `http://localhost:8081` |
| `SPRING_PROFILES_ACTIVE` | `postgres` (default) |
| `PORT`, `DB_POOL_SIZE`, `JWT_EXPIRATION_SECONDS` | Defaults 8080, 5, 3600; local port 8081 |
| `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` | Separate test database for integration tests |

No external AI credentials are used in this release. The deterministic interpreter handles the required demo without them. Do not add API keys to the frontend.

## Testing

```powershell
npm run lint
npm run build
# Install once, from frontend/: npx playwright install chromium
npm run test:e2e
# With JAVA_HOME set, from root:
./scripts/test-database.ps1
```

The database runner loads ignored `backend/.env.test`, then runs Maven `-Pdatabase-tests verify`. Fast tests alone: `backend/mvnw.cmd -B -ntp verify` from `backend/`. Missing test credentials fail the integration run.

Final verification: **89 backend tests and 14 browser tests pass**, including all original tests, natural-understanding, calculation and action-persistence coverage. Approved and completed actions were verified across separate application processes without stock changes. Browser widths: 375px, 768px and 1280px.

Coverage includes the original authentication/manual inventory tests, all required parsing phrases, real PostgreSQL previews/confirmations, owner isolation, duplicate confirmation, stale stock, shortages, grounding, alerts and separate-process persistence. Browser tests run real APIs, the exact demo, edit/cancel, refresh, responsive layouts and network failure. Simulated browser recognition events test speech wiring/denied/unsupported paths; they do **not** prove physical microphone transcription. The business browser test creates REMOVE entries through the API, then adjusts timestamps only for its own fixture IDs to exercise a seven-day window. It requires the local PostgreSQL helper credentials and installed Windows psql. It never backdates ordinary users' inventory. Browser test accounts use `e2e-...@example.invalid`; ignored test-results contain their IDs for scoped cleanup.

## Deployment

Local PostgreSQL is verified. Hosted Supabase/Vercel/Render deployment requires your accounts and has not been performed.

1. Create a GitHub repository and push this code, preserving `.gitignore`; never commit `.env`, `.run`, test outputs or credentials.
2. Create an empty Supabase project. Copy the direct/session connection details from Connect. For IPv4 use its **session pooler**, port 5432. Set the JDBC URL with TLS, database username and password on Render. See [Supabase connection guidance](https://supabase.com/docs/guides/database/connecting-to-postgres).
3. Connect the GitHub repository to Render as a Blueprint using root `render.yaml`. It builds `backend/Dockerfile`. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`. Use one backend instance; health check is `/actuator/health`. See [Render Blueprint reference](https://render.com/docs/blueprint-spec).
4. Import the same repository into Vercel with root directory `frontend`, Vite framework, output `dist`, and `VITE_API_BASE_URL=https://<your-render-host>`. The project includes `frontend/vercel.json`. See [Vercel Vite deployment](https://vercel.com/docs/frameworks/frontend/vite).
5. Set Render's CORS origin to the exact Vercel HTTPS URL. Rebuild frontend if the API URL changes. Verify health, registration, the demo below, refresh and mobile/microphone access on the hosted site.

Browser speech recognition has [limited support](https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognition), can use a network recognition provider and needs browser permissions. Test the actual demo device on localhost or HTTPS. Typed input stays available when speech fails.

## Demo flow

Create Rice BAGS 50/min10, Sugar KG 4/min5, Biscuits BOXES 0/min5. Manually add20/remove5 Rice →65. Process and confirm `Add 20 bags of rice` →85, `Rice 5 bags add cheyyi` →90, `Rice mein se 5 bags hatao` →85. Ask stock →85, low →Sugar, out →Biscuits, reorder →Sugar6/Biscuits10. Reject remove500; `Add rice` asks quantity/unit; preview add100 then Cancel. Refresh and verify Rice85 and three VOICE entries.

## Future scope

Optional external LLM fallback, broader native-script vocabulary, shared persistent confirmation storage for multiple backend instances, suppliers, barcode scanning and advanced forecasting. These are not part of this tested hackathon MVP.




