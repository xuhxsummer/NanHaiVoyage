package com.shipgame.nanhai.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serialized REST transport with a durable, account- and server-scoped outbox. */
public class AccountStore {
    public interface Callback<T> { void success(T value); void failure(String message); }
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r,"cloud-save"); t.setDaemon(true); return t;
    });
    private final Preferences prefs = Gdx.app.getPreferences("nanhai-cloud-session");
    private volatile String token = "", user = "";
    private final String base;
    private Pending pending;
    private boolean draining;
    private static final class Pending {
        final String user, token, body; final Callback<Void> callback;
        Pending(String u,String t,String b,Callback<Void> c) {user=u;token=t;body=b;callback=c;}
    }
    public static class Credentials { public String username, password; }
    public AccountStore() {
        base = System.getProperty("nanhai.apiUrl", Gdx.files.internal("cloud.properties").readString().trim()).replaceAll("/+$", "");
    }
    private static Json json() {
        Json j=new Json(); j.setOutputType(JsonWriter.OutputType.json); j.setUsePrototypes(false); return j;
    }
    public void authenticate(String username,String password,boolean register,Callback<SaveData> callback) {
        final String u=username.trim();
        worker.execute(() -> {
            try {
                Credentials credentials=new Credentials();
                credentials.username=u; credentials.password=password;
                String body=json().toJson(credentials);
                if(register) request("POST","/auth/register",body,"");
                String result=request("POST","/auth/login",body,"");
                String nextToken=new JsonReader().parse(result).getString("token");
                // Retry only this authenticated account's outbox before pulling state.
                synchronized(this) {
                    FileHandle f=outbox(u);
                    if(f.exists()) { request("PUT","/me/state",f.readString("UTF-8"),nextToken); f.delete(); }
                }
                String state=request("GET","/me/state",null,nextToken);
                SaveData data=state==null?null:json().fromJson(SaveData.class,state);
                token=nextToken; user=u;
                Gdx.app.postRunnable(() -> {
                    prefs.putString("token",nextToken); prefs.flush(); callback.success(data);
                });
            } catch(Exception e) { fail(callback,e); }
        });
    }
    /** Call on the render thread. Checkpoint is durable before network work starts. */
    public synchronized boolean sync(String u,SaveData state,Callback<Void> callback) {
        if(!u.equals(user) || token.isEmpty()) { callback.failure("登录已失效，请重新登录。进度尚未同步。"); return false; }
        try {
            String body=json().toJson(state);
            FileHandle f=outbox(u), temp=f.sibling(f.name()+".tmp");
            temp.parent().mkdirs();
            try(FileOutputStream stream=new FileOutputStream(temp.file())) {
                stream.write(body.getBytes(StandardCharsets.UTF_8)); stream.getFD().sync();
            }
            if(!temp.file().renameTo(f.file())) throw new IOException("checkpoint rename failed");
            pending=new Pending(u,token,body,callback);
            if(!draining) { draining=true; worker.execute(this::drain); }
            return true;
        } catch(Exception e) {
            callback.failure("本机缓存写入失败，进度未同步，请勿退出。");
            return false;
        }
    }
    private void drain() {
        while(true) {
            Pending p;
            synchronized(this) { p=pending; pending=null; if(p==null) {draining=false;return;} }
            try {
                request("PUT","/me/state",p.body,p.token);
                synchronized(this) { if(pending==null) outbox(p.user).delete(); }
                Gdx.app.postRunnable(() -> p.callback.success(null));
            } catch(Exception e) { fail(p.callback,e); }
        }
    }
    public void logout() {
        String old=token; token=""; user=""; prefs.clear(); prefs.flush();
        worker.execute(() -> { try {request("POST","/auth/logout","",old);} catch(Exception ignored) {} });
    }
    private FileHandle outbox(String u) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest((base+"\n"+u).getBytes(StandardCharsets.UTF_8));
            StringBuilder name=new StringBuilder(); for(byte b:digest) name.append(String.format("%02x",b & 255));
            return Gdx.files.local("cloud-outbox/"+name+".json");
        } catch(Exception e) {throw new IllegalStateException(e);}
    }
    private static <T> void fail(Callback<T> c,Exception e) {
        String message=e instanceof ApiFailure?e.getMessage():"网络连接失败，未同步进度将保留并自动重试。登录时请联网。";
        Gdx.app.postRunnable(() -> c.failure(message));
    }
    private static class ApiFailure extends IOException { ApiFailure(String message){super(message);} }
    private String request(String method,String path,String body,String bearer) throws IOException {
        HttpURLConnection c=(HttpURLConnection)new URL(base+"/api"+path).openConnection();
        try {
            c.setConnectTimeout(8000); c.setReadTimeout(8000); c.setInstanceFollowRedirects(false);
            c.setRequestMethod(method); c.setRequestProperty("Accept","application/json");
            if(!bearer.isEmpty()) c.setRequestProperty("Authorization","Bearer "+bearer);
            if(body!=null) {
                c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json; charset=utf-8");
                try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
            }
            int status=c.getResponseCode();
            if(status==204) return null;
            if(status<200 || status>=300) {
                if(status==401) throw new ApiFailure(path.contains("login")?"用户名或密码错误。":"登录已失效，请重新登录。未同步进度已保留。");
                if(status==409) throw new ApiFailure("账号已存在，请登录。");
                if(status==400) throw new ApiFailure(path.contains("auth")?"用户名需三至三十二位，密码至少八位且不超过七十二字节。":"云端拒绝此进度，缓存已保留，请联系维护者。");
                throw new ApiFailure("云端服务暂不可用，未同步进度已保留，请稍后重试。");
            }
            try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buf=new byte[4096]; int n;
                while((n=in.read(buf))!=-1) {out.write(buf,0,n);if(out.size()>262144) throw new IOException("response too large");}
                return out.toString("UTF-8");
            }
        } finally {c.disconnect();}
    }
}
