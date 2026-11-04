import { execFileSync } from 'node:child_process';

const serial = process.env.TAPILOT_E2E_DEVICE_ID;
const packageName = process.env.TAPILOT_E2E_PACKAGE;
if (serial !== 'emulator-5554' || packageName !== 'lifeos.md.tapilot.e2everify') {
  throw new Error('CI provisioning requires the disposable emulator and verification package');
}
const adb = (...args) => execFileSync('adb', ['-s', serial, ...args], { encoding: 'utf8', timeout: 30_000 }).trim();
if (adb('shell', 'getprop', 'ro.kernel.qemu') !== '1') throw new Error('Refusing to provision a physical device');
const key = process.env.MIDSCENE_MODEL_API_KEY?.trim();
if (!key) throw new Error('Missing CI model key');
adb('shell', 'am', 'start', '-W', '-n', `${packageName}/lifeos.md.tapilot.MainActivity`);
// Allow the initial conversation store import to finish before provisioning.
await new Promise(resolve => setTimeout(resolve, 3000));
try {
  execFileSync('adb', ['-s', serial, 'shell', 'run-as', packageName, 'sh', '-c',
    "'umask 077; cat > files/ci-model-key'"], { input: key, stdio: ['pipe', 'ignore', 'pipe'], timeout: 15_000 });
  const result = adb('shell', 'am', 'broadcast', '-n', `${packageName}/lifeos.md.tapilot.performance.CiModelSetupReceiver`);
  if (!result.includes('result=0') || !result.includes('CI model configured')) throw new Error('Phone model provisioning failed');
} finally {
  adb('shell', 'run-as', packageName, 'rm', '-f', 'files/ci-model-key');
}
const service = `${packageName}/lifeos.md.tapilot.accessibility.MidsceneAccessibilityService`;
adb('shell', 'settings', 'put', 'secure', 'enabled_accessibility_services', service);
adb('shell', 'settings', 'put', 'secure', 'accessibility_enabled', '1');
console.log('Configured official Terra and accessibility on the disposable verification app');
