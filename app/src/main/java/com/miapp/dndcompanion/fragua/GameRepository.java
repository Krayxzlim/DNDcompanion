package com.miapp.dndcompanion.fragua;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.room.Room;
import com.miapp.dndcompanion.network.ApiClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;

/** Cache never accepts a local game mutation. Only server-confirmed snapshots are published. */
public final class GameRepository {
  public interface Result {
    void ok(Object data, boolean cached, long savedAt);

    void error(String message, int status);
  }

  private static GameRepository instance;
  private final ApiClient api;
  private final SnapshotDatabase db;
  private final ExecutorService disk = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final SharedPreferences prefs;

  private GameRepository(Context c) {
    api = ApiClient.get(c);
    db = Room.databaseBuilder(c, SnapshotDatabase.class, "fragua-snapshots.db").build();
    prefs = c.getSharedPreferences("fragua_selection", Context.MODE_PRIVATE);
  }

  public static synchronized GameRepository get(Context c) {
    if (instance == null) instance = new GameRepository(c.getApplicationContext());
    return instance;
  }

  public String account() {
    return api.userId();
  }

  public String selected() {
    return prefs.getString(account() + ":selected", "");
  }

  public void select(String id) {
    prefs.edit().putString(account() + ":selected", id).apply();
  }

  public boolean hasSession() {
    return api.hasSession();
  }

  public void read(String path, Result callback) {
    String owner = account();
    disk.execute(
        () -> {
          SnapshotDatabase.Entry e = db.cache().read(owner, path);
          if (e != null)
            try {
              Object data = new JSONTokener(e.json).nextValue();
              main.post(
                  () -> {
                    if (owner.equals(account())) callback.ok(data, true, e.savedAt);
                  });
            } catch (Exception ignored) {
            }
          main.post(
              () -> {
                if (owner.equals(account())) remote("GET", path, null, callback);
              });
        });
  }

  public void remote(String method, String path, JSONObject body, Result callback) {
    String owner = account();
    api.request(
        method,
        path,
        body,
        new ApiClient.Callback() {
          public void success(Object data) {
            if (!owner.equals(account())) return;
            if (method.equals("GET") && data != null)
              disk.execute(
                  () -> {
                    SnapshotDatabase.Entry e = new SnapshotDatabase.Entry();
                    e.account = owner;
                    e.key = path;
                    e.json = data.toString();
                    e.savedAt = System.currentTimeMillis();
                    db.cache().put(e);
                    main.post(
                        () -> {
                          if (owner.equals(account())) callback.ok(data, false, e.savedAt);
                        });
                  });
            else callback.ok(data, false, System.currentTimeMillis());
          }

          public void failure(String message, int status) {
            if (owner.equals(account()) || status == 401) callback.error(message, status);
          }
        });
  }

  public void saveSheet(JSONObject sheet, Runnable done) {
    String owner = account();
    disk.execute(
        () -> {
          SnapshotDatabase.Entry e = new SnapshotDatabase.Entry();
          e.account = owner;
          e.key = "/characters/" + sheet.optString("characterId") + "/sheet";
          e.json = sheet.toString();
          e.savedAt = System.currentTimeMillis();
          db.cache().put(e);
          main.post(
              () -> {
                if (owner.equals(account())) done.run();
              });
        });
  }

  public void pending(String character, JSONObject command) {
    prefs.edit().putString(account() + ":pending:" + character, command.toString()).commit();
  }

  public JSONObject pending(String character) {
    try {
      return new JSONObject(prefs.getString(account() + ":pending:" + character, ""));
    } catch (Exception e) {
      return null;
    }
  }

  public void clearPending(String character) {
    prefs.edit().remove(account() + ":pending:" + character).commit();
  }

  public void logout(Runnable done) {
    String owner = account();
    disk.execute(
        () -> {
          db.cache().clear(owner);
          for (String k : prefs.getAll().keySet())
            if (k.startsWith(owner + ":")) prefs.edit().remove(k).commit();
          main.post(
              () ->
                  api.logout(
                      new ApiClient.Callback() {
                        public void success(Object v) {
                          done.run();
                        }

                        public void failure(String m, int s) {
                          done.run();
                        }
                      }));
        });
  }
}
