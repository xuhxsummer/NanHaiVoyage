package com.shipgame.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipgame.nanhai.data.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:cloud;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class CloudApiTest {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    String account(String name) throws Exception {
        String credentials="{\"username\":\""+name+"\",\"password\":\"password123\"}";
        api.perform(post("/api/auth/register").contentType("application/json").content(credentials)).andExpect(status().isCreated());
        api.perform(post("/api/auth/register").contentType("application/json").content(credentials)).andExpect(status().isConflict());
        String result=api.perform(post("/api/auth/login").contentType("application/json").content(credentials)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer "+json.readTree(result).get("token").asText();
    }
    @Test void fullRoundTripIsolationValidationAndLogout() throws Exception {
        String auth=account("captain"), other=account("other");
        api.perform(get("/api/me/state")).andExpect(status().isUnauthorized());
        api.perform(get("/api/me/state").header("Authorization",auth)).andExpect(status().isNoContent());
        SaveData s=GameState.newGame().toSave();
        s.trade[0]=17; s.fish[0]=4; s.costPaid[0]=91; s.beastFound[0]=true;
        s.questClaimBuy=true; s.questBuyCount=7; s.silver=4321;
        s.dockedPort=-1; s.x=123; s.y=456; s.headingDeg=90;
        String body=json.writeValueAsString(s);
        api.perform(put("/api/me/state").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        api.perform(put("/api/me/state").header("Authorization",auth).contentType("application/json").content("{\"userId\":1}")).andExpect(status().isBadRequest());
        api.perform(put("/api/me/state").header("Authorization",auth).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.updatedAt").exists());
        String saved=api.perform(get("/api/me/state").header("Authorization",auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(json.readTree(body),json.readTree(saved));
        GameState restored=GameState.fromSave(json.readValue(saved,SaveData.class));
        assertEquals(-1,restored.dockedPort); assertEquals(123,restored.x); assertEquals(456,restored.y);
        api.perform(get("/api/me/state").header("Authorization",other)).andExpect(status().isNoContent());
        s.trade[0]=-1;
        api.perform(put("/api/me/state").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(s))).andExpect(status().isBadRequest());
        assertEquals(17,db.queryForObject("SELECT qty FROM player_cargo WHERE kind='trade' AND item_index=0",Integer.class));
        s.trade[0]=2; s.silver=19;
        api.perform(put("/api/me/state").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(s))).andExpect(status().isOk());
        api.perform(get("/api/me/state").header("Authorization",auth)).andExpect(jsonPath("$.silver").value(19)).andExpect(jsonPath("$.trade[0]").value(2));
        assertTrue(db.queryForObject("SELECT password_hash FROM users WHERE username='captain'",String.class).startsWith("$2"));
        api.perform(post("/api/auth/logout").header("Authorization",auth)).andExpect(status().isOk());
        api.perform(get("/api/me/state").header("Authorization",auth)).andExpect(status().isUnauthorized());
    }
    @Test void rejectsBadCredentialsAndExpiredSession() throws Exception {
        api.perform(post("/api/auth/register").contentType("application/json").content("{\"username\":\"xx\",\"password\":\"short\"}")).andExpect(status().isBadRequest());
        String auth=account("expiry");
        api.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"expiry\",\"password\":\"wrongpass\"}")).andExpect(status().isUnauthorized());
        db.update("UPDATE sessions SET expires_at=TIMESTAMP '2000-01-01 00:00:00' WHERE user_id=(SELECT id FROM users WHERE username='expiry')");
        api.perform(get("/api/me/state").header("Authorization",auth)).andExpect(status().isUnauthorized());
    }
}
