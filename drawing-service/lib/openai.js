import { DRAWING_INSTRUCTIONS, drawingUserPrompt } from "./prompt.js";
import { normalizeModelResult, ModelResultError } from "./result.js";
import { DRAWING_TEXT_FORMAT } from "./schema.js";

const OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
const MAX_RESPONSE_CHARACTERS = 2_000_000;

export const PRODUCTION_OPENAI_SETTINGS = Object.freeze({
  model: "gpt-5.6-sol",
  reasoningEffort: "medium",
  timeoutMs: 90_000
});

export async function analyzeDrawingWithOpenAI(
  drawingRequest,
  environment = process.env,
  dependencies = {}
) {
  const apiKey = environment.OPENAI_API_KEY;
  if (typeof apiKey !== "string" || apiKey.trim().length < 20) {
    throw new OpenAIServiceError("configuration", "Drawing AI is not configured.");
  }

  const fetchImpl = dependencies.fetchImpl || globalThis.fetch;
  if (typeof fetchImpl !== "function") {
    throw new OpenAIServiceError("configuration", "The server cannot contact Drawing AI.");
  }

  // These production settings are deliberately code-owned. Stale dashboard variables must
  // not change the reviewed model, reasoning, timeout, or one-request/no-fallback contract.
  return requestModel({
    apiKey,
    drawingRequest,
    fetchImpl,
    ...PRODUCTION_OPENAI_SETTINGS
  });
}

export function openAIRequestBody(
  model,
  drawingRequest,
  reasoningEffort = PRODUCTION_OPENAI_SETTINGS.reasoningEffort
) {
  return {
    model,
    instructions: DRAWING_INSTRUCTIONS,
    input: [
      {
        role: "user",
        content: [
          {
            type: "input_text",
            text: drawingUserPrompt(drawingRequest.stoveDefaultInches)
          },
          {
            type: "input_image",
            image_url: drawingRequest.imageDataUrl,
            detail: "original"
          }
        ]
      }
    ],
    reasoning: { effort: openAIReasoningEffort(reasoningEffort) },
    text: {
      format: DRAWING_TEXT_FORMAT,
      verbosity: "low"
    },
    max_output_tokens: 12_000,
    store: false
  };
}

async function requestModel({
  apiKey,
  drawingRequest,
  fetchImpl,
  model,
  reasoningEffort,
  timeoutMs
}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  let upstreamResponse;
  let responseText;
  try {
    upstreamResponse = await fetchImpl(OPENAI_RESPONSES_URL, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(openAIRequestBody(model, drawingRequest, reasoningEffort)),
      signal: controller.signal
    });
    responseText = await readBoundedResponse(upstreamResponse);
  } catch (error) {
    if (error instanceof OpenAIServiceError) throw error;
    if (error?.name === "AbortError" || controller.signal.aborted) {
      throw new OpenAIServiceError("timeout", "Drawing AI took too long to respond.");
    }
    throw new OpenAIServiceError("network", "Drawing AI could not be reached.");
  } finally {
    clearTimeout(timeout);
  }

  const responseJson = parseJsonObject(responseText);
  if (!upstreamResponse.ok) {
    const errorObject = responseJson?.error;
    throw new OpenAIServiceError(
      "upstream",
      "Drawing AI could not analyze the image.",
      {
        statusCode: upstreamResponse.status,
        upstreamCode: safeMetadata(errorObject?.code, 80),
        upstreamType: safeMetadata(errorObject?.type, 80),
        upstreamMessage: safeMetadata(errorObject?.message, 240)
      }
    );
  }
  if (!responseJson) {
    throw new OpenAIServiceError("invalid_response", "Drawing AI returned an invalid response.");
  }
  if (responseJson.status && responseJson.status !== "completed") {
    throw new OpenAIServiceError("incomplete", "Drawing AI did not finish the analysis.");
  }
  if (hasRefusal(responseJson)) {
    throw new OpenAIServiceError("refusal", "Drawing AI could not analyze this image.");
  }

  const outputText = extractOutputText(responseJson);
  if (!outputText) {
    throw new OpenAIServiceError("invalid_response", "Drawing AI returned no usable result.");
  }

  let modelResult;
  try {
    modelResult = JSON.parse(outputText);
  } catch {
    throw new OpenAIServiceError("invalid_response", "Drawing AI returned invalid structured data.");
  }
  try {
    return normalizeModelResult(modelResult, {
      stoveDefaultInches: drawingRequest.stoveDefaultInches
    });
  } catch (error) {
    if (error instanceof ModelResultError) {
      throw new OpenAIServiceError(
        "invalid_response",
        "Drawing AI returned unsafe measurement data.",
        { validationCode: error.validationCode }
      );
    }
    throw error;
  }
}

async function readBoundedResponse(response) {
  if (!response || typeof response.text !== "function") {
    throw new OpenAIServiceError("invalid_response", "Drawing AI returned an invalid response.");
  }
  const text = await response.text();
  if (text.length > MAX_RESPONSE_CHARACTERS) {
    throw new OpenAIServiceError("invalid_response", "Drawing AI returned too much data.");
  }
  return text;
}

function extractOutputText(response) {
  if (typeof response.output_text === "string") return response.output_text.trim();
  if (!Array.isArray(response.output)) return "";
  const text = [];
  for (const item of response.output) {
    if (!item || !Array.isArray(item.content)) continue;
    for (const content of item.content) {
      if (content?.type === "output_text" && typeof content.text === "string") {
        text.push(content.text);
      }
    }
  }
  return text.join("").trim();
}

function hasRefusal(response) {
  if (!Array.isArray(response.output)) return false;
  return response.output.some((item) => Array.isArray(item?.content)
    && item.content.some((content) => content?.type === "refusal"));
}

function parseJsonObject(value) {
  try {
    const parsed = JSON.parse(value);
    return parsed !== null && typeof parsed === "object" && !Array.isArray(parsed)
      ? parsed
      : null;
  } catch {
    return null;
  }
}

export function openAIReasoningEffort(value) {
  if (value === undefined || value === null || value === "") {
    return PRODUCTION_OPENAI_SETTINGS.reasoningEffort;
  }
  if (typeof value !== "string") {
    throw new OpenAIServiceError(
      "configuration",
      "The Drawing AI reasoning effort is not configured safely."
    );
  }
  const normalized = value.trim().toLowerCase();
  if (normalized === "medium" || normalized === "high") return normalized;
  throw new OpenAIServiceError(
    "configuration",
    "The Drawing AI reasoning effort must be medium or high."
  );
}

function safeMetadata(value, maximum) {
  return typeof value === "string"
    ? value.replace(/[\u0000-\u001f\u007f]/g, " ").slice(0, maximum)
    : "";
}

export class OpenAIServiceError extends Error {
  constructor(kind, message, details = {}) {
    super(message);
    this.name = "OpenAIServiceError";
    this.kind = kind;
    this.details = details;
  }
}
