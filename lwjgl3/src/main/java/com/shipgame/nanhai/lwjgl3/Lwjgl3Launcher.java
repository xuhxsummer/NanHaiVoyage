package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.shipgame.nanhai.NanHaiVoyage;

/** Desktop (LWJGL3) entry: {@code ./gradlew lwjgl3:run} */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("南海航程");
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setWindowedMode(1280, 720);
        config.setResizable(true);
        new Lwjgl3Application(new NanHaiVoyage(), config);
    }
}
