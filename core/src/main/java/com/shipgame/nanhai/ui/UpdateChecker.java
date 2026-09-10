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

        /**
         * 0.28.19: exactly one of the check-phase callbacks fires per
         * {@link #checkForUpdate()}; called on the GL thread.
         * {@code onUpdateAvailable} fires when a newer release exists;
         * {@code onCheckFinished(false)} fires when already latest, and also
         * on any network/parse failure (offline is treated as allow-login).
         */
        default void onCheckFinished(boolean updateAvailable) { }
    }

    /** Re-checks on every call (0.28.19: the old once-per-process latch is gone). */
    void checkForUpdate();

    default void setListener(Listener listener) { }
    default void cancelDownload() { }
}
