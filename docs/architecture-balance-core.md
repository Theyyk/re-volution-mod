# Balance domain architecture

The canonical balance foundation is a dedicated domain under `com.example.revolution.balance`.

- `balance.definition` — immutable data-only canonical definitions.
- `balance.config` — JSON parsing, canonical serialization and deterministic revision hashing.
- `balance.curve` — pure deterministic curve evaluation.
- `balance.validation` — semantic validation performed before activation.
- `balance.service` — application-level prepare/activate lifecycle for complete snapshots.

The domain follows the existing architecture contract: definitions are engine-independent, services receive dependencies through constructors, runtime mutable state is instance-owned, and no balance class accesses persistence, commands, client code, Forge events, or the service locator.

`v1.4.1` deliberately does not wire `BalanceService` into `ServerServices`. That integration is deferred until a concrete gameplay consumer is migrated, avoiding an unused global service and preserving the project's rule against speculative abstractions.
