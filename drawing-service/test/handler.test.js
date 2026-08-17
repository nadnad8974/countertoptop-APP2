import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import test from "node:test";

import { createAnalyzeHandler } from "../api/analyze.js";
import { DEVICE_TOKEN_HASHES_ENV, MAX_AUTHORIZED_DEVICES } from "../lib/device-auth.js";
import { OpenAIServiceError } from "../lib/openai.js";
import { drawingBody } from "./helpers.js";

const DEVICE_ID = "installer-phone-01";
const DEVICE_TOKEN = "A".repeat(43);
const DEVICE_TOKEN_HASH = createHash("sha256").update(DEVICE_TOKEN, "ascii").digest("hex");
const AUTH_ENVIRONMENT = {
  [DEVICE_TOKEN_HASHES_ENV]: JSON.stringify({ [DEVICE_ID]: DEVICE_TOKEN_HASH })
};

test("handler requires per-device authorization and treats the app header as telemetry", async () => {
  let analysisCalls = 0;
  const handler = createAnalyzeHandler({
    analyze: async () => {
      analysisCalls += 1;
      return { can_calculate: false };
    },
    environment: AUTH_ENVIRONMENT
  });

  const unauthorized = responseRecorder();
  await handler({
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-ramsiers-app": "countertop-quote-v1"
    },
    body: drawingBody({ image: "not-even-a-data-url" })
  }, unauthorized);
  assert.equal(unauthorized.statusCode, 401);
  assert.deepEqual(unauthorized.body, { error: "Unauthorized." });
  assert.equal(analysisCalls, 0);

  const allowed = responseRecorder();
  await handler({
    method: "POST",
    headers: authorizedHeaders({ "x-ramsiers-app": "telemetry-value-can-change" }),
    body: drawingBody()
  }, allowed);
  assert.equal(allowed.statusCode, 200);
  assert.equal(allowed.headers["Cache-Control"], "no-store");
  assert.equal(analysisCalls, 1);
});

test("missing and invalid device credentials never invoke drawing analysis", async () => {
  let analysisCalls = 0;
  const handler = createAnalyzeHandler({
    analyze: async () => {
      analysisCalls += 1;
      return { can_calculate: false };
    },
    environment: AUTH_ENVIRONMENT
  });

  const invalidHeaders = [
    { "content-type": "application/json" },
    {
      "content-type": "application/json",
      "x-ramsiers-device": DEVICE_ID
    },
    {
      ...authorizedHeaders(),
      "x-ramsiers-device": "Installer-Phone-01"
    },
    {
      ...authorizedHeaders(),
      "x-ramsiers-device": "unknown-device-01"
    },
    {
      ...authorizedHeaders(),
      "x-ramsiers-device": "constructor"
    },
    {
      ...authorizedHeaders(),
      authorization: "Bearer too-short"
    },
    {
      ...authorizedHeaders(),
      authorization: `Bearer ${"B".repeat(43)}`
    },
    {
      ...authorizedHeaders(),
      authorization: ["Bearer duplicate", `Bearer ${DEVICE_TOKEN}`]
    }
  ];

  for (const headers of invalidHeaders) {
    const response = responseRecorder();
    await handler({
      method: "POST",
      headers,
      body: drawingBody({ image: "invalid-image-must-not-be-parsed" })
    }, response);
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.body, { error: "Unauthorized." });
  }
  assert.equal(analysisCalls, 0);
});

