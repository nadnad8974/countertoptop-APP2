import assert from "node:assert/strict";
import test from "node:test";

import {
  calculateSquareFeet,
  joinMissingQuestionsForAndroid,
  ModelResultError,
  normalizeModelResult
} from "../lib/result.js";
import { DRAWING_RESULT_SCHEMA } from "../lib/schema.js";
import { fixture } from "./helpers.js";

test("model validation diagnostics use only allowlisted codes", () => {
  assert.equal(
    new ModelResultError(
      "RAW_MODEL_VALUE_THAT_MUST_NEVER_BECOME_A_DIAGNOSTIC"
    ).validationCode,
    "invalid_model_result"
  );
  assert.equal(
    new ModelResultError(
      "The model drawing canvas must be exactly 1000 by 700."
    ).validationCode,
    "invalid_canvas_size"
  );
});

test("legacy missing information preserves all 30 whole max-length questions", () => {
  const items = Array.from({ length: 30 }, (_unused, index) => {
    const prefix = `Q${String(index + 1).padStart(2, "0")}:`;
    return {
      question: prefix.padEnd(240, String.fromCharCode(65 + (index % 26)))
    };
  });
  const expectedQuestions = items.map((item) => item.question);
  const joined = joinMissingQuestionsForAndroid(items);

  assert.equal(joined.length, (30 * 240) + 29);
  assert.ok(joined.length <= (30 * 240) + 29);
  assert.deepEqual(joined.split(" "), expectedQuestions);
  assert.ok(expectedQuestions.every((question) => question.length === 240));
  assert.equal(joined.endsWith(expectedQuestions.at(-1)), true);
});

test("returns the legacy Android fields from deterministic sanitized math", () => {
  const result = normalizeModelResult(fixture("model-complete.json"));
  assert.equal(result.can_calculate, true);
  assert.equal(result.square_feet, 44.27);
  assert.equal(result.structured_result.schema_version, 3);
  assert.equal(result.structured_result.can_calculate, true);
  assert.equal(result.calculation_parts.length, 4);
  assert.equal(result.verification_drawing.canvas_width, 1000);
  assert.equal(result.verification_drawing.canvas_height, 700);
  assert.ok(result.verification_drawing.shapes.every((shape) => shape.id === shape.link_id));
  assert.deepEqual(result.verification_drawing.dimensions[0].part_ids, ["wall_run"]);
  assert.equal(result.verification_drawing.dimensions[0].label, "100\"");
  assert.equal(
    result.verification_drawing.shapes.find((shape) => shape.feature_type === "stove")
      .has_four_burner_circles,
    true
  );
});

test("keeps an undimensioned shape editable and asks the user instead of guessing", () => {
  const result = normalizeModelResult(fixture("model-missing.json"), {
    stoveDefaultInches: 30
  });
  assert.equal(result.can_calculate, false);
  assert.equal(result.square_feet, 0);
  assert.match(result.missing_information, /across-run length/i);
  assert.ok(result.verification_drawing.shapes.some((shape) => shape.id === "right_piece"));
  const rightPiece = result.calculation_parts.find((part) => part.id === "right_piece");
  assert.equal(rightPiece.length_inches, null);
  const stove = result.calculation_parts.find((part) => part.id === "stove_opening");
  assert.equal(stove.length_inches, 30);
  assert.equal(stove.measurement_source, "default");
});

test("calculates add and subtract operations without accepting a model total", () => {
  assert.equal(calculateSquareFeet([
    {
      feature_type: "countertop",
      operation: "add",
      length_inches: 120,
      width_inches: 25.5,
      quantity: 1
    },
    {
      feature_type: "stove",
      operation: "subtract",
      length_inches: 30,
      width_inches: 25.5,
      quantity: 1
    }
  ]), 15.94);
});

test("rejects model data outside the strict schema", () => {
  const unsafe = fixture("model-complete.json");
  unsafe.customer_name = "Customer";
  assert.throws(() => normalizeModelResult(unsafe), ModelResultError);
});

test("rejects a 2000 by 1400 model canvas instead of collapsing its coordinates", () => {
  const oversizedCanvas = fixture("model-complete.json");
  oversizedCanvas.drawing.canvas_width = 2000;
  oversizedCanvas.drawing.canvas_height = 1400;
  oversizedCanvas.drawing.shapes[0].points = [
    { x: 100, y: 100 },
    { x: 1200, y: 100 },
    { x: 1200, y: 440 },
    { x: 100, y: 440 }
  ];
  assert.throws(
    () => normalizeModelResult(oversizedCanvas),
    /canvas must be exactly 1000 by 700/i
  );
});

