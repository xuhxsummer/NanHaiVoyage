package com.shipgame.server;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.shipgame.nanhai.data.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.datasource.url=jdbc:h2:mem:transport;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class ClientTransportTest {
    @LocalServerPort int port;
    @TempDir Path local;
    static volatile int putFailure, getFailure;
    @TestConfiguration static class Failures {
        @Bean Filter simulateOutage() {
            return (request,response,chain) -> {
                if (((HttpServletRequest)request).getMethod().equals("PUT") && putFailure!=0) {
                    ((HttpServletResponse)response).setStatus(putFailure); return;
                }
                if (((HttpServletRequest)request).getMethod().equals("GET") && getFailure!=0) {
                    ((HttpServletResponse)response).setStatus(getFailure); return;
                }
                chain.doFilter(request,response);
            };
        }
    }
    static class Result<T> implements AccountStore.Callback<T> {
        CountDownLatch done=new CountDownLatch(1); T value; String error;
        public void success(T v) {value=v;done.countDown();}
        public void failure(String e) {error=e;done.countDown();}
        Result<T> await() throws Exception {assertTrue(done.await(20,TimeUnit.SECONDS));return this;}
    }
    @BeforeEach void setup() throws Exception {
        Gdx.app=mock(Application.class); Gdx.files=mock(com.badlogic.gdx.Files.class);
        Preferences prefs=mock(Preferences.class,RETURNS_SELF);
        when(Gdx.app.getPreferences(anyString())).thenReturn(prefs);
        doAnswer(call -> {((Runnable)call.getArgument(0)).run();return null;}).when(Gdx.app).postRunnable(any());
        when(Gdx.files.local(anyString())).thenAnswer(call -> new FileHandle(local.resolve((String)call.getArgument(0)).toFile()));
        when(Gdx.files.internal("cloud.properties")).thenReturn(new FileHandle("../assets/cloud.properties"));
        System.setProperty("nanhai.apiUrl","http://127.0.0.1:"+port);
        putFailure=0; getFailure=0;
    }
    @AfterEach void cleanup() { System.clearProperty("nanhai.apiUrl"); putFailure=0; getFailure=0; }
    @Test void actualClientAuthOutboxRecoveryAndOwnership() throws Exception {
        AccountStore client=new AccountStore();
        getFailure=503;
        Result<SaveData> unavailable=new Result<>();client.authenticate("transport","password123",true,unavailable);
        assertNotNull(unavailable.await().error);
        getFailure=0;
        Result<SaveData> login=new Result<>();client.authenticate("transport","password123",false,login);
        assertNull(login.await().error);assertNull(login.value);
        SaveData state=GameState.newGame().toSave();state.silver=6789;
        Result<Void> save=new Result<>(); client.sync("transport",state,save);assertNull(save.await().error);
        putFailure=503; state.silver=9876;
        Result<Void> offline=new Result<>();client.sync("transport",state,offline);assertNotNull(offline.await().error);
        try(var paths=java.nio.file.Files.list(local.resolve("cloud-outbox"))) {assertEquals(1,paths.count());}
        client.logout();
        // A fresh process/account must not consume another account's queued state.
        AccountStore restarted=new AccountStore();
        Result<SaveData> other=new Result<>();restarted.authenticate("separate","password123",true,other);
        assertNull(other.await().error);assertNull(other.value);
        Result<SaveData> blocked=new Result<>();restarted.authenticate("transport","password123",false,blocked);
        assertNotNull(blocked.await().error); // failed state sync is never treated as an empty account
        putFailure=0;
        Result<SaveData> recovered=new Result<>();restarted.authenticate("transport","password123",false,recovered);
        assertNull(recovered.await().error);assertEquals(9876,recovered.value.silver);
        try(var paths=java.nio.file.Files.list(local.resolve("cloud-outbox"))) {assertEquals(0,paths.count());}
        putFailure=401;
        Result<Void> expired=new Result<>();restarted.sync("transport",state,expired);
        assertTrue(expired.await().error.contains("登录已失效"));
        java.nio.file.Files.writeString(local.resolve("blocked"), "not a directory");
        when(Gdx.files.local(anyString())).thenAnswer(call -> new FileHandle(local.resolve("blocked/outbox.json").toFile()));
        Result<Void> diskFailure=new Result<>();
        assertFalse(restarted.sync("transport",state,diskFailure));
        assertTrue(diskFailure.await().error.contains("缓存写入失败"));
    }
}
