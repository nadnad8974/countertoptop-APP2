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
    }
}
