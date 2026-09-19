# Manual inventory MVP — 19 September 2026

Implemented in the same project using the approved React/Vite/Tailwind, Spring Boot, JWT, and PostgreSQL architecture. No voice capture or AI was implemented.

## Working features

- Registration and login, BCrypt hashing, signed/expiring JWT bearer authentication, refresh persistence within a browser tab, logout, and personal/business/language profile settings.
- User-scoped product creation, listing, detail, search by name/category, editing, and archiving. Archive preserves historical transactions and blocks further stock changes.
- All ten approved units. Opening quantities and subsequent stock operations persist in PostgreSQL. Positive opening stock records a transaction; zero opening stock does not create a zero-quantity transaction.
- Atomic ADD/REMOVE stock operations with a database row lock, positive-quantity and unit validation, ownership checks, overflow protection, and rejection of insufficient stock. Manual endpoints always record MANUAL; clients cannot spoof a voice source.
- Live database dashboard counts, local-day transaction count, newest-first history, stock statuses, and deterministic nonnegative reorder suggestions.
- Responsive desktop/mobile pages, prominent microphone entry point, and manual fallback links from the voice placeholder.

The latest user request changes low-stock eligibility from the original document's strict inequality to `stock <= minimum`. Out of stock takes precedence at zero, so dashboard low/out counts are disjoint.

## API surface

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register and return token/profile |
| POST | `/api/auth/login` | Verify credentials and return token/profile |
| GET / PUT | `/api/users/profile` | Read/update own profile |
| GET / POST | `/api/products` | Search/paginate own active products; create product |
| GET / PUT / DELETE | `/api/products/{id}` | Own product details/edit/archive |
| GET | `/api/inventory` | Active inventory listing |
| POST | `/api/inventory/add` | Confirmed manual stock addition |
| POST | `/api/inventory/remove` | Confirmed manual stock removal |
| GET | `/api/inventory/low-stock` | Paginated low/out-of-stock products and reorder quantities |
| GET | `/api/transactions` | Paginated own history; optional productId filter |
| GET | `/api/dashboard?timeZone=Asia/Calcutta` | Actual counts and five recent transactions |
| GET | `/api/health`, `/actuator/health` | Live application/database health |

Authentication and health are public. All other APIs derive ownership from the validated token, never a client-supplied user ID. Cross-user product lookups/changes return 404; history queries cannot return another user's records.

Product edit DTOs intentionally omit currentStock. Stock changes go through the transactional stock endpoints. The shared stock service accepts a trusted MANUAL/VOICE source so future voice execution can reuse the same validation/transaction logic after user confirmation.

## Configuration

Local services: frontend <http://127.0.0.1:5173>, backend <http://localhost:8081>, project-local PostgreSQL on 55432. Database configuration remains in ignored backend/.env. A random signing key was generated there as JWT_SECRET without displaying it. No manual configuration is needed for the current local run; create an account in the UI.

For a new environment, configure DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, and the appropriate frontend URL/CORS origins. JWT_SECRET is base64-encoded random bytes (at least 32); retain it across restarts. Supabase/cloud deployment remains unverified without hosted credentials. Existing local PostgreSQL validation remains applicable.

Logout clears the tab's sessionStorage token. Tokens expire after one hour by default; logout does not maintain a server-side revocation list. Preferred language is stored, but interface translation and speech language handling are future integration work.

## Verification

- Frontend lint and production build passed.
- 7 existing backend HTTP/context tests passed.
- 11 real-PostgreSQL integration tests passed: 7 repository/constraint checks, separate-process persistence verification, and 3 authentication/inventory workflow tests.
- 6 Chromium tests passed across desktop and mobile, including the complete UI workflow, unauthenticated API protection, and network-error recovery.
- Desktop/mobile dashboard screenshots inspected. No page errors or horizontal overflow in the workflow tests.

The requested workflow was verified through real APIs and browser forms: register; Rice 50 BAGS at minimum 10 and price 1200; add 20 to reach 70; remove 5 to reach 65; reject removing 100; refresh and still see 65; Sugar 4 KG/minimum 5 is low; Biscuits 0 BOXES/minimum 5 is out; dashboard shows 3 products, 1 low, 1 out, and 4 transactions including opening entries. History, edit/search, archive, profile, logout, and login were verified.

Additional tests verified password hashing, expired/invalid token rejection, duplicate registration rejection, unit mismatch and zero-quantity rejection, cross-user read/write denial, and simultaneous removals (only one succeeds when both would oversell). Rejected stock operations do not create history entries.

No fake inventory is used by application pages. Test accounts are isolated or removed by exact fixture IDs. The next implementation pass can connect voice parsing/confirmation to the existing validated stock service.
