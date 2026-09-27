package com.miapp.dndcompanion.fragua;

import androidx.lifecycle.*;
import java.util.UUID;
import org.json.*;

public final class GameViewModel extends ViewModel {
  public static class State {
    public JSONObject sheet;
    public JSONArray characters = new JSONArray();
    public boolean online = false, busy = false;
    public String message = "Cargando tu aventura…";
    public long syncedAt;
  }

  private final MutableLiveData<State> live = new MutableLiveData<>(new State());
  private final GameRepository repo;
  private String selected = "";
  public String screen = "home";
  public JSONObject noteDraft;

  public GameViewModel(GameRepository repo) {
    this.repo = repo;
    selected = repo.selected();
  }

  public LiveData<State> state() {
    return live;
  }

  public State current() {
    return live.getValue();
  }

  private void emit() {
    live.setValue(current());
  }

  public void message(String text) {
    current().message = text;
    emit();
  }

  public void load() {
    repo.read(
        "/characters",
        new GameRepository.Result() {
          public void ok(Object d, boolean cached, long at) {
            current().characters = (JSONArray) d;
            if (selected.isEmpty() && current().characters.length() > 0)
              select(current().characters.optJSONObject(0).optString("id"));
            else if (!selected.isEmpty()) loadSheet();
            current().message = cached ? "Copia guardada · solo consulta" : "";
            emit();
          }

          public void error(String m, int status) {
            current().online = false;
            message(m);
          }
        });
  }

  public void select(String id) {
    selected = id;
    repo.select(id);
    current().sheet = null;
    current().online = false;
    emit();
    loadSheet();
  }

  public String selected() {
    return selected;
  }

  public void loadSheet() {
    if (selected.isEmpty()) return;
    String id = selected;
    repo.read(
        "/characters/" + id + "/sheet",
        new GameRepository.Result() {
          public void ok(Object d, boolean cached, long at) {
            if (!id.equals(selected)) return;
            JSONObject incoming = (JSONObject) d;
            JSONObject existing = current().sheet;
            if (existing == null
                || incoming.optInt("stateVersion") >= existing.optInt("stateVersion"))
              current().sheet = incoming;
            current().online = !cached;
            current().syncedAt = at;
            current().message =
                cached
                    ? "Sin conexión confirmada · solo consulta"
                    : repo.pending(id) == null
                        ? ""
                        : "Hay una operación por comprobar. Abrí el historial.";
            emit();
          }

          public void error(String m, int status) {
            if (!id.equals(selected)) return;
            current().online = false;
            if (status == 403 || status == 404) current().sheet = null;
            message(m + " · solo consulta");
          }
        });
  }

  public void command(String type, JSONObject data) {
    if (!current().online || current().sheet == null || current().busy) {
      message("Necesitás conexión y una ficha actualizada.");
      return;
    }
    if (repo.pending(selected) != null) {
      message("Primero comprobá la operación pendiente desde el historial.");
      return;
    }
    try {
      JSONObject e =
          new JSONObject()
              .put("commandId", UUID.randomUUID().toString())
              .put("expectedVersion", current().sheet.getInt("stateVersion"))
              .put("type", type)
              .put("data", data);
      repo.pending(selected, e);
      send(e);
    } catch (Exception e) {
      message("No se pudo preparar la operación.");
    }
  }

  private void send(JSONObject e) {
    String id = selected;
    current().busy = true;
    message("Guardando…");
    repo.remote(
        "POST",
        "/characters/" + id + "/commands",
        e,
        new GameRepository.Result() {
          public void ok(Object value, boolean cached, long at) {
            JSONObject output = (JSONObject) value;
            repo.clearPending(id);
            JSONObject next = output.optJSONObject("sheet");
            repo.saveSheet(
                next,
                () -> {
                  if (id.equals(selected)) {
                    current().busy = false;
                    current().sheet = next;
                    JSONObject result = output.optJSONObject("result");
                    message(
                        result != null && result.length() > 0
                            ? "Resultado: " + result.toString()
                            : "Guardado");
                  }
                });
          }

          public void error(String m, int status) {
            if (status >= 400 && status < 500) repo.clearPending(id);
            if (id.equals(selected)) {
              current().busy = false;
              if (status == 0 || status >= 500) current().online = false;
              message(m);
              if (status == 409) loadSheet();
            }
          }
        });
  }

  public void reconcile() {
    JSONObject pending = repo.pending(selected);
    if (pending == null) {
      loadSheet();
      return;
    }
    String id = selected;
    repo.remote(
        "GET",
        "/characters/" + id + "/commands/" + pending.optString("commandId"),
        null,
        new GameRepository.Result() {
          public void ok(Object v, boolean c, long t) {
            repo.clearPending(id);
            loadSheet();
            message("Operación confirmada; no se repitió.");
          }

          public void error(String m, int status) {
            if (status == 404) {
              send(pending);
            } else message(m);
          }
        });
  }

  public static ViewModelProvider.Factory factory(GameRepository repo) {
    return new ViewModelProvider.Factory() {
      @Override
      public <T extends ViewModel> T create(Class<T> cls) {
        return cls.cast(new GameViewModel(repo));
      }
    };
  }
}
