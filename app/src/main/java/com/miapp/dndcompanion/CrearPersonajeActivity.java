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
    "Conocé tu especie",
    "Conocé tu clase",
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
              || d.step == CharacterDraft.Step.REVIEW) render();
        });
    if (!model.loaded && !model.loading) loadCatalog();
  }

  protected void loadCatalog() {
    model.load(GameRepository.get(this)::read);
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
    root.addView(text("CREAR PERSONAJE · " + (d.step.ordinal() + 1) + " de 8", 14));
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
      case SPECIES_GUIDE:
        speciesGuide();
        break;
      case CLASS_GUIDE:
        classGuide();
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
    body.addView(
        text(
            "Tu especie define tus rasgos naturales. Podés consultar los detalles antes de elegir.",
            16));
    if (!model.catalogMessage.isEmpty()) body.addView(text(model.catalogMessage, 14));
    if (!model.loading) body.addView(button("Actualizar especies", this::loadCatalog));
    for (JSONObject row : model.species) {
      String key = CreationViewModel.key(row), name = row.optString("name");
      body.addView(
          button(
              (key.equals(d.speciesKey) ? "✓ " : "") + name,
              () -> {
                d.speciesKey = key;
                d.speciesName = name;
                d.speciesDescription = row.optString("description");
                render();
              }));
    }
    if (!d.speciesKey.isEmpty()) details("Detalles de " + d.speciesName, d.speciesDescription);
  }

  private void classes() {
    body.addView(
        text(
            "Elegí cómo querés afrontar la aventura. Después veremos los rasgos de tu clase.", 16));
    for (int i = 0; i < CharacterDraft.CLASSES.length; i++) {
      final int index = i;
      body.addView(
          button(
              (d.classIndex == i ? "✓ " : "") + CharacterDraft.CLASSES[i],
              () -> {
                d.classIndex = index;
                render();
              }));
    }
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
    AbilityScores a = d.abilities;
    body.addView(
        text("Prioridad de " + CharacterDraft.CLASSES[d.classIndex] + ": " + priority() + ".", 16));
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
    if (a.method != AbilityScores.Method.POINT_BUY)
      body.addView(
          text(
              "Cada resultado se usa una vez. Elegir uno ocupado intercambia las asignaciones.",
              14));
    for (int i = 0; i < 6; i++) {
      final int ability = i;
      int value = a.score(i);
      body.addView(
          text(
              CharacterDraft.ABILITIES[i]
                  + (value == 0
                      ? " · sin asignar"
                      : " · "
                          + value
                          + " ("
                          + String.format(
                              java.util.Locale.ROOT, "%+d", AbilityScores.modifier(value))
                          + ")"),
              18));
      if (a.method == AbilityScores.Method.POINT_BUY) {
        LinearLayout row = new LinearLayout(this);
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
        row.addView(minus, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(plus, new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(row);
      } else {
        String[] entries = new String[7];
        entries[0] = "Sin asignar";
        for (int j = 0; j < 6; j++) entries[j + 1] = "Resultado " + (j + 1) + ": " + a.pool(j);
        Spinner spinner =
            spinner(
                entries,
                a.assigned(i) + 1,
                position -> {
                  if (a.assigned(ability) != position - 1) {
                    a.assign(ability, position - 1);
                    render();
                  }
                });
        spinner.setContentDescription("Asignar " + CharacterDraft.ABILITIES[i]);
        body.addView(spinner);
      }
    }
  }

  private String priority() {
    return new String[] {
          "Fuerza",
          "Carisma",
          "Sabiduría",
          "Sabiduría",
          "Fuerza o Destreza",
          "Inteligencia",
          "Destreza y Sabiduría",
          "Fuerza y Carisma",
          "Destreza y Sabiduría",
          "Destreza",
          "Carisma",
          "Carisma"
        }
        [d.classIndex];
  }

  private void speciesGuide() {
    body.addView(text(d.speciesName, 22));
    body.addView(
        text(
            "Revisá sus rasgos: sentidos, velocidad y capacidades especiales. Los detalles del"
                + " catálogo te ayudan a conocerla.",
            16));
    details("Consultar rasgos de la especie", d.speciesDescription);
    body.addView(
        text(
            "Este paso es una guía. Las elecciones de rasgos no se asignan aquí. Después de crear,"
                + " revisá el manual con tu DM y completá la ficha.",
            14));
  }

  private void classGuide() {
    body.addView(text(CharacterDraft.CLASSES[d.classIndex] + " · prioridad: " + priority(), 22));
    String[] guides = {
      "La Furia potencia tu combate y tu resistencia. Revisá sus usos y condiciones, la Defensa sin"
          + " armadura y las maestrías de armas.",
      "La Inspiración bárdica ayuda a tus aliados. Revisá sus usos, tus competencias y los trucos y"
          + " conjuros que preparás.",
      "Tu magia usa Sabiduría. Revisá tus conjuros preparados y la elección de Orden divina de"
          + " nivel 1.",
      "Tu magia usa Sabiduría. Revisá tus trucos, conjuros preparados y la elección de Orden"
          + " primigenia de nivel 1.",
      "Revisá tu estilo de combate, Segundo aliento y las maestrías de armas. Fuerza o Destreza"
          + " dependerá de tus armas.",
      "Tu magia usa Inteligencia. A nivel 1, elegís 3 trucos y tu libro contiene 6 conjuros de"
          + " nivel 1. Preparás 4 de esos conjuros y tenés 2 espacios de nivel 1. Revisá también"
          + " Recuperación arcana y Adepto ritual.",
      "Artes marciales usa tus ataques sin armas y armas de monje. Revisá Defensa sin armadura;"
          + " Destreza y Sabiduría son importantes.",
      "Revisá Imposición de manos, tus conjuros y las maestrías de armas. Fuerza ayuda en combate y"
          + " Carisma a tu magia.",
      "Revisá Enemigo predilecto, tus conjuros y las maestrías de armas. Destreza y Sabiduría"
          + " orientan tus capacidades.",
      "Ataque furtivo requiere condiciones concretas. Revisá Pericia, Jerga de ladrones y las"
          + " maestrías de armas.",
      "Tu magia usa Carisma. Revisá los trucos, conjuros preparados y Hechicería innata de nivel"
          + " 1.",
      "Tu magia usa Carisma. Magia de pacto tiene espacios que recuperás con un descanso corto o"
          + " largo. Revisá tus conjuros e Invocaciones sobrenaturales."
    };
    body.addView(text(guides[d.classIndex], 17));
    body.addView(
        text(
            "Guía de nivel 1 · D&D 2024. Este creador guarda la ficha base; las elecciones de"
                + " conjuros, competencias y rasgos quedan para completar después. Para niveles"
                + " superiores, consultá la progresión de tu clase.",
            14));
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
    for (int i = 0; i < 6; i++)
      body.addView(text(CharacterDraft.ABILITIES[i] + ": " + d.abilities.score(i), 16));
    body.addView(
        text(
            "Se guardarán tu identidad y tus atributos base. Después, usá Completar ficha para PV,"
                + " competencias y recursos. Revisá también tu trasfondo, rasgos y conjuros con el"
                + " manual.",
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
    for (int i = 0; i < 6; i++) result.putExtra("personaje_" + keys[i], d.abilities.score(i));
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
