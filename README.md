# BloodBridge: A Smart Blood Donation Network with Intelligent Donor Matching

Full Stack Development-II capstone (24CSP-337), Chandigarh University.
**React (Vite) front end + Spring Boot back end + PostgreSQL**, as a modular monolith.

> A coordination prototype with synthetic demonstration data and simulated locations. Matches are
> preliminary candidates, not medically cleared donors. Qualified healthcare professionals must confirm
> donor eligibility and clinical compatibility. It is not a medical system.

## Deployment status (read this first)

This code was written in a sandbox with no access to Maven Central or npm and no way to publish to a
public host. It has **not been compiled, tested or deployed yet**. Before evaluation, run the
verification steps below on your machine (or let the Docker build do it) and fix anything they report.
Nothing here has a live URL until you deploy it.

## Features

| Module | What it does |
|---|---|
| Auth | Register/login, BCrypt password hashes, JWT, roles Donor / Requester / Coordinator / Administrator |
| RBAC | 16 fine-grained permissions mapped to roles in the database; admin edits the matrix in the UI; permissions load on every request so edits apply immediately; ownership rules for request data; audit log of logins, role changes, verification and permission edits |
| Donor profile | Blood group, simulated location, availability + confirmation timestamp, contact consent, verification |
| Requests | Component, units, hospital, urgency, deadline; controlled status transitions |
| Matching | Stage 1 filters (compatibility per component, availability, consent, donation interval, active commitments, active account); Stage 2 weighted 0-100 score with per-factor reasons |
| Priority queue | `PriorityQueue` ordered by urgency, deadline, submission time, id. Runs on request creation (event) and every 15 s |
| Notifications | In-app records, optional SMTP email, per-invitation delivery status, retry (max 3, plus admin retry), accept/decline once only (409 on duplicates, optimistic lock) |
| Control room | Tabs shown by permission: analytics with charts, blood stock, donations and CSV reports, failed deliveries, users & roles, permission matrix, audit log |
| Blood stock | Units per group and component, low-stock thresholds, audited adjustments |
| Donations and impact | Staff record completed donations (updates the donation-interval filter, auto-fulfils a fully served request); donors earn badges and see a history |
| Blood drives | Coordinators publish drives; donors register within capacity; cancellations notify registrants |
| Donor radar | Per-request distance radar of invited donors (simulated distance, illustrative direction, no coordinates exposed) |
| Security extras | Login lockout (5 failures / 10 min), CSV formula-injection guard, interactive API docs at `/swagger-ui.html` |
| UI | Sidebar app shell, dark mode, responsive layout, Recharts dashboards |
| CI | GitHub Actions workflow runs backend tests and the frontend build |

Metric definitions: response rate = (accepted + declined) / invitations sent; time to first response =
request creation to first accept or decline; requests with matches = requests with at least one invitation;
pending emergencies = EMERGENCY requests that are OPEN or MATCHING.

Privacy: donors' coordinates and email are never returned to other users; requesters see a shortened name,
the donor's city, and a phone number only after the donor accepted and consented.

## Role-based access control

| Role | Default permissions |
|---|---|
| Donor | manage own donor card, view and answer invitations, register for blood drives |
| Requester | create requests, view and manage own requests |
| Coordinator (MODERATOR) | view all requests, priority queue, review urgency, verify donors, analytics, failed deliveries, blood stock, record donations, manage drives, export CSV, view users |
| Administrator | everything above plus manage users, assign roles, edit the permission matrix, view the audit log (fixed, cannot be edited) |

Rules worth knowing: the server checks every endpoint with `hasAuthority('PERM_...')`, the UI only hides what the
server would refuse anyway; only an administrator can create, demote or deactivate administrators; nobody can
change their own role or status; a donor's phone number is visible only to the request owner.

**Upgrading an existing database:** this version adds roles, permissions and new tables, and Hibernate does not relax old
enum check constraints. Use a fresh database (`docker compose down -v`, or recreate the Render database).

## Run locally

Backend (Java 21, Maven): `cd backend && mvn spring-boot:run` (H2 in-memory DB, port 8080)
Frontend (Node 20+): `cd frontend && npm install && npm run dev` (http://localhost:5173, proxies /api to 8080)
Tests: `cd backend && mvn test`

Everything with PostgreSQL: `docker compose up --build`, then open http://localhost:8080

## Deploy (single service, public URL)

**Render (Blueprint):** push this folder to a GitHub repo, then in Render choose New > Blueprint and select it.
`render.yaml` creates the Docker web service plus a PostgreSQL database. Read the generated `ADMIN_PASSWORD`
in the service's Environment tab. Check Render's current free-tier limits (cold starts, database expiry)
before the evaluation date; use a paid instance if the demo must always be awake.

**Any Docker host (Railway, Fly.io, a VM):** build the root `Dockerfile`; set `JWT_SECRET`, `ADMIN_PASSWORD`,
and either `DATABASE_URL` (postgres://user:pass@host:port/db) or `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`;
expose the `PORT` the platform provides.

## Demo accounts (synthetic data, SEED_DEMO_DATA=true)

| Role | Email | Password |
|---|---|---|
| Admin | admin@bloodbridge.local (or ADMIN_EMAIL) | Admin@12345 (or ADMIN_PASSWORD) |
| Coordinator | coordinator@demo.bloodbridge.local | Demo@12345 |
| Requester | requester@demo.bloodbridge.local | Demo@12345 |
| Donors | donor01@demo.bloodbridge.local ... donor12@... | Demo@12345 |

Set `SEED_DEMO_DATA=false` and a strong `ADMIN_PASSWORD` for any deployment that is not a private demo.

## Suggested demo script

1. Log in as the requester; create an Emergency O+ red-cells request at Chandigarh. Candidates appear ranked with reasons.
2. Open a second browser session as `donor01@...`; accept the invitation. The requester sees the response and the donor's phone.
3. Try accepting again: the API returns 409 (duplicate response rejected).
4. Log in as the coordinator; open Control room: analytics, priority queue (change an urgency), verify a donor. Note there is no Users management or Audit tab.
5. As coordinator: open Blood stock (adjust a cell), open a request with an accepted donor and click Record donation, then publish a blood drive under Blood drives; export a CSV under Donations & reports.
6. As a donor: register for a drive and check My impact for badges.
7. Log in as admin; open Access control, remove a coordinator permission, save; the coordinator loses that tab on next refresh. Check the Audit log.

## Verification checklist (to run on your machine)

- [ ] `mvn test` passes (compatibility, queue ordering, RBAC, stock, drives, donations, lockout)
- [ ] `/swagger-ui.html` lists the API
- [ ] `npm run build` succeeds in `frontend/`
- [ ] `docker compose up --build` starts and http://localhost:8080 shows the login page
- [ ] The demo script above works end to end
- [ ] Public URL responds at `/actuator/health`

## Known limitations

- Compatibility tables are simplified prototype rules, not clinically validated.
- Eligibility values (e.g. 90-day donation interval) are configurable placeholders in `application.yml`.
- No rate limiting, password reset, MFA or audit log yet.
- Schema is managed by Hibernate `ddl-auto=update`, not migrations.
- JWT is kept in browser localStorage.
