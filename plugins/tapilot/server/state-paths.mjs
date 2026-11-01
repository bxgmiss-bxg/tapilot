import { existsSync } from "node:fs";
import { homedir } from "node:os";
import { join, resolve } from "node:path";

// Reuse existing state so renaming the plugin does not create a second runtime
// or lose the user's authorization, settings and reports.
export function stateRoot({ env = process.env, home = homedir() } = {}) {
  const override = env.TAPILOT_STATE_DIR || env.ANDROID_USE_STATE_DIR;
  if (override) return resolve(override);
  const legacy = join(home, ".local/share/android-use");
  return existsSync(legacy) ? legacy : join(home, ".local/share/tapilot");
}
