package com.ramsiers.graniteapp.drawing;

/** Decides how an original crop source may be released after it is no longer needed. */
public final class DrawingCropSourcePolicy {
    public enum CleanupAction {
        NONE,
        DELETE_APP_TEMPORARY,
        RELEASE_PERSISTED_READ_GRANT
    }

    private static final String TEMPORARY_CAMERA_PREFIX = "countertop-drawing-";

    private DrawingCropSourcePolicy() {
    }

    public static CleanupAction cleanupAction(String sourceUri, String appFileProviderAuthority) {
        if (sourceUri == null || sourceUri.trim().isEmpty()) return CleanupAction.NONE;
        String authority = appFileProviderAuthority == null
                ? ""
                : appFileProviderAuthority.trim();
        String appContentPrefix = "content://" + authority + "/";
        if (!authority.isEmpty() && sourceUri.startsWith(appContentPrefix)) {
            String path = sourceUri.substring(appContentPrefix.length());
            int queryStart = path.indexOf('?');
            if (queryStart >= 0) path = path.substring(0, queryStart);
            int fragmentStart = path.indexOf('#');
            if (fragmentStart >= 0) path = path.substring(0, fragmentStart);
            int lastSlash = path.lastIndexOf('/');
            String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            return fileName.startsWith(TEMPORARY_CAMERA_PREFIX)
                    ? CleanupAction.DELETE_APP_TEMPORARY
                    : CleanupAction.NONE;
        }
        return CleanupAction.RELEASE_PERSISTED_READ_GRANT;
    }
}
