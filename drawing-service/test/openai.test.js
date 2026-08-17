import assert from "node:assert/strict";
import test from "node:test";

import {
  analyzeDrawingWithOpenAI,
  OpenAIServiceError,
  openAIReasoningEffort,
  openAIRequestBody,
  PRODUCTION_OPENAI_SETTINGS
} from "../lib/openai.js";
import { validateDrawingRequest } from "../lib/request.js";
import { DRAWING_INSTRUCTIONS, drawingUserPrompt } from "../lib/prompt.js";
import { drawingBody, fixture, openAIResponse } from "./helpers.js";

const ENVIRONMENT = {
  OPENAI_API_KEY: "mocked-api-token-value-never-sent",
  OPENAI_DRAWING_MODEL: "gpt-4.1",
  OPENAI_DRAWING_FALLBACK_MODEL: "gpt-5",
  OPENAI_REASONING_EFFORT: "high",
  OPENAI_TIMEOUT_MS: "15000"
};

test("uses Responses with unchanged original detail and strict schema", async () => {
  const drawingRequest = validateDrawingRequest(drawingBody());
  let observed;
  const result = await analyzeDrawingWithOpenAI(drawingRequest, ENVIRONMENT, {
    fetchImpl: async (url, options) => {
      observed = { url, options };
      return openAIResponse(fixture("model-complete.json"));
    }
  });

  assert.equal(observed.url, "https://api.openai.com/v1/responses");
  assert.equal(observed.options.method, "POST");
  assert.equal(observed.options.headers.Authorization, `Bearer ${ENVIRONMENT.OPENAI_API_KEY}`);
  const payload = JSON.parse(observed.options.body);
  assert.equal(payload.model, "gpt-5.6-sol");
  assert.equal(payload.reasoning.effort, "medium");
  assert.equal(payload.store, false);
  assert.equal(payload.input[0].content[1].type, "input_image");
  assert.equal(payload.input[0].content[1].detail, "original");
  assert.equal(payload.input[0].content[1].image_url, drawingRequest.imageDataUrl);
  assert.equal(payload.text.format.type, "json_schema");
  assert.equal(payload.text.format.strict, true);
  assert.equal(payload.text.format.schema.additionalProperties, false);
  assert.equal(observed.options.body.includes(ENVIRONMENT.OPENAI_API_KEY), false);
  assert.equal(result.square_feet, 44.27);
});

test("production analysis ignores every stale model, fallback, reasoning, and timeout override", async () => {
  const drawingRequest = validateDrawingRequest(drawingBody());
  let calls = 0;
  let observedPayload;
  await analyzeDrawingWithOpenAI(drawingRequest, ENVIRONMENT, {
    fetchImpl: async (_url, options) => {
      calls += 1;
      observedPayload = JSON.parse(options.body);
      return openAIResponse(fixture("model-complete.json"));
    }
  });
  assert.equal(calls, 1);
  assert.equal(observedPayload.model, "gpt-5.6-sol");
  assert.equal(observedPayload.reasoning.effort, "medium");
});

test("production analysis reads only the OpenAI API key from its environment", async () => {
  const environment = new Proxy(
    { OPENAI_API_KEY: ENVIRONMENT.OPENAI_API_KEY },
    {
      get(target, property) {
        assert.equal(property, "OPENAI_API_KEY");
        return target[property];
      }
    }
  );
  await analyzeDrawingWithOpenAI(
    validateDrawingRequest(drawingBody()),
    environment,
    { fetchImpl: async () => openAIResponse(fixture("model-complete.json")) }
  );
});

test("normalization failures retain only an allowlisted internal code", async () => {
  const drawingRequest = validateDrawingRequest(drawingBody());
  const rawSentinel = "RAW_MODEL_RESPONSE_MUST_NOT_LEAK";
  const unsafeModelResult = fixture("model-complete.json");
  unsafeModelResult.explanation = rawSentinel;
  unsafeModelResult.drawing.canvas_width = 2000;

  let capturedError;
  await assert.rejects(
    () => analyzeDrawingWithOpenAI(drawingRequest, ENVIRONMENT, {
      fetchImpl: async () => openAIResponse(unsafeModelResult)
    }),
    (error) => {
      capturedError = error;
      return error instanceof OpenAIServiceError;
    }
  );

  assert.equal(capturedError.kind, "invalid_response");
  assert.deepEqual(capturedError.details, {
    validationCode: "invalid_canvas_size"
  });
  const serializedError = JSON.stringify({
    message: capturedError.message,
    details: capturedError.details
  });
  assert.equal(serializedError.includes(rawSentinel), false);
  assert.equal(serializedError.includes(drawingRequest.imageDataUrl), false);
});

