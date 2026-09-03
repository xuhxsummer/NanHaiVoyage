package com.shipgame.nanhai.ui;

/**
 * Hook the Android backend implements to check GitHub Releases for a newer
 * APK. Implementations must be fire-and-forget: never throw, never block the
 * GL thread, and never block login — all failures are silent.
 */
public interface UpdateChecker {

    /** Called once after the login UI appears. */
    void checkForUpdate();
}
