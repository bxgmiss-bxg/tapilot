import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// Public npm audit is explicit: mirrors may not implement the security endpoint.
const registry = '--registry=https://registry.npmjs.org';

// Audit all lockfiles even if an earlier tree fails; never hide later findings.
const root = fileURLToPath(new URL('../', import.meta.url));
const checks = [
  ['pnpm', ['audit']],
  ['pnpm', ['--dir', 'node-runtime/bundle', 'audit']],
  ['npm', ['audit', '--prefix', 'plugins/tapilot']],
  ['npm', ['audit', '--prefix', 'tests/midscene']],
];
for (const [command, args] of checks) {
  console.log(`\nAuditing: ${command} ${args.join(' ')}`);
  const result = spawnSync(command, [...args, registry], { cwd: root, stdio: 'inherit' });
  if (result.error) console.error(result.error.message);
  if (result.status !== 0) process.exitCode = 1;
}
