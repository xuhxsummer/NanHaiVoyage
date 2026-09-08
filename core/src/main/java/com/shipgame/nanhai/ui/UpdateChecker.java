package com.shipgame.nanhai.ui;

/**
 * Hook the Android backend implements to check GitHub Releases for a newer
 * APK. Implementations must be fire-and-forget: never throw, never block the
 * GL thread, and never block login — all failures are silent.
 */
public interface UpdateChecker {

    interface Listener {
        void onUpdateAvailable(String version, Runnable accept, Runnable decline);
        void onDownloadProgress(int percent);
        void onDownloadFinished(boolean success, String message);
    }

    /** Called once after the login UI appears. */
    void checkForUpdate();

    default void setListener(Listener listener) { }
    default void cancelDownload() { }
}
