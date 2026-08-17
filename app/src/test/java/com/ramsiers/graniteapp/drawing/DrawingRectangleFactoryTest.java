package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingRectangleFactoryTest {
    @Test
    public void addsLinkedCountertopShapeAndEditableDimensions() throws Exception {
        JSONObject drawing = new JSONObject()
                .put("canvas_width", 1000)
                .put("canvas_height", 700);

        assertTrue(DrawingRectangleFactory.append(
                drawing,
                "user_area_1",
                DrawingRules.FEATURE_COUNTERTOP,
                300,
                250,
                100,
                100,
                72,
                36));

        JSONObject shape = drawing.getJSONArray("shapes").getJSONObject(0);
        assertEquals("countertop", shape.getString("kind"));
        assertEquals("user_area_1", shape.getString("link_id"));
        JSONArray dimensions = drawing.getJSONArray("dimensions");
        assertEquals(2, dimensions.length());
        assertEquals("length", dimensions.getJSONObject(0).getString("role"));
        assertEquals(72, dimensions.getJSONObject(0).getDouble("value_inches"), 0.001);
        assertEquals("width", dimensions.getJSONObject(1).getString("role"));
        assertEquals(36, dimensions.getJSONObject(1).getDouble("value_inches"), 0.001);
    }

    @Test
    public void addsBacksplashAsBacksplashShape() throws Exception {
        JSONObject drawing = new JSONObject();

        assertTrue(DrawingRectangleFactory.append(
                drawing,
                "user_area_2",
                DrawingRules.FEATURE_BACKSPLASH,
                50,
                50,
                500,
                90,
                120,
                4));

        JSONObject shape = drawing.getJSONArray("shapes").getJSONObject(0);
        assertEquals("backsplash", shape.getString("kind"));
        assertEquals("backsplash", shape.getString("feature_type"));
    }

    @Test
    public void rejectsTinyOrUnmeasuredRectangle() {
        JSONObject drawing = new JSONObject();
        assertFalse(DrawingRectangleFactory.append(
                drawing,
                "user_area_3",
                DrawingRules.FEATURE_COUNTERTOP,
                10,
                10,
                12,
                12,
                72,
                36));
        assertFalse(DrawingRectangleFactory.append(
                drawing,
                "user_area_3",
                DrawingRules.FEATURE_COUNTERTOP,
                10,
                10,
                200,
                100,
                0,
                36));
    }
}
