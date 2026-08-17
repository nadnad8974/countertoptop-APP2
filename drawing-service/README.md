# Ramsier's drawing AI service

This is an isolated Vercel Function for reading a single countertop drawing. It uses the OpenAI Responses API with original-detail image input and strict Structured Outputs. The original image data URL is sent unchanged; the service does not force black-and-white conversion or destructive preprocessing.

The current Android response fields remain available:

- `square_feet`
- `can_calculate`
- `confidence`
- `explanation`
- `missing_information`
- `calculation_parts`
- `verification_drawing`

The response also contains `structured_result`, the validated schema-v3 result used to produce those legacy fields. Square footage is calculated by server code from sanitized measurements; it is never accepted from the model.

Measurement contract:

- `length_inches` is the across-run or horizontal dimension.
- `width_inches` is the front-to-back depth; model roles use `width`, never `depth`.
- Every calculation part represents one rectangle and must have `quantity: 1`.
- Every area calculation part has exactly one linked editable shape. If the model omits it, the server supplies a safe placeholder and requires user verification before calculation.
- When geometry is recognizable but a required measurement is absent or unclear, the model is instructed to return the visible editable shape promptly with a null measurement. Deterministic validation asks the user directly instead of waiting for or accepting a guess.
- A linked `length` or `width` dimension must match the value used by its calculation part within 0.01 inch. If it does not, the annotation stays visible and editable, the conflicting calculation value is cleared, and user verification is required before calculation.
- Every numeric area-affecting length or width must have linked visible measurement evidence with the same value and role (except the server-applied stove length default). Text classified as a piece label, identifier, square-foot annotation, inside-shape note, or unknown is never measurement evidence.
- Ambiguous text such as `#4` versus `14`, and annotations such as `0 sq ft`, are cleared from deterministic math and become a direct correction question instead of a guessed dimension.
- A schema-valid empty or unknown dimension evidence token is treated as unresolved evidence, not a response-format error: its value is excluded from math and the server asks for that measurement.
- The 30-inch unmarked stove default is applied by server code to a missing stove `length_inches` only. It is never used for front-to-back width.
- Only explicitly read measurements and the server-applied stove length default can be finalized automatically. `missing`, `derived`, and unsupported `default` sources require user verification even when they contain numeric values.
- Low-confidence area parts or linked dimensions require user verification and cannot produce a final square-foot total.
- Final correction questions are generated deterministically from the normalized parts, shapes, dimensions, confidence, and measurement source. Model-written missing-question text is schema-validated but never returned.
- The legacy `missing_information` string includes every whole correction question (up to 30 questions of 240 characters each) and is bounded to 7,229 characters; it never slices a question in the middle.

## Privacy and security

- The request accepts only one JPEG data URL plus the known drawing-analysis flags already sent by the Android app.
- Unknown fields are rejected. Names, phone numbers, email addresses, project addresses, customer notes, and other identity fields are not accepted.
- `OPENAI_API_KEY` is read only from the server environment. Never put it in Android, GitHub, screenshots, logs, or a response.
- Every request requires a provisioned device ID and bearer token. The raw token remains in Android secure storage; Vercel stores only its SHA-256 hash.
- `X-Ramsiers-App` is a telemetry identifier only. It is not authentication and must not be used as an allow/deny control.
- OpenAI requests use `store: false`. The service does not log the image, request body, response body, key, or upstream error details.
- Rejected normalized output keeps only an allowlisted validation code in the internal server error. The public endpoint returns a generic message and never returns that code or raw model/image data.
- The image is limited to 3 MiB decoded and the complete JSON request to 4,400,000 bytes so it remains below the serverless request ceiling.
- Missing or uncertain dimensions are returned as direct questions. The service never invents a measurement.

## Local setup

Use Node 24. Put local secrets in ignored `.env.local`; do not commit them. This service intentionally has no production dependencies.

```bash
npm ci
npm test
```

Tests mock the OpenAI endpoint. They do not read `OPENAI_API_KEY`, make live API calls, or deploy anything.

## Vercel setup

Deploy this directory as its own Vercel project only after the exact Vercel account and project have been confirmed. Configure these encrypted server-side variables:

- `OPENAI_API_KEY` (required; Responses Write permission only)
- `RAMSIERS_DRAWING_DEVICE_TOKEN_HASHES` (required and Sensitive; a compact JSON object containing at most 20 provisioned devices)

Production analysis is fixed in code to the exact `gpt-5.6-sol` model, original
image detail, `medium` reasoning, one Responses API request, no fallback, and a
`90000` ms timeout. Stale `OPENAI_DRAWING_MODEL`,
`OPENAI_DRAWING_FALLBACK_MODEL`, `OPENAI_REASONING_EFFORT`, and
`OPENAI_TIMEOUT_MS` values are ignored and should be removed during the next
authorized Vercel environment cleanup. Do not add those variables to a new
deployment.

Endpoint: `POST /api/analyze`

Required headers:

```text
Content-Type: application/json
Authorization: Bearer <43-character-base64url-device-token>
X-Ramsiers-Device: <lowercase-provisioned-device-id>
X-Ramsiers-App: countertop-quote-v1
```

`X-Ramsiers-Device` must be 8-64 lowercase ASCII characters matching
`^[a-z0-9][a-z0-9._-]{7,63}$`. The raw bearer token is exactly 32 random bytes
encoded as 43 base64url characters without padding. Never put that raw token in
Vercel, source code, GitHub, logs, screenshots, or support messages.

In Vercel, mark `RAMSIERS_DRAWING_DEVICE_TOKEN_HASHES` as **Sensitive** and set
its value to one compact JSON object. Each value is the lowercase SHA-256 hex
hash of that device's raw token, never the token itself. This is an illustrative
placeholder only:

```json
{"installer-phone-01":"<64-lowercase-sha256-hex>"}
```

The private enrollment QR must contain exactly this URI shape, with no extra
parameters, path, fragment, padding, or URL encoding:

```text
ramsiers-drawing://enroll?device=<same-lowercase-device-id>&token=<43-character-base64url-device-token>
```

On the phone, an owner opens **Edit app**, enters the admin PIN, then taps
**Scan connection QR**. The app validates the exact QR format and encrypts the
credential with Android Keystore. Keep the QR private and delete the temporary
QR file after enrollment has been verified. **Disconnect Drawing AI** removes
the encrypted credential from that phone without deleting jobs or drawings.

Malformed configuration, an unknown device, or an invalid token all fail closed
with the same generic `401 Unauthorized` response. To revoke one device, remove
only its entry from the JSON mapping and redeploy; no Android or OpenAI secret
belongs in this mapping.

Before enforcement, stage the Vercel WAF rate-limit rule in **Log** mode for the
exact `POST /api/analyze` route. Use `header:x-ramsiers-device` as the rate-limit
key, observe legitimate traffic, and only then change the reviewed rule to
**Deny (429)**. The header is a rate-limit bucket key, not proof of identity;
the bearer-token check remains mandatory in the function.

Do not switch the Android endpoint until mocked tests, representative drawing fixtures, a non-customer test image, and a reversible production verification have all passed.
