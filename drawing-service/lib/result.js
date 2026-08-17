const MAX_PARTS = 40;
const MAX_SHAPES = 24;
const MAX_DIMENSIONS = 50;
const MAX_POINTS = 16;
const MAX_MISSING = 30;
const MAX_QUESTION_CHARACTERS = 240;
const MAX_MISSING_INFORMATION_CHARACTERS =
  (MAX_MISSING * MAX_QUESTION_CHARACTERS) + (MAX_MISSING - 1);
const MAX_WARNINGS = 20;
const MAX_MEASUREMENT_INCHES = 2400;
const CANVAS_WIDTH = 1000;
const CANVAS_HEIGHT = 700;
const DIMENSION_MATCH_TOLERANCE_INCHES = 0.01;
const MAX_DIMENSION_SOURCE_CHARACTERS = 48;
const MODEL_VALIDATION_CODE_BY_MESSAGE = new Map([
  ["The model result uses an unsupported schema.", "unsupported_schema"],
  ["The model returned duplicate part identifiers.", "duplicate_part_id"],
  ["The model drawing canvas must be exactly 1000 by 700.", "invalid_canvas_size"],
  ["The model returned duplicate shape identifiers.", "duplicate_shape_id"],
  [
    "A linked shape feature type does not match its calculation part.",
    "shape_feature_mismatch"
  ],
  ["A model shape has fewer than three points.", "invalid_shape_points"],
  ["A dimension is not linked to a known part or shape.", "unlinked_dimension"],
  [
    "An area calculation part has more than one linked editable shape.",
    "duplicate_area_shape"
  ],
  ["An area calculation part is missing an editable shape.", "missing_area_shape"],
  [
    "An unclassified part cannot be ignored safely by the Android calculation.",
    "unsafe_other_ignore"
  ]
]);

const TOP_LEVEL_FIELDS = [
  "schema_version",
  "units",
  "can_calculate",
  "confidence",
  "explanation",
  "warnings",
  "missing_measurements",
  "parts",
  "drawing"
];
const PART_FIELDS = [
  "id",
  "feature_type",
  "operation",
  "length_inches",
  "width_inches",
  "quantity",
  "confidence",
  "measurement_source"
];
const MISSING_FIELDS = [
  "id",
  "part_id",
  "role",
  "question",
  "reason",
  "affects_square_feet"
];
const DRAWING_FIELDS = ["canvas_width", "canvas_height", "shapes", "dimensions"];
const SHAPE_FIELDS = [
  "id",
  "link_id",
  "feature_type",
  "kind",
  "opening_type",
  "burner_count",
  "points"
];
const DIMENSION_FIELDS = [
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
];

const FEATURES = new Set([
  "countertop",
  "backsplash",
  "sink",
  "stove",
  "cooktop",
  "other"
]);
const CONFIDENCE = new Set(["high", "medium", "low"]);
const MEASUREMENT_SOURCE = new Set(["explicit", "derived", "default", "missing"]);
const ROLES = new Set(["length", "width", "other"]);
const OPERATIONS = new Set(["add", "subtract", "ignore"]);
const KINDS = new Set(["countertop", "backsplash", "opening"]);
const OPENING_TYPES = new Set(["none", "sink", "stove", "cooktop", "other"]);
const DIMENSION_SOURCE_KINDS = new Set([
  "measurement",
  "piece_label",
  "square_foot_annotation",
  "identifier",
  "unknown"
]);
const DIMENSION_PLACEMENTS = new Set([
  "outside_shape",
  "on_dimension_line",
  "inside_shape",
  "unknown"
]);

