import assert from 'node:assert/strict';
import { copyFileSync, globSync, mkdirSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { ReportMergingTool } from '@midscene/core/report';

// Use Midscene's own merger: it preserves case status, steps, agent executions,
// screenshot assets and evidence references. Never manufacture a report landing page.
const merger = new ReportMergingTool();
const cases = [];
let failed = false;
for (let lane = 1; lane <= 4; lane++) {
  const root = `shard-results/midscene-apk-shard-${lane}`;
  const summaries = [...globSync(`${root}/.midscene/test-results/*/summary.json`)];
  assert.equal(summaries.length, 1, `Lane ${lane} must contain exactly one native run`);
  const summaryPath = summaries[0];
  const summary = JSON.parse(readFileSync(summaryPath, 'utf8'));
  assert.equal(summary.schemaVersion, 3, 'Review merger when upgrading Midscene');
  assert.equal(summary.projects.length, 1);
  assert.equal(summary.projects[0].name, `android-${lane}`);
  const report = resolve(dirname(summaryPath), summary.report);
  assert.ok(report.startsWith(`${resolve(root)}/`), 'Report must belong to its lane artifact');
  merger.append({ reportFilePath: report, reportAttributes: {
    testTitle: `Android lane ${lane}`, testId: `lane-${lane}`,
    testDescription: 'Isolated runner and Android 35 emulator; zero retries',
    testDuration: summary.durationMs,
    testStatus: summary.status === 'success' ? 'passed' : 'failed',
  } });
  cases.push(...summary.projects[0].cases);
  failed ||= summary.exitCode !== 0 || summary.status !== 'success';
  const resultDirectory = `.midscene/test-results/lane-${lane}`;
  mkdirSync(resultDirectory, { recursive: true });
  copyFileSync(resolve(dirname(summaryPath), summary.markdown), `${resultDirectory}/result.md`);
}
const report = merger.mergeReports('midscene-e2e-complete', { outputDir: 'midscene_run/report' });
assert.ok(report, 'Native report was not generated');
const expected = ['CORE-01', 'CORE-02', 'CHAT-01', 'DRAFT-01', 'DRAFT-02', 'DRAFT-03', 'UI-01', 'UI-02', 'UI-03', 'UI-04', 'UI-05'];
assert.deepEqual(cases.map(item => item.name.split(' ')[0]).sort(), expected.sort(), 'All eleven cases must run exactly once');
assert.ok(cases.every(item => item.attempts.length === 1), 'Retries or missing attempts are not accepted');
console.log(`Complete native report: ${report}; ${cases.filter(item => item.status === 'success').length}/11 passed`);
if (failed || cases.some(item => item.status !== 'success')) process.exitCode = 1;
