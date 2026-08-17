package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DrawingCropSourcePolicyTest {
    private static final String AUTHORITY = "com.ramsiers.graniteapp.fileprovider";

    @Test
    public void appOwnedTemporaryCameraOriginalIsDeleted() {
        assertEquals(
                DrawingCropSourcePolicy.CleanupAction.DELETE_APP_TEMPORARY,
                DrawingCropSourcePolicy.cleanupAction(
                        "content://" + AUTHORITY
                                + "/saved_drawing_photos/countertop-drawing-123.jpg",
                        AUTHORITY));
    }

    @Test
    public void keptCroppedCopyIsNeverClassifiedAsTemporaryOriginal() {
        assertEquals(
                DrawingCropSourcePolicy.CleanupAction.NONE,
                DrawingCropSourcePolicy.cleanupAction(
                        "content://" + AUTHORITY
                                + "/saved_drawing_photos/cropped-drawing-123.jpg",
                        AUTHORITY));
    }

    @Test
    public void galleryContentIsNeverDeleted() {
        assertEquals(
                DrawingCropSourcePolicy.CleanupAction.RELEASE_PERSISTED_READ_GRANT,
                DrawingCropSourcePolicy.cleanupAction(
                        "content://com.android.providers.media.documents/document/image%3A42",
                        AUTHORITY));
    }

    @Test
    public void missingSourceNeedsNoCleanup() {
        assertEquals(
                DrawingCropSourcePolicy.CleanupAction.NONE,
                DrawingCropSourcePolicy.cleanupAction(null, AUTHORITY));
    }
}