test("rejects feature other with operation ignore to match Android math", () => {
  const unsafeIgnore = fixture("model-complete.json");
  unsafeIgnore.parts.push({
    id: "unknown_piece",
    feature_type: "other",
    operation: "ignore",
    length_inches: 40,
    width_inches: 25.5,
    quantity: 1,
    confidence: "low",
    measurement_source: "explicit"
  });
  assert.throws(
    () => normalizeModelResult(unsafeIgnore),
    /cannot be ignored safely/i
  );
  assert.equal(calculateSquareFeet([
    {
      feature_type: "countertop",
      operation: "add",
      length_inches: 100,
      width_inches: 25.5,
      quantity: 1
    },
    {
      feature_type: "other",
      operation: "ignore",
      length_inches: 40,
      width_inches: 25.5,
      quantity: 1
    }
  ]), null);
});

test("applies the stove default only to length and requires front-to-back width", () => {
  const unsafeSecondDefault = fixture("model-complete.json");
  const stove = unsafeSecondDefault.parts.find((part) => part.id === "stove_opening");
  stove.length_inches = 30;
  stove.width_inches = 30;
  stove.measurement_source = "default";
  unsafeSecondDefault.can_calculate = true;

  const result = normalizeModelResult(unsafeSecondDefault, { stoveDefaultInches: 30 });
  const normalizedStove = result.calculation_parts.find(
    (part) => part.id === "stove_opening"
  );
  assert.equal(normalizedStove.length_inches, 30);
  assert.equal(normalizedStove.width_inches, null);
  assert.equal(result.can_calculate, false);
  assert.equal(result.square_feet, 0);
  assert.ok(result.structured_result.missing_measurements.some((item) =>
    item.part_id === "stove_opening"
      && item.role === "width"
      && /front-to-back/i.test(item.question)));

  const explicitLength = fixture("model-complete.json");
  const secondStove = explicitLength.parts.find((part) => part.id === "stove_opening");
  secondStove.length_inches = 30;
  secondStove.width_inches = null;
  secondStove.measurement_source = "missing";
  const secondResult = normalizeModelResult(explicitLength);
  const secondNormalizedStove = secondResult.calculation_parts.find(
    (part) => part.id === "stove_opening"
  );
  assert.equal(secondNormalizedStove.length_inches, null);
  assert.equal(secondNormalizedStove.width_inches, null);
  assert.ok(secondResult.structured_result.missing_measurements.some((item) =>
    item.part_id === "stove_opening" && item.role === "length"));
});

test("uses width for front-to-back roles and rejects depth", () => {
  const roleSchema = DRAWING_RESULT_SCHEMA.properties.missing_measurements
    .items.properties.role.enum;
  const dimensionRoleSchema = DRAWING_RESULT_SCHEMA.properties.drawing.properties
    .dimensions.items.properties.role.enum;
  assert.deepEqual(roleSchema, ["length", "width", "other"]);
  assert.deepEqual(dimensionRoleSchema, ["length", "width", "other"]);

  const invalidDepth = fixture("model-missing.json");
  invalidDepth.missing_measurements[0].role = "depth";
  assert.throws(() => normalizeModelResult(invalidDepth), /role is invalid/i);
});

test("strict dimensions require classified short source evidence", () => {
  const dimensionSchema = DRAWING_RESULT_SCHEMA.properties.drawing.properties
    .dimensions.items;
  assert.ok(dimensionSchema.required.includes("source_text"));
  assert.ok(dimensionSchema.required.includes("source_kind"));
  assert.ok(dimensionSchema.required.includes("placement"));
  assert.deepEqual(dimensionSchema.properties.source_kind.enum, [
    "measurement",
    "piece_label",
    "square_foot_annotation",
    "identifier",
    "unknown"
  ]);
  assert.deepEqual(dimensionSchema.properties.placement.enum, [
    "outside_shape",
    "on_dimension_line",
    "inside_shape",
    "unknown"
  ]);
});

