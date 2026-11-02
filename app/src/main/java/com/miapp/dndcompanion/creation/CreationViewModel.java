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
  public boolean creationLoading, creationLoaded;
  public JSONObject creation;
  public String creationMessage = "";

  public JSONObject selectedClass() {
    if (creation == null
        || draft.classIndex < 0
        || draft.classIndex >= CharacterDraft.CLASSES.length) return null;
    JSONArray rows = creation.optJSONArray("classes");
    if (rows != null)
      for (int i = 0; i < rows.length(); i++) {
        JSONObject row = rows.optJSONObject(i);
        if (row != null && CharacterDraft.CLASSES[draft.classIndex].equals(row.optString("name")))
          return row;
      }
    return null;
  }

  public int[] recommendedOrder() {
    JSONObject row = selectedClass();
    JSONArray order = row == null ? null : row.optJSONArray("recommendedOrder");
    if (order == null || order.length() != 6) return null;
    int[] result = new int[6];
    boolean[] seen = new boolean[6];
    for (int i = 0; i < 6; i++) {
      int index = abilityIndex(order.optString(i));
      if (index < 0 || seen[index]) return null;
      seen[index] = true;
      result[i] = index;
    }
    return result;
  }

  public static int abilityIndex(String key) {
    for (int i = 0; i < CharacterDraft.KEYS.length; i++)
      if (CharacterDraft.KEYS[i].equals(key)) return i;
    return -1;
  }

  public void loadCreation(Catalog repository) {
    if (creationLoading) return;
    creationLoading = true;
    creationMessage = "Cargando información de clases…";
    changed();
    repository.read(
        "/catalog/creation",
        new GameRepository.Result() {
          public void ok(Object data, boolean cached, long at) {
            JSONObject value = data instanceof JSONObject ? (JSONObject) data : null;
            if (value == null
                || !"srd-2024".equals(value.optString("ruleset"))
                || value.optInt("version") != 1
                || value.optJSONArray("classes") == null) {
              error("Información de clases no disponible.", 502);
              return;
            }
            creation = value;
            creationLoaded = true;
            creationLoading = cached;
            creationMessage = cached ? "Información guardada · actualizando…" : "";
            changed();
          }

          public void error(String message, int status) {
            creationLoading = false;
            creationMessage =
                creation == null
                    ? "Información de clases no disponible. Podés continuar o reintentar."
                    : "Mostrando información guardada. No se pudo actualizar.";
            if (status == 401) errorStatus = status;
            changed();
          }
        });
  }

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

  public JSONObject startingStats;
  public boolean previewLoading;
  public String previewError = "";
  private String previewKey = "", attemptedPreviewKey = "";
  private int previewRevision;

  private String currentPreviewKey() {
    if (!draft.defense.supported) return "";
    try {
      return draft.payload().get("creation").toString();
    } catch (IllegalStateException e) {
      return "";
    }
  }

  public void invalidatePreviewIfChanged() {
    String current = currentPreviewKey();
    if (!attemptedPreviewKey.equals(current)) {
      previewRevision++;
      attemptedPreviewKey = "";
      previewKey = "";
      startingStats = null;
      previewLoading = false;
      previewError = "";
    }
  }

  public boolean previewReady() {
    return !draft.defense.supported
        || (startingStats != null
            && !previewKey.isEmpty()
            && previewKey.equals(currentPreviewKey()));
  }

  public void preview(Submit submit, boolean retry) {
    if (!draft.defense.supported || saving || uncertainSubmission) return;
    invalidatePreviewIfChanged();
    final String key = currentPreviewKey();
    if (key.isEmpty()
        || previewLoading
        || previewReady()
        || (!retry && key.equals(attemptedPreviewKey))) return;
    final int revision = ++previewRevision;
    final JSONObject body = new JSONObject((java.util.Map<?, ?>) draft.payload().get("creation"));
    attemptedPreviewKey = key;
    previewLoading = true;
    previewError = "";
    changed();
    submit.post(
        body,
        new ApiClient.Callback() {
          private boolean current() {
            return revision == previewRevision && key.equals(currentPreviewKey());
          }

          public void success(Object data) {
            if (!current()) return;
            previewLoading = false;
            JSONObject stats =
                data instanceof JSONObject
                    ? ((JSONObject) data).optJSONObject("startingStats")
                    : null;
            if (stats == null
                || !stats.has("hp")
                || !stats.has("ac")
                || !stats.has("initiative")
                || !stats.has("speed")) {
              previewError = "No pudimos comprobar la ficha. Reintentá el cálculo.";
            } else {
              startingStats = stats;
              previewKey = key;
            }
            changed();
          }

          public void failure(String message, int status) {
            if (!current()) return;
            previewLoading = false;
            previewError = message;
            if (status == 401) errorStatus = status;
            changed();
          }
        });
  }

  public void save(Submit submit) {
    if (saving || uncertainSubmission || createdId != null) return;
    if (!previewReady()) {
      saveError = "Primero comprobá los valores de la ficha.";
      changed();
      return;
    }
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
