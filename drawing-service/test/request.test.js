import assert from "node:assert/strict";
import test from "node:test";

import {
  DrawingRequestError,
  MAX_IMAGE_BYTES,
  MAX_REQUEST_BYTES,
  validateContentLength,
  validateDrawingRequest
} from "../lib/request.js";
import { drawingBody, JPEG_DATA_URL } from "./helpers.js";

test("accepts exactly the current Android drawing contract", () => {
  const request = validateDrawingRequest(drawingBody());
  assert.equal(request.imageDataUrl, JPEG_DATA_URL);
  assert.equal(request.mimeType, "image/jpeg");
  assert.equal(request.stoveDefaultInches, 30);
});

test("rejects customer, payment, and unknown information", () => {
  for (const extra of [
    { customerName: "Customer" },
    { phone: "555-555-5555" },
    { email: "customer@example.com" },
    { projectAddress: "123 Main Street" },
    { amountCents: 1000 },
    { quoteReference: "rq_private" }
  ]) {
    assert.throws(
      () => validateDrawingRequest({ ...drawingBody(), ...extra }),
      DrawingRequestError
    );
  }
});

test("requires a JPEG whose bytes match the declared image type", () => {
  assert.throws(
    () => validateDrawingRequest(drawingBody({ image: "data:image/png;base64,iVBORw0KGgo=" })),
    DrawingRequestError
  );
  assert.throws(
    () => validateDrawingRequest(drawingBody({ image: "data:image/jpeg;base64,AAAA" })),
    DrawingRequestError
  );
});

test("enforces request size before analysis", () => {
  assert.equal(MAX_IMAGE_BYTES, 3 * 1024 * 1024);
  assert.equal(MAX_REQUEST_BYTES, 4_400_000);
  assert.throws(() => validateContentLength(MAX_REQUEST_BYTES + 1), (error) => {
    assert.equal(error.statusCode, 413);
    return true;
  });
});
