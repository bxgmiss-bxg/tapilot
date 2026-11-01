#!/usr/bin/env node
import { existsSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
const root = process.env.TAPILOT_ROOT || process.env.ANDROID_USE_ROOT;
const entry = root
  ? resolve(root, "server/cli.mjs")
  : fileURLToPath(new URL("../../../server/cli.mjs", import.meta.url));
if (!existsSync(entry)) {
  process.stderr.write(
    "Tapilot runtime is missing. Set TAPILOT_ROOT to the installed package containing server/cli.mjs.\n",
  );
  process.exitCode = 1;
} else await import(pathToFileURL(entry).href);
