# CI policy

CI optimization changes build scheduling and caches only. The unified DEV channel,
trusted completion boundary, manifest schema, PROD stable selector and runtime data
remain the same. No CI job deploys to the local DEV or PROD runtime.

## Change detection

Every existing push/PR trigger still starts a lightweight `changes` job. It validates
**all** workflow YAML with actionlint 1.7.12 and tests `.github/ci/changes.py` before
selecting components. `.github/ci/components.json` is the authoritative path policy;
`.github/ci/cases.json` contains regression examples.

Detection uses the complete Git diff, including deleted paths and both sides of
renames. PRs compare their head with the merge base. It does not use GitHub's
300-file trigger limit or paginated changed-files API. Unclassified non-documentation inputs also run all components. New branches, tags, manual
dispatch and unavailable comparison objects run all components. Docs-only changes
run lightweight checks; every workflow or filter change runs the component checks.
There is no workflow-level path skip that would leave a required check pending.

## Cache policy

Gradle has one owner for its user home: `gradle/actions/setup-gradle@v4`; do not add
`setup-java cache: gradle` or a second generic Gradle cache. It saves wrapper and
dependency downloads, compiled scripts/transforms and the local task build cache.
`--build-cache` permits reuse of cacheable compilation outputs. PRs read caches;
trusted branch builds may write their branch-scoped cache. Build directories,
credentials, project gradle.properties, daemon logs, runtime files and secrets are
not separately archived. Clean builds still run tests and rebuild final JARs.
Forge reobfuscation mutates its JAR, so archive tasks are explicitly excluded from
the task cache. Stable entry ordering/timestamps make JAR bytes reproducible.

.NET jobs use `actions/cache@v4` for an isolated `$GITHUB_WORKSPACE/.nuget/packages` directory only. There are no lock files:
keys include OS, SDK major, job family, and hashes of csproj/props/targets,
global.json, NuGet.config and any future packages.lock.json. Prefix fallback reuses
packages but never skips restore. No bin/obj/publish files, credentials or NuGet
configuration are cached. Existing publish/profile flags and deterministic compiler
behavior are preserved; a cache miss must yield the same build semantics.

References: [Gradle setup/cache](https://github.com/gradle/actions/blob/v4/docs/setup-gradle.md),
[NuGet caching inputs](https://github.com/actions/setup-dotnet#caching-nuget-packages),
[BuildKit GHA cache](https://docs.docker.com/build/cache/backends/gha/).

## Mod triggers and publication

- `release.yml`: pushes to all branches and v* tags, PRs to develop, manual dispatch.
  Java build runs for src, Gradle/wrapper/buildSrc, validation scripts and workflow
  inputs. Startup PowerShell tests run for tools/startup or CI policy changes.
  Gradle `check` already runs architecture tests, so the duplicate pre-build invocation
  was removed. Implementation-Timestamp comes from SOURCE_DATE_EPOCH or Git commit
  time in UTC, rather than wall clock. Archive entry timestamps/order are normalized.
- `dispatch-dev-docker.yml`: trusted successful official push workflow_run only,
  excluding stable branches. It checks for one actual unexpired gameplay artifact;
  a successful docs-only run never dispatches a Docker build. It executes no branch code.
- Branch push/PR CI cancels older runs with the same workflow/event/ref. v* tag and
  manual runs do not cancel. Dispatch has no cancellation: a completed candidate
  notification must reach Docker; the downstream watermark remains authoritative.
- Gameplay CI artifacts: 7 days for branches, 90 for tag runs. GitHub release JARs
  and release history are untouched. Seven days allows retries; immutable public
  DEV receipts carry the exact JAR after publication.

Existing other branches retain their own workflow version until these CI changes
are merged there. This policy is installed on main and develop; it does not rewrite stable
history or create a release/tag.

ForgeGradle 5 resets timestamps during reobfuscation; gradle/reproducible-jar.gradle
normalizes the final ZIP envelope after reobfJar without changing class/resource bytes.