export function normalizeModelResult(raw, options = {}) {
  assertObject(raw, "The model result is not an object.");
  assertExactFields(raw, TOP_LEVEL_FIELDS, "model result");
  if (raw.schema_version !== 3 || raw.units !== "inches") {
    throw new ModelResultError("The model result uses an unsupported schema.");
  }
  assertBoolean(raw.can_calculate, "can_calculate");
  if (typeof raw.explanation !== "string") {
    throw new ModelResultError("explanation must be text.");
  }

  const stoveDefaultInches = positiveBoundedNumber(
    options.stoveDefaultInches ?? 30,
    1,
    120,
    "stove default"
  );
  const idMap = new Map();
  const usedPartIds = new Set();
  const parts = boundedArray(raw.parts, MAX_PARTS, "parts").map((part, index) => {
    assertObject(part, `part ${index + 1}`);
    assertExactFields(part, PART_FIELDS, `part ${index + 1}`);
    const rawId = requiredText(part.id, 64, `part ${index + 1} id`);
    const id = normalizedId(rawId, `part_${index + 1}`);
    if (usedPartIds.has(id)) {
      throw new ModelResultError("The model returned duplicate part identifiers.");
    }
    usedPartIds.add(id);
    idMap.set(rawId, id);
    idMap.set(id, id);

    const featureType = enumValue(part.feature_type, FEATURES, "feature type");
    const requestedOperation = enumValue(part.operation, OPERATIONS, "part operation");
    let lengthInches = nullableMeasurement(part.length_inches, "part length");
    let widthInches = nullableMeasurement(part.width_inches, "part width");
    let measurementSource = enumValue(
      part.measurement_source,
      MEASUREMENT_SOURCE,
      "measurement source"
    );
    if (featureType === "stove"
        && measurementSource === "default"
        && widthInches === stoveDefaultInches) {
      widthInches = null;
    }
    if (featureType === "stove" && lengthInches === null) {
      lengthInches = stoveDefaultInches;
      measurementSource = "default";
    }
    if (part.quantity !== 1) {
      throw new ModelResultError("Each calculation part must have quantity exactly 1.");
    }

    return {
      id,
      link_id: id,
      feature_type: featureType,
      operation: canonicalOperation(featureType, requestedOperation),
      length_inches: lengthInches,
      width_inches: widthInches,
      quantity: 1,
      confidence: enumValue(part.confidence, CONFIDENCE, "part confidence"),
      measurement_source: measurementSource
    };
  });

  const partById = new Map(parts.map((part) => [part.id, part]));
  const drawing = normalizeDrawing(raw.drawing, idMap, partById);
  requireSafeAreaMeasurementEvidence(parts, drawing.dimensions);
  // Validate the model's required schema, but do not reuse model-written
  // questions. Final questions come only from normalized server state so stale,
  // unknown, or duplicate wording cannot reach Android.
  normalizeMissing(raw.missing_measurements, idMap);
  const missingMeasurements = [];
  addMissingPartQuestions(parts, missingMeasurements);
  ensureAreaPartShapes(parts, drawing.shapes, missingMeasurements);
  addMissingShapeQuestions(parts, drawing.shapes, missingMeasurements);
  addMeasurementSourceQuestions(parts, missingMeasurements, stoveDefaultInches);
  addLowConfidenceQuestions(parts, drawing, missingMeasurements);
  sortMissingQuestions(parts, drawing.shapes, missingMeasurements);

  const warnings = boundedArray(raw.warnings, MAX_WARNINGS, "warnings")
    .map((warning) => requiredText(warning, 240, "warning"));
  const confidence = enumValue(raw.confidence, CONFIDENCE, "confidence");
  const deterministicSquareFeet = calculateSquareFeet(parts);
  const unresolvedArea = missingMeasurements.some((item) => item.affects_square_feet);
  const canCalculate = raw.can_calculate
    && deterministicSquareFeet !== null
    && deterministicSquareFeet > 0
    && !unresolvedArea;

  const structuredResult = {
    schema_version: 3,
    units: "inches",
    can_calculate: canCalculate,
    confidence,
    explanation: cleanText(raw.explanation, 800),
    warnings,
    missing_measurements: missingMeasurements,
    parts: parts.map(({ link_id: _linkId, ...part }) => part),
    drawing: {
      canvas_width: CANVAS_WIDTH,
      canvas_height: CANVAS_HEIGHT,
      shapes: drawing.shapes,
      dimensions: drawing.dimensions.map(publicDimension)
    }
  };

  return {
    square_feet: canCalculate ? deterministicSquareFeet : 0,
    can_calculate: canCalculate,
    confidence,
    explanation: legacyExplanation(structuredResult, deterministicSquareFeet),
    missing_information: joinMissingQuestionsForAndroid(missingMeasurements),
    calculation_parts: parts,
    verification_drawing: legacyDrawing(drawing),
    structured_result: structuredResult
  };
}

