package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class DrawingApiRequestTest {
    @Test
    public void payloadUsesVerifiedSingleImageContract() throws Exception {
        JSONObject request = DrawingApiRequest.singleImage("data:image/jpeg;base64,abc");

        assertEquals("data:image/jpeg;base64,abc", request.getString("image"));
        assertFalse(request.has("images"));
        assertTrue(request.getBoolean("include_verification_drawing"));
        assertEquals(2, request.getInt("drawing_schema_version"));
        assertTrue(request.getBoolean("preserve_source_layout"));
        assertTrue(request.getBoolean("preserve_source_orientation"));
        assertTrue(request.getBoolean("rectangle_decomposition_required"));
        assertTrue(request.getBoolean("use_dotted_lines_as_partition_guides"));
        assertTrue(request.getBoolean("calculate_each_rectangle_separately"));
        assertTrue(request.getBoolean("four_burner_circles_mean_stove"));
        assertEquals(30, request.getInt("default_unmarked_stove_width_inches"));
        assertTrue(request.getBoolean("explicit_stove_dimension_overrides_default"));
        assertTrue(request.getBoolean("return_undimensioned_countertop_shapes_for_user_editing"));
    }
}
