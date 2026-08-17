import { createHash, timingSafeEqual } from "node:crypto";

export const DEVICE_TOKEN_HASHES_ENV = "RAMSIERS_DRAWING_DEVICE_TOKEN_HASHES";
export const MAX_AUTHORIZED_DEVICES = 20;

const MAX_DEVICE_MAP_BYTES = 4096;
const DEVICE_ID_PATTERN = /^[a-z0-9][a-z0-9._-]{7,63}$/;
const DEVICE_TOKEN_PATTERN = /^[A-Za-z0-9_-]{43}$/;
const TOKEN_HASH_PATTERN = /^[0-9a-f]{64}$/;
const UNKNOWN_DEVICE_HASH = "0".repeat(64);

export function isAuthorizedDrawingDevice(headers, environment = process.env) {
  const deviceId = singleHeaderValue(headers?.deviceId);
  const authorization = singleHeaderValue(headers?.authorization);
  if (!DEVICE_ID_PATTERN.test(deviceId)) return false;

  const tokenMatch = /^Bearer ([A-Za-z0-9_-]{43})$/.exec(authorization);
  if (!tokenMatch || !DEVICE_TOKEN_PATTERN.test(tokenMatch[1])) return false;

  const authorizedDevices = parseAuthorizedDevices(environment?.[DEVICE_TOKEN_HASHES_ENV]);
  if (!authorizedDevices) return false;

  const suppliedHash = createHash("sha256").update(tokenMatch[1], "ascii").digest();
  const hasAuthorizedDevice = Object.hasOwn(authorizedDevices, deviceId);
  const expectedHashHex = hasAuthorizedDevice
    ? authorizedDevices[deviceId]
    : UNKNOWN_DEVICE_HASH;
  const expectedHash = Buffer.from(expectedHashHex, "hex");

  return timingSafeEqual(suppliedHash, expectedHash)
    && hasAuthorizedDevice;
}

function parseAuthorizedDevices(rawValue) {
  if (typeof rawValue !== "string"
      || rawValue.length === 0
      || Buffer.byteLength(rawValue, "utf8") > MAX_DEVICE_MAP_BYTES) {
    return null;
  }

  let parsed;
  try {
    parsed = JSON.parse(rawValue);
  } catch {
    return null;
  }

  if (parsed === null || Array.isArray(parsed) || typeof parsed !== "object") return null;

  const entries = Object.entries(parsed);
  if (entries.length === 0 || entries.length > MAX_AUTHORIZED_DEVICES) return null;
  for (const [deviceId, tokenHash] of entries) {
    if (!DEVICE_ID_PATTERN.test(deviceId)
        || typeof tokenHash !== "string"
        || !TOKEN_HASH_PATTERN.test(tokenHash)) {
      return null;
    }
  }

  return parsed;
}

function singleHeaderValue(value) {
  return typeof value === "string" ? value : "";
}