export function calculateSquareFeet(parts) {
  if (!Array.isArray(parts) || parts.length === 0 || parts.length > MAX_PARTS) return null;
  let squareInches = 0;
  let includedAreaPart = false;

  for (const part of parts) {
    if (!part || typeof part !== "object") return null;
    const feature = part.feature_type;
    if (feature === "sink" || feature === "cooktop") {
      continue;
    }
    const length = part.length_inches;
    const width = part.width_inches;
    const quantity = part.quantity;
    if (!isPositiveFinite(length)
        || !isPositiveFinite(width)
        || quantity !== 1) return null;
    const area = length * width * quantity;
    if (!Number.isFinite(area)) return null;
    if (part.operation === "add") {
      squareInches += area;
      includedAreaPart = true;
    } else if (part.operation === "subtract") {
      squareInches -= area;
    } else {
      return null;
    }
  }

  if (!includedAreaPart || !Number.isFinite(squareInches) || squareInches <= 0) return null;
  return Math.round((squareInches / 144) * 100) / 100;
}

function normalizeMissing(rawItems, idMap) {
  const usedIds = new Set();
  return boundedArray(rawItems, MAX_MISSING, "missing measurements").map((item, index) => {
    assertObject(item, `missing measurement ${index + 1}`);
    assertExactFields(item, MISSING_FIELDS, `missing measurement ${index + 1}`);
    const rawId = requiredText(item.id, 64, "missing measurement id");
    const id = normalizedId(rawId, `missing_${index + 1}`);
    if (usedIds.has(id)) {
      throw new ModelResultError("The model returned duplicate missing-measurement identifiers.");
    }
    usedIds.add(id);
    assertBoolean(item.affects_square_feet, "affects_square_feet");
    const rawPartId = requiredText(item.part_id, 64, "missing measurement part id");
    const partId = idMap.get(rawPartId) || normalizedId(rawPartId, "unlinked_shape");
    return {
      id,
      part_id: partId,
      role: enumValue(item.role, ROLES, "missing measurement role"),
      question: requiredText(
        item.question,
        MAX_QUESTION_CHARACTERS,
        "missing measurement question"
      ),
      reason: cleanText(item.reason, 240),
      affects_square_feet: item.affects_square_feet
    };
  });
}

function normalizeDrawing(rawDrawing, idMap, partById) {
  assertObject(rawDrawing, "drawing");
  assertExactFields(rawDrawing, DRAWING_FIELDS, "drawing");
  const rawCanvasWidth = finiteNumber(rawDrawing.canvas_width, "canvas width");
  const rawCanvasHeight = finiteNumber(rawDrawing.canvas_height, "canvas height");
  if (rawCanvasWidth !== CANVAS_WIDTH || rawCanvasHeight !== CANVAS_HEIGHT) {
    throw new ModelResultError("The model drawing canvas must be exactly 1000 by 700.");
  }

  const usedShapeIds = new Set();
  const shapes = boundedArray(rawDrawing.shapes, MAX_SHAPES, "shapes")
    .map((shape, index) => {
      assertObject(shape, `shape ${index + 1}`);
      assertExactFields(shape, SHAPE_FIELDS, `shape ${index + 1}`);
      const rawShapeId = requiredText(shape.id, 64, "shape id");
      const rawLinkId = requiredText(shape.link_id, 64, "shape link id");
      const linkId = idMap.get(rawLinkId) || normalizedId(rawLinkId, `shape_${index + 1}`);
      if (usedShapeIds.has(linkId)) {
        throw new ModelResultError("The model returned duplicate shape identifiers.");
      }
      usedShapeIds.add(linkId);
      idMap.set(rawShapeId, linkId);
      idMap.set(shape.link_id, linkId);
      idMap.set(linkId, linkId);

      const featureType = enumValue(shape.feature_type, FEATURES, "shape feature type");
      const linkedPart = partById.get(linkId);
      if (linkedPart && linkedPart.feature_type !== featureType) {
        throw new ModelResultError(
          "A linked shape feature type does not match its calculation part."
        );
      }
      enumValue(shape.kind, KINDS, "shape kind");
      enumValue(shape.opening_type, OPENING_TYPES, "shape opening type");
      const points = boundedArray(shape.points, MAX_POINTS, "shape points");
      if (points.length < 3) {
        throw new ModelResultError("A model shape has fewer than three points.");
      }
      const normalizedPoints = points.map((point) => {
        assertObject(point, "shape point");
        assertExactFields(point, ["x", "y"], "shape point");
        return {
          x: clamp(finiteNumber(point.x, "point x"), 0, CANVAS_WIDTH),
          y: clamp(finiteNumber(point.y, "point y"), 0, CANVAS_HEIGHT)
        };
      });

      const burnerCount = shape.burner_count === null
        ? null
        : integerInRange(shape.burner_count, 0, 12, "burner count");
      const kind = canonicalKind(featureType);
      const openingType = kind === "opening" ? featureType : "none";
      return {
        id: linkId,
        link_id: linkId,
        feature_type: featureType,
        kind,
        opening_type: openingType,
        burner_count: burnerCount,
        points: normalizedPoints
      };
    });

  const knownIds = new Set([...idMap.values(), ...usedShapeIds]);
  const dimensions = boundedArray(rawDrawing.dimensions, MAX_DIMENSIONS, "dimensions")
    .map((dimension, index) => {
      assertObject(dimension, `dimension ${index + 1}`);
      assertExactFields(dimension, DIMENSION_FIELDS, `dimension ${index + 1}`);
      const rawPartId = requiredText(dimension.part_id, 64, "dimension part id");
      const partId = idMap.get(rawPartId) || normalizedId(rawPartId, "unlinked_shape");
      if (!knownIds.has(partId)) {
        throw new ModelResultError("A dimension is not linked to a known part or shape.");
      }
      let value = nullableMeasurement(dimension.value_inches, "dimension value");
      if (typeof dimension.label !== "string") {
        throw new ModelResultError("dimension label must be text.");
      }
      const sourceText = boundedWholeText(
        dimension.source_text,
        MAX_DIMENSION_SOURCE_CHARACTERS,
        "dimension source text"
      );
      const sourceKind = enumValue(
        dimension.source_kind,
        DIMENSION_SOURCE_KINDS,
        "dimension source kind"
      );
      const placement = enumValue(
        dimension.placement,
        DIMENSION_PLACEMENTS,
        "dimension placement"
      );
      if (!isSafeMeasurementEvidence(sourceText, sourceKind, placement)) {
        value = null;
      }
      const normalizedDimension = {
        part_id: partId,
        role: enumValue(dimension.role, ROLES, "dimension role"),
        value_inches: value,
        label: value === null ? "" : formatInches(value),
        source_text: sourceText,
        source_kind: sourceKind,
        placement,
        x1: clamp(finiteNumber(dimension.x1, "dimension x1"), 0, CANVAS_WIDTH),
        y1: clamp(finiteNumber(dimension.y1, "dimension y1"), 0, CANVAS_HEIGHT),
        x2: clamp(finiteNumber(dimension.x2, "dimension x2"), 0, CANVAS_WIDTH),
        y2: clamp(finiteNumber(dimension.y2, "dimension y2"), 0, CANVAS_HEIGHT),
        confidence: enumValue(dimension.confidence, CONFIDENCE, "dimension confidence")
      };
      reconcileLinkedDimension(partById.get(partId), normalizedDimension);
      return normalizedDimension;
    });

  return { shapes, dimensions };
}

