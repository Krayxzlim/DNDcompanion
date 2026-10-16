package com.miapp.dndcompanion;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.miapp.dndcompanion.creation.*;
import com.miapp.dndcompanion.fragua.GameRepository;
import com.miapp.dndcompanion.network.ApiClient;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class CrearPersonajeActivity extends AppCompatActivity {
  private CreationViewModel model;
  private CharacterDraft d;
  private LinearLayout root, body;
  private Button next;
  private ScrollView currentScroll;
  private CharacterDraft.Step renderedStep;
  private static final String[] TITLES = {
    "Elegí tu especie",
    "Elegí tu clase",
    "¿Cómo obtenés tus atributos?",
    "Asigná tus atributos",
    "Elegí tu trasfondo",
    "Prepará tu equipo",
    "Completá tu dote de origen",
    "Conocé tu especie",
    "Conocé tu clase",
    "Elegí tus habilidades",
    "Dale una identidad",
    "Revisá tu personaje"
  };

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    model = new ViewModelProvider(this).get(CreationViewModel.class);
    if (state != null
        && model.draft.step == CharacterDraft.Step.SPECIES
        && model.draft.speciesKey.isEmpty()) {
      CharacterDraft restored = (CharacterDraft) state.getSerializable("creation_draft");
      if (restored != null) model.draft = restored;
      model.uncertainSubmission = state.getBoolean("submission_pending", false);
      if (model.uncertainSubmission) model.draft.step = CharacterDraft.Step.REVIEW;
    }
    d = model.draft;
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              public void handleOnBackPressed() {
                goBack();
              }
            });
    model.updates.observe(
        this,
        ignored -> {
          if (model.createdId != null) {
            finishCreated();
            return;
          }
          if (model.errorStatus == 401) {
            startActivity(
                new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
          }
          if (root == null
              || d.step == CharacterDraft.Step.SPECIES
              || d.step == CharacterDraft.Step.CLASS
              || d.step == CharacterDraft.Step.BACKGROUND
              || d.step == CharacterDraft.Step.ALLOCATION
              || d.step == CharacterDraft.Step.SPECIES_GUIDE
              || d.step == CharacterDraft.Step.CLASS_GUIDE
              || d.step == CharacterDraft.Step.REVIEW) render();
        });
    if (!model.loaded && !model.loading) loadCatalog();
  }

  protected void loadCatalog() {
    model.load(GameRepository.get(this)::read);
    model.loadCreation(GameRepository.get(this)::read);
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putSerializable("creation_draft", d);
    state.putBoolean("submission_pending", model.saving || model.uncertainSubmission);
    super.onSaveInstanceState(state);
  }

  private void goBack() {
    if (model.saving) return;
    if (model.uncertainSubmission) {
      finish();
      return;
    }
    if (d.step == CharacterDraft.Step.SPECIES)
      new AlertDialog.Builder(this)
          .setTitle("¿Salir del creador?")
          .setMessage("Se descartará este borrador.")
          .setNegativeButton("Seguir creando", null)
          .setPositiveButton("Salir", (a, b) -> finish())
          .show();
    else {
      d.back();
      render();
    }
  }

  private void render() {
    syncClassSkills();
    View focused = root != null && renderedStep == d.step ? focus(root) : null;
    String focusKey = focused == null ? null : focusKey(focused);
    boolean accessibilityFocus = focused != null && focused.isAccessibilityFocused();
    int scrollY = currentScroll != null && renderedStep == d.step ? currentScroll.getScrollY() : 0;
    renderedStep = d.step;
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(getColor(R.color.fondo));
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    root.addView(
        text(
            "CREAR PERSONAJE · " + (d.steps().indexOf(d.step) + 1) + " de " + d.steps().size(),
            14));
    root.addView(text(TITLES[d.step.ordinal()], 24));
    ScrollView scroll = new ScrollView(this);
    currentScroll = scroll;
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(16), dp(8), dp(16), dp(16));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    switch (d.step) {
      case SPECIES:
        species();
        break;
      case CLASS:
        classes();
        break;
      case METHOD:
        methods();
        break;
      case ALLOCATION:
        allocation();
        break;
      case BACKGROUND:
        background();
        break;
      case BACKGROUND_LOADOUT:
        backgroundLoadout();
        break;
      case ORIGIN_FEAT:
        originFeat();
        break;
      case SPECIES_GUIDE:
        speciesGuide();
        break;
      case CLASS_GUIDE:
        classGuide();
        break;
      case CLASS_SKILLS:
        classSkills();
        break;
      case IDENTITY:
        identity();
        break;
      case REVIEW:
        review();
        break;
    }
    LinearLayout footer = new LinearLayout(this);
    footer.setPadding(dp(12), 0, dp(12), dp(8));
    Button back = button("Atrás", this::goBack);
    footer.addView(back, new LinearLayout.LayoutParams(0, -2, 1));
    next =
        button(
            model.saving
                ? "Guardando…"
                : d.step == CharacterDraft.Step.REVIEW ? "Crear personaje" : "Continuar",
            () -> {
              if (d.step == CharacterDraft.Step.REVIEW)
                model.save(
                    (json, callback) ->
                        ApiClient.get(getApplicationContext())
                            .request("POST", "/characters", json, callback));
              else {
                d.next();
                render();
              }
            });
    footer.addView(next, new LinearLayout.LayoutParams(0, -2, 2));
    root.addView(footer);
    next.setEnabled(d.canAdvance() && !model.saving && !model.uncertainSubmission);
    if (model.saving) enable(root, false);
    setContentView(root);
    ViewCompat.requestApplyInsets(root);
    scroll.post(
        () -> {
          View target = focusKey == null ? null : matchingFocus(root, focusKey);
          if (target != null) {
            if (accessibilityFocus)
              target.performAccessibilityAction(
                  android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                  null);
            else target.requestFocus();
          }
          scroll.scrollTo(0, scrollY);
        });
  }

  private void species() {
    body.addView(text("Elegí tu especie para conocer sus rasgos.", 16));
    if (!model.catalogMessage.isEmpty()) body.addView(text(model.catalogMessage, 14));
    if (!model.loading) body.addView(button("Actualizar especies", this::loadCatalog));
    List<JSONObject> options = new ArrayList<>(model.species);
    int selected = 0;
    for (int i = 0; i < options.size(); i++)
      if (CreationViewModel.key(options.get(i)).equals(d.speciesKey)) selected = i + 1;
    if (selected == 0 && !d.speciesKey.isEmpty()) {
      JSONObject saved = new JSONObject();
      try {
        saved
            .put("key", d.speciesKey)
            .put("name", d.speciesName)
            .put("description", d.speciesDescription);
      } catch (org.json.JSONException ignored) {
      }
      options.add(saved);
      selected = options.size();
    }
    String[] names = new String[options.size() + 1];
    names[0] = "Seleccioná una especie";
    for (int i = 0; i < options.size(); i++) names[i + 1] = options.get(i).optString("name");
    Spinner selector =
        spinner(
            names,
            selected,
            position -> {
              if (position == 0) {
                if (!d.speciesKey.isEmpty()) {
                  d.speciesKey = "";
                  d.speciesName = "";
                  d.speciesDescription = "";
                  render();
                }
                return;
              }
              JSONObject row = options.get(position - 1);
              if (CreationViewModel.key(row).equals(d.speciesKey)) return;
              d.speciesKey = CreationViewModel.key(row);
              d.speciesName = row.optString("name");
              d.speciesDescription = row.optString("description");
              render();
            });
    selector.setContentDescription("Seleccionar especie");
    body.addView(selector);
    if (!d.speciesKey.isEmpty()) speciesContext();
  }

  private void speciesContext() {
    JSONObject selected = null;
    for (JSONObject row : model.species)
      if (CreationViewModel.key(row).equals(d.speciesKey)) selected = row;
    body.addView(text(d.speciesName, 22));
    if (selected == null) {
      details("Detalles de " + d.speciesName, d.speciesDescription);
      return;
    }
    String size = selected.optString("size"), speed = selected.optString("speed");
    if (!size.isEmpty()) body.addView(text("Tamaño: " + size, 16));
    if (!speed.isEmpty()) body.addView(text("Velocidad: " + speed, 16));
    details("Descripción de " + d.speciesName, selected.optString("description"));
    JSONArray traits = selected.optJSONArray("traits");
    if (traits != null)
      for (int i = 0; i < traits.length(); i++) {
        JSONObject trait = traits.optJSONObject(i);
        if (trait != null
            && !"SIZE".equals(trait.optString("type"))
            && !"SPEED".equals(trait.optString("type")))
          details(trait.optString("name"), trait.optString("description"));
      }
    provenance(selected);
  }

  private void classes() {
    body.addView(text("Elegí cómo querés afrontar la aventura.", 16));
    String[] names = new String[CharacterDraft.CLASSES.length + 1];
    names[0] = "Seleccioná una clase";
    System.arraycopy(CharacterDraft.CLASSES, 0, names, 1, CharacterDraft.CLASSES.length);
    Spinner selector =
        spinner(
            names,
            d.classIndex + 1,
            position -> {
              if (d.classIndex == position - 1) return;
              d.classSkills.clear();
              d.classIndex = position - 1;
              render();
            });
    selector.setContentDescription("Seleccionar clase");
    body.addView(selector);
    catalogStatus();
    if (d.classIndex >= 0) classContext(false);
  }

  private void catalogStatus() {
    if (!model.creationMessage.isEmpty()) body.addView(text(model.creationMessage, 14));
    if (!model.creationLoading && (!model.creationLoaded || !model.creationMessage.isEmpty()))
      body.addView(button("Reintentar información de clases", this::loadCatalog));
  }

  private String abilities(JSONArray keys) {
    List<String> labels = new ArrayList<>();
    if (keys != null)
      for (int i = 0; i < keys.length(); i++) {
        int index = CreationViewModel.abilityIndex(keys.optString(i));
        if (index >= 0) labels.add(CharacterDraft.ABILITIES[index]);
      }
    return android.text.TextUtils.join(", ", labels);
  }

  private void provenance(JSONObject row) {
    String source = row.optString("source");
    if (!source.isEmpty())
      details("Fuente del catálogo", source + " · El texto original puede estar en inglés.");
  }

  private void classContext(boolean guide) {
    body.addView(text(CharacterDraft.CLASSES[d.classIndex], 22));
    JSONObject row = model.selectedClass();
    if (row == null) {
      body.addView(text("Información de esta clase no disponible. Podés continuar.", 16));
      return;
    }
    body.addView(text(row.optString("description"), 16));
    body.addView(
        text("Atributos principales: " + abilities(row.optJSONArray("primaryAbilities")), 16));
    if (row.optInt("hitDie") > 0) body.addView(text("Dado de golpe: d" + row.optInt("hitDie"), 16));
    body.addView(text("Salvaciones: " + abilities(row.optJSONArray("savingThrows")), 16));
    if (!row.optString("coreTraits").isEmpty())
      details("Rasgos básicos y competencias", row.optString("coreTraits"));
    if (guide) {
      JSONArray features = row.optJSONArray("levelOneFeatures");
      if (features != null)
        for (int i = 0; i < features.length(); i++) {
          JSONObject feature = features.optJSONObject(i);
          if (feature != null) details(feature.optString("name"), feature.optString("description"));
        }
      if (!row.optString("progressionSummary").isEmpty())
        details("Progresión de clase", row.optString("progressionSummary"));
    }
    provenance(row);
  }

  private void methods() {
    choice("Valores estándar", "Asigná 15, 14, 13, 12, 10 y 8.", AbilityScores.Method.STANDARD);
    choice(
        "Compra de puntos",
        "Repartí 27 puntos. Cada atributo empieza en 8 y llega hasta 15.",
        AbilityScores.Method.POINT_BUY);
    choice(
        "Tiradas",
        "Seis tiradas de 4d6; se descarta el dado menor de cada una.",
        AbilityScores.Method.ROLL);
    body.addView(
        text(
            "Atributos base · En D&D 2024, los aumentos de atributos dependen del trasfondo, no de"
                + " la especie. La revisión del trasfondo queda pendiente.",
            14));
  }

  private void choice(String title, String description, AbilityScores.Method method) {
    body.addView(
        button(
            (d.abilities.method == method ? "✓ " : "") + title,
            () -> {
              d.abilities.method = method;
              render();
            }));
    body.addView(text(description, 16));
  }

  private void allocation() {
    catalogStatus();
    AbilityScores a = d.abilities;
    JSONObject selectedClass = model.selectedClass();
    if (selectedClass != null)
      body.addView(
          text(
              "Atributos principales: " + abilities(selectedClass.optJSONArray("primaryAbilities")),
              16));
    if (a.method == AbilityScores.Method.POINT_BUY) {
      body.addView(text("Puntos disponibles: " + a.remaining() + " de 27", 18));
      body.addView(
          button(
              "Distribución equilibrada",
              () -> {
                a.balanced();
                render();
              }));
      details(
          "Costo de los valores",
          "8: 0 · 9: 1 · 10: 2 · 11: 3 · 12: 4 · 13: 5 · 14: 7 · 15: 9 puntos. Gastá los 27 puntos"
              + " para continuar.");
    } else if (a.method == AbilityScores.Method.ROLL) {
      body.addView(
          button(
              a.hasRolls() ? "Volver a tirar" : "Tirar los dados",
              () -> {
                if (!a.hasRolls()) {
                  a.roll(new Random());
                  render();
                } else
                  new AlertDialog.Builder(this)
                      .setTitle("¿Reemplazar las seis tiradas?")
                      .setMessage(
                          "Se perderán los dados y sus asignaciones. Consultá con tu DM antes de"
                              + " repetir.")
                      .setNegativeButton("Conservar", null)
                      .setPositiveButton(
                          "Volver a tirar",
                          (dialog, which) -> {
                            a.roll(new Random());
                            render();
                          })
                      .show();
              }));
      if (!a.hasRolls()) {
        body.addView(text("Tirá una vez y asigná cada resultado a un atributo.", 16));
        return;
      }
      StringBuilder rolls = new StringBuilder();
      for (int i = 0; i < 6; i++)
        rolls
            .append("Tirada ")
            .append(i + 1)
            .append(": ")
            .append(Arrays.toString(a.dice(i)))
            .append(" → ")
            .append(a.pool(i))
            .append("\n");
      details("Ver los dados (se descarta el menor)", rolls.toString());
    }
    int[] order = model.recommendedOrder();
    if (order != null)
      body.addView(
          button(
              "Aplicar recomendación de " + CharacterDraft.CLASSES[d.classIndex],
              () -> {
                a.recommend(order);
                render();
              }));
    if (a.method != AbilityScores.Method.POINT_BUY)
      body.addView(
          text(
              "Cada resultado se usa una vez. Elegir uno ocupado intercambia las asignaciones.",
              14));
    for (int i = 0; i < 6; i++) {
      final int ability = i;
      int value = a.score(i);
      LinearLayout row = new LinearLayout(this);
      row.setGravity(android.view.Gravity.CENTER_VERTICAL);
      TextView label = text(CharacterDraft.ABILITIES[i], 15);
      label.setPadding(0, dp(8), dp(4), dp(8));
      row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
      if (a.method == AbilityScores.Method.POINT_BUY) {
        Button minus =
            button(
                "−",
                () -> {
                  a.adjust(ability, -1);
                  render();
                });
        minus.setContentDescription("Reducir " + CharacterDraft.ABILITIES[i]);
        minus.setEnabled(value > 8);
        Button plus =
            button(
                "+",
                () -> {
                  a.adjust(ability, 1);
                  render();
                });
        plus.setContentDescription("Aumentar " + CharacterDraft.ABILITIES[i]);
        plus.setEnabled(value < 15 && a.remaining() >= (value >= 13 ? 2 : 1));
        minus.setPadding(0, 0, 0, 0);
        plus.setPadding(0, 0, 0, 0);
        row.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView number = text(String.valueOf(value), 16);
        number.setGravity(android.view.Gravity.CENTER);
        number.setPadding(0, 0, 0, 0);
        row.addView(number, new LinearLayout.LayoutParams(dp(32), -2));
        row.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
      } else {
        String[] entries = new String[7];
        String[] compact = new String[7];
        entries[0] = "Sin asignar";
        compact[0] = "—";
        for (int j = 0; j < 6; j++) {
          int owner = a.ownerOf(j);
          entries[j + 1] =
              "Resultado "
                  + (j + 1)
                  + ": "
                  + a.pool(j)
                  + " · "
                  + (owner < 0 ? "Disponible" : "Usado en " + CharacterDraft.ABILITIES[owner]);
          compact[j + 1] = String.valueOf(a.pool(j));
        }
        Spinner selector =
            spinner(
                entries,
                a.assigned(i) + 1,
                position -> {
                  if (a.assigned(ability) != position - 1) {
                    a.assign(ability, position - 1);
                    render();
                  }
                });
        ArrayAdapter<String> adapter =
            new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, compact) {
              @Override
              public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(entries[position]);
                view.setSingleLine(false);
                return view;
              }
            };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selector.setAdapter(adapter);
        selector.setSelection(a.assigned(i) + 1);
        selector.setContentDescription("Asignar " + CharacterDraft.ABILITIES[i]);
        row.addView(selector, new LinearLayout.LayoutParams(dp(88), -2));
      }
      TextView modifier =
          text(
              value == 0
                  ? "—"
                  : String.format(java.util.Locale.ROOT, "%+d", AbilityScores.modifier(value)),
              16);
      modifier.setPadding(dp(4), dp(8), 0, dp(8));
      modifier.setGravity(android.view.Gravity.CENTER);
      modifier.setContentDescription(
          "Modificador de " + CharacterDraft.ABILITIES[i] + ": " + modifier.getText());
      row.addView(modifier, new LinearLayout.LayoutParams(dp(40), -2));
      body.addView(row);
    }
  }

  private void background() {
    body.addView(
        text(
            "Tu trasfondo aumenta los atributos. La especie no agrega aumentos en las reglas de"
                + " 2024.",
            16));
    catalogStatus();
    JSONArray entries =
        model.creation == null || model.creation.optInt("backgroundDecisionsVersion") != 1
            ? null
            : model.creation.optJSONArray("backgrounds");
    if (entries == null && !model.creationLoading)
      body.addView(button("Actualizar trasfondos", this::loadCatalog));
    List<JSONObject> rows = new ArrayList<>();
    if (entries != null)
      for (int i = 0; i < entries.length(); i++) {
        JSONObject row = entries.optJSONObject(i);
        if (row != null
            && row.optJSONArray("abilities") != null
            && row.optJSONArray("abilities").length() == 3) rows.add(row);
      }
    if (!d.background.key.isEmpty()
        && rows.stream().noneMatch(row -> row.optString("key").equals(d.background.key))) {
      JSONObject saved = new JSONObject();
      try {
        saved.put("key", d.background.key).put("name", d.background.name);
      } catch (org.json.JSONException ignored) {
      }
      rows.add(saved);
    }
    if (!rows.isEmpty()) {
      String[] names = new String[rows.size() + 1];
      names[0] = "Seleccioná un trasfondo";
      int selected = 0;
      for (int i = 0; i < rows.size(); i++) {
        names[i + 1] = rows.get(i).optString("name");
        if (rows.get(i).optString("key").equals(d.background.key)) selected = i + 1;
      }
      Spinner selector =
          spinner(
              names,
              selected,
              position -> {
                if (position == 0) {
                  if (!d.background.key.isEmpty()) {
                    d.background.select("", "", "", new int[0]);
                    render();
                  }
                  return;
                }
                JSONObject row = rows.get(position - 1);
                if (row.optString("key").equals(d.background.key)) return;
                JSONArray keys = row.optJSONArray("abilities");
                int[] allowed = new int[3];
                for (int i = 0; i < 3; i++)
                  allowed[i] = CreationViewModel.abilityIndex(keys.optString(i));
                if (Arrays.stream(allowed).anyMatch(i -> i < 0)) return;
                d.background.select(
                    row.optString("key"),
                    row.optString("name"),
                    "Dote: "
                        + row.optString("feat")
                        + "\nCompetencias: "
                        + row.optString("skills")
                        + "\nHerramientas: "
                        + row.optString("tools")
                        + "\nEquipo: "
                        + row.optString("equipment")
                        + "\n"
                        + row.optString("source"),
                    allowed);
                d.background.skillGrantsApplied =
                    model.creation != null && model.creation.optInt("backgroundSkillsVersion") == 1;
                JSONArray toolOptions = row.optJSONArray("toolOptions");
                if (model.creation.optInt("backgroundLoadoutVersion") == 1
                    && toolOptions != null
                    && toolOptions.length() > 0) {
                  d.background.loadoutSupported = true;
                  d.background.toolRequired = row.optBoolean("toolChoiceRequired");
                  d.background.toolKeys = new String[toolOptions.length()];
                  d.background.toolNames = new String[toolOptions.length()];
                  for (int i = 0; i < toolOptions.length(); i++) {
                    d.background.toolKeys[i] = toolOptions.optJSONObject(i).optString("key");
                    d.background.toolNames[i] = toolOptions.optJSONObject(i).optString("name");
                  }
                  if (!d.background.toolRequired)
                    d.background.toolChoice = d.background.toolKeys[0];
                  d.background.packageDetails = row.optString("equipment");
                  JSONArray equipment = row.optJSONArray("equipmentOptions");
                  if (equipment != null && equipment.length() > 0)
                    d.background.packageGold = equipment.optJSONObject(0).optInt("gold");
                }
                JSONObject feat = row.optJSONObject("originFeat");
                if (model.creation.optInt("originFeatsVersion") == 1 && feat != null) {
                  d.background.featSupported = true;
                  d.background.featKey = feat.optString("key");
                  d.background.featName = feat.optString("name");
                  d.background.featDescription = feat.optString("description");
                  d.background.featList = feat.optString("list");
                  JSONArray spells = model.creation.optJSONArray("originSpells");
                  if (spells != null) d.background.spellOptionsJson = spells.toString();
                }
                d.level = 1;
                render();
              });
      selector.setContentDescription("Seleccionar trasfondo");
      body.addView(selector);
    } else
      body.addView(
          text(
              "El catálogo de trasfondos no está disponible. Podés reintentar o completar este paso"
                  + " más adelante.",
              16));
    if (!d.background.key.isEmpty()) {
      body.addView(text(d.background.name, 20));
      body.addView(text("Elegí +2 y +1 en dos atributos distintos, o +1 en los tres.", 16));
      body.addView(
          button(
              "Aplicar +1 a los tres",
              () -> {
                Arrays.fill(d.background.increases, 0);
                for (int i : d.background.allowed) d.background.increases[i] = 1;
                render();
              }));
      for (int index : d.background.allowed) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(
            text(CharacterDraft.ABILITIES[index] + " · base " + d.abilities.score(index), 16),
            new LinearLayout.LayoutParams(0, -2, 1));
        Spinner increase =
            spinner(
                new String[] {"+0", "+1", "+2"},
                d.background.increases[index],
                value -> {
                  if (d.background.increases[index] != value) {
                    d.background.increases[index] = value;
                    render();
                  }
                });
        increase.setContentDescription("Aumento de " + CharacterDraft.ABILITIES[index]);
        row.addView(increase, new LinearLayout.LayoutParams(dp(88), -2));
        body.addView(row);
      }
      if (!d.background.valid())
        body.addView(text("Completá un reparto válido para continuar.", 14));
      details("Beneficios del trasfondo", d.background.details);
      body.addView(
          text(
              d.background.loadoutSupported
                  ? "Se aplican los aumentos y las competencias de habilidad. A continuación"
                      + " elegirás herramientas, equipo y la dote de origen."
                  : d.background.skillGrantsApplied
                      ? "Se aplican los aumentos y las competencias de habilidad. Quedan pendientes"
                          + " la dote, las herramientas y el equipo. Este flujo admite nivel 1."
                      : "Se guardarán el trasfondo y sus aumentos. Sus demás beneficios quedan"
                          + " pendientes de completar. Este flujo admite nivel 1.",
              14));
    }
    if (d.background.deferred)
      body.addView(text("Trasfondo pendiente · se guardarán solo los atributos base.", 16));
    else
      body.addView(
          button(
              "Completar trasfondo más adelante",
              () -> {
                d.background.defer();
                render();
              }));
  }

  private void backgroundLoadout() {
    body.addView(text(d.background.name, 20));
    body.addView(
        text(
            "Elegí tu equipo inicial. La competencia con herramientas se conserva aunque elijas el"
                + " oro.",
            16));
    if (d.background.toolRequired) {
      String[] names = new String[d.background.toolNames.length + 1];
      names[0] = "Elegí un juego";
      System.arraycopy(d.background.toolNames, 0, names, 1, d.background.toolNames.length);
      int selected = Arrays.asList(d.background.toolKeys).indexOf(d.background.toolChoice) + 1;
      Spinner tool =
          spinner(
              names,
              selected,
              value -> {
                String key = value == 0 ? "" : d.background.toolKeys[value - 1];
                if (!key.equals(d.background.toolChoice)) {
                  d.background.toolChoice = key;
                  render();
                }
              });
      tool.setContentDescription("Herramienta del trasfondo");
      body.addView(tool);
    } else if (d.background.toolNames.length > 0)
      body.addView(text("Competencia: " + d.background.toolNames[0], 16));
    for (String option : new String[] {"A", "B"}) {
      String label =
          option.equals("A")
              ? "Paquete A · " + d.background.packageGold + " po"
              : "Opción B · 50 po";
      body.addView(
          button(
              (option.equals(d.background.equipmentOption) ? "✓ " : "") + label,
              () -> {
                d.background.equipmentOption = option;
                render();
              }));
    }
    details("Ver contenido del paquete", d.background.packageDetails);
    body.addView(
        text(
            "Se agrega al inventario una sola vez al crear. Las armas quedan sin equipar; revisá su"
                + " competencia y propiedades antes de usarlas.",
            14));
  }

  private void originFeat() {
    body.addView(text(d.background.featName, 20));
    body.addView(text(d.background.featDescription, 16));
    if (!d.background.featKey.equals("magicInitiate")) return;
    String[] keys = {"", "intelligence", "wisdom", "charisma"};
    body.addView(text("Característica para estos conjuros", 16));
    Spinner ability =
        spinner(
            new String[] {"Elegí una característica", "Inteligencia", "Sabiduría", "Carisma"},
            Arrays.asList(keys).indexOf(d.background.castingAbility),
            i -> {
              if (!keys[i].equals(d.background.castingAbility)) {
                d.background.castingAbility = keys[i];
                render();
              }
            });
    ability.setContentDescription("Característica de Iniciado en la magia");
    body.addView(ability);
    featSpellSelector("Primer truco", 0, 0);
    featSpellSelector("Segundo truco", 0, 1);
    featSpellSelector("Conjuro de nivel 1", 1, 2);
    if (!d.background.featValid())
      body.addView(
          text("Elegí una característica, dos trucos distintos y un conjuro de nivel 1.", 14));
  }

  private void featSpellSelector(String label, int level, int slot) {
    List<JSONObject> options = new ArrayList<>();
    try {
      JSONArray spells = new JSONArray(d.background.spellOptionsJson);
      for (int i = 0; i < spells.length(); i++) {
        JSONObject spell = spells.optJSONObject(i);
        if (spell != null
            && spell.optInt("level") == level
            && spell.optJSONArray("lists") != null
            && spell.optJSONArray("lists").toString().contains("\"" + d.background.featList + "\""))
          options.add(spell);
      }
    } catch (org.json.JSONException ignored) {
    }
    String current =
        slot == 0 ? d.background.cantrip1 : slot == 1 ? d.background.cantrip2 : d.background.spell;
    String[] names = new String[options.size() + 1];
    names[0] = "Elegir…";
    int selected = 0;
    for (int i = 0; i < options.size(); i++) {
      names[i + 1] = options.get(i).optString("name");
      if (options.get(i).optString("key").equals(current)) selected = i + 1;
    }
    body.addView(text(label, 16));
    Spinner selector =
        spinner(
            names,
            selected,
            i -> {
              String key = i == 0 ? "" : options.get(i - 1).optString("key");
              if (!key.equals(current)) {
                if (slot == 0) d.background.cantrip1 = key;
                else if (slot == 1) d.background.cantrip2 = key;
                else d.background.spell = key;
                render();
              }
            });
    selector.setContentDescription(label);
    body.addView(selector);
    if (selected > 0) {
      JSONObject spell = options.get(selected - 1);
      details(
          "Detalles de " + spell.optString("name"),
          spell.optString("description") + "\n" + spell.optString("source"));
    }
  }

  private String spellName(String key) {
    try {
      JSONArray spells = new JSONArray(d.background.spellOptionsJson);
      for (int i = 0; i < spells.length(); i++) {
        JSONObject spell = spells.optJSONObject(i);
        if (spell != null && key.equals(spell.optString("key"))) return spell.optString("name");
      }
    } catch (org.json.JSONException ignored) {
    }
    return key;
  }

  private void speciesGuide() {
    speciesContext();
    body.addView(
        text(
            "Este paso es una guía. Las elecciones de rasgos no se asignan aquí. Después de crear,"
                + " revisá el manual con tu DM y completá la ficha.",
            14));
  }

  private void classGuide() {
    catalogStatus();
    classContext(true);
    body.addView(
        text(
            "Guía de nivel 1 · D&D 2024. Este creador guarda la ficha base; las elecciones de"
                + " conjuros y rasgos quedan para completar después. Las habilidades de clase"
                + " tienen un paso propio si elegiste trasfondo y el catálogo lo permite. Para"
                + " niveles superiores, consultá la progresión de tu clase.",
            14));
  }

  private void syncClassSkills() {
    JSONObject row = model.selectedClass();
    if (row == null || model.creation.optInt("classSkillsVersion") != 1) return;
    String key = row.optString("key");
    if (!d.classSkills.classKey.equals(key)) d.classSkills.clear();
    JSONObject choices = row.optJSONObject("skillChoices");
    if (choices == null) return;
    d.classSkills.classKey = key;
    d.classSkills.count = choices.optInt("count");
    d.classSkills.keys.clear();
    d.classSkills.names.clear();
    JSONArray options = choices.optJSONArray("options"),
        labels = model.creation.optJSONArray("skills");
    if (options == null || labels == null) return;
    for (int i = 0; i < options.length(); i++) {
      String option = options.optString(i);
      for (int j = 0; j < labels.length(); j++) {
        JSONObject label = labels.optJSONObject(j);
        if (label != null && option.equals(label.optString("key"))) {
          d.classSkills.keys.add(option);
          d.classSkills.names.add(label.optString("name"));
          break;
        }
      }
    }
  }

  private void classSkills() {
    body.addView(
        text(
            "Elegí "
                + d.classSkills.count
                + " habilidades de "
                + CharacterDraft.CLASSES[d.classIndex]
                + ".",
            18));
    body.addView(
        text("Seleccionadas: " + d.classSkills.selected.size() + " de " + d.classSkills.count, 16));
    body.addView(
        text(
            "Si una habilidad también viene del trasfondo, su bono no se duplica. La pericia se"
                + " elige por separado cuando un rasgo la permite.",
            14));
    for (int i = 0; i < d.classSkills.keys.size(); i++) {
      String key = d.classSkills.keys.get(i);
      CheckBox choice = new CheckBox(this);
      choice.setText(d.classSkills.names.get(i));
      choice.setTextColor(getColor(R.color.texto));
      choice.setMinHeight(dp(48));
      choice.setChecked(d.classSkills.selected.contains(key));
      choice.setEnabled(choice.isChecked() || d.classSkills.selected.size() < d.classSkills.count);
      choice.setOnCheckedChangeListener(
          (button, checked) -> {
            if (checked) d.classSkills.selected.add(key);
            else d.classSkills.selected.remove(key);
            render();
          });
      body.addView(choice);
    }
  }

  private void identity() {
    body.addView(text("Nombre", 18));
    EditText name = new EditText(this);
    name.setSingleLine(true);
    name.setHint("Nombre del personaje");
    name.setTextColor(getColor(R.color.texto));
    name.setHintTextColor(getColor(R.color.texto_secundario));
    name.setText(d.name);
    name.setFilters(
        new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(160)});
    body.addView(name);
    name.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            d.name = s.toString();
            next.setEnabled(d.canAdvance());
          }

          public void afterTextChanged(Editable e) {}
        });
    body.addView(text("Alineación", 18));
    body.addView(spinner(CharacterDraft.ALIGNMENTS, d.alignment, value -> d.alignment = value));
    if (!d.background.key.isEmpty()) {
      body.addView(text("Nivel 1 · el trasfondo se guarda con los atributos iniciales.", 16));
      return;
    }
    body.addView(text("Nivel (1 para empezar)", 18));
    String[] levels = new String[20];
    for (int i = 0; i < 20; i++) levels[i] = String.valueOf(i + 1);
    body.addView(spinner(levels, d.level - 1, value -> d.level = value + 1));
  }

  private void review() {
    if (model.uncertainSubmission) {
      body.addView(
          text(
              "No pudimos confirmar el guardado. El personaje puede haberse creado. Volvé a la"
                  + " ficha y comprobá el último personaje antes de crear otro.",
              18));
      body.addView(button("Volver a la ficha", this::finish));
    }
    body.addView(text(d.name.trim(), 24));
    body.addView(
        text(
            d.speciesName
                + " · "
                + CharacterDraft.CLASSES[d.classIndex]
                + " · Nivel "
                + d.level
                + "\n"
                + CharacterDraft.ALIGNMENTS[d.alignment],
            18));
    if (!d.background.key.isEmpty()) body.addView(text("Trasfondo: " + d.background.name, 18));
    if (d.background.loadoutSupported)
      body.addView(
          text(
              "Equipo: "
                  + (d.background.equipmentOption.equals("A") ? "Paquete A" : "50 po")
                  + " · Herramienta: "
                  + d.background
                      .toolNames[
                      Arrays.asList(d.background.toolKeys).indexOf(d.background.toolChoice)],
              16));
    if (d.background.featSupported) {
      body.addView(text("Dote: " + d.background.featName, 18));
      if (d.background.featKey.equals("magicInitiate"))
        body.addView(
            text(
                spellName(d.background.cantrip1)
                    + " · "
                    + spellName(d.background.cantrip2)
                    + " · "
                    + spellName(d.background.spell)
                    + "\nCaracterística: "
                    + CharacterDraft.ABILITIES[
                        CreationViewModel.abilityIndex(d.background.castingAbility)],
                16));
    }
    if (d.hasClassSkills()) {
      java.util.List<String> names = new ArrayList<>();
      for (String key : d.classSkills.selected)
        names.add(d.classSkills.names.get(d.classSkills.keys.indexOf(key)));
      body.addView(text("Habilidades de clase: " + String.join(", ", names), 16));
    }
    for (int i = 0; i < 6; i++) {
      String score =
          d.background.key.isEmpty()
              ? String.valueOf(d.abilities.score(i))
              : d.abilities.score(i) + " + " + d.background.increases[i] + " = " + d.finalScore(i);
      body.addView(
          text(
              CharacterDraft.ABILITIES[i]
                  + ": "
                  + score
                  + " · Mod "
                  + String.format(
                      java.util.Locale.ROOT, "%+d", AbilityScores.modifier(d.finalScore(i))),
              16));
    }
    body.addView(
        text(
            "Se guardarán tu identidad y los atributos del resumen. Después, usá Completar ficha"
                + " para PV, competencias y recursos. Revisá también tu trasfondo, rasgos y"
                + " conjuros con el manual.",
            16));
    if (!model.saveError.isEmpty()) body.addView(text(model.saveError, 16));
  }

  private void finishCreated() {
    Intent result = new Intent();
    result.putExtra("personaje_id", model.createdId);
    result.putExtra("personaje_nombre", d.name.trim());
    result.putExtra("personaje_raza", d.speciesName);
    result.putExtra("personaje_clase", CharacterDraft.CLASSES[d.classIndex]);
    result.putExtra("personaje_nivel", d.level);
    result.putExtra("personaje_alineacion", CharacterDraft.ALIGNMENTS[d.alignment]);
    String[] keys = {"fue", "des", "con", "int", "sab", "car"};
    for (int i = 0; i < 6; i++) result.putExtra("personaje_" + keys[i], d.finalScore(i));
    setResult(RESULT_OK, result);
    finish();
  }

  private void details(String label, String content) {
    TextView detail =
        text(content.isEmpty() ? "No hay detalles disponibles en el catálogo." : content, 16);
    detail.setVisibility(View.GONE);
    body.addView(
        button(
            label,
            () ->
                detail.setVisibility(
                    detail.getVisibility() == View.GONE ? View.VISIBLE : View.GONE)));
    body.addView(detail);
  }

  private TextView text(String value, int size) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(getColor(R.color.texto));
    t.setPadding(dp(12), dp(10), dp(12), dp(10));
    return t;
  }

  private Button button(String title, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.boton_bg)));
    b.setMinHeight(dp(48));
    b.setTextColor(getColor(R.color.dorado_claro));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private interface Selection {
    void set(int value);
  }

  private Spinner spinner(String[] values, int selected, Selection action) {
    Spinner s = new Spinner(this);
    s.setMinimumHeight(dp(48));
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(adapter);
    s.setSelection(selected);
    s.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
            action.set(position);
          }

          public void onNothingSelected(AdapterView<?> p) {}
        });
    return s;
  }

  private void enable(View view, boolean enabled) {
    view.setEnabled(enabled);
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        enable(((ViewGroup) view).getChildAt(i), enabled);
  }

  private View focus(View view) {
    if (view.isAccessibilityFocused() || view.isFocused()) return view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View found = focus(((ViewGroup) view).getChildAt(i));
        if (found != null) return found;
      }
    return null;
  }

  private String focusKey(View view) {
    if (view.getContentDescription() != null) return view.getContentDescription().toString();
    return view instanceof Button ? ((Button) view).getText().toString() : null;
  }

  private View matchingFocus(View view, String key) {
    if (key.equals(focusKey(view))) return view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View found = matchingFocus(((ViewGroup) view).getChildAt(i), key);
        if (found != null) return found;
      }
    return null;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
