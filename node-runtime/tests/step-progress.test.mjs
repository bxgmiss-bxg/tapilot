import test from 'node:test';
import assert from 'node:assert/strict';
import { createStepProgress, runDirectStep } from './step-progress.mjs';

test('emits ordered actual steps and updates status without duplicate snapshots', () => {
  const events = [];
  const update = createStepProgress(e => events.push(e), 'run');
  const task = { taskId: '1', type: 'Action Space', subType: 'Tap', status: 'pending', param: { locate: { prompt: 'Equals button' } } };
  update('', { tasks: [task] });
  assert.equal(events.length, 0);
  task.status = 'running';
  update('', { tasks: [task] });
  update('', { tasks: [task] });
  task.status = 'finished';
  update('', { tasks: [task] });
  for (const event of events) assert.equal(new Date(event.timestamp).toISOString(), event.timestamp);
  assert.deepEqual(events.map(e => [e.id, e.action, e.target, e.status]), [
    ['run:1', 'Tap', 'Equals button', 'running'], ['run:1', 'Tap', 'Equals button', 'finished'],
  ]);
});

test('omits input, model reasoning, screenshots, raw errors and pending actions', () => {
  const events = [];
  createStepProgress(e => events.push(e), 'run')('', { tasks: [{
    taskId: '2', type: 'Action Space', subType: 'Input', status: 'failed',
    param: { value: 'secret', text: 'secret', locate: { prompt: 'secret' } }, thought: 'private reasoning',
    log: { rawResponse: 'secret' }, errorMessage: 'secret', uiContext: { screenshot: 'secret' },
  }, { taskId: '3', type: 'Planning', subType: 'Plan', status: 'cancelled', param: { userInstruction: 'secret' } }] });
  assert.equal(events.length, 2);
  assert.doesNotMatch(JSON.stringify(events), /secret|reasoning|screenshot/);
  assert.equal(events[1].status, 'cancelled');
});

test('direct workflow steps emit completion or failure and preserve the original error', async () => {
  const events = [];
  assert.equal(await runDirectStep('Launch', async () => 42, e => events.push(e)), 42);
  const failure = new Error('private native diagnostic');
  await assert.rejects(runDirectStep('AndroidBackButton', async () => { throw failure; }, e => events.push(e)), e => e === failure);
  for (const event of events) assert.equal(new Date(event.timestamp).toISOString(), event.timestamp);
  assert.deepEqual(events.map(e => e.status), ['running', 'finished', 'running', 'failed']);
  assert.equal(events[0].id, events[1].id);
  assert.notEqual(events[0].id, events[2].id);
  assert.doesNotMatch(JSON.stringify(events), /private/);
});

test('the bundled Midscene Agent delivers real action snapshots through onDumpUpdate', async () => {
  const { Agent } = await import('@midscene/core/agent');
  const { AndroidAccessibilityDevice } = await import('@tapilot/android-accessibility');
  const { default: sharp } = await import('sharp');
  const png = await sharp({create: {width: 40, height: 80, channels: 3, background: '#ffffff'}}).png().toBuffer();
  const actions = [], events = [];
  const device = new AndroidAccessibilityDevice({
    execute: async action => actions.push(action),
    completeSession: async () => {},
    getSize: async () => ({width: 40, height: 80}),
    capture: async () => ({dataUrl: `data:image/png;base64,${png.toString('base64')}`, geometry: {frameId: 'test-frame', capturedAt: 1, displayId: 0, rotation: 0, screenWidth: 40, screenHeight: 80, imageWidth: 40, imageHeight: 80}}),
  });
  const agent = new Agent(device, {generateReport: false, autoPrintReportMsg: false,
    modelConfig: {MIDSCENE_MODEL_BASE_URL: 'https://example.invalid/v1', MIDSCENE_MODEL_NAME: 'fixture', MIDSCENE_MODEL_FAMILY: 'doubao-vision', MIDSCENE_MODEL_API_KEY: 'unused'}});
  agent.onDumpUpdate = createStepProgress(e => events.push(e), 'sdk');
  try {
    await agent.callActionInActionSpace('AndroidHomeButton', {});
    assert.deepEqual(actions, [{type: 'home'}]);
    assert.ok(events.some(e => e.action === 'AndroidHomeButton' && e.status === 'running'));
    assert.ok(events.some(e => e.action === 'AndroidHomeButton' && e.status === 'finished'));
  } finally { await agent.destroy(); }
});
