package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.*;
import com.shipgame.nanhai.screen.VoyageScreen;
import java.lang.reflect.*;
import java.nio.file.Path;

/** Real captain-menu confirmation + local save round-trip in an isolated account directory. */
public final class MaxAccountSmokeLauncher {
    private static int result=1;
    public static void main(String[] args) throws Exception {
        Path dir=java.nio.file.Files.createTempDirectory("nanhai-max-account-");
        Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280,720); config.disableAudio(true);
        config.setPreferencesConfig(dir.resolve("prefs").toString(),Files.FileType.Absolute);
        new Lwjgl3Application(new NanHaiVoyage() {
            VoyageScreen voyage; Stage stage; int frames;
            @Override public void create() {
                Files original=Gdx.files;
                Gdx.files=(Files)Proxy.newProxyInstance(Files.class.getClassLoader(),new Class[]{Files.class},(proxy,m,a)-> {
                    if(m.getName().equals("local")) return new FileHandle(dir.resolve((String)a[0]).toFile());
                    return m.invoke(original,a);
                });
                super.create();
                try {
                    accounts.register("max-test","local-password");
                    accounts.register("other-test","other-password");
                    GameState other=GameState.newGame(); other.silver=123;
                    accounts.save("other-test",other.toSave());
                    currentUser="max-test"; state=GameState.newGame();
                    state.trade[0]=state.cargoCap; state.beasts[0]=7; state.herbs[0]=4;
                    state.hull=1; state.supply=1; state.silver=10;
                    accounts.save(currentUser,state.toSave());
                    voyage=new VoyageScreen(this); setScreen(voyage);
                    Field f=VoyageScreen.class.getDeclaredField("stage"); f.setAccessible(true); stage=(Stage)f.get(voyage);
                    f=VoyageScreen.class.getDeclaredField("overlay"); f.setAccessible(true);
                    for(Object value:f.getType().getEnumConstants()) if(value.toString().equals("AVATAR")) f.set(voyage,value);
                    Method rebuild=VoyageScreen.class.getDeclaredMethod("rebuildMenu"); rebuild.setAccessible(true); rebuild.invoke(voyage);
                } catch(Throwable t) { fail(t); }
            }
            @Override public void render() {
                try {
                    voyage.render(1f/60); frames++;
                    if(frames==2) {
                        open(); require(state.silver==10,"opening confirmation mutated state");
                        answer(false); require(state.silver==10,"cancel mutated state");
                        require(accounts.load(currentUser).silver==10,"cancel overwrote save");
                    }
                    if(frames==35) {
                        require(stage.getRoot().findActor("confirmFillAccount")==null,"cancel did not dismiss dialog");
                        open(); answer(true);
                    }
                    if(frames==70) {
                        check(state);
                        GameState loaded=GameState.fromSave(accounts.load(currentUser)); check(loaded);
                        require(loaded.crewMax()==state.crewMax(),"crew capacity inflated on reload");
                        require(loaded.trade[0]==Catalog.START_CARGO_CAP && loaded.beasts[0]==7 && loaded.herbs[0]==4,"existing cargo changed");
                        require(accounts.login(currentUser,"local-password"),"credentials changed");
                        require(accounts.login("other-test","other-password") && accounts.load("other-test").silver==123,"other account changed");
                        int cap=loaded.cargoCap,crew=loaded.crewMax(); loaded.fillAccount();
                        require(loaded.cargoCap==cap && loaded.crewMax()==crew,"repeat application inflates capacity");
                        System.out.println("MAX ACCOUNT PASS: one confirmation, cancel, fill, local auto-save, reload, cargo, crew, credentials, account isolation, idempotence");
                        result=0; Gdx.app.exit();
                    }
                } catch(Throwable t) { fail(t); }
            }
            private void open() {
                TextButton debug=stage.getRoot().findActor("调试");
                for(EventListener listener:debug.getListeners()) if(listener instanceof ClickListener)
                    ((ClickListener)listener).clicked(new InputEvent(),1,1);
                TextButton button=stage.getRoot().findActor("满级账号"); require(button!=null,"captain button missing");
                for(EventListener listener:button.getListeners()) if(listener instanceof ClickListener)
                    ((ClickListener)listener).clicked(new InputEvent(),1,1);
                require(stage.getRoot().findActor("confirmFillAccount")!=null,"confirmation missing");
            }
            private void answer(boolean yes) {
                Table confirmation=stage.getRoot().findActor("confirmFillAccount");
                TextButton button=confirmation.findActor(yes?"继续":"取消");
                if(button!=null) {
                    for(EventListener listener:button.getListeners()) if(listener instanceof ClickListener)
                        ((ClickListener)listener).clicked(new InputEvent(),1,1);
                    return;
                }
                throw new AssertionError("confirmation answer missing");
            }
            private void fail(Throwable t) { t.printStackTrace(); result=1; Gdx.app.exit(); }
        },config);
        System.exit(result);
    }
    private static void check(GameState g) {
        for(int i=0;i<Catalog.SHIPS.length;i++) require(g.ownsShip(i),"ship missing");
        for(int i=0;i<g.beasts.length;i++) require(g.beastFound[i] && g.beasts[i]>=1,"beast missing");
        for(int i=0;i<g.herbs.length;i++) require(g.herbFound[i] && g.herbs[i]>=1,"herb missing");
        require(g.cargoUsed()<=g.cargoCap,"cargo overflow on starter ship");
        require(g.silver>=99999 && g.hull==g.hullMax && g.supply==g.supplyMax && g.crew==g.crewMax(),"resources not full");
        require(g.questBeastsFound>=Catalog.BEASTS.length && g.questSilverPeak>=g.silver,"quest counters");
    }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
