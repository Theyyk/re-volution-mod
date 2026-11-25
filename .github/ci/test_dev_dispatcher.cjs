// CI-ISOLATION-01: enforce PR-only Gameplay CI without publish or Docker dispatch.
// Default-branch workflow_run dispatches successful PUSH builds even if
// the upstream workflow has been renamed. PR-only prevents that trust leak.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const ci = fs.readFileSync('.github/workflows/release.yml', 'utf8');
const dispatcher = fs.readFileSync('.github/workflows/dispatch-dev-docker.yml', 'utf8');

const trigger = ci.split(/\n(?=concurrency:)/)[0];
assert.match(ci, /^name: Gameplay CI \(PR-only build\)$/m);
assert.match(trigger, /^on:\s*\n  pull_request:\s*\n    branches: \[develop\]/m);
for (const forbidden of ['  push:', '  workflow_dispatch:', '  workflow_run:', '  repository_dispatch:', '  pull_request_target:']) {
  assert.equal(trigger.includes(forbidden), false, 'forbidden trigger: ' + forbidden);
}
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
// Dispatcher runs from repository default branch, not this feature branch.
// Critical guard is the *source event*, NOT the renamed workflow name.
assert.match(dispatcher, /github\.event\.workflow_run\.event == 'push'/);
assert.match(dispatcher, /workflow_run:/);
assert.match(dispatcher, /workflows: \['Build and Release'\]/);
console.log('Gameplay CI isolation: PR-only build, no publish, downstream push admission denied');
