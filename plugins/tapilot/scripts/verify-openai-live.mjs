import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, rm, cp } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import sharp from 'sharp';
import { MidsceneController } from '../server/midscene.mjs';

// Real provider + production controller/worker/SDK; only the phone transport is a fixture.
const model = 'gpt-6.1-terra';
const key = process.env.TAPILOT_OPENAI_API_KEY?.trim();
delete process.env.TAPILOT_OPENAI_API_KEY;
assert.ok(key, 'TAPILOT_OPENAI_API_KEY is required; this test must not silently skip');
const root = await mkdtemp(join(tmpdir(), 'tapilot-openai-live-'));
const output = resolve(process.env.TAPILOT_LIVE_OUTPUT || 'build/midscene-openai-live');
const evidence = { model, endpoint: 'https://api.openai.com/v1', device: 'synthetic screenshot fixture', checks: [] };
let complete = false;
let frame = 0;
let taps = 0;
const render = async (done) => 'data:image/png;base64,' + (await sharp(Buffer.from(
  `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="800">
  <rect width="600" height="800" fill="white"/>
  <text x="50" y="100" font-family="sans-serif" font-size="38">Tapilot model check</text>
  ${done ? '<text x="90" y="400" font-family="sans-serif" font-size="48">Task complete</text>' :
  '<rect x="100" y="300" width="400" height="140" rx="20" fill="#166534"/><text x="215" y="385" font-family="sans-serif" font-size="40" fill="white">START</text>'}
  </svg>`)).png().toBuffer()).toString('base64');
const images = [await render(false), await render(true)];
const phone = {
  connected: true,
  open: async () => {},
  request: async (method, params) => {
    if (method === 'device.capture') return {
      dataUrl: images[Number(complete)],
      geometry: { frameId: `fixture-${++frame}`, imageWidth: 600, imageHeight: 800 },
    };
    assert.equal(method, 'device.execute');
    assert.equal(params.frameId, `fixture-${frame}`, 'Action must use latest captured frame');
    const action = params.action;
    assert.equal(action.type, 'tap');
    assert.equal(complete, false, 'Do not replay the completed action');
    assert.ok(action.x >= 100 && action.x <= 500 && action.y >= 300 && action.y <= 440, 'Tap must hit START');
    taps++;
    complete = true;
  },
};
try {
  const apiKeyFile = join(root, 'key.txt');
  await writeFile(apiKeyFile, key, { mode: 0o600 });
  await writeFile(join(root, 'openai.json'), JSON.stringify({ model, apiKeyFile }), { mode: 0o600 });
  const controller = new MidsceneController({ root });
  await controller.configure({ action: 'settings', mode: 'ai' });
  async function check(name, action, text, expected = true) {
    const response = await controller.run(phone, { action, text, report_id: name });
    const result = response.structuredContent;
    evidence.checks.push({ name, ok: result.ok, expected, error: result.error, model: result.model, timing: result.timing });
    assert.equal(result.ok, expected, `${name}: ${result.error || 'unexpected verdict'}`);
    if (expected) {
      assert.equal(result.model, model);
      assert.equal(result.decision_source, 'openai_api_key');
    } else {
      assert.match(result.error, /Assertion failed/i, 'Negative case must fail on the assertion, not the provider');
    }
    assert.ok(result.report, 'Native Midscene report required');
    assert.ok((await readFile(result.report, 'utf8')).length > 0);
    console.log(`${name}: passed (${result.timing.totalMs} ms)`);
  }
  await check('initial-screen', 'assert', 'A green START button is visible on a white background.');
  await check('negative-verdict', 'assert', 'The screen shows a red STOP button.', false);
  await check('tap-start', 'act', 'Tap the green START button once. Finish when Task complete is displayed.');
  assert.equal(taps, 1, 'Model must execute exactly one correct tap');
  assert.equal(complete, true);
  await check('final-screen', 'assert', 'Task complete is visible and the START button is gone.');
  evidence.passed = true;
} finally {
  try {
    await mkdir(output, { recursive: true });
    await writeFile(join(output, 'result.json'), JSON.stringify(evidence, null, 2));
    // Upload reports only, never the state directory containing credentials.
    const reports = join(root, 'midscene_run', 'report');
    await cp(reports, join(output, 'reports'), { recursive: true }).catch(error => {
      if (error.code !== 'ENOENT') throw error;
    });
  } finally { await rm(root, { recursive: true, force: true }); }
}
