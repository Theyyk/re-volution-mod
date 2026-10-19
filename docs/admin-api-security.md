# RE:volution Admin API Security Contract (v1.3.2)

This document is normative for the v1.3.2 external administration boundary.

## Trust model

- The Minecraft client is not an administrator transport.
- The external RE:volution Admin application is a separate trust boundary.
- Loopback origin is not authentication. Any local process may attempt to reach a loopback listener.
- Minecraft UUID whitelist authorization and Admin API authentication are separate concerns and must not be coupled.
- MongoDB is never an Admin API surface. External tools must not read or mutate gameplay collections directly.

## Network exposure

- v1.3.2 MUST bind the Admin API to a loopback address only.
- Non-loopback addresses, wildcard addresses (`0.0.0.0`, `::`) and automatic firewall exposure MUST be rejected.
- Remote administration is explicitly out of scope for v1.3.2. Enabling it later requires a separate security review and protocol change.
- CORS MUST remain disabled; the v1.3.2 client is a desktop application, not a browser application.

## Authentication

- Every non-health request MUST require Admin API authentication, including requests from loopback.
- There MUST be no hard-coded, default or repository-stored credential.
- A server-installation secret MUST be generated using `SecureRandom` with at least 256 bits of entropy.
- The long-lived installation secret MUST NOT be sent as a bearer credential on ordinary API requests.
- Authentication MUST use a challenge-response construction based on HMAC-SHA-256 over a server-issued nonce and protocol context.
- A challenge MUST be random, short-lived and single-use.
- Successful authentication MUST issue a cryptographically random, short-lived session token.
- Session tokens MUST be stored in memory only on the server and MUST expire after bounded idle and absolute lifetimes.
- Session tokens MUST be invalidated when the server shuts down or the Admin API is restarted.
- Authentication failures MUST be rate-limited and MUST NOT reveal whether a guessed secret prefix or token fragment was correct.
- Secret, HMAC values and session tokens MUST never be written to logs.

## Authorization

- Authentication and authorization are separate checks.
- Every endpoint MUST declare a required capability, even while the first local administrator receives all capabilities.
- Initial capability names:
  - `players.read`
  - `resources.read`
  - `resources.write`
  - `runes.read`
  - `runes.write`
  - `mobs.read`
  - `mobs.write`
  - `server.read`
- Future Realm/Location/Progression capabilities are not implemented in v1.3.2.
- No endpoint may provide arbitrary command execution, shell execution, reflection-based invocation, arbitrary Mongo queries, arbitrary file access or a generic `action` string dispatcher.

## Request validation

- Only explicitly registered routes and methods are accepted.
- Request bodies MUST have a strict byte limit before parsing.
- String fields MUST have explicit maximum lengths.
- UUIDs, resource identifiers, mob identifiers, numeric values and enums MUST be validated server-side.
- Unknown fields SHOULD be rejected for state-changing requests.
- Numeric parsing MUST reject overflow, NaN/infinity and values outside domain limits.
- Invalid or unauthorized requests MUST be side-effect free.

## Concurrency and Minecraft state

- HTTP worker threads MUST NOT mutate Minecraft world/player/entity state directly.
- State-changing operations that touch Minecraft runtime MUST be scheduled onto the Minecraft server thread.
- The API MUST use a bounded worker pool and a bounded number of pending state-changing tasks.
- Requests MUST have bounded processing time; shutdown MUST close the listener and invalidate sessions.

## Destructive operations

- Destructive operations MUST be distinct typed operations, not flags on generic endpoints.
- The desktop client MUST display an explicit confirmation for destructive actions.
- Server authorization MUST NOT rely on the client confirmation UI.
- The protocol must permit adding a server-issued confirmation nonce for especially destructive future operations without changing business services.

## Logging and errors

- Administrative state changes MUST produce an audit event containing timestamp, operation, target and outcome.
- Audit records MUST exclude credentials, HMAC responses, session tokens, Mongo URI credentials and sensitive request headers.
- API errors MUST return stable error codes and sanitized messages.
- Stack traces, filesystem paths, Mongo connection strings and implementation exceptions MUST NOT be returned to the client.

## Secret storage

- Secrets MUST NOT be committed to `re-volution-mod` or `re-volution-admin`, including private Git history.
- Secret files MUST live outside repository-tracked paths.
- Server startup MUST create the installation secret if absent and apply the most restrictive owner-only filesystem permissions available to the Java 8 runtime.
- If restrictive permissions cannot be applied, startup MUST log a security warning without printing the secret.
- Secret rotation MUST invalidate all active sessions.

## Protocol compatibility

- Admin API routes are versioned under `/api/v1`.
- Forge packet discriminators 5 and 6 may become unused/reserved after removal of the Minecraft admin panel; discriminator 7 MUST NOT be renumbered.
- The external Admin API protocol is independent from the Forge `revolution:main` channel.

## Mandatory regression tests

CI MUST cover at least:

- non-loopback bind rejection;
- missing/invalid authentication rejection;
- challenge expiry and one-time use;
- session expiry and invalidation;
- capability denial;
- request-size rejection before parsing;
- malformed input with zero side effects;
- generic/unknown action rejection;
- redaction of secrets/tokens from logs and errors;
- server-thread handoff for runtime mutations;
- reserved Forge discriminator 5/6 and unchanged discriminator 7;
- no client-side F8/admin GUI registration;
- no direct Mongo dependency from the external Admin API adapter.

## Remote administration

Remote/non-loopback administration is forbidden in v1.3.2. A future CR must define TLS, certificate validation, credential lifecycle, stronger host authentication, firewall exposure, replay resistance, session revocation and updated threat modelling before any remote bind is permitted.
