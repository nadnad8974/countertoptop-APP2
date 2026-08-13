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
        assertFalse(request.has("preserve_source_layout"));
        assertFalse(request.has("preserve_source_orientation"));
        assertFalse(request.has("rectangle_decomposition_required"));
        assertFalse(request.has("use_dotted_lines_as_partition_guides"));
        assertFalse(request.has("calculate_each_rectangle_separately"));
    }
}
