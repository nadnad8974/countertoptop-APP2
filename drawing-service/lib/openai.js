import { DRAWING_INSTRUCTIONS, drawingUserPrompt } from "./prompt.js";
import { normalizeModelResult, ModelResultError } from "./result.js";
import { DRAWING_TEXT_FORMAT } from "./schema.js";

const OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
const DEFAULT_MODEL = "gpt-5.6-sol";
const DEFAULT_TIMEOUT_MS = 160_000;
const MIN_TIMEOUT_MS = 15_000;
const MAX_TIMEOUT_MS = 165_000;
const MAX_RESPONSE_CHARACTERS = 2_000_000;
const MODEL_NAME = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,79}$/;

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

  const primaryModel = configuredModel(environment.OPENAI_DRAWING_MODEL, DEFAULT_MODEL);
  const fallbackModel = configuredFallbackModel(environment.OPENAI_DRAWING_FALLBACK_MODEL);
  const timeoutMs = openAITimeoutMs(environment.OPENAI_TIMEOUT_MS);
  const reasoningEffort = openAIReasoningEffort(environment.OPENAI_REASONING_EFFORT);

  try {
    return await requestModel({
      apiKey,
      drawingRequest,
      fetchImpl,
      model: primaryModel,
      reasoningEffort,
      timeoutMs
    });
  } catch (error) {
    if (!isModelUnavailable(error)
        || !fallbackModel
        || fallbackModel === primaryModel) {
      throw error;
    }
    return requestModel({
      apiKey,
      drawingRequest,
      fetchImpl,
      model: fallbackModel,
      reasoningEffort,
      timeoutMs
    });
  }
}

export function openAIRequestBody(model, drawingRequest, reasoningEffort = "high") {
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

function isModelUnavailable(error) {
  if (!(error instanceof OpenAIServiceError) || error.kind !== "upstream") return false;
  const code = String(error.details.upstreamCode || "").toLowerCase();
  const message = String(error.details.upstreamMessage || "").toLowerCase();
  return code === "model_not_found"
    || code === "unsupported_model"
    || code === "invalid_model"
    || (error.details.statusCode === 404 && message.includes("model"))
    || (error.details.statusCode === 400
      && message.includes("model")
      && (message.includes("not found")
        || message.includes("does not exist")
        || message.includes("not available")));
}

function configuredModel(value, fallback) {
  const model = typeof value === "string" && value.trim() ? value.trim() : fallback;
  if (!MODEL_NAME.test(model)) {
    throw new OpenAIServiceError("configuration", "The Drawing AI model is not configured safely.");
  }
  return model;
}

function configuredFallbackModel(value) {
  if (typeof value !== "string" || !value.trim()) return null;
  const normalized = value.trim();
  if (["none", "off", "disabled"].includes(normalized.toLowerCase())) return null;
  if (!MODEL_NAME.test(normalized)) {
    throw new OpenAIServiceError(
      "configuration",
      "The Drawing AI fallback model is not configured safely."
    );
  }
  return normalized;
}

export function openAITimeoutMs(value) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return DEFAULT_TIMEOUT_MS;
  return Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, Math.round(parsed)));
}

export function openAIReasoningEffort(value) {
  if (value === undefined || value === null || value === "") return "high";
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
