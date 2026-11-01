import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { stateRoot } from '../server/state-paths.mjs';

test('rename preserves existing state and shares the old runtime directory', (t) => {
  const home = mkdtempSync(join(tmpdir(), 'tapilot-state-'));
  t.after(() => rmSync(home, { recursive: true, force: true }));
  assert.equal(stateRoot({ home, env: {} }), join(home, '.local/share/tapilot'));
  const legacy = join(home, '.local/share/android-use');
  mkdirSync(join(legacy, 'chatgpt'), { recursive: true });
  writeFileSync(join(legacy, 'chatgpt', 'fixture.json'), '{"saved":true}');
  assert.equal(stateRoot({ home, env: {} }), legacy);
  assert.equal(readFileSync(join(stateRoot({ home, env: {} }), 'chatgpt', 'fixture.json'), 'utf8'), '{"saved":true}');
  mkdirSync(join(home, '.local/share/tapilot'));
  assert.equal(stateRoot({ home, env: {} }), legacy);
  assert.equal(stateRoot({ home, env: { ANDROID_USE_STATE_DIR: join(home, 'old') } }), join(home, 'old'));
  assert.equal(stateRoot({ home, env: { TAPILOT_STATE_DIR: join(home, 'new'), ANDROID_USE_STATE_DIR: join(home, 'old') } }), join(home, 'new'));
});
