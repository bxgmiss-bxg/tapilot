import { SCREEN_URI } from "./preview.mjs";
import { CONTROL_SCHEMA } from "./panel-controls.mjs";
import { AjvJsonSchemaValidator } from "@modelcontextprotocol/sdk/validation/ajv";
export const panelTools = [
  {
    name:'tapilot_devices', description:'App-only saved devices: list, connect, disconnect, or remove local pairing.',
    inputSchema:{type:'object',properties:{action:{type:'string',enum:['list','connect','disconnect','forget','update_address']},
      device_id:{type:'string',pattern:'^[a-f0-9]{64}$'},host:{type:'string'},port:{type:'integer',minimum:1024,maximum:65535}},required:['action'],additionalProperties:false},
    _meta:{ui:{visibility:['app']}},
  },
  {
    name: "tapilot_preview_open",
    description:
      "The only screen panel opener. At the start of EACH USER TURN that invokes Tapilot, requests its panel, or starts or continues a phone viewing, pairing, or control task, call once with recover:true BEFORE CLI/device checks, even if an earlier panel is open. Recovery requests a fresh host widget session so a crashed previous view is not reused; it does not reset phone pairing, resume a privacy pause, or replay phone actions. Reuse the panel for all remaining actions in the same turn. Plain opens reuse the current widget session. Mere discussion of plugin code does not require opening. For an open-only request, open and stop.",
    inputSchema: {
      type: "object",
      properties: {
        recover: {type:"boolean", description:"Request a fresh host panel session. Use once at the start of each user turn involving the phone, or when the user asks to recover a grey/crashed panel."},
      },
      additionalProperties: false,
    },
    _meta: {
      ui: { resourceUri: SCREEN_URI },
      "openai/ui": { entrypoints: [{ type: "thread" }] },
    },
  },
  {
    name: "tapilot_preview_control",
    description:
      "App-only direct user control of the displayed Android screen. One action per fresh frame; never retry automatically.",
    inputSchema: CONTROL_SCHEMA,
    annotations: {
      readOnlyHint: false,
      destructiveHint: true,
      idempotentHint: false,
      openWorldHint: true,
    },
    _meta: { ui: { visibility: ["app"] } },
  },
  {
    name: "tapilot_preview",
    _meta: { ui: { visibility: ["app"] } },
    description:
      "Pause or resume the existing Android panel without opening a tab. Use tapilot_preview_open once to display it. Legacy open only enables preview polling. Pause before private authentication; resume after user confirmation.",
    inputSchema: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["open", "pause", "resume", "suspend"],
        },
      },
      required: ["action"],
      additionalProperties: false,
    },
  },
  {
    name: "tapilot_preview_frame",
    description:
      "App-only live frame. Does not replace model observation or invalidate action frames.",
    inputSchema: {
      type: "object",
      properties: {},
      additionalProperties: false,
    },
    _meta: { ui: { visibility: ["app"] } },
  },
  {
    name: "tapilot_preview_resume",
    description:
      "App-only user confirmation: unlock/authentication completed. Recheck the phone before resuming.",
    inputSchema: {
      type: "object",
      properties: {},
      additionalProperties: false,
    },
    _meta: { ui: { visibility: ["app"] } },
  },
  {
    name: "tapilot_preview_pair",
    description: "App-only pairing controls.",
    inputSchema: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["pair", "pair_status", "pair_confirm", "pair_cancel"],
        },
      },
      required: ["action"],
      additionalProperties: false,
    },
    _meta: { ui: { visibility: ["app"] } },
  },
  {
    name: "tapilot_disconnect",
    description: "App-only release phone.",
    inputSchema: {
      type: "object",
      properties: {},
      additionalProperties: false,
    },
    _meta: { ui: { visibility: ["app"] } },
  },
];

for (const tool of panelTools) {
  if (['tapilot_preview_frame','tapilot_preview','tapilot_preview_resume'].includes(tool.name))
    tool.inputSchema.properties.viewer_id = {type:'string',pattern:'^[A-Za-z0-9_-]{1,64}$'};
}

const validator = new AjvJsonSchemaValidator();
const validators = new Map(
  panelTools.map((t) => [t.name, validator.getValidator(t.inputSchema)]),
);
export function panelRequest(request) {
  const validate = validators.get(request?.name);
  if (!validate || !validate(request.arguments ?? {}).valid)
    throw new Error("Unsupported panel request");
  return request.name === "tapilot_preview_pair"
    ? { ...request, name: "tapilot_midscene" }
    : request;
}
