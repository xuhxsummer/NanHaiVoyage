package com.shipgame.nanhai.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

/**
 * 0.28.18 游戏音频：场景 BGM、海浪环境音与一次性音效。
 *
 * 全部防御式实现：任何缺文件、解码失败或 disableAudio(true)（冒烟回归）环境
 * 都不得抛异常影响游戏逻辑。加载失败时对应声道静默关闭。
 *
 * 场景 BGM（任一时刻最多一首，切换即停旧开新）：
 *   LOGIN  登录/标题      bgm_login
 *   SAIL   海上航行        bgm_sail
 *   PORT   港口/岛屿经营    bgm_port
 *   BATTLE 海盗战斗        bgm_battle
 * 环境音：SAIL 场景下海浪低音量循环；靠泊/锚泊/菜单/登录时更安静或静音。
 * 音效：cannon（开炮）、coin（银两）、ui（主要按钮/面板开关）。
 */
public final class VoyageAudio implements Disposable {
    public enum Scene { LOGIN, SAIL, PORT, BATTLE }

    private static final String PREF_KEY_MUSIC = "mute_music";
    private static final String PREF_KEY_SFX = "mute_sfx";

    private static VoyageAudio instance;

    /** App-wide accessor; null until first initialize(). All callers must null-check. */
    public static VoyageAudio get() {
        return instance;
    }

    public static VoyageAudio initialize() {
        if (instance == null) {
            instance = new VoyageAudio();
        }
        return instance;
    }

    private final Preferences prefs;
    private final Music loginBgm, sailBgm, portBgm, battleBgm, waves;
    private final Sound cannon, coin, ui;
    private Scene scene = Scene.LOGIN;
    private float wavesVolume;

    private VoyageAudio() {
        prefs = Gdx.app.getPreferences("nanhai-settings");
        loginBgm = music("audio/bgm_login.mp3");
        sailBgm = music("audio/bgm_sail.mp3");
        portBgm = music("audio/bgm_port.mp3");
        battleBgm = music("audio/bgm_battle.mp3");
        waves = music("audio/sfx_waves.mp3");
        cannon = sound("audio/sfx_cannon.mp3");
        coin = sound("audio/sfx_coin.mp3");
        ui = sound("audio/sfx_ui.mp3");
        applyScene(true);
    }

    private Music music(String path) {
        try {
            if (!Gdx.files.internal(path).exists()) return null;
            Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
            return m;
        } catch (Throwable t) {
            Gdx.app.error("VoyageAudio", "music unavailable: " + path, t);
            return null;
        }
    }

    private Sound sound(String path) {
        try {
            if (!Gdx.files.internal(path).exists()) return null;
            return Gdx.audio.newSound(Gdx.files.internal(path));
        } catch (Throwable t) {
            Gdx.app.error("VoyageAudio", "sound unavailable: " + path, t);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 场景 BGM
    // ------------------------------------------------------------------

    /** Switch the background track when the scene changes (same scene = no-op). */
    public void setScene(Scene next) {
        if (next == null) next = Scene.LOGIN;
        if (next == scene) return;
        scene = next;
        applyScene(false);
    }

    public Scene scene() {
        return scene;
    }

    private void applyScene(boolean initial) {
        Music bgm;
        float volume;
        float wavesTarget;
        switch (scene) {
            case PORT:
                bgm = portBgm; volume = 0.85f; wavesTarget = 0.10f; break;
            case BATTLE:
                bgm = battleBgm; volume = 0.8f; wavesTarget = 0.12f; break;
            case SAIL:
                bgm = sailBgm; volume = 0.7f; wavesTarget = 0.16f; break;
            case LOGIN:
            default:
                bgm = loginBgm; volume = 0.75f; wavesTarget = 0f; break;
        }
        playOnly(bgm, volume, initial);
        // 海浪：航行时低音量；靠泊/锚泊/菜单/登录更安静（PORT/LOGIN）。
        // 音量渐变到目标，避免切换瞬间突兀。
        wavesVolume = wavesTarget;
        if (waves != null) {
            try {
                waves.setLooping(true);
                waves.setVolume(musicMuted() ? 0f : wavesTarget);
                if (wavesTarget > 0f) {
                    if (!waves.isPlaying()) waves.play();
                } else if (waves.isPlaying()) {
                    waves.pause();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** Stop everything else, then loop the requested BGM. Never stacks two tracks. */
    private void playOnly(Music bgm, float volume, boolean initial) {
        Music[] all = {loginBgm, sailBgm, portBgm, battleBgm};
        for (Music m : all) {
            if (m == null || m == bgm) continue;
            try {
                if (m.isPlaying()) m.stop();
            } catch (Throwable ignored) {
            }
        }
        if (bgm == null) return;
        try {
            bgm.setLooping(true);
            bgm.setVolume(musicMuted() ? 0f : volume);
            if (initial) {
                bgm.play();
            } else if (!bgm.isPlaying()) {
                bgm.play();
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------
    // 音效
    // ------------------------------------------------------------------

    /** One-shot cannon SFX; never throws even if audio is disabled. */
    public void playCannon() {
        play(cannon, 0.9f);
    }

    /** Silver gain/spend/reward SFX. */
    public void playCoin() {
        play(coin, 0.8f);
    }

    /** Major UI taps (panel open/close, main buttons); do not spam on hover. */
    public void playUi() {
        play(ui, 0.65f);
    }

    private void play(Sound s, float volume) {
        if (s == null || sfxMuted()) return;
        try {
            s.play(volume);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------
    // 静音偏好（nanhai-settings，与登录页设置同一份 prefs）
    // ------------------------------------------------------------------

    public boolean musicMuted() {
        try {
            return prefs.getBoolean(PREF_KEY_MUSIC, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean sfxMuted() {
        try {
            return prefs.getBoolean(PREF_KEY_SFX, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public void setMusicMuted(boolean muted) {
        prefs.putBoolean(PREF_KEY_MUSIC, muted).flush();
        applyScene(false);
    }

    public void setSfxMuted(boolean muted) {
        prefs.putBoolean(PREF_KEY_SFX, muted).flush();
    }

    @Override
    public void dispose() {
        Music[] allMusic = {loginBgm, sailBgm, portBgm, battleBgm, waves};
        for (Music m : allMusic) {
            if (m == null) continue;
            try { m.dispose(); } catch (Throwable ignored) { }
        }
        Sound[] allSounds = {cannon, coin, ui};
        for (Sound s : allSounds) {
            if (s == null) continue;
            try { s.dispose(); } catch (Throwable ignored) { }
        }
        if (instance == this) {
            instance = null;
        }
    }
}
