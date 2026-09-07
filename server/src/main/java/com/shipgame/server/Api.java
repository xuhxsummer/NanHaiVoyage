package com.shipgame.server;

import com.shipgame.nanhai.data.SaveData;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
public class Api {
    private final JdbcTemplate db;
    private final StateStore states;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private final String dummyHash = passwords.encode("invalid-password-placeholder");
    public Api(JdbcTemplate db, StateStore states) { this.db=db; this.states=states; }
    public record Credentials(String username,String password) {}
    @PostMapping("/auth/register")
    @ResponseStatus(CREATED)
    public Map<String,String> register(@RequestBody Credentials c) {
        validate(c);
        try { db.update("INSERT INTO users(username,password_hash) VALUES (?,?)", c.username().trim(), passwords.encode(c.password())); }
        catch (DuplicateKeyException e) { throw new ResponseStatusException(CONFLICT,"username_taken"); }
        return Map.of("status","registered");
    }
    @PostMapping("/auth/login")
    public Map<String,String> login(@RequestBody Credentials c) {
        validate(c);
        var users = db.queryForList("SELECT id,password_hash FROM users WHERE username=?",c.username().trim());
        boolean matches = passwords.matches(c.password(),users.isEmpty()?dummyHash:(String)users.get(0).get("password_hash"));
        if (users.isEmpty() || !matches) throw new ResponseStatusException(UNAUTHORIZED,"invalid_credentials");
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry=Instant.now().plus(Duration.ofDays(30));
        db.update("DELETE FROM sessions WHERE expires_at < CURRENT_TIMESTAMP");
        db.update("INSERT INTO sessions VALUES (?,?,?)", hash(token), users.get(0).get("id"), OffsetDateTime.ofInstant(expiry,ZoneOffset.UTC));
        return Map.of("token",token,"expiresAt",expiry.toString());
    }
    @PostMapping("/auth/logout")
    public void logout(@RequestHeader(value="Authorization",required=false) String auth) {
        user(auth); db.update("DELETE FROM sessions WHERE token_hash=?",hash(auth.substring(7)));
    }
    @GetMapping("/me/state")
    public ResponseEntity<SaveData> get(@RequestHeader(value="Authorization",required=false) String auth) throws ReflectiveOperationException {
        SaveData state=states.get(user(auth));
        return state==null ? ResponseEntity.noContent().build() : ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(state);
    }
    @PutMapping("/me/state")
    public Map<String,String> put(@RequestHeader(value="Authorization",required=false) String auth,@RequestBody SaveData state) throws ReflectiveOperationException {
        return Map.of("updatedAt",states.put(user(auth),state));
    }
    private long user(String auth) {
        if (auth==null || !auth.startsWith("Bearer ") || auth.length()>128) throw new ResponseStatusException(UNAUTHORIZED,"invalid_session");
        var ids=db.queryForList("SELECT user_id FROM sessions WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP",Long.class,hash(auth.substring(7)));
        if(ids.isEmpty()) throw new ResponseStatusException(UNAUTHORIZED,"invalid_session");
        return ids.get(0);
    }
    private static void validate(Credentials c) {
        if(c.username()==null || !c.username().trim().matches("[\\p{L}\\p{N}_-]{3,32}") || c.password()==null
                || c.password().length()<8 || c.password().getBytes(StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(BAD_REQUEST,"invalid_credentials_format");
    }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
