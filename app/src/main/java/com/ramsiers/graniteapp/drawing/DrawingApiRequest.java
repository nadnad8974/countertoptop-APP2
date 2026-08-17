package com.ramsiers.graniteapp.drawing;

import org.json.JSONObject;

/** Builds the single-image payload supported by the hosted Ramsiers drawing service. */
public final class DrawingApiRequest {
    private DrawingApiRequest() {
    }

    public static JSONObject singleImage(String imageDataUrl) {
        JSONObject request = new JSONObject();
        try {
            request.put("image", imageDataUrl == null ? "" : imageDataUrl);
            request.put("include_verification_drawing", true);
            request.put("drawing_schema_version", 2);
            request.put("preserve_source_layout", true);
            request.put("preserve_source_orientation", true);
            request.put("rectangle_decomposition_required", true);
            request.put("use_dotted_lines_as_partition_guides", true);
            request.put("calculate_each_rectangle_separately", true);
            request.put("four_burner_circles_mean_stove", true);
            request.put("default_unmarked_stove_width_inches", 30);
            request.put("explicit_stove_dimension_overrides_default", true);
            request.put("return_undimensioned_countertop_shapes_for_user_editing", true);
        } catch (Exception ignored) {
        }
        return request;
    }
}