test("malformed or over-limit device configuration fails closed before analysis", async () => {
  const tooManyDevices = Object.fromEntries(
    Array.from({ length: MAX_AUTHORIZED_DEVICES + 1 }, (_, index) => [
      `installer-${String(index).padStart(2, "0")}`,
      DEVICE_TOKEN_HASH
    ])
  );
  const malformedConfigurations = [
    undefined,
    "{",
    "[]",
    "{}",
    JSON.stringify({ "UPPERCASE-device": DEVICE_TOKEN_HASH }),
    JSON.stringify({ [DEVICE_ID]: DEVICE_TOKEN_HASH.toUpperCase() }),
    JSON.stringify(tooManyDevices),
    " ".repeat(4097)
  ];

  for (const configuration of malformedConfigurations) {
    let analysisCalls = 0;
    const handler = createAnalyzeHandler({
      analyze: async () => {
        analysisCalls += 1;
        return { can_calculate: false };
      },
      environment: configuration === undefined
        ? {}
        : { [DEVICE_TOKEN_HASHES_ENV]: configuration }
    });
    const response = responseRecorder();
    await handler({
      method: "POST",
      headers: authorizedHeaders(),
      body: drawingBody()
    }, response);
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.body, { error: "Unauthorized." });
    assert.equal(analysisCalls, 0);
  }
});

test("authentication errors and logs never expose a token or configured hash", async () => {
  const wrongToken = "Z".repeat(43);
  const handler = createAnalyzeHandler({
    analyze: async () => assert.fail("unauthorized request reached analyzer"),
    environment: AUTH_ENVIRONMENT
  });
  const response = responseRecorder();
  const capturedLogs = [];
  const consoleMethods = ["debug", "error", "info", "log", "warn"];
  const originalConsoleMethods = Object.fromEntries(
    consoleMethods.map((method) => [method, console[method]])
  );

  try {
    for (const method of consoleMethods) {
      console[method] = (...values) => capturedLogs.push(values);
    }
    await handler({
      method: "POST",
      headers: authorizedHeaders({ authorization: `Bearer ${wrongToken}` }),
      body: drawingBody()
    }, response);
  } finally {
    for (const method of consoleMethods) console[method] = originalConsoleMethods[method];
  }

  assert.equal(response.statusCode, 401);
  assert.deepEqual(capturedLogs, []);
  const publicResponse = JSON.stringify({ response: response.body, logs: capturedLogs });
  assert.equal(publicResponse.includes(wrongToken), false);
  assert.equal(publicResponse.includes(DEVICE_TOKEN), false);
  assert.equal(publicResponse.includes(DEVICE_TOKEN_HASH), false);
  assert.equal(publicResponse.includes(DEVICE_ID), false);
});

test("handler never exposes internal normalization diagnostics", async () => {
  const rawSentinel = "RAW_RESPONSE_OR_IMAGE_MUST_NOT_REACH_ANDROID";
  const handler = createAnalyzeHandler({
    analyze: async () => {
      throw new OpenAIServiceError(
        "invalid_response",
        `Internal diagnostic only: ${rawSentinel}`,
        { validationCode: "invalid_canvas_size" }
      );
    },
    environment: AUTH_ENVIRONMENT
  });
  const response = responseRecorder();

  await handler({
    method: "POST",
    headers: authorizedHeaders(),
    body: drawingBody()
  }, response);

  assert.equal(response.statusCode, 502);
  assert.deepEqual(response.body, {
    error: "The drawing could not be analyzed safely. Please try again."
  });
  const publicResponse = JSON.stringify(response.body);
  assert.equal(publicResponse.includes(rawSentinel), false);
  assert.equal(publicResponse.includes("invalid_canvas_size"), false);
  assert.equal(publicResponse.includes(drawingBody().image), false);
});

function authorizedHeaders(overrides = {}) {
  return {
    "content-type": "application/json",
    "x-ramsiers-app": "countertop-quote-v1",
    "x-ramsiers-device": DEVICE_ID,
    authorization: `Bearer ${DEVICE_TOKEN}`,
    ...overrides
  };
}

function responseRecorder() {
  return {
    headers: {},
    statusCode: null,
    body: null,
    setHeader(name, value) {
      this.headers[name] = value;
    },
    status(code) {
      this.statusCode = code;
      return this;
    },
    json(value) {
      this.body = value;
      return this;
    }
  };
}
