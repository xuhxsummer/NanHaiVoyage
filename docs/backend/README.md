# Cloud save v1

> **DISABLED for now — local saves active until a rented server is ready.**
> Since v0.27.7 the client uses device-local register/login (Preferences) and
> `saves/<user>.json`; `assets/cloud.properties` carries no URL. Keep this
> backend, its Flyway SQL and the commented cloud `AccountStore` in the tree
> for the future cloud rollout.

The Android/libGDX client now uses a Spring Boot REST service. PostgreSQL is the deployment database; file-backed H2 in PostgreSQL mode provides zero-install local development. JDBC keeps the normalized SQL mapping explicit; Flyway owns schema changes. The `shared` module contains the wire DTO and catalog; it has no libGDX or Spring dependency. Spring Boot 3.5 uses Java 17 ([requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)).

## Run locally

From the repository root, with JDK 17 or newer on PATH:

```sh
./gradlew :server:test :core:compileJava :server:bootJar
./gradlew :server:bootRun
```

The server listens on port 8080. Flyway applies `server/src/main/resources/db/migration/V1__cloud_save.sql` on startup. The default database is `server/data/cloud.mv.db`, relative to the server's working directory. Use an absolute `DB_URL` for a predictable location. Run only one server process against a file-backed H2 database. Local defaults are for development, not a publicly exposed deployment.

This workspace's JDK is at `/home/box/.local/jdk/current`; set `JAVA_HOME` accordingly if Java is not on PATH. A pre-existing `org.gradle.java.home` setting also points there; other machines should adjust/remove that local setting.

## PostgreSQL

```sh
export DB_PASSWORD='choose-a-long-local-password'
docker compose -f server/docker-compose.yml up -d --wait
export DB_URL='jdbc:postgresql://127.0.0.1:5432/nanhai'
export DB_USER=nanhai
./gradlew :server:bootRun
```

The compose file keeps PostgreSQL bound to loopback and persists data in a named volume. The same migration runs on PostgreSQL and H2; no JSON database column is used. An H2 database is not automatically copied into PostgreSQL. Start PostgreSQL with a fresh schema or perform an explicit table-level data transfer before switching a populated installation.

To run the built server without Gradle:

```sh
java -jar server/build/libs/server-0.23.0.jar
```

Configuration: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT` (8080 by default). Keep database credentials outside source control. Put the service behind HTTPS, configure request-size and authentication-rate limits at the reverse proxy, and back up PostgreSQL before hosting it for players. Session tokens expire after 30 days; logging out revokes the current token. Passwords use bcrypt cost 12; only SHA-256 hashes of random 256-bit session tokens are stored in the database.

## Point the client at the API

`assets/cloud.properties` contains a single base URL, currently `http://10.0.2.2:8080` for the Android emulator. For a physical device, use the development machine's LAN IP, e.g. `http://192.168.1.50:8080`, with both devices on the same network and port 8080 reachable. For desktop, change that file to `http://localhost:8080`, or pass JVM property `-Dnanhai.apiUrl=http://localhost:8080` to the desktop process. Restart the app after changing the endpoint. Rebuild Android after editing the asset.

Debug Android builds permit HTTP for emulator/LAN development. Release builds require HTTPS. Android backup is disabled to avoid backing up bearer tokens/outboxes. Preferences contain only the cloud token; passwords are never persisted by the new client. Endpoint and username scope the outbox filename by SHA-256, so accounts or different servers never share queued saves.

Register using 3–32 letters/digits/underscore/hyphen for the username and a password of at least 8 characters and at most 72 UTF-8 bytes. Usernames are trimmed and case-sensitive. Both register and login first authenticate, then fetch cloud state. Only HTTP 204 means there is no state: the client creates the existing `GameState.newGame()` starter and automatically uploads it. Network, parsing, and authorization failures never create a replacement starter.

## Sync and offline behavior