test("requires one calculation part per rectangle with quantity exactly one", () => {
  assert.deepEqual(
    DRAWING_RESULT_SCHEMA.properties.parts.items.properties.quantity.enum,
    [1]
  );
  const repeatedPart = fixture("model-complete.json");
  repeatedPart.parts[0].quantity = 2;
  assert.throws(
    () => normalizeModelResult(repeatedPart),
    /quantity exactly 1/i
  );
  assert.equal(calculateSquareFeet([
    {
      feature_type: "countertop",
      operation: "add",
      length_inches: 100,
      width_inches: 25.5,
      quantity: 2
    }
  ]), null);
});

test("ignores raw missing-question text and generates one required role question", () => {
  const uncertain = fixture("model-missing.json");
  uncertain.missing_measurements[0].affects_square_feet = false;
  uncertain.missing_measurements.push({
    ...uncertain.missing_measurements[0],
    id: "duplicate_right_length",
    question: "Duplicate question that must not be returned."
  });
  const result = normalizeModelResult(uncertain);
  const rightLength = result.structured_result.missing_measurements.filter(
    (item) => item.part_id === "right_piece" && item.role === "length"
  );
  assert.equal(rightLength.length, 1);
  assert.equal(rightLength[0].affects_square_feet, true);
  assert.doesNotMatch(result.missing_information, /duplicate question/i);
  assert.doesNotMatch(result.missing_information, /top length/i);
  assert.equal(result.can_calculate, false);
});

test("returns exactly four useful live-style correction questions", () => {
  const liveStyle = fixture("model-missing.json");
  const derivedPiece = liveStyle.parts.find((part) => part.id === "main_run");
  derivedPiece.length_inches = 74.5;
  derivedPiece.width_inches = null;
  derivedPiece.confidence = "high";
  derivedPiece.measurement_source = "derived";
  liveStyle.drawing.dimensions.find(
    (dimension) => dimension.part_id === "main_run"
  ).value_inches = 74.5;

  const rightPiece = liveStyle.parts.find((part) => part.id === "right_piece");
  rightPiece.confidence = "high";
  const stove = liveStyle.parts.find((part) => part.id === "stove_opening");
  stove.width_inches = null;
  stove.confidence = "low";
  liveStyle.drawing.dimensions.push({
    part_id: "stove_opening",
    role: "length",
    value_inches: null,
    label: "",
    source_text: "unmarked",
    source_kind: "unknown",
    placement: "unknown",
    x1: 550,
    y1: 50,
    x2: 700,
    y2: 50,
    confidence: "low"
  });

  liveStyle.missing_measurements.push(
    {
      ...liveStyle.missing_measurements[0],
      id: "stale_duplicate_right_length",
      question: "STALE DUPLICATE RIGHT QUESTION"
    },
    {
      ...liveStyle.missing_measurements[0],
      id: "stale_stove_length",
      part_id: "stove_opening",
      role: "length",
      question: "STALE STOVE LENGTH QUESTION"
    },
    {
      ...liveStyle.missing_measurements[0],
      id: "unknown_piece_question",
      part_id: "unknown_piece",
      question: "UNKNOWN PIECE QUESTION"
    }
  );

  const result = normalizeModelResult(liveStyle, { stoveDefaultInches: 30 });
  const questions = result.structured_result.missing_measurements;
  assert.equal(result.can_calculate, false);
  assert.equal(questions.length, 4);
  assert.deepEqual(
    questions.map((item) => `${item.part_id}:${item.role}`),
    [
      "main_run:length",
      "main_run:width",
      "right_piece:length",
      "stove_opening:width"
    ]
  );
  assert.equal(new Set(questions.map(
    (item) => `${item.part_id}:${item.role}`
  )).size, 4);
  assert.ok(questions.find(
    (item) => item.part_id === "main_run" && item.role === "length"
  ).question.includes("derived"));
  assert.equal(questions.some(
    (item) => item.part_id === "stove_opening" && item.role === "length"
  ), false);
  assert.doesNotMatch(
    result.missing_information,
    /STALE|UNKNOWN PIECE/i
  );
});

test("rejects a linked shape whose feature disagrees with its part", () => {
  const mismatch = fixture("model-complete.json");
  mismatch.drawing.shapes.find((shape) => shape.link_id === "wall_run")
    .feature_type = "backsplash";
  assert.throws(
    () => normalizeModelResult(mismatch),
    /feature type does not match/i
  );
});