function addMissingPartQuestions(parts, missingItems) {
  for (const part of parts) {
    if (!affectsArea(part)) continue;
    for (const role of ["length", "width"]) {
      const key = `${role}_inches`;
      if (part[key] !== null) continue;
      addMissingQuestion(missingItems, {
        id: uniqueGeneratedId(missingItems, `missing_${part.id}_${role}`),
        part_id: part.id,
        role,
        question: role === "length"
          ? `What is the across-run length in inches for this ${readableFeature(part.feature_type)}?`
          : `What is the front-to-back width in inches for this ${readableFeature(part.feature_type)}?`,
        reason: "The measurement was not confidently readable in the drawing.",
        affects_square_feet: true
      });
    }
  }
}

function addMissingShapeQuestions(parts, shapes, missingItems) {
  const partIds = new Set(parts.map((part) => part.id));
  for (const shape of shapes) {
    if (!affectsArea(shape) || partIds.has(shape.link_id)) continue;
    addMissingQuestion(missingItems, {
      id: uniqueGeneratedId(missingItems, `missing_${shape.link_id}_size`),
      part_id: shape.link_id,
      role: "other",
      question: `What are the across-run length and front-to-back width in inches for this ${readableFeature(shape.feature_type)}?`,
      reason: "The shape is visible but does not have enough readable dimensions.",
      affects_square_feet: true
    });
  }
}

