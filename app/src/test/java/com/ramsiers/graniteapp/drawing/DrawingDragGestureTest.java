package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DrawingDragGestureTest {
    @Test
    public void fingerMustPassTouchSlopBeforeMovingShape() {
        assertFalse(DrawingDragGesture.passedSlop(100, 100, 103, 104, 6));
        assertTrue(DrawingDragGesture.passedSlop(100, 100, 108, 100, 6));
    }

    @Test
    public void canvasSelectionKeepsAtLeastFortyEightDpDiameter() {
        assertEquals(72f, DrawingDragGesture.accessibleRadius(3f, 1f), 0.001f);
        assertEquals(20f, DrawingDragGesture.accessibleRadius(1f, 2f), 0.001f);
    }

    @Test
    public void newRectangleCanBeGrabbedJustOutsideItsVisibleStroke() {
        assertTrue(DrawingDragGesture.contains(100, 100, 200, 180, 88, 140, 12));
        assertFalse(DrawingDragGesture.contains(100, 100, 200, 180, 87, 140, 12));
    }

    @Test
    public void controllerUsesImmutableDownPointAndActualClampedMovement() {
        DrawingDragGesture.Controller controller = new DrawingDragGesture.Controller();
        controller.start(100, 100);
        assertFalse(controller.requested(104, 103, 8).moved);

        DrawingDragGesture.AppliedDelta first = controller.requested(130, 110, 8);
        assertEquals(30f, first.deltaX, 0.001f);
        assertEquals(10f, first.deltaY, 0.001f);
        controller.applied(DrawingDragGesture.AppliedDelta.of(20, 10));

        DrawingDragGesture.AppliedDelta catchUp = controller.requested(135, 110, 8);
        assertEquals(15f, catchUp.deltaX, 0.001f);
        assertEquals(0f, catchUp.deltaY, 0.001f);
        assertTrue(controller.isDragging());

        controller.reset();
        assertFalse(controller.isDragging());
        assertFalse(controller.requested(200, 200, 8).moved);
    }
}
