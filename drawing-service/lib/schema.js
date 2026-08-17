const nullableNumber = {
  anyOf: [
    { type: "number" },
    { type: "null" }
  ]
};

const nullableInteger = {
  anyOf: [
    { type: "integer" },
    { type: "null" }
  ]
};

export const DRAWING_RESULT_SCHEMA = Object.freeze({
  type: "object",
  additionalProperties: false,
  required: [
    "schema_version",
    "units",
    "can_calculate",
    "confidence",
    "explanation",
    "warnings",
    "missing_measurements",
    "parts",
    "drawing"
  ],
  properties: {
    schema_version: { type: "integer", enum: [3] },
    units: { type: "string", enum: ["inches"] },
    can_calculate: { type: "boolean" },
    confidence: { type: "string", enum: ["high", "medium", "low"] },
    explanation: { type: "string" },
    warnings: {
      type: "array",
      items: { type: "string" }
    },
    missing_measurements: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: [
          "id",
          "part_id",
          "role",
          "question",
          "reason",
          "affects_square_feet"
        ],
        properties: {
          id: { type: "string" },
          part_id: { type: "string" },
          role: {
            type: "string",
            enum: ["length", "width", "other"]
          },
          question: { type: "string" },
          reason: { type: "string" },
          affects_square_feet: { type: "boolean" }
        }
      }
    },
    parts: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: [
          "id",
          "feature_type",
          "operation",
          "length_inches",
          "width_inches",
          "quantity",
          "confidence",
          "measurement_source"
        ],
        properties: {
          id: { type: "string" },
          feature_type: {
            type: "string",
            enum: ["countertop", "backsplash", "sink", "stove", "cooktop", "other"]
          },
          operation: {
            type: "string",
            enum: ["add", "subtract", "ignore"]
          },
          length_inches: nullableNumber,
          width_inches: nullableNumber,
          quantity: { type: "number", enum: [1] },
          confidence: { type: "string", enum: ["high", "medium", "low"] },
          measurement_source: {
            type: "string",
            enum: ["explicit", "derived", "default", "missing"]
          }
        }
      }
    },
    drawing: {
      type: "object",
      additionalProperties: false,
      required: ["canvas_width", "canvas_height", "shapes", "dimensions"],
      properties: {
        canvas_width: { type: "number" },
        canvas_height: { type: "number" },
        shapes: {
          type: "array",
          items: {
            type: "object",
            additionalProperties: false,
            required: [
              "id",
              "link_id",
              "feature_type",
              "kind",
              "opening_type",
              "burner_count",
              "points"
            ],
            properties: {
              id: { type: "string" },
              link_id: { type: "string" },
              feature_type: {
                type: "string",
                enum: ["countertop", "backsplash", "sink", "stove", "cooktop", "other"]
              },
              kind: {
                type: "string",
                enum: ["countertop", "backsplash", "opening"]
              },
              opening_type: {
                type: "string",
                enum: ["none", "sink", "stove", "cooktop", "other"]
              },
              burner_count: nullableInteger,
              points: {
                type: "array",
                items: {
                  type: "object",
                  additionalProperties: false,
                  required: ["x", "y"],
                  properties: {
                    x: { type: "number" },
                    y: { type: "number" }
                  }
                }
              }
            }
          }
        },
        dimensions: {
          type: "array",
          items: {
            type: "object",
            additionalProperties: false,
            required: [
              "part_id",
              "role",
              "value_inches",
              "label",
              "source_text",
              "source_kind",
              "placement",
              "x1",
              "y1",
              "x2",
              "y2",
              "confidence"
            ],
            properties: {
              part_id: { type: "string" },
              role: {
                type: "string",
                enum: ["length", "width", "other"]
              },
              value_inches: nullableNumber,
              label: { type: "string" },
              source_text: { type: "string" },
              source_kind: {
                type: "string",
                enum: [
                  "measurement",
                  "piece_label",
                  "square_foot_annotation",
                  "identifier",
                  "unknown"
                ]
              },
              placement: {
                type: "string",
                enum: [
                  "outside_shape",
                  "on_dimension_line",
                  "inside_shape",
                  "unknown"
                ]
              },
              x1: { type: "number" },
              y1: { type: "number" },
              x2: { type: "number" },
              y2: { type: "number" },
              confidence: { type: "string", enum: ["high", "medium", "low"] }
            }
          }
        }
      }
    }
  }
});

export const DRAWING_TEXT_FORMAT = Object.freeze({
  type: "json_schema",
  name: "ramsiers_countertop_drawing_v3",
  description: "Measured countertop parts with classified visible dimension evidence, editable source-layout geometry, and explicit questions for every missing or uncertain dimension.",
  strict: true,
  schema: DRAWING_RESULT_SCHEMA
});