test("low-confidence area parts and dimensions require deduplicated verification", () => {
  const lowPart = fixture("model-complete.json");
  lowPart.parts.find((part) => part.id === "wall_run").confidence = "low";
  const lowPartResult = normalizeModelResult(lowPart);
  assert.equal(lowPartResult.can_calculate, false);
  assert.equal(lowPartResult.square_feet, 0);
  assert.equal(lowPartResult.structured_result.missing_measurements.filter(
    (item) => item.part_id === "wall_run" && item.role === "other"
  ).length, 1);

  const lowDimension = fixture("model-complete.json");
  lowDimension.drawing.dimensions[0].confidence = "low";
  lowDimension.missing_measurements.push({
    id: "verify_wall_length_from_model",
    part_id: "wall_run",
    role: "length",
    question: "Please verify the across-run length.",
    reason: "The handwriting is uncertain.",
    affects_square_feet: false
  });
  const lowDimensionResult = normalizeModelResult(lowDimension);
  const lengthQuestions = lowDimensionResult.structured_result.missing_measurements.filter(
    (item) => item.part_id === "wall_run" && item.role === "length"
  );
  assert.equal(lowDimensionResult.can_calculate, false);
  assert.equal(lengthQuestions.length, 1);
  assert.equal(lengthQuestions[0].affects_square_feet, true);
});

test("missing and derived area sources require verification even with numeric values", () => {
  for (const source of ["missing", "derived"]) {
    const uncertainSource = fixture("model-complete.json");
    uncertainSource.parts.find((part) => part.id === "wall_run")
      .measurement_source = source;
    uncertainSource.can_calculate = true;
    const result = normalizeModelResult(uncertainSource);
    assert.equal(result.can_calculate, false, source);
    assert.equal(result.square_feet, 0, source);
    assert.ok(result.structured_result.missing_measurements.some((item) =>
      item.part_id === "wall_run"
        && item.role === "other"
        && item.affects_square_feet), source);
  }

  const unsupportedDefault = fixture("model-complete.json");
  unsupportedDefault.parts.find((part) => part.id === "wall_run")
    .measurement_source = "default";
  assert.equal(normalizeModelResult(unsupportedDefault).can_calculate, false);

  assert.equal(normalizeModelResult(fixture("model-complete.json")).can_calculate, true);
});

test("keeps mismatched dimensions editable while excluding them from math", () => {
  const wrongValue = fixture("model-complete.json");
  wrongValue.drawing.dimensions[0].value_inches = 99;
  const wrongValueResult = normalizeModelResult(wrongValue);
  assert.equal(wrongValueResult.can_calculate, false);
  assert.equal(wrongValueResult.calculation_parts.find(
    (part) => part.id === "wall_run"
  ).length_inches, null);
  assert.equal(wrongValueResult.verification_drawing.dimensions[0].value_inches, 99);
  assert.ok(wrongValueResult.structured_result.missing_measurements.some((item) =>
    item.part_id === "wall_run"
      && item.role === "length"
      && item.affects_square_feet));

  const wrongRole = fixture("model-complete.json");
  wrongRole.drawing.dimensions[0].role = "width";
  const wrongRoleResult = normalizeModelResult(wrongRole);
  assert.equal(wrongRoleResult.can_calculate, false);
  assert.equal(wrongRoleResult.calculation_parts.find(
    (part) => part.id === "wall_run"
  ).width_inches, null);
  assert.equal(wrongRoleResult.verification_drawing.dimensions[0].role, "width");
  assert.ok(wrongRoleResult.structured_result.missing_measurements.some((item) =>
    item.part_id === "wall_run"
      && item.role === "width"
      && item.affects_square_feet));

  const missingValue = fixture("model-complete.json");
  missingValue.drawing.dimensions[0].value_inches = null;
  const missingValueResult = normalizeModelResult(missingValue);
  assert.equal(missingValueResult.can_calculate, false);
  assert.equal(missingValueResult.calculation_parts.find(
    (part) => part.id === "wall_run"
  ).length_inches, null);
  assert.equal(missingValueResult.verification_drawing.dimensions[0].value_inches, null);

  const roundedDisplay = fixture("model-complete.json");
  roundedDisplay.drawing.dimensions[0].value_inches = 100.009;
  assert.equal(normalizeModelResult(roundedDisplay).can_calculate, true);
});

