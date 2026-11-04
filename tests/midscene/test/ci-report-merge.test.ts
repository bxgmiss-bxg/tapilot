import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync, readdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';
import { runTestProject } from '@midscene/test/config';

test('native report merge retains all cases, evidence hierarchy and failed verdicts', async () => {
  const root = mkdtempSync(join(tmpdir(), 'tapilot-report-merge-'));
  const sdk = import.meta.resolve('@midscene/test');
  const merger = fileURLToPath(new URL('../scripts/merge-ci-reports.mjs', import.meta.url));
  const lanes = [['CORE-01', 'CHAT-01', 'UI-05'], ['CORE-02', 'DRAFT-01'], ['DRAFT-02', 'DRAFT-03', 'UI-01'], ['UI-02', 'UI-03', 'UI-04']];
  try {
    for (let index = 0; index < lanes.length; index++) {
      const directory = join(root, `shard-results/midscene-apk-shard-${index + 1}`);
      mkdirSync(directory, { recursive: true });
      writeFileSync(join(directory, 'cases.yaml'), JSON.stringify({ cases: lanes[index].map(name => ({ name, steps: [{ probe: {} }] })) }));
      writeFileSync(join(directory, 'config.mjs'), `import {defineNode,z} from ${JSON.stringify(sdk)};
export default {projects:[{name:'android-${index + 1}',files:{include:['cases.yaml']},retry:0}],nodes:[defineNode({name:'probe',inputSchema:z.strictObject({}),execute(){${index === 1 ? "throw new Error('deliberate fixture failure')" : "return {summary:'fixture evidence'}"}}})]};`);
      await runTestProject({ cwd: directory, projectRoot: directory, configPath: join(directory, 'config.mjs') });
    }
    const result = spawnSync(process.execPath, [merger], { cwd: root, encoding: 'utf8' });
    assert.equal(result.status, 1, result.stderr);
    assert.match(result.stdout, /9\/11 passed/);
    const html = readFileSync(join(root, 'midscene_run/report/midscene-e2e-complete.html'), 'utf8');
    const match = html.match(/<script[^>]*type="midscene_test_run_dump"[^>]*>([\s\S]*?)<\/script>/);
    assert.ok(match, 'Must use the native Midscene test report');
    const dump = JSON.parse(match[1]);
    assert.equal(dump.summary.total, 11);
    assert.equal(dump.summary.failed, 2);
    assert.equal(dump.projects.length, 4);
    assert.equal(readdirSync(join(root, '.midscene/test-results')).length, 4);
  } finally { rmSync(root, { recursive: true, force: true }); }
});
