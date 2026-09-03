package com.shipgame.nanhai.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/** Device-local username plus per-user JSON save. No network. */
public class AccountStore {

    private static final String PREF = "nanhai-accounts";
    private final Preferences prefs;
    private final Json json;

    public AccountStore() {
        prefs = Gdx.app.getPreferences(PREF);
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
    }

    public boolean userExists(String user) {
        return prefs.contains(key(user));
    }

    public boolean register(String user, String password) {
        if (user == null || user.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        String u = user.trim();
        if (userExists(u)) {
            return false;
        }
        prefs.putString(key(u), password);
        prefs.flush();
        return true;
    }

    public boolean login(String user, String password) {
        if (user == null || password == null) {
            return false;
        }
        String u = user.trim();
        String stored = prefs.getString(key(u), "");
        return !stored.isEmpty() && stored.equals(password);
    }

    public SaveData load(String user) {
        FileHandle f = saveFile(user);
        if (!f.exists()) {
            return null;
        }
        try {
            return json.fromJson(SaveData.class, f);
        } catch (Exception ex) {
            Gdx.app.error("AccountStore", "load failed", ex);
            return null;
        }
    }

    public void save(String user, SaveData data) {
        try {
            FileHandle f = saveFile(user);
            f.parent().mkdirs();
            f.writeString(json.prettyPrint(data), false);
        } catch (Exception ex) {
            Gdx.app.error("AccountStore", "save failed", ex);
        }
    }

    private FileHandle saveFile(String user) {
        return Gdx.files.local("saves/" + sanitize(user) + ".json");
    }

    private static String key(String user) {
        return "u:" + user.trim();
    }

    private static String sanitize(String user) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < user.length(); i++) {
            char c = user.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c > 127) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString().trim();
    }
}