function ensureAreaPartShapes(parts, shapes, missingItems) {
  const shapeCounts = new Map();
  for (const shape of shapes) {
    shapeCounts.set(shape.link_id, (shapeCounts.get(shape.link_id) || 0) + 1);
  }

  let placeholderIndex = 0;
  for (const part of parts) {
    if (!affectsArea(part)) continue;
    const count = shapeCounts.get(part.id) || 0;
    if (count > 1) {
      throw new ModelResultError(
        "An area calculation part has more than one linked editable shape."
      );
    }
    if (count === 1) continue;
    if (shapes.length >= MAX_SHAPES) {
      throw new ModelResultError(
        "An area calculation part is missing an editable shape."
      );
    }

    shapes.push(placeholderShape(part, placeholderIndex++));
    shapeCounts.set(part.id, 1);
    addMissingQuestion(missingItems, {
      id: uniqueGeneratedId(missingItems, `verify_${part.id}_outline`),
      part_id: part.id,
      role: "other",
      question: `Please verify the editable outline for this ${readableFeature(part.feature_type)} because the AI did not link one.`,
      reason: "A safe placeholder outline was added so the piece remains editable.",
      affects_square_feet: true
    });
  }
}

function placeholderShape(part, index) {
  const column = index % 5;
  const row = Math.floor(index / 5) % 4;
  const left = 40 + (column * 185);
  const top = 70 + (row * 150);
  const right = Math.min(CANVAS_WIDTH - 20, left + 130);
  const bottom = Math.min(CANVAS_HEIGHT - 20, top + 80);
  const kind = canonicalKind(part.feature_type);
  return {
    id: part.id,
    link_id: part.id,
    feature_type: part.feature_type,
    kind,
    opening_type: kind === "opening" ? part.feature_type : "none",
    burner_count: null,
    points: [
      { x: left, y: top },
      { x: right, y: top },
      { x: right, y: bottom },
      { x: left, y: bottom }
    ]
  };
}

function addMeasurementSourceQuestions(parts, missingItems, stoveDefaultInches) {
  for (const part of parts) {
    if (!affectsArea(part) || part.measurement_source === "explicit") continue;
    const allowedStoveDefault = part.measurement_source === "default"
      && part.feature_type === "stove"
      && part.length_inches === stoveDefaultInches;
    if (allowedStoveDefault) continue;

    const missingRoles = ["length", "width"].filter(
      (role) => part[`${role}_inches`] === null
    );
    const numericRoles = ["length", "width"].filter(
      (role) => part[`${role}_inches`] !== null
    );
    if (part.measurement_source === "missing" && missingRoles.length > 0) {
      // The role-specific null-value questions already block calculation and
      // tell the editor exactly which field needs an answer.
      continue;
    }
    if (part.measurement_source === "derived" && missingRoles.length === 2) {
      // Both direct missing-role questions are more useful than a third source
      // question when no derived numeric value remains.
      continue;
    }

    if (part.measurement_source === "derived"
        && numericRoles.length === 1
        && missingRoles.length === 1) {
      const role = numericRoles[0];
      const description = role === "length"
        ? "across-run length"
        : "front-to-back width";
      addMissingQuestion(missingItems, {
        id: uniqueGeneratedId(missingItems, `verify_${part.id}_${role}_source`),
        part_id: part.id,
        role,
        question: `Please verify the ${description} in inches for this ${readableFeature(part.feature_type)} because it was derived rather than read explicitly.`,
        reason: "A derived area measurement must be confirmed before calculation.",
        affects_square_feet: true
      });
      continue;
    }

    const sourceDescription = part.measurement_source === "derived"
      ? "was derived by the model and cannot be independently verified by the server"
      : part.measurement_source === "default"
        ? "uses an unsupported default"
        : "is marked missing even though numeric values may be present";
    addMissingQuestion(missingItems, {
      id: uniqueGeneratedId(missingItems, `verify_${part.id}_source`),
      part_id: part.id,
      role: "other",
      question: `Please verify the measurements for this ${readableFeature(part.feature_type)} because the measurement source ${sourceDescription}.`,
      reason: "Only explicit measurements and the server-applied stove length default can be finalized automatically.",
      affects_square_feet: true
    });
  }
}

