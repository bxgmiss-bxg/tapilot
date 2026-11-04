import { readFile } from "node:fs/promises";
import { isAbsolute, join } from "node:path";

// Explicit local override. Keep credentials outside settings, reports and IPC.
// A broken override must fail closed rather than fall back to another account.
export async function openAIConfig(root) {
  let config;
  try {
    config = JSON.parse(await readFile(join(root, "openai.json"), "utf8"));
  } catch (error) {
    if (error.code === "ENOENT") return null;
    throw new Error("Invalid local OpenAI configuration");
  }
  if (!config || typeof config.model !== "string" ||
      !/^gpt-[a-zA-Z0-9.-]+$/.test(config.model) ||
      typeof config.apiKeyFile !== "string" || !isAbsolute(config.apiKeyFile)) {
    throw new Error("OpenAI configuration requires model and absolute apiKeyFile");
  }
  let key;
  try { key = (await readFile(config.apiKeyFile, "utf8")).trim(); }
  catch { throw new Error("Cannot read configured OpenAI API key file"); }
  if (!key || /\s/.test(key)) throw new Error("Invalid OpenAI API key file");
  return { model: config.model, accessToken: async () => key };
}
