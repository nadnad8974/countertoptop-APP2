import { readFileSync } from "node:fs";

export const JPEG_DATA_URL = "data:image/jpeg;base64,/9j/2Q==";

export function drawingBody(overrides = {}) {
  return {
    image: JPEG_DATA_URL,
    include_verification_drawing: true,
    drawing_schema_version: 2,
    preserve_source_layout: true,
    preserve_source_orientation: true,
    rectangle_decomposition_required: true,
    use_dotted_lines_as_partition_guides: true,
    calculate_each_rectangle_separately: true,
    four_burner_circles_mean_stove: true,
    default_unmarked_stove_width_inches: 30,
    explicit_stove_dimension_overrides_default: true,
    return_undimensioned_countertop_shapes_for_user_editing: true,
    ...overrides
  };
}

export function fixture(name) {
  return JSON.parse(readFileSync(new URL(`./fixtures/${name}`, import.meta.url), "utf8"));
}

export function openAIResponse(modelResult, status = 200) {
  const body = status >= 200 && status < 300
    ? {
      status: "completed",
      output: [
        {
          type: "message",
          content: [
            { type: "output_text", text: JSON.stringify(modelResult) }
          ]
        }
      ]
    }
    : modelResult;
  return {
    ok: status >= 200 && status < 300,
    status,
    text: async () => JSON.stringify(body)
  };
}
