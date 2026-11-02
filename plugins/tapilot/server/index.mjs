import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { randomUUID } from "node:crypto";
import { readFile } from "node:fs/promises";
import { SCREEN_URI, SCREEN_META, PANEL_META } from "./preview.mjs";
import { panelTools, panelRequest } from "./panel-tools.mjs";
import { runtimeRequest } from "./runtime-client.mjs";

let panelSessionId = PANEL_META["openai/widgetSessionId"];

const server = new Server(
  { name: "tapilot", version: "0.6.15" },
  {
    capabilities: { tools: {}, resources: {} },
    instructions:
      "At the start of each user turn that invokes Tapilot or continues a phone task, call tapilot_preview_open with recover:true once, even if an old panel exists. Reuse that panel within the turn; do not reopen for every action. All agent phone operations use the Tapilot CLI described in the bundled skill. Other tools are for panel interaction only. CLI and panel share one local runtime and Wi-Fi connection.",
  },
);
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: panelTools,
}));
server.setRequestHandler(ListResourcesRequestSchema, async () => ({
  resources: [
    {
      uri: SCREEN_URI,
      name: "Tapilot Screen",
      mimeType: "text/html;profile=mcp-app",
      _meta: SCREEN_META,
    },
  ],
}));
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  if (request.params.uri !== SCREEN_URI) throw new Error("Unknown UI resource");
  return {
    contents: [
      {
        uri: SCREEN_URI,
        mimeType: "text/html;profile=mcp-app",
        text: await readFile(
          new URL("../assets/screen.html", import.meta.url),
          "utf8",
        ),
        _meta: SCREEN_META,
      },
    ],
  };
});
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  try {
    panelRequest(request.params);
    // Return the shell before starting the runtime or waiting for its phone queue.
    // The mounted panel fetches devices and frames through its normal polling.
    if (request.params.name === 'tapilot_preview_open') {
      // A new user turn can replace a dead host view without touching the runtime.
      // Plain opens keep the latest session; only recovery rotates its identity.
      if (request.params.arguments?.recover) panelSessionId = `tapilot-screen-${randomUUID()}`;
      return {
        content: [{type:'text',text:'Tapilot screen panel'}],
        structuredContent: {state:'connecting'},
        _meta: {...PANEL_META, "openai/widgetSessionId": panelSessionId},
      };
    }
    return await runtimeRequest("/panel", request.params);
  } catch (error) {
    return { isError: true, content: [{ type: "text", text: error.message }] };
  }
});
await server.connect(new StdioServerTransport());