test("does not retry or fall back after a model-unavailable response", async () => {
  const drawingRequest = validateDrawingRequest(drawingBody());
  const models = [];
  await assert.rejects(
    () => analyzeDrawingWithOpenAI(drawingRequest, ENVIRONMENT, {
      fetchImpl: async (_url, options) => {
        models.push(JSON.parse(options.body).model);
        return openAIResponse({
          error: {
            code: "model_not_found",
            type: "invalid_request_error",
            message: "The requested model was not found."
          }
        }, 404);
      }
    }),
    OpenAIServiceError
  );
  assert.deepEqual(models, ["gpt-5.6-sol"]);
});

test("does not retry rate limits with a different model", async () => {
  const drawingRequest = validateDrawingRequest(drawingBody());
  let calls = 0;
  await assert.rejects(
    () => analyzeDrawingWithOpenAI(drawingRequest, ENVIRONMENT, {
      fetchImpl: async () => {
        calls += 1;
        return openAIResponse({
          error: {
            code: "rate_limit_exceeded",
            type: "rate_limit_error",
            message: "Rate limited."
          }
        }, 429);
      }
    }),
    OpenAIServiceError
  );
  assert.equal(calls, 1);
});

test("the request builder contains no customer identity values or server secret fields", () => {
  const payload = JSON.stringify(openAIRequestBody(
    "gpt-5.6",
    validateDrawingRequest(drawingBody())
  ));
  for (const forbidden of [
    "customerName",
    "555-555-5555",
    "customer@example.com",
    "projectAddress",
    "OPENAI_API_KEY"
  ]) {
    assert.equal(payload.includes(forbidden), false);
  }
});

test("production OpenAI settings are exact, fixed, and below the Vercel maximum", () => {
  assert.deepEqual(PRODUCTION_OPENAI_SETTINGS, {
    model: "gpt-5.6-sol",
    reasoningEffort: "medium",
    timeoutMs: 90_000
  });
  assert.equal(Object.isFrozen(PRODUCTION_OPENAI_SETTINGS), true);
  assert.ok(PRODUCTION_OPENAI_SETTINGS.timeoutMs < 180_000);
});

test("request-builder test helper defaults medium and accepts explicit high", () => {
  assert.equal(openAIReasoningEffort(undefined), "medium");
  assert.equal(openAIReasoningEffort(""), "medium");
  assert.equal(openAIReasoningEffort("medium"), "medium");
  assert.equal(openAIReasoningEffort("HIGH"), "high");
  assert.equal(
    openAIRequestBody(
      "gpt-5.6",
      validateDrawingRequest(drawingBody()),
      "medium"
    ).reasoning.effort,
    "medium"
  );
  assert.equal(
    openAIRequestBody(
      "gpt-5.6",
      validateDrawingRequest(drawingBody()),
      "high"
    ).reasoning.effort,
    "high"
  );
  assert.throws(() => openAIReasoningEffort("low"), OpenAIServiceError);
  assert.throws(() => openAIReasoningEffort("xhigh"), OpenAIServiceError);
});

test("prompt keeps inch marks separate from written half-inch fractions", () => {
  assert.match(DRAWING_INSTRUCTIONS, /trailing double-quote mark means inches/i);
  assert.match(DRAWING_INSTRUCTIONS, /must never be read as an added one-half/i);
  assert.match(DRAWING_INSTRUCTIONS, /visible 1\/2, ½/i);
  assert.match(DRAWING_INSTRUCTIONS, /width_inches always means its front-to-back depth/i);
  assert.match(DRAWING_INSTRUCTIONS, /never use a depth role/i);
  assert.match(DRAWING_INSTRUCTIONS, /do not apply the Ramsiers stove default yourself/i);
  assert.match(DRAWING_INSTRUCTIONS, /#4.*14/i);
  assert.match(DRAWING_INSTRUCTIONS, /sq ft.*not a dimension/i);
  assert.match(DRAWING_INSTRUCTIONS, /source_kind measurement/i);
});

test("prompt returns editable geometry promptly instead of guessing a missing measurement", () => {
  assert.match(DRAWING_INSTRUCTIONS, /fail fast on missing measurements/i);
  assert.match(DRAWING_INSTRUCTIONS, /do not prolong analysis trying to infer/i);
  assert.match(DRAWING_INSTRUCTIONS, /return the visible editable shape promptly/i);
  assert.match(DRAWING_INSTRUCTIONS, /length_inches or width_inches to null/i);
  assert.match(DRAWING_INSTRUCTIONS, /one short, direct question/i);
  assert.match(DRAWING_INSTRUCTIONS, /safe partial result is better than a delayed guess/i);
  assert.match(drawingUserPrompt(30), /return the visible shape promptly/i);
  assert.match(drawingUserPrompt(30), /value null and ask one direct question/i);
  assert.match(drawingUserPrompt(30), /instead of continuing to infer it/i);
});