function addLowConfidenceQuestions(parts, drawing, missingItems) {
  const areaIds = areaAffectingIds(parts, drawing.shapes);
  for (const part of parts) {
    if (!affectsArea(part) || part.confidence !== "low") continue;
    if (usesStoveLengthDefault(part)) {
      if (missingItems.some((item) => item.part_id === part.id
          && item.role === "width")) {
        continue;
      }
      addMissingQuestion(missingItems, {
        id: uniqueGeneratedId(missingItems, `verify_${part.id}_width`),
        part_id: part.id,
        role: "width",
        question: "Please verify the front-to-back width in inches for this stove opening.",
        reason: "The model marked the stove opening as low confidence.",
        affects_square_feet: true
      });
      continue;
    }
    addMissingQuestion(missingItems, {
      id: uniqueGeneratedId(missingItems, `verify_${part.id}_measurements`),
      part_id: part.id,
      role: "other",
      question: `Please verify the across-run length and front-to-back width for this ${readableFeature(part.feature_type)}.`,
      reason: "The model marked this area-affecting part as low confidence.",
      affects_square_feet: true
    });
  }

  for (const dimension of drawing.dimensions) {
    if (dimension.confidence !== "low" || !areaIds.has(dimension.part_id)) continue;
    const linkedPart = parts.find((part) => part.id === dimension.part_id);
    if (usesStoveLengthDefault(linkedPart) && dimension.role === "length") {
      continue;
    }
    const description = dimension.role === "length"
      ? "across-run length"
      : dimension.role === "width"
        ? "front-to-back width"
        : "marked measurement";
    addMissingQuestion(missingItems, {
      id: uniqueGeneratedId(
        missingItems,
        `verify_${dimension.part_id}_${dimension.role}`
      ),
      part_id: dimension.part_id,
      role: dimension.role,
      question: `Please verify the ${description} in inches for this drawing piece.`,
      reason: "The linked dimension was read with low confidence.",
      affects_square_feet: true
    });
  }
}

function sortMissingQuestions(parts, shapes, missingItems) {
  const partOrder = new Map();
  for (const part of parts) partOrder.set(part.id, partOrder.size);
  for (const shape of shapes) {
    if (!partOrder.has(shape.link_id)) {
      partOrder.set(shape.link_id, partOrder.size);
    }
  }
  const roleOrder = new Map([
    ["length", 0],
    ["width", 1],
    ["other", 2]
  ]);
  missingItems.sort((left, right) => {
    const leftPart = partOrder.get(left.part_id) ?? Number.MAX_SAFE_INTEGER;
    const rightPart = partOrder.get(right.part_id) ?? Number.MAX_SAFE_INTEGER;
    if (leftPart !== rightPart) return leftPart - rightPart;
    const leftRole = roleOrder.get(left.role) ?? Number.MAX_SAFE_INTEGER;
    const rightRole = roleOrder.get(right.role) ?? Number.MAX_SAFE_INTEGER;
    if (leftRole !== rightRole) return leftRole - rightRole;
    if (left.id === right.id) return 0;
    return left.id < right.id ? -1 : 1;
  });
}

function addMissingQuestion(missingItems, item) {
  item = {
    ...item,
    question: requiredWholeText(
      item.question,
      MAX_QUESTION_CHARACTERS,
      "generated missing measurement question"
    )
  };
  const existing = missingItems.find((candidate) => candidate.part_id === item.part_id
    && candidate.role === item.role);
  if (existing) {
    existing.affects_square_feet = existing.affects_square_feet
      || item.affects_square_feet;
    return false;
  }
  if (missingItems.length >= MAX_MISSING) {
    throw new ModelResultError("Too many measurements are missing.");
  }
  missingItems.push(item);
  return true;
}

function reconcileLinkedDimension(part, dimension) {
  if (!part || (dimension.role !== "length" && dimension.role !== "width")) return;
  const partField = `${dimension.role}_inches`;
  const expectedValue = part[partField];
  const defaultedUnmarkedStoveLength = part.feature_type === "stove"
    && part.measurement_source === "default"
    && dimension.role === "length"
    && dimension.value_inches === null;
  if (defaultedUnmarkedStoveLength) return;

  if (expectedValue === null) return;
  if (dimension.value_inches === null
      || Math.abs(expectedValue - dimension.value_inches)
        > DIMENSION_MATCH_TOLERANCE_INCHES) {
    // Preserve the visible/editable annotation, but never use a conflicting
    // value in deterministic math. addMissingPartQuestions adds the direct
    // role-specific question after every dimension has been normalized.
    part[partField] = null;
  }
}

