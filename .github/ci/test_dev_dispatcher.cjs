// Evaluate the actual job expression against workflow_run payloads; no dispatch/API calls.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const workflow = fs.readFileSync('.github/workflows/dispatch-dev-docker.yml', 'utf8');
const expression = workflow.match(/    if: >-\r?\n([\s\S]*?)    runs-on:/)[1].trim();
const repository = 'Theyyk/re-volution-mod';
function accepts(overrides = {}) {
  const run = {conclusion: 'success', event: 'push', head_branch: 'develop',
    head_repository: {full_name: repository}, ...overrides};
  return vm.runInNewContext(expression, {
    github: {repository, event: {workflow_run: run}},
    startsWith: (value, prefix) => value.startsWith(prefix),
  });
}

// Relevant fields from the real v1.3.9 tag run 40114068931.
assert.equal(accepts({head_branch: 'v1.3.9'}), false, 'stable tag must never dispatch DEV');
assert.equal(accepts(), true, 'successful develop push must dispatch DEV');
for (const branch of ['v1.3.10', 'v1.3.10-rc.1', 'main', 'master', 'stable', 'release',
  'release/v1.3.10', 'feature/test-foundation', 'fix/test', 'develop-copy', 'develop/test', '']) {
  assert.equal(accepts({head_branch: branch}), false, branch);
}
for (const conclusion of ['failure', 'cancelled', 'skipped', 'timed_out', null]) {
  assert.equal(accepts({conclusion}), false, String(conclusion));
}
for (const event of ['pull_request', 'workflow_dispatch', 'release']) {
  assert.equal(accepts({event}), false, event);
}
assert.equal(accepts({head_repository: {full_name: 'fork/re-volution-mod'}}), false);
assert.match(workflow, /workflow_run:\s*\n\s+workflows: \['Build and Release'\]\s*\n\s+branches: \[develop\]/);
assert.match(workflow, /event_type=mod-dev-build/);
console.log('DEV dispatcher: routing cases passed, including real v1.3.9 tag regression');

