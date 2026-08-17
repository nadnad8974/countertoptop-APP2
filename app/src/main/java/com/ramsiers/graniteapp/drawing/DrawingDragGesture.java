package com.ramsiers.graniteapp.drawing;

/** Small, platform-independent calculations used by one-finger redraw dragging. */
public final class DrawingDragGesture {
    private DrawingDragGesture() {
    }

    public static boolean passedSlop(
            float startX,
            float startY,
            float currentX,
            float currentY,
            float slop) {
        float deltaX = currentX - startX;
        float deltaY = currentY - startY;
        float minimum = Math.max(0, slop);
        return deltaX * deltaX + deltaY * deltaY >= minimum * minimum;
    }

    public static float accessibleRadius(float density, float drawingScale) {
        float safeDensity = density > 0 ? density : 1f;
        float safeScale = drawingScale > 0 ? drawingScale : 1f;
        return Math.min(80f, Math.max(20f, 24f * safeDensity / safeScale));
    }

    public static boolean contains(
            float left,
            float top,
            float right,
            float bottom,
            float x,
            float y,
            float tolerance) {
        float padding = Math.max(0, tolerance);
        return x >= Math.min(left, right) - padding
                && x <= Math.max(left, right) + padding
                && y >= Math.min(top, bottom) - padding
                && y <= Math.max(top, bottom) + padding;
    }

    /**
     * Converts a finger's total travel from ACTION_DOWN into safe incremental requests. Applied
     * movement is reported back separately because drawing edges can clamp either axis.
     */
    public static final class Controller {
        private float startX;
        private float startY;
        private float appliedX;
        private float appliedY;
        private boolean active;
        private boolean dragging;

        public void start(float x, float y) {
            startX = x;
            startY = y;
            appliedX = 0;
            appliedY = 0;
            active = true;
            dragging = false;
        }

        public AppliedDelta requested(float currentX, float currentY, float slop) {
            if (!active || (!dragging && !passedSlop(
                    startX,
                    startY,
                    currentX,
                    currentY,
                    slop))) {
                return AppliedDelta.none();
            }
            dragging = true;
            return AppliedDelta.of(
                    currentX - startX - appliedX,
                    currentY - startY - appliedY);
        }

        public void applied(AppliedDelta actual) {
            if (actual == null || !actual.moved) return;
            appliedX += actual.deltaX;
            appliedY += actual.deltaY;
        }

        public boolean isDragging() {
            return dragging;
        }

        public void reset() {
            active = false;
            dragging = false;
            appliedX = 0;
            appliedY = 0;
        }
    }

    public static final class AppliedDelta {
        public final boolean moved;
        public final float deltaX;
        public final float deltaY;

        private AppliedDelta(boolean moved, float deltaX, float deltaY) {
            this.moved = moved;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

        public static AppliedDelta of(float deltaX, float deltaY) {
            boolean moved = Math.abs(deltaX) >= 0.0001f || Math.abs(deltaY) >= 0.0001f;
            return new AppliedDelta(moved, moved ? deltaX : 0, moved ? deltaY : 0);
        }

        public static AppliedDelta none() {
            return new AppliedDelta(false, 0, 0);
        }
    }
}
