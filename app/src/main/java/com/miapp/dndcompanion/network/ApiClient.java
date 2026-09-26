package com.miapp.dndcompanion.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.miapp.dndcompanion.BuildConfig;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Supabase Auth + Express REST. A single queue serializes refresh-token rotation. */
public final class ApiClient {
    public interface Callback { void success(Object value); void failure(String message, int status); }
    private interface Work { Object run() throws Exception; }
    private static ApiClient instance;
    private final SessionStore store;
    private final ExecutorService queue = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile JSONObject session;
    private ApiClient(Context context) { store = new SessionStore(context); session = store.load(); }
    public static synchronized ApiClient get(Context context) {
        if (instance == null) instance = new ApiClient(context.getApplicationContext());
        return instance;
    }
    public boolean hasSession() { return session != null; }
    public String userId() { JSONObject s = session; return s == null ? "" : s.optJSONObject("user").optString("id"); }
    private void run(Work work, Callback callback) {
        queue.execute(() -> {
            try { Object result = work.run(); main.post(() -> callback.success(result)); }
            catch (Exception e) { int status = e instanceof ApiError ? ((ApiError)e).status : 0;
                String message = status == 0 ? "No se pudo conectar. Revisá la conexión y configuración." : e.getMessage();
                main.post(() -> callback.failure(message, status)); }
        });
    }
    private JSONObject credentials(String email, String password) throws Exception {
        return new JSONObject().put("email", email.trim()).put("password", password);
    }
    public void login(String email, String password, Callback callback) {
        run(() -> { JSONObject data = (JSONObject) http(authUrl("/token?grant_type=password"), "POST", credentials(email, password), null, true); save(data); return data; }, callback);
    }
    public void recoverPassword(String email, Callback callback) {
        run(() -> {
            String redirect = BuildConfig.PASSWORD_RESET_URL;
            if (redirect.isEmpty()) throw new ApiError(400, "Configurá PASSWORD_RESET_URL con la dirección de recuperación de la web.");
            URL target = new URL(redirect);
            boolean local = "localhost".equals(target.getHost()) || "127.0.0.1".equals(target.getHost()) || "10.0.2.2".equals(target.getHost());
            if (!"https".equals(target.getProtocol()) && !(BuildConfig.DEBUG && local && "http".equals(target.getProtocol()))) {
                throw new ApiError(400, "La página de recuperación debe usar HTTPS.");
            }
            http(authUrl("/recover?redirect_to=" + java.net.URLEncoder.encode(redirect, "UTF-8")),
                    "POST", new JSONObject().put("email", email.trim()), null, true);
            return null;
        }, callback);
    }
    public void register(String email, String password, Callback callback) {
        run(() -> {
            JSONObject data = (JSONObject) http(authUrl("/signup"), "POST", credentials(email, password).put("data", new JSONObject().put("username", email.split("@")[0])), null, true);
            if (data.has("access_token") && !data.isNull("access_token")) save(data);
            return data;
        }, callback);
    }
    private void save(JSONObject data) throws Exception {
        if (!data.has("expires_at")) data.put("expires_at", System.currentTimeMillis()/1000 + data.getLong("expires_in"));
        data.getString("access_token"); data.getString("refresh_token"); data.getJSONObject("user").getString("id");
        store.save(data); session = data;
    }
    private void clear() { session = null; store.clear(); }
    private String token(boolean force) throws Exception {
        if (session == null) throw new ApiError(401, "Iniciá sesión para continuar");
        if (force || session.optLong("expires_at") <= System.currentTimeMillis()/1000 + 60) {
            try { save((JSONObject) http(authUrl("/token?grant_type=refresh_token"), "POST", new JSONObject().put("refresh_token", session.getString("refresh_token")), null, true)); }
            catch (ApiError error) {
                if (error.status == 400 || error.status == 401 || error.status == 403) { clear(); throw new ApiError(401, "La sesión expiró. Volvé a ingresar."); }
                throw error;
            }
        }
        return session.getString("access_token");
    }
    public void request(String method, String path, JSONObject body, Callback callback) {
        run(() -> {
            String access = token(false);
            try { return http(apiUrl(path), method, body, access, false); }
            catch (ApiError error) {
                if (error.status != 401) throw error;
                try { return http(apiUrl(path), method, body, token(true), false); }
                catch (ApiError retry) { if (retry.status == 401) clear(); throw retry; }
            }
        }, callback);
    }
    public void logout(Callback callback) {
        run(() -> {
            try { if (session != null) http(authUrl("/logout?scope=local"), "POST", null, token(false), true); }
            finally { clear(); }
            return null;
        }, callback);
    }
    private String authUrl(String path) throws ApiError {
        if (BuildConfig.SUPABASE_URL.isEmpty() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isEmpty()) throw new ApiError(400, "Configurá Supabase en local.properties");
        return BuildConfig.SUPABASE_URL.replaceAll("/$", "") + "/auth/v1" + path;
    }
    private String apiUrl(String path) throws ApiError {
        if (BuildConfig.API_BASE_URL.isEmpty()) throw new ApiError(400, "Configurá API_BASE_URL en local.properties");
        return BuildConfig.API_BASE_URL.replaceAll("/$", "") + path;
    }
    private Object http(String endpoint, String method, JSONObject body, String token, boolean auth) throws Exception {
        URL url = new URL(endpoint);
        if (!"https".equals(url.getProtocol()) && !(BuildConfig.DEBUG && "http".equals(url.getProtocol()) && ("10.0.2.2".equals(url.getHost()) || "localhost".equals(url.getHost()) || "127.0.0.1".equals(url.getHost())))) throw new ApiError(400, "La conexión debe usar HTTPS");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
            connection.setRequestMethod(method); connection.setRequestProperty("Accept", "application/json");
            if (auth) connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY);
            if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true); connection.setRequestProperty("Content-Type", "application/json");
                try (java.io.OutputStream out = connection.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            }
            int status = connection.getResponseCode();
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (input != null) try (InputStream in = input) { byte[] buffer = new byte[4096]; int n; while ((n = in.read(buffer)) != -1) { output.write(buffer, 0, n); if (output.size() > 2_000_000) throw new ApiError(502, "Respuesta demasiado grande"); } }
            String raw = output.toString(StandardCharsets.UTF_8.name());
            Object value = raw.isEmpty() ? null : new JSONTokener(raw).nextValue();
            if (status < 200 || status >= 300) {
                JSONObject error = value instanceof JSONObject ? (JSONObject)value : new JSONObject();
                throw new ApiError(status, error.optString("msg", error.optString("error_description", error.optString("error", "Error HTTP " + status))));
            }
            return value;
        } finally { connection.disconnect(); }
    }
    private static final class ApiError extends Exception { private static final long serialVersionUID = 1L; final int status; ApiError(int status, String message) { super(message); this.status = status; } }
}
