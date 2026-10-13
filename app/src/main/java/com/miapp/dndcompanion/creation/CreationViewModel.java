package com.miapp.dndcompanion.creation;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.miapp.dndcompanion.fragua.GameRepository;
import com.miapp.dndcompanion.network.ApiClient;
import java.util.ArrayList;
import java.util.List;
import org.json.*;

public final class CreationViewModel extends ViewModel {
  public CharacterDraft draft = new CharacterDraft();
  public final MutableLiveData<Integer> updates = new MutableLiveData<>(0);
  public final List<JSONObject> species = new ArrayList<>();
  public boolean loading, saving, loaded, uncertainSubmission;
  public String catalogMessage = "", saveError = "", createdId;
  public int errorStatus;

  public void changed() {
    updates.setValue(updates.getValue() + 1);
  }

  public interface Catalog {
    void read(String path, GameRepository.Result callback);
  }

  public void load(Catalog repository) {
    if (loading) return;
    loading = true;
    catalogMessage = "Cargando especies…";
    changed();
    repository.read(
        "/catalog/species",
        new GameRepository.Result() {
          public void ok(Object data, boolean cached, long at) {
            JSONArray rows =
                data instanceof JSONObject ? ((JSONObject) data).optJSONArray("results") : null;
            if (rows == null || rows.length() == 0) {
              error("No hay especies disponibles. Reintentá la carga.", 502);
              return;
            }
            species.clear();
            for (int i = 0; i < rows.length(); i++) {
              JSONObject row = rows.optJSONObject(i);
              if (row != null && !row.optString("name").isEmpty()) species.add(row);
            }
            loaded = !species.isEmpty();
            loading = cached;
            catalogMessage = cached ? "Catálogo guardado · actualizando…" : "";
            changed();
          }

          public void error(String message, int status) {
            loading = false;
            catalogMessage = message;
            if (status == 401) errorStatus = status;
            changed();
          }
        });
  }

  public static String key(JSONObject row) {
    return row.optString("key", row.optString("slug", row.optString("id", row.optString("name"))));
  }

  public interface Submit {
    void post(JSONObject body, ApiClient.Callback callback);
  }

  public void save(Submit submit) {
    if (saving || uncertainSubmission || createdId != null) return;
    final JSONObject body;
    try {
      body = new JSONObject(draft.payload());
    } catch (IllegalStateException e) {
      saveError = e.getMessage();
      changed();
      return;
    }
    saving = true;
    saveError = "";
    changed();
    submit.post(
        body,
        new ApiClient.Callback() {
          public void success(Object data) {
            String id = data instanceof JSONObject ? ((JSONObject) data).optString("id") : "";
            saving = false;
            if (id.isEmpty()) {
              uncertainSubmission = true;
              saveError =
                  "No se pudo confirmar el personaje creado. Volvé a la ficha para comprobarlo.";
            } else createdId = id;
            changed();
          }

          public void failure(String message, int status) {
            saving = false;
            saveError = message;
            errorStatus = status;
            if (status == 0) uncertainSubmission = true;
            changed();
          }
        });
  }
}