function requireSafeAreaMeasurementEvidence(parts, dimensions) {
  for (const part of parts) {
    if (!affectsArea(part)) continue;
    for (const role of ["length", "width"]) {
      if (usesStoveLengthDefault(part) && role === "length") continue;
      const field = `${role}_inches`;
      const value = part[field];
      if (value === null) continue;

      const linked = dimensions.filter(
        (dimension) => dimension.part_id === part.id && dimension.role === role
      );
      const reliable = linked.filter((dimension) =>
        dimension.value_inches !== null
          && dimension.confidence !== "low"
          && isSafeMeasurementEvidence(
            dimension.source_text,
            dimension.source_kind,
            dimension.placement
          ));
      const hasConflict = linked.some((dimension) =>
        dimension.value_inches !== null
          && Math.abs(dimension.value_inches - value)
            > DIMENSION_MATCH_TOLERANCE_INCHES);
      const hasLowConfidenceEvidence = linked.some(
        (dimension) => dimension.confidence === "low"
      );
      const hasMatchingEvidence = reliable.some((dimension) =>
        Math.abs(dimension.value_inches - value)
          <= DIMENSION_MATCH_TOLERANCE_INCHES);

      if (!hasMatchingEvidence || hasConflict || hasLowConfidenceEvidence) {
        part[field] = null;
      }
    }
  }
}

function isSafeMeasurementEvidence(sourceText, sourceKind, placement) {
  if (sourceKind !== "measurement") return false;
  if (placement !== "outside_shape" && placement !== "on_dimension_line") {
    return false;
  }
  return !looksLikeNonMeasurementText(sourceText);
}

function looksLikeNonMeasurementText(value) {
  const text = String(value || "").trim().toLowerCase();
  if (!text) return true;
  return /(?:^|\s)#\s*\d+\b/.test(text)
    || /\b(?:piece|part|item)\s*[-:#]?\s*\d+\b/.test(text)
    || /\b(?:sq\.?\s*ft|sqft|square\s+(?:foot|feet))\b/.test(text)
    || /^(?:id|ref|h)\s*[-:#]?\s*\d+$/i.test(text);
}

function publicDimension(dimension) {
  const {
    source_text: _sourceText,
    source_kind: _sourceKind,
    placement: _placement,
    ...publicFields
  } = dimension;
  return publicFields;
}

function areaAffectingIds(parts, shapes) {
  const ids = new Set();
  for (const part of parts) {
    if (affectsArea(part)) ids.add(part.id);
  }
  for (const shape of shapes) {
    if (affectsArea(shape)) ids.add(shape.link_id);
  }
  return ids;
}

function legacyDrawing(drawing) {
  return {
    canvas_width: CANVAS_WIDTH,
    canvas_height: CANVAS_HEIGHT,
    units: "inches",
    shapes: drawing.shapes.map((shape) => {
      const legacy = {
        id: shape.link_id,
        link_id: shape.link_id,
        feature_type: shape.feature_type,
        kind: shape.kind,
        points: shape.points
      };
      if (shape.kind === "opening") legacy.opening_type = shape.opening_type;
      if (shape.burner_count !== null) legacy.burner_count = shape.burner_count;
      if (shape.feature_type === "stove" && shape.burner_count === 4) {
        legacy.four_burner_symbol = true;
        legacy.has_four_burner_circles = true;
      }
      return legacy;
    }),
    dimensions: drawing.dimensions.map((dimension) => ({
      role: dimension.role,
      value_inches: dimension.value_inches,
      label: dimension.label,
      part_ids: [dimension.part_id],
      x1: dimension.x1,
      y1: dimension.y1,
      x2: dimension.x2,
      y2: dimension.y2,
      confidence: dimension.confidence
    }))
  };
}

function legacyExplanation(result, calculatedSquareFeet) {
  const parts = [];
  if (result.explanation) parts.push(result.explanation);
  if (result.warnings.length > 0) parts.push(result.warnings.join(" "));
  if (result.can_calculate && calculatedSquareFeet !== null) {
    parts.push(`Verified piece total: ${calculatedSquareFeet.toFixed(2)} sq ft.`);
  } else {
    parts.push("Review the highlighted drawing and enter the unanswered measurements before using the square footage.");
  }
  return cleanText(parts.join(" "), 1200);
}

export function joinMissingQuestionsForAndroid(items) {
  const questions = boundedArray(items, MAX_MISSING, "missing measurements")
    .map((item) => requiredWholeText(
      item?.question,
      MAX_QUESTION_CHARACTERS,
      "missing measurement question"
    ));
  const joined = questions.join(" ");
  if (joined.length > MAX_MISSING_INFORMATION_CHARACTERS) {
    throw new ModelResultError("The missing-information response exceeds the safe limit.");
  }
  return joined;
}

function canonicalOperation(featureType, requestedOperation) {
  if (featureType === "countertop" || featureType === "backsplash") return "add";
  if (featureType === "sink" || featureType === "cooktop") return "ignore";
  if (featureType === "stove") return "subtract";
  if (featureType === "other" && requestedOperation === "ignore") {
    throw new ModelResultError(
      "An unclassified part cannot be ignored safely by the Android calculation."
    );
  }
  if (!OPERATIONS.has(requestedOperation)) {
    throw new ModelResultError("A model part has an invalid operation.");
  }
  return requestedOperation;
}

function canonicalKind(featureType) {
  if (featureType === "countertop") return "countertop";
  if (featureType === "backsplash") return "backsplash";
  return "opening";
}

function affectsArea(item) {
  return item.feature_type !== "sink"
    && item.feature_type !== "cooktop"
    && item.operation !== "ignore";
}

function usesStoveLengthDefault(part) {
  return Boolean(part)
    && part.feature_type === "stove"
    && part.measurement_source === "default"
    && isPositiveFinite(part.length_inches);
}

function readableFeature(feature) {
  if (feature === "backsplash") return "backsplash piece";
  if (feature === "stove") return "stove opening";
  return "countertop piece";
}

function normalizedId(value, fallback) {
  const normalized = cleanText(value, 64)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 48);
  return normalized || fallback;
}

function uniqueGeneratedId(items, desired) {
  const used = new Set(items.map((item) => item.id));
  let candidate = normalizedId(desired, "missing_measurement");
  let suffix = 2;
  while (used.has(candidate)) candidate = `${desired}_${suffix++}`.slice(0, 48);
  return candidate;
}

function formatInches(value) {
  const rounded = Math.round(value * 100) / 100;
  return `${Number.isInteger(rounded) ? rounded : String(rounded)}\"`;
}

function nullableMeasurement(value, field) {
  if (value === null) return null;
  return positiveBoundedNumber(value, 0.01, MAX_MEASUREMENT_INCHES, field);
}

function boundedArray(value, maximum, field) {
  if (!Array.isArray(value)) throw new ModelResultError(`${field} must be an array.`);
  if (value.length > maximum) throw new ModelResultError(`${field} exceeds the safe limit.`);
  return value;
}

function assertObject(value, field) {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    throw new ModelResultError(`${field} is invalid.`);
  }
}

