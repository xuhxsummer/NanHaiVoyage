package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.screen.VoyageScreen;
import java.lang.reflect.*;
import java.nio.file.Path;

/** Real input, GL and isolated account saves for HUD-card dialogue and the separate quest log. */
public final class QuestDialogueSmokeLauncher {
    private static int result = 1;
    public static void main(String[] args) throws Exception {
        Path dir = java.nio.file.Files.createTempDirectory("nanhai-dialogue-");
        Path output = Path.of("../Builds/dialogue2815").toAbsolutePath();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 720); config.disableAudio(true); config.setForegroundFPS(0);
        config.setPreferencesConfig(dir.resolve("prefs").toString(), Files.FileType.Absolute);
        new Lwjgl3Application(new NanHaiVoyage() {
            VoyageScreen voyage; Stage stage; int frame; boolean failed;
            @Override public void create() {
                Files original = Gdx.files;
                Gdx.files = (Files) Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class}, (p,m,a) -> {
                    if (m.getName().equals("local")) return new FileHandle(dir.resolve((String)a[0]).toFile());
                    return m.invoke(original,a);
                });
                try {
                    super.create();
                    accounts.register("dialogue-a", "local-password"); currentUser = "dialogue-a";
                    state = GameState.newGame(); state.undockInPlace(); state.x = 2000; state.y = 12000;
                    state.speed = 40; state.pirateSpawnTimer = 10000; state.startAutoSail(0);
                    enter();
                } catch (Throwable t) { fail(t); }
            }
            @Override public void render() {
                if (failed) return;
                try {
                    if (frame == 0) {
                        interactions();
                        Gdx.graphics.setWindowedMode(1600, 720);
                        frame++;
                    } else {
                        voyage.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); voyage.render(0);
                        Actor panel = actor("dialoguePanel");
                        require(panel.getWidth() > 1400 && panel.getHeight() >= 220 && panel.getHeight() <= 280,
                                "wide-screen bottom panel fills width and preserves readable height");
                        capture("wide");
                        tap("dialogueClose"); voyage.render(0);
                        accounts.register("dialogue-b", "other-password"); currentUser = "dialogue-b";
                        state = GameState.newGame(); state.undockInPlace(); state.x = 2000; state.y = 12000;
                        enter(); voyage.render(0);
                        require(isDialogue() && state.questDialogueSeen == 1, "new account gets its own first playback");
                        require(accounts.load("dialogue-a").questDialogueSeen == 7, "other account playback flags unchanged");
                        result = 0;
                        System.out.println("QUEST DIALOGUE PASS: tutorial defer, autoplay once, tap advance, dimmer input isolation, pause/resume with autopilot, manual card replay, locked preview, separate rail log, log/dialogue rewards, next chapter, reload/account isolation, wide resize, GL");
                        Gdx.app.exit();
                    }
                } catch (Throwable t) { fail(t); }
            }
            private void interactions() throws Exception {
                voyage.render(0);
                require(!isDialogue() && state.questDialogueSeen == 0, "tutorial defers autoplay");
                invoke("dismissHowto"); voyage.render(0);
                require(isDialogue() && state.questDialogueSeen == 1, "first active quest auto-opens once");
                require(text("dialogueBody").contains("武则天"), "Wu Zhou dialogue starts at first line");
                require(accounts.load(currentUser).questDialogueSeen == 1, "seen marker saved immediately");
                capture("first-line");
                float x = state.x, y = state.y, minute = state.dayMin, pirateTime = state.pirateSpawnTimer;
                for (int i = 0; i < 8; i++) voyage.render(.1f);
                require(state.x == x && state.y == y && state.dayMin == minute && state.pirateSpawnTimer == pirateTime,
                        "dialogue pauses world, clock and encounters");
                require(state.autoSail, "dialogue preserves autopilot target");
                tapPoint(640, 520); voyage.render(0);
                require(text("dialogueSpeaker").equals("老掌柜"), "dimmer tap advances exactly one line");
                tap("dialogueBody"); voyage.render(0);
                require(text("dialogueSpeaker").equals("你"), "panel tap advances exactly one line");
                tap("加速"); voyage.render(0); // Covered HUD control: should advance dialogue only.
                require(!state.holdAccel && !state.holdDecel && state.steerInput == 0, "covered controls cannot steer or accelerate");
                require(((TextButton) actor("dialogueAction")).getText().toString().equals("前往"), "navigation on final page");
                capture("objective"); tap("dialogueAction"); voyage.render(.1f);
                require(!isDialogue() && state.autoSailIsle == 0 && (state.x != x || state.y != y), "navigation resumes sailing");
                tap("任务卡"); voyage.render(0);
                require(isDialogue() && text("dialogueSpeaker").equals("旁白"), "HUD card manually reopens first line");
                tap("dialogueClose"); voyage.render(0); require(!isDialogue(), "skip does not auto-reopen");
                tap("下一程任务卡"); voyage.render(0);
                require(text("dialogueBody").contains("尚未开启") && state.questDialogueSeen == 1, "locked preview cannot consume next autoplay");
                capture("locked-next"); tap("dialogueClose"); voyage.render(0);

                state.questIslandVisits = 1;
                tap("任务"); voyage.render(0);
                require(!isDialogue() && stage.getRoot().findActor("questStory") != null, "rail still opens full quest log");
                TextButton claim = findButton(stage.getRoot(), "领取"); require(claim != null, "full log retains claim action");
                for (Actor p = claim.getParent(); p != null; p = p.getParent()) if (p instanceof ScrollPane) {
                    ((ScrollPane)p).setScrollPercentY(1); ((ScrollPane)p).updateVisualScroll();
                }
                voyage.render(0); int silver = state.silver;
                tapActor(claim); voyage.render(0);
                require(state.questClaimIslandVisit && state.silver == silver + 30, "log claim pays original reward");
                require(!isDialogue() && state.questDialogueSeen == 1, "next autoplay waits for log dismissal");
                tapActor(findButton(stage.getRoot(), "关闭")); voyage.render(0);
                require(isDialogue() && text("dialogueSpeaker").equals("水手") && state.questDialogueSeen == 3,
                        "claim unlocks next chapter autoplay");
                tap("dialogueClose"); voyage.render(0);
                state = GameState.fromSave(accounts.load(currentUser)); state.undockInPlace(); enter(); voyage.render(0);
                require(!isDialogue() && state.questDialogueSeen == 3, "saved quest does not auto-replay after reload");
                state.questRefillCount = 1;
                tap("任务卡"); voyage.render(0);
                for (int i = 0; i < 3; i++) { tap("dialogueBody"); voyage.render(0); }
                require(((TextButton)actor("dialogueAction")).getText().toString().equals("领奖"), "completed dialogue offers reward");
                TextButton oldAction = (TextButton)actor("dialogueAction");
                silver = state.silver; tapActor(oldAction); voyage.render(0);
                require(state.questClaimRefill && state.silver == silver + 20 && isDialogue() && state.questDialogueSeen == 7,
                        "dialogue reward advances to next chapter once");
                // A queued callback from a removed page must neither pay twice nor close the next chapter.
                for (EventListener listener : oldAction.getListeners()) if (listener instanceof com.badlogic.gdx.scenes.scene2d.utils.ClickListener)
                    ((com.badlogic.gdx.scenes.scene2d.utils.ClickListener)listener).clicked(new InputEvent(), 1, 1);
                require(state.silver == silver + 20 && isDialogue(), "stale reward callback is harmless");
                capture("next-chapter");
            }
            private void enter() throws Exception {
                voyage = new VoyageScreen(this); setScreen(voyage);
                Field field = VoyageScreen.class.getDeclaredField("stage"); field.setAccessible(true); stage = (Stage)field.get(voyage);
            }
            private void invoke(String name) throws Exception {
                Method m = VoyageScreen.class.getDeclaredMethod(name); m.setAccessible(true); m.invoke(voyage);
            }
            private boolean isDialogue() { return stage.getRoot().findActor("questDialogue") != null; }
            private Actor actor(String name) {
                Actor a = stage.getRoot().findActor(name); require(a != null, "actor exists: " + name); return a;
            }
            private String text(String name) { return ((Label)actor(name)).getText().toString(); }
            private void tap(String name) { tapActor(actor(name)); }
            private void tapActor(Actor a) {
                require(a != null, "tap target exists");
                Vector2 v = a.localToStageCoordinates(new Vector2(a.getWidth()/2, a.getHeight()/2));
                tapPoint(v.x, v.y);
            }
            private void tapPoint(float x, float y) {
                Vector2 v = stage.stageToScreenCoordinates(new Vector2(x,y));
                InputProcessor input = Gdx.input.getInputProcessor();
                input.touchDown(Math.round(v.x),Math.round(v.y),0,0);
                input.touchUp(Math.round(v.x),Math.round(v.y),0,0);
            }
            private void capture(String name) {
                require(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "GL at " + name);
                Pixmap p = Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
                PixmapIO.writePNG(new FileHandle(output.resolve(name + ".png").toFile()),p,-1,true); p.dispose();
            }
            private void fail(Throwable t) { t.printStackTrace(); failed = true; result = 1; Gdx.app.exit(); }
        },config);
        System.exit(result);
    }
    private static TextButton findButton(Group group, String text) {
        for (Actor a : group.getChildren()) {
            if (a instanceof TextButton && ((TextButton)a).getText().toString().equals(text)) return (TextButton)a;
            if (a instanceof Group) { TextButton found = findButton((Group)a,text); if (found != null) return found; }
        }
        return null;
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