test("never turns a #4 or square-foot annotation into a 14-inch measurement", () => {
  const confused = fixture("model-label-confusion.json");
  const result = normalizeModelResult(confused);
  const part = result.calculation_parts.find((item) => item.id === "narrow_piece");

  assert.equal(result.can_calculate, false);
  assert.equal(result.square_feet, 0);
  assert.equal(part.length_inches, 10);
  assert.equal(part.width_inches, null);
  assert.ok(result.structured_result.missing_measurements.some((item) =>
    item.part_id === "narrow_piece"
      && item.role === "width"
      && /front-to-back width/i.test(item.question)));
  assert.equal(result.verification_drawing.dimensions[1].value_inches, null);
  assert.equal(result.verification_drawing.dimensions[1].label, "");
  assert.equal(result.verification_drawing.dimensions[2].value_inches, 25.5);
  assert.equal(JSON.stringify(result).includes("#4 0 sq ft"), false);
  assert.equal(JSON.stringify(result).includes("source_kind"), false);
  assert.equal(JSON.stringify(result).includes("placement"), false);
});

test("inside-shape numeric text cannot support area math even if called a measurement", () => {
  const disguisedLabel = fixture("model-label-confusion.json");
  disguisedLabel.drawing.dimensions.splice(2, 1);
  const falseFourteen = disguisedLabel.drawing.dimensions[1];
  falseFourteen.source_text = "14";
  falseFourteen.source_kind = "measurement";
  falseFourteen.placement = "inside_shape";

  const result = normalizeModelResult(disguisedLabel);
  assert.equal(result.can_calculate, false);
  assert.equal(result.calculation_parts[0].width_inches, null);
  assert.ok(result.structured_result.missing_measurements.some((item) =>
    item.part_id === "narrow_piece" && item.role === "width"));
});

test("schema-valid empty dimension evidence becomes a question instead of a format error", () => {
  const emptyEvidence = fixture("model-label-confusion.json");
  emptyEvidence.drawing.dimensions.splice(2, 1);
  const uncertainDimension = emptyEvidence.drawing.dimensions[1];
  uncertainDimension.source_text = "";
  uncertainDimension.source_kind = "unknown";
  uncertainDimension.placement = "unknown";

  const result = normalizeModelResult(emptyEvidence);
  assert.equal(result.can_calculate, false);
  assert.equal(result.square_feet, 0);
  assert.equal(result.calculation_parts[0].width_inches, null);
  assert.ok(result.structured_result.missing_measurements.some((item) =>
    item.part_id === "narrow_piece"
      && item.role === "width"
      && item.affects_square_feet));
  assert.equal(JSON.stringify(result).includes("source_text"), false);
});

test("every calculated area role requires matching non-low-confidence evidence", () => {
  const absentEvidence = fixture("model-complete.json");
  absentEvidence.drawing.dimensions = absentEvidence.drawing.dimensions.filter(
    (dimension) => !(dimension.part_id === "island" && dimension.role === "length")
  );
  const absentResult = normalizeModelResult(absentEvidence);
  assert.equal(absentResult.can_calculate, false);
  assert.equal(absentResult.calculation_parts.find(
    (part) => part.id === "island"
  ).length_inches, null);

  const lowEvidence = fixture("model-complete.json");
  lowEvidence.drawing.dimensions.find(
    (dimension) => dimension.part_id === "island" && dimension.role === "length"
  ).confidence = "low";
  const lowResult = normalizeModelResult(lowEvidence);
  assert.equal(lowResult.can_calculate, false);
  assert.equal(lowResult.calculation_parts.find(
    (part) => part.id === "island"
  ).length_inches, null);
  assert.ok(lowResult.structured_result.missing_measurements.some((item) =>
    item.part_id === "island" && item.role === "length"));
});

test("adds one linked editable placeholder and blocks math when an area shape is missing", () => {
  const missingShape = fixture("model-complete.json");
  missingShape.drawing.shapes = missingShape.drawing.shapes.filter(
    (shape) => shape.link_id !== "island"
  );
  const result = normalizeModelResult(missingShape);
  assert.equal(result.can_calculate, false);
  assert.equal(result.square_feet, 0);
  assert.equal(result.verification_drawing.shapes.filter(
    (shape) => shape.link_id === "island"
  ).length, 1);
  assert.ok(result.structured_result.missing_measurements.some((item) =>
    item.part_id === "island"
      && item.role === "other"
      && /editable outline/i.test(item.question)));

  const duplicateShape = fixture("model-complete.json");
  duplicateShape.drawing.shapes.push({
    ...duplicateShape.drawing.shapes[0],
    id: "wall_run_duplicate",
    points: duplicateShape.drawing.shapes[0].points.map((point) => ({ ...point }))
  });
  assert.throws(
    () => normalizeModelResult(duplicateShape),
    /duplicate shape identifiers/i
  );
});
