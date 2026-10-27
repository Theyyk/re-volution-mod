# Admin tools 1.4.0-preview

Local candidate, not a published/final gameplay release. Based on develop 6aeaf36 (1.3.12). No Location 1 balance values are changed automatically.

The existing HMAC/session/loopback/strict form/server-thread boundary remains. Status adds `environment`, persistent public `installationId` and explicit capabilities. Set `-Drevolution.admin.environment=DEV` or `PROD` in the corresponding deployment. Unconfigured environments are not eligible for Control tools writes. The installation ID is stored separately from the secret in `config/revolution-admin.installation-id`; no bearer/session/credential enters configuration journals.

Explicit additional routes:

- GET `/api/v1/stages`: configuration revision plus stable Location/stage IDs, exact numeric strings, population, mob binding, geometry, derived runtime boss and persisted kind.
- POST `/api/v1/players/progression`: UUID, online-player stage access/current position.
- POST `/api/v1/players/grant-stage`: UUID/location/stage. Also grants the first stage of the selected Location; does not trigger travel or combat rewards.
- POST `/api/v1/players/reset-current`: UUID. Re-evaluates actual player position and revokes that stage and later stages/Locations, using the existing progression contract.
- POST `/api/v1/players/reset-location`: UUID/location. Resets exactly this Location, retaining records for other Locations.
- POST `/api/v1/players/reset-all`: UUID. Clears all explicit access records. The first stage of the first configured Location remains implicitly available.
- POST `/api/v1/stages/preview`: revision and JSON array of typed patches.
- POST `/api/v1/stages/apply`: one-use prepared token only.

Stage patches allow only `location`, `stage`, nullable/absent `hp`, `population`, `resource`, `amount`. Exact numeric values are strings; omitted fields remain unchanged. Unknown/duplicate keys, conflicting/duplicate stage rows, invalid values, shared/non-canonical mobs and inconsistent persisted boss layout are rejected. HP/reward are positive under the existing Mob contract. Population preserves the existing maximum 500. A batch is limited to 100 stages, 64 KiB form, 48k decoded patch text and a conservative 4 MiB serialized recovery journal.

The final configured stage remains the only boss. Neither patch input nor Control exposes a kind dropdown. Existing spawn points and disabled records are retained; additional population slots reuse an existing fallback point with RANDOM_AREA mode. Population creation without an existing fallback is rejected. This does not generate terrain, build stages or add a template builder.

Preview validates the whole list before storing a prepared change in memory. It contains exact before/after values, revision and a token bound to the authenticated session, expiring in two minutes. A new preview replaces that session's previous preview. Apply rechecks the complete configuration revision and consumes the token. Configuration changes invalidate all other prepared previews. There is no automatic Apply retry.

## Recovery guarantees and limits

The existing Mongo deployment is standalone. This implementation does not claim Mongo multi-document transaction isolation and does not change replica-set topology.

All administrative configuration operations run on the one authoritative server thread. Before the first configuration write, a complete before/after journal is durably acknowledged using JOURNALED write concern in `admin_stage_commit`, `_id=active`, state=PENDING. Existing `custom_mobs` / `custom_mob_spawns` document contracts are retained; unknown fields are preserved. A failed write restores the whole before-image. The commit marker is acknowledged only after every after-image was written. Runtime mobs reload once after a successful complete commit (full HP/combat reset, disclosed by preview).

Startup recovery runs before bootstrap/runtime loading. A PENDING journal restores the complete before-image and is then marked REVERTED. Recovery also guards stage/mob projections and mob configuration reload/mutation entry points. A failed rollback retains PENDING for later recovery; an unavailable storage/recovery fails closed rather than acknowledging success. An uncertain final marker causes recovery and runtime reload before returning an error; clients must re-read state before retrying.

This provides recoverable, all-or-before application through the server boundary and restart recovery. It does not provide isolation to external raw-Mongo readers/writers or multiple independent server processes writing the same gameplay database; those are outside this administration contract. A database/driver acknowledgement failure can leave the client uncertain whether a full batch committed; the client disconnects and requires a fresh snapshot. The previous COMMITTED/REVERTED journal is retained until the next batch, not an unbounded audit history.

Progression reset/grant operations use existing persistence/service semantics and confirmation; they are not folded into the stage configuration batch journal.

## Validation / rollout

Pure tests cover selected-field preservation, complete validation, session/revision/token rules, recovery fault points, strict patch parsing, exact numeric transport and progression scope/cache behavior. A separately enabled integration test uses an isolated temporary standalone Mongo and a generated `admin140_test_*` namespace to verify commit, stale revision rejection and PENDING recovery. No gameplay database is used.

Build with Java 17/Gradle 7.6.4 (`gradlew check build`). The candidate targets the existing Java 8 game runtime. The resource architecture check now normalizes `.txt` metadata LF/CRLF like other text assets; PNGs and reviewed asset digest values remain unchanged.

The running DEV/PROD are not upgraded by source edits or by replacing Control.exe. Publish/review this server candidate through the existing trusted image/DEV manifest flow, with independent server/client artifacts. Do not replace a mounted JAR behind an accepted image/manifest or fake a matching compatibility status. Keep the existing world/Mongo backup and runtime rollback procedure. PROD Admin relay/profile enablement and release promotion are separate steps.
