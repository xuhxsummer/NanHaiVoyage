package com.shipgame.server;

import com.shipgame.nanhai.data.SaveData;
import com.shipgame.nanhai.data.Catalog;
import java.lang.reflect.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** DTO fields map to explicit migrated columns; arrays map to child rows. */
@Service
public class StateStore {
    private final JdbcTemplate db;
    public StateStore(JdbcTemplate db) { this.db = db; }
    static String column(Field f) { return f.getName().replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT); }
    static List<Field> scalars(boolean quests) {
        return Arrays.stream(SaveData.class.getFields()).filter(f -> !f.getType().isArray()
                && f.getName().startsWith("quest") == quests).toList();
    }
    @Transactional
    public SaveData get(long user) throws ReflectiveOperationException {
        // Serialize reads and writes per account, including the first insert.
        db.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, user);
        var rows = db.queryForList("SELECT * FROM player_profiles WHERE user_id=?", user);
        if (rows.isEmpty()) return null;
        SaveData s = new SaveData();
        readScalars(s, rows.get(0), false);
        readScalars(s, db.queryForMap("SELECT * FROM player_quests WHERE user_id=?", user), true);
        for (Field f : SaveData.class.getFields()) {
            if (!f.getType().isArray()) continue;
            String name = f.getName();
            boolean bool = f.getType() == boolean[].class;
            String table = bool ? "player_codex" : name.equals("marketOff") ? "player_market" : "player_cargo";
            var items = name.equals("marketOff")
                    ? db.queryForList("SELECT item_index, price_offset AS item_value FROM " + table + " WHERE user_id=? ORDER BY item_index", user)
                    : db.queryForList("SELECT item_index, " + (bool ? "found" : "qty") + " AS item_value FROM " + table + " WHERE user_id=? AND kind=? ORDER BY item_index", user, name);
            Object arr = Array.newInstance(bool ? boolean.class : int.class, items.size());
            for (int i = 0; i < items.size(); i++) Array.set(arr, i, items.get(i).get("item_value"));
            f.set(s, arr);
        }
        return s;
    }
    private void readScalars(SaveData s, Map<String,Object> row, boolean quests) throws IllegalAccessException {
        for (Field f : scalars(quests)) {
            Object v = row.get(column(f));
            if (f.getType() == float.class) f.setFloat(s, ((Number)v).floatValue());
            else if (f.getType() == int.class) f.setInt(s, ((Number)v).intValue());
            else f.setBoolean(s, (Boolean)v);
        }
    }
    @Transactional
    public String put(long user, SaveData s) throws ReflectiveOperationException {
        validate(s);
        db.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, user);
        java.time.OffsetDateTime updatedAt = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
        for (boolean quests : new boolean[]{false,true}) {
            String table = quests ? "player_quests" : "player_profiles";
            db.update("DELETE FROM " + table + " WHERE user_id=?", user);
            List<Object> values = new ArrayList<>(); values.add(user);
            List<String> cols = new ArrayList<>(); cols.add("user_id");
            for (Field f : scalars(quests)) { cols.add(column(f)); values.add(f.get(s)); }
            cols.add("updated_at"); values.add(updatedAt);
            db.update("INSERT INTO " + table + " (" + String.join(",",cols) + ") VALUES ("
                    + String.join(",", Collections.nCopies(cols.size(),"?")) + ")", values.toArray());
        }
        for (String table : List.of("player_cargo","player_codex","player_market")) db.update("DELETE FROM " + table + " WHERE user_id=?",user);
        for (Field f : SaveData.class.getFields()) {
            if (!f.getType().isArray()) continue;
            Object a = f.get(s);
            for (int i=0;i<Array.getLength(a);i++) {
                if (f.getName().equals("marketOff")) db.update("INSERT INTO player_market VALUES (?,?,?)",user,i,Array.get(a,i));
                else db.update("INSERT INTO " + (f.getType()==boolean[].class ? "player_codex" : "player_cargo") + " VALUES (?,?,?,?)",user,f.getName(),i,Array.get(a,i));
            }
        }
        return db.queryForObject("SELECT updated_at FROM player_profiles WHERE user_id=?",String.class,user);
    }
    static void validate(SaveData s) throws IllegalAccessException {
        if (s == null) throw new ResponseStatusException(BAD_REQUEST,"invalid_state");
        for (Field f : SaveData.class.getFields()) {
            Object v = f.get(s);
            if (f.getType().isArray()) {
                int expected = switch (f.getName()) {
                    case "trade", "costPaid" -> Catalog.GOODS.length;
                    case "beasts", "beastFound" -> Catalog.BEASTS.length;
                    case "herbs", "herbFound" -> Catalog.HERBS.length;
                    case "fish" -> Catalog.FISH.length;
                    case "marketOff" -> Catalog.PORTS.length * Catalog.GOODS.length;
                    default -> throw new IllegalStateException("Unmapped array " + f.getName());
                };
                if (v == null || Array.getLength(v) != expected) throw new ResponseStatusException(BAD_REQUEST,"invalid_array");
                if (v instanceof int[] values) for (int n:values)
                    if ((!f.getName().equals("marketOff") && n<0) || Math.abs((long)n)>1_000_000_000L)
                        throw new ResponseStatusException(BAD_REQUEST,"invalid_quantity");
            } else if (v instanceof Number n) {
                double d=n.doubleValue();
                if (!Double.isFinite(d) || Math.abs(d)>1_000_000_000 || (d<0 && !List.of("x","y","headingDeg","dockedPort").contains(f.getName())))
                    throw new ResponseStatusException(BAD_REQUEST,"invalid_number");
            }
        }
        if (s.ship >= Catalog.SHIPS.length || s.lastPort >= Catalog.PORTS.length
                || s.dockedPort < -1 || s.dockedPort >= Catalog.PORTS.length
                || s.x < 0 || s.x > Catalog.WORLD_W || s.y < 0 || s.y > Catalog.WORLD_H
                || s.gameDay < 1 || s.hullMax <= 0 || s.supplyMax <= 0
                || s.dayMin>=1440 || s.hull>s.hullMax || s.supply>s.supplyMax)
            throw new ResponseStatusException(BAD_REQUEST,"invalid_limits");
    }
}
