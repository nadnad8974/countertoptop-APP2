package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class DrawingFallbackTest {
    @Test
    public void blankEditableCreatesSafeEmptyInchCanvas() throws Exception {
        JSONObject drawing = DrawingFallback.blankEditable();

        assertNotNull(drawing);
        assertEquals(1000, drawing.getInt("canvas_width"));
        assertEquals(700, drawing.getInt("canvas_height"));
        assertEquals("inches", drawing.getString("units"));
        assertTrue(drawing.getBoolean("partial_user_edit"));
        assertEquals(0, drawing.getJSONArray("shapes").length());
        assertEquals(0, drawing.getJSONArray("dimensions").length());
    }
}
