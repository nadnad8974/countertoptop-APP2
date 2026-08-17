const MEBIBYTE = 1024 * 1024;

export const MAX_IMAGE_BYTES = 3 * MEBIBYTE;
export const MAX_REQUEST_BYTES = 4_400_000;

const ALLOWED_FIELDS = new Set([
  "image",
  "include_verification_drawing",
  "drawing_schema_version",
  "preserve_source_layout",
  "preserve_source_orientation",
  "rectangle_decomposition_required",
  "use_dotted_lines_as_partition_guides",
  "calculate_each_rectangle_separately",
  "four_burner_circles_mean_stove",
  "default_unmarked_stove_width_inches",
  "explicit_stove_dimension_overrides_default",
  "return_undimensioned_countertop_shapes_for_user_editing"
]);

const BOOLEAN_FIELDS = [
  "include_verification_drawing",
  "preserve_source_layout",
  "preserve_source_orientation",
  "rectangle_decomposition_required",
  "use_dotted_lines_as_partition_guides",
  "calculate_each_rectangle_separately",
  "four_burner_circles_mean_stove",
  "explicit_stove_dimension_overrides_default",
  "return_undimensioned_countertop_shapes_for_user_editing"
];

const DATA_URL = /^data:image\/(jpeg);base64,([A-Za-z0-9+/]+={0,2})$/;

export function validateDrawingRequest(body) {
  if (!isPlainObject(body)) {
    throw new DrawingRequestError("The drawing request is invalid.");
  }

  const fields = Object.keys(body);
  if (fields.some((field) => !ALLOWED_FIELDS.has(field))) {
    throw new DrawingRequestError(
      "The drawing request contains unsupported information."
    );
  }
  if (Buffer.byteLength(JSON.stringify(body), "utf8") > MAX_REQUEST_BYTES) {
    throw new DrawingRequestError("The drawing image is too large.", 413);
  }

  for (const field of BOOLEAN_FIELDS) {
    if (field in body && typeof body[field] !== "boolean") {
      throw new DrawingRequestError("The drawing options are invalid.");
    }
  }
  if ("drawing_schema_version" in body
      && (!Number.isInteger(body.drawing_schema_version)
        || body.drawing_schema_version < 1
        || body.drawing_schema_version > 3)) {
    throw new DrawingRequestError("The drawing schema version is invalid.");
  }
  if ("default_unmarked_stove_width_inches" in body
      && (!Number.isFinite(body.default_unmarked_stove_width_inches)
        || body.default_unmarked_stove_width_inches < 1
        || body.default_unmarked_stove_width_inches > 120)) {
    throw new DrawingRequestError("The stove default is invalid.");
  }

  const image = validateImageDataUrl(body.image);
  return Object.freeze({
    imageDataUrl: image.dataUrl,
    mimeType: image.mimeType,
    imageBytes: image.byteLength,
    stoveDefaultInches: Number.isFinite(body.default_unmarked_stove_width_inches)
      ? body.default_unmarked_stove_width_inches
      : 30
  });
}

export function validateContentLength(value) {
  if (value === undefined || value === null || value === "") return;
  const length = Number(value);
  if (!Number.isSafeInteger(length) || length < 0) {
    throw new DrawingRequestError("The request size is invalid.");
  }
  if (length > MAX_REQUEST_BYTES) {
    throw new DrawingRequestError("The drawing image is too large.", 413);
  }
}

function validateImageDataUrl(value) {
  if (typeof value !== "string") {
    throw new DrawingRequestError("A JPEG drawing is required.");
  }
  const match = DATA_URL.exec(value);
  if (!match || match[2].length % 4 !== 0) {
    throw new DrawingRequestError("The drawing must be a valid JPEG image.");
  }

  const approximateBytes = Math.floor((match[2].length * 3) / 4);
  if (approximateBytes > MAX_IMAGE_BYTES + 2) {
    throw new DrawingRequestError("The drawing image is too large.", 413);
  }

  let bytes;
  try {
    bytes = Buffer.from(match[2], "base64");
  } catch {
    throw new DrawingRequestError("The drawing image is invalid.");
  }
  if (bytes.length === 0 || bytes.length > MAX_IMAGE_BYTES) {
    throw new DrawingRequestError(
      bytes.length > MAX_IMAGE_BYTES
        ? "The drawing image is too large."
        : "The drawing image is empty.",
      bytes.length > MAX_IMAGE_BYTES ? 413 : 400
    );
  }

  const mimeType = `image/${match[1]}`;
  if (!matchesMagicBytes(bytes, mimeType)) {
    throw new DrawingRequestError("The drawing image type does not match its contents.");
  }

  return {
    dataUrl: value,
    mimeType,
    byteLength: bytes.length
  };
}

function matchesMagicBytes(bytes, mimeType) {
  return mimeType === "image/jpeg"
    && bytes.length >= 3
    && bytes[0] === 0xff
    && bytes[1] === 0xd8
    && bytes[2] === 0xff;
}

function isPlainObject(value) {
  return value !== null
    && typeof value === "object"
    && !Array.isArray(value)
    && (Object.getPrototypeOf(value) === Object.prototype
      || Object.getPrototypeOf(value) === null);
}

export class DrawingRequestError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.name = "DrawingRequestError";
    this.statusCode = statusCode;
  }
}