function assertExactFields(value, allowed, field) {
  const allowedSet = new Set(allowed);
  const keys = Object.keys(value);
  if (keys.length !== allowed.length
      || keys.some((key) => !allowedSet.has(key))
      || allowed.some((key) => !(key in value))) {
    throw new ModelResultError(`${field} does not match the required schema.`);
  }
}

function assertBoolean(value, field) {
  if (typeof value !== "boolean") throw new ModelResultError(`${field} must be true or false.`);
}

function enumValue(value, allowed, field) {
  if (typeof value !== "string" || !allowed.has(value)) {
    throw new ModelResultError(`${field} is invalid.`);
  }
  return value;
}

function integerInRange(value, minimum, maximum, field) {
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new ModelResultError(`${field} is invalid.`);
  }
  return value;
}

function finiteNumber(value, field) {
  if (!Number.isFinite(value)) throw new ModelResultError(`${field} is invalid.`);
  return value;
}

function positiveBoundedNumber(value, minimum, maximum, field) {
  if (!Number.isFinite(value) || value < minimum || value > maximum) {
    throw new ModelResultError(`${field} is invalid.`);
  }
  return Math.round(value * 1000) / 1000;
}

function requiredText(value, maximum, field) {
  const text = cleanText(value, maximum);
  if (!text) throw new ModelResultError(`${field} is required.`);
  return text;
}

function requiredWholeText(value, maximum, field) {
  const text = boundedWholeText(value, maximum, field);
  if (!text) throw new ModelResultError(`${field} is required.`);
  return text;
}

function boundedWholeText(value, maximum, field) {
  if (typeof value !== "string") {
    throw new ModelResultError(`${field} is required.`);
  }
  const text = value
    .replace(/[\u0000-\u001f\u007f]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  if (text.length > maximum) {
    throw new ModelResultError(`${field} exceeds the safe limit.`);
  }
  return text;
}

function cleanText(value, maximum) {
  if (typeof value !== "string") return "";
  return value
    .replace(/[\u0000-\u001f\u007f]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, maximum);
}

function clamp(value, minimum, maximum) {
  return Math.min(maximum, Math.max(minimum, value));
}

function isPositiveFinite(value) {
  return Number.isFinite(value) && value > 0;
}

export class ModelResultError extends Error {
  constructor(message) {
    super(message);
    this.name = "ModelResultError";
    this.validationCode = MODEL_VALIDATION_CODE_BY_MESSAGE.get(message)
      || "invalid_model_result";
  }
}
