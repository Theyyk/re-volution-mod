// CI-ISOLATION-01: Gameplay tests/build must never publish packages or dispatch Docker.
// Read-only static contract, executable by existing Gameplay CI via Node.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const ci = fs.readFileSync('.github/workflows/release.yml', 'utf8');
const dispatcher = fs.readFileSync('.github/workflows/dispatch-dev-docker.yml', 'utf8');

assert.match(ci, /^name: Gameplay CI \(build only\)$/m);
assert.match(ci, /^  pull_request:\s*\n    branches: \[develop\]/m);
assert.match(ci, /^  push:\s*\n    branches:\s*\n      - "\*\*"/m);
assert.match(ci, /permissions:\s*\n  contents: read/);
assert.doesNotMatch(ci, /(?:^|\n)\s+(?:contents|packages|actions): write\s*(?:\n|$)/);
assert.match(ci, /run: \.\/gradlew .*clean build/);
assert.match(ci, /Verify packaged classes and resources/);
assert.match(ci, /runs-on: ubuntu-latest/);
assert.match(ci, /runs-on: windows-latest/);
for (const forbidden of [
  'docker/login-action', 'docker/build-push-action', 'gh release create',
  'gh api', 'push: true', 'secrets.', 'RE_VOLUTION_DOCKER',
  'Publish exact DEV gameplay package', 'Create GitHub Release',
]) {
  assert.equal(ci.includes(forbidden), false, 'forbidden CI side effect: ' + forbidden);
}
// Default-branch legacy dispatcher listens to OLD workflow name only.
// The newly renamed build-only workflow must not match it.
assert.match(dispatcher, /workflows: \['Build and Release'\]/);
assert.doesNotMatch(dispatcher, /workflows: \['Gameplay CI \(build only\)'\]/);
assert.doesNotMatch(ci, /^name: Build and Release$/m);
console.log('Gameplay CI isolation: build retained, publication and dispatch routing disabled');
