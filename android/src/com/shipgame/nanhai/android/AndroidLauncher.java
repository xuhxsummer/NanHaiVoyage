package com.shipgame.nanhai.android;

import android.content.Intent;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.shipgame.nanhai.NanHaiVoyage;

/** Android entry. APK build needs the Android SDK (see README.md). */
public class AndroidLauncher extends AndroidApplication {

    private AndroidUpdateChecker updateChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useImmersiveMode = true;
        NanHaiVoyage game = new NanHaiVoyage();
        updateChecker = new AndroidUpdateChecker(this);
        game.updateChecker = updateChecker;
        initialize(game, config);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (updateChecker != null) {
            updateChecker.onSettingsResult(requestCode, resultCode);
        }
    }
}
