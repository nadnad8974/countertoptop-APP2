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
        } catch (Exception ignored) {
        }
        return request;
    }
}
