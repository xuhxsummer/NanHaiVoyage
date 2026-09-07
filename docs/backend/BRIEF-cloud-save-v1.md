# NanHaiVoyage — Backend + DB cloud save (v1) — Codex brief (ENGLISH)

## Goal
Replace device-local JSON saves + Preferences accounts with a **frontend/backend split**:
- Android/libGDX client talks to a REST API
- Real **database tables** (not a single blob JSON column as the only model — normalize core fields)
- Remove manual **存档 / 读档** UI; progress syncs with the account automatically
- Keep gameplay working; migrate from current `AccountStore` / `SaveData` / `GameState`

## Current local stack (replace)
- Accounts: `Preferences` `nanhai-accounts` (plaintext password) — `AccountStore`
- Saves: `Gdx.files.local("saves/<user>.json")` JSON of `SaveData`
- Runtime: `GameState.toSave()` / `fromSave(SaveData)`

## Backend (new module under repo, e.g. `server/`)
Suggested stack (Codex may adjust if justified): Java Spring Boot 3 + JPA + PostgreSQL **or** SQLite for local/dev with a clear path to Postgres. Provide `docker-compose.yml` for Postgres if using it. OpenAPI/Swagger optional but helpful.

### Auth
- `POST /api/auth/register` { username, password }
- `POST /api/auth/login` → JWT (or session token)
- Passwords hashed (bcrypt/argon2), never store plaintext
- Client stores token securely (libGDX Preferences OK for token only)

### Normalized tables (minimum v1)
Design SQL migrations (Flyway/Liquibase). Suggested entities (refine as needed):
1. **users** — id, username unique, password_hash, created_at, updated_at
2. **player_profiles** — user_id FK, display fields, last_port, ship, ship_owned, game_day, day_min, silver, debt, hull, hull_max, supply, supply_max, cargo_cap, warehouse_level, cannon_level, cannon_damage, crew, crew_cap, crew_cap_level, x, y, heading_deg, updated_at
3. **player_cargo** — user_id, kind (trade|beast|herb), item_index, qty
4. **player_codex** — user_id, kind (beast|herb), item_index, found bool
5. **player_quests** — user_id + quest counter/claim columns mirroring SaveData quest fields (or quest_id + progress + claimed rows)
6. **player_market** — user_id, item_index, price_offset (from marketOff)
7. Optional **player_meta** for howto_shown / flags

Do **not** dump entire SaveData as the only row. Arrays become rows. Document schema in `docs/backend/schema.md`.

### Sync API
- `GET /api/me/state` — full state DTO for client `GameState.fromSave`-compatible mapping
- `PUT /api/me/state` — upsert full state (validate ownership via JWT)
- Auto-save triggers on client: dock/enter port, leave port, app pause/exit, death restart — **no** manual save/load buttons
- On login: pull server state; if none, create starter state (same defaults as new game)
- Conflict policy v1: last-write-wins with `updated_at`; document it

### Client changes
- Replace `AccountStore` network calls; remove local JSON save path for primary path
- LoginScreen: register/login against API; remove/disable 存档/读档 if present in captain/avatar menus
- VoyageScreen: call sync instead of `accounts.save/load`
- Configurable API base URL (default localhost for emulator `10.0.2.2`, document device LAN)
- Offline: if network fails, show toast; optionally keep last successful local cache — do not resurrect manual save UI

### Docs & quality
- `docs/backend/README.md` — how to run server + DB + point the APK at it
- `docs/backend/schema.md` — tables + ER notes
- Migration scripts checked in
- Basic smoke: register → login → put state → get state
- Do **not** package APK until server boots and client compiles against API; then `./scripts/release.sh` next patch with notes that cloud save needs server

## Constraints
- ENGLISH for all Codex discussion; Chinese only for player-facing UI strings
- Visible Codex TUI; gpt-6-astra medium; do not kill FreeBuff
- No commit of bug-*.jpg / assets/saves/
- Notify parent only with: Release URL if packaged, OR blocker/quota, OR server-ready summary if packaging deferred pending hosting

## Success
1. Server runs with migrations applied
2. Schema documented and normalized
3. Client uses API; manual save/load removed
4. compileJava / server tests or smoke OK
5. Package when client+server are demonstrably wired (or report exact remaining host step)