Automatic checkpoints occur on docking/entering port, both departure paths, economic/quest actions, every 30 seconds, app pause, exit/hide, and death/restart. Captain and failure menus have no manual save/load controls. Sailing position, docking status, death flag, fishing inventory, trade costs, visited-port bitmask, quest counters and claims all persist. Transient navigation/combat and fishing timers reset when restoring; island menus reopen through normal interaction. The existing first-run how-to preference remains device-local.

Each checkpoint first atomically replaces a local outbox file, then sends a PUT on one background worker. Pending snapshots coalesce while a request is in flight. A successful PUT clears only the corresponding latest outbox. Network/server/auth failures retain it and show a Chinese toast; an already open game continues offline and retries automatically. A failed local checkpoint shows a distinct warning to keep the app open and blocks logout so the running state is retained. Failed writes are not called successful cloud saves.

Offline login is deliberately unavailable. On the next authenticated login to the same account/server, the client uploads that account's pending checkpoint before fetching server state. If either operation fails, login remains on the login screen; no new state is created. Logout keeps failed outbox data, clears the local session, and attempts server revocation after queued writes. A force-kill can lose gameplay since the last checkpoint (normally up to 30 seconds); the OS does not guarantee an exit callback or completion of network requests on pause.

Conflict policy: **last successfully applied full PUT wins**, including delayed offline retries from another device. Writes and reads lock the owning user row; replacement of all normalized rows is transactional. `updated_at` is assigned by the server after acquiring that lock and returned by PUT. There is no merge, revision precondition, or cross-device conflict prompt in v1. Avoid playing one account concurrently on multiple devices if overwriting progress is undesirable.

Existing `nanhai-accounts` plaintext Preferences and `saves/<user>.json` are not read, uploaded, or deleted. Local usernames do not establish server ownership. Players register a cloud account; automatic import of old local credentials/saves is intentionally absent. Keep old files for a future explicit migration tool if needed.

## API and checks

All state endpoints use `Authorization: Bearer <token>`; identity comes exclusively from the session, never the DTO. JSON errors contain an `error` code. Successful GET responses are not cacheable.

| Request | Result |
| --- | --- |
| `POST /api/auth/register` `{username,password}` | 201; duplicate username 409; invalid format 400 |
| `POST /api/auth/login` `{username,password}` | 200 `{token,expiresAt}`; invalid credentials 401 |
| `POST /api/auth/logout` | 200; revokes current session |
| `GET /api/me/state` | 200 full `SaveData` JSON or 204 for a new account |
| `PUT /api/me/state` full `SaveData` JSON | 200 `{updatedAt}`; malformed state 400 |

Missing, invalid, expired, or revoked tokens return 401. State validation checks complete catalog-sized arrays, nonnegative quantities, finite numeric values, port/ship/position ranges, and basic capacity limits. This is persistence validation, not an authoritative anti-cheat simulation.

```sh
./gradlew :server:test :core:compileJava :android:compileDebugJavaWithJavac :server:bootJar
python3 tools/check_ui_font.py
```

`CloudApiTest` covers register → login → PUT → GET, all DTO fields, normalized row values, replacement, invalid input, account isolation, expiry and revocation. `ClientTransportTest` boots an HTTP server and drives the actual libGDX `AccountStore`, including simulated outages, outbox recovery across client instances, account separation and authentication errors. Tests use H2; a PostgreSQL runtime check requires the compose stack above. Android compilation requires an SDK.

## Hosting and release handoff

Packaging is deferred until a player-reachable endpoint is supplied. Remaining step: deploy the built service with PostgreSQL behind HTTPS, set `assets/cloud.properties` to that HTTPS origin, and verify login/sync from a physical device. No hosting credentials or public endpoint were supplied with this brief. A default-emulator APK would not connect from ordinary phones.

Once hosting is verified, rerun checks and use the existing release script for the next patch:

```sh
./scripts/release.sh 0.27.6 '账号云端同步：需连接云存档服务器，移除手动存读档'
```

The script builds, commits, pushes and publishes a GitHub release. Do not include old save files or `bug-*.jpg` in commits. No APK or release was produced during the server implementation.
