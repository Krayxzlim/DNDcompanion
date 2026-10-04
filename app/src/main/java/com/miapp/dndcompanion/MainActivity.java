package com.miapp.dndcompanion;

import static com.miapp.dndcompanion.fragua.FraguaUi.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.miapp.dndcompanion.fragua.*;
import java.util.*;
import org.json.*;

/** Fragua host. Navigation preserves the character; business changes are backend commands. */
public class MainActivity extends AppCompatActivity {
  public static final String EXTRA_USER_EMAIL = "user_email";
  private GameRepository repo;
  private GameViewModel vm;
  private LinearLayout body;
  private TextView status, title;
  private final String[] classKeys = {
    "barbarian",
    "bard",
    "cleric",
    "druid",
    "fighter",
    "monk",
    "paladin",
    "ranger",
    "rogue",
    "sorcerer",
    "warlock",
    "wizard"
  };
  private final String[] classLabels = {
    "Bárbaro",
    "Bardo",
    "Clérigo",
    "Druida",
    "Guerrero",
    "Monje",
    "Paladín",
    "Explorador",
    "Pícaro",
    "Hechicero",
    "Brujo",
    "Mago"
  };

  private JSONObject sheet() {
    return vm.current().sheet;
  }

  private JSONObject state() {
    return sheet() == null ? new JSONObject() : sheet().optJSONObject("state");
  }

  private JSONObject derived() {
    return sheet() == null ? new JSONObject() : sheet().optJSONObject("derived");
  }

  private JSONArray array(JSONObject o, String key) {
    JSONArray a = o.optJSONArray(key);
    return a == null ? new JSONArray() : a;
  }

  private JSONObject obj(Object... pairs) {
    JSONObject o = new JSONObject();
    try {
      for (int i = 0; i < pairs.length; i += 2) o.put((String) pairs[i], pairs[i + 1]);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    return o;
  }

  private int px(float n) {
    return dp(this, n);
  }

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    repo = GameRepository.get(this);
    if (!repo.hasSession()) {
      login();
      return;
    }
    vm = new ViewModelProvider(this, GameViewModel.factory(repo)).get(GameViewModel.class);
    if (saved != null) vm.screen = saved.getString("screen", "home");
    else if (getIntent().hasExtra("screen")) vm.screen = getIntent().getStringExtra("screen");
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new androidx.activity.OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (vm != null && !vm.screen.equals("home")) go("home");
                else {
                  setEnabled(false);
                  getOnBackPressedDispatcher().onBackPressed();
                }
              }
            });
    shell();
    vm.state()
        .observe(
            this,
            s -> {
              if (!repo.hasSession()) {
                login();
                return;
              }
              status.setText(s.message);
              render();
            });
    if (vm.noteDraft != null) {
      JSONObject draft = vm.noteDraft;
      noteEditor(
          draft.optJSONObject("note"), array(draft, "sections"), draft.optBoolean("editable"));
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    if (captureNote != null) captureNote.run();
    super.onSaveInstanceState(out);
    if (vm != null) out.putString("screen", vm.screen);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (vm != null) vm.load();
  }

  private void login() {
    startActivity(
        new Intent(this, LoginActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    finish();
  }

  private void shell() {
    setContentView(R.layout.view_fragua_shell);
    View root = findViewById(R.id.fragua_root);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          androidx.core.graphics.Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(px(12) + b.left, b.top, px(12) + b.right, b.bottom);
          return insets;
        });
    title = findViewById(R.id.fragua_title);
    status = findViewById(R.id.fragua_status);
    body = findViewById(R.id.fragua_body);
    findViewById(R.id.fragua_menu).setOnClickListener(v -> menu());
    findViewById(R.id.fragua_refresh).setOnClickListener(v -> vm.load());
  }

  private void menu() {
    new AlertDialog.Builder(this)
        .setTitle("Tu aventura")
        .setItems(
            new String[] {
              "Inicio",
              "Cambiar personaje",
              "Crear personaje",
              "Inventario",
              "Notas",
              "Compañeros",
              "Historial / operación pendiente",
              "Cerrar sesión"
            },
            (d, i) -> {
              switch (i) {
                case 0:
                  go("home");
                  break;
                case 1:
                  chooseCharacter();
                  break;
                case 2:
                  startActivity(new Intent(this, CrearPersonajeActivity.class));
                  break;
                case 3:
                  go("inventory");
                  break;
                case 4:
                  go("notes");
                  break;
                case 5:
                  go("companions");
                  break;
                case 6:
                  go("history");
                  break;
                case 7:
                  repo.logout(this::login);
                  break;
              }
            })
        .show();
  }

  private void go(String screen) {
    vm.screen = screen;
    render();
  }

  private void chooseCharacter() {
    JSONArray a = vm.current().characters;
    String[] names = new String[a.length()];
    for (int i = 0; i < a.length(); i++) names[i] = a.optJSONObject(i).optString("name");
    new AlertDialog.Builder(this)
        .setTitle("Personaje activo")
        .setItems(
            names,
            (d, i) -> {
              vm.select(a.optJSONObject(i).optString("id"));
              go("home");
            })
        .setNegativeButton("Cancelar", null)
        .show();
  }

  private void render() {
    if (body == null) return;
    body.removeAllViews();
    if (!vm.screen.equals("home")) {
      body.addView(button(this, "‹  Volver al inicio", () -> go("home")));
    }
    if (vm.screen.equals("notes")) {
      notes();
      return;
    }
    if (vm.screen.equals("missions")) {
      missions();
      return;
    }
    if (sheet() == null) {
      LinearLayout p = panel(this);
      p.addView(text(this, "Tu historia comienza aquí", 25, GOLD));
      p.addView(
          text(
              this,
              "Elegí un personaje o creá uno para abrir tu hoja. Si hay un error de conexión, podés"
                  + " volver a intentar.",
              16,
              MUTED));
      p.addView(button(this, "Elegir personaje", this::chooseCharacter));
      p.addView(
          button(
              this,
              "Crear personaje",
              () -> startActivity(new Intent(this, CrearPersonajeActivity.class))));
      body.addView(p);
      return;
    }
    switch (vm.screen) {
      case "sheet":
        characterSheet();
        break;
      case "inventory":
        inventory();
        break;
      case "companions":
        companions();
        break;
      case "spells":
        spells();
        break;
      case "actions":
        actions("Todas");
        break;
      case "history":
        history();
        break;
      default:
        home();
    }
  }

  private void home() {
    body.addView(
        new HomeScreen(
            this,
            sheet(),
            vm.current().online,
            (destination, item) -> {
              switch (destination) {
                case "configure":
                  configure();
                  break;
                case "dice":
                  dice();
                  break;
                case "rest":
                  restMenu();
                  break;
                case "hp":
                  hpDialog();
                  break;
                case "saves":
                  checks(false);
                  break;
                case "skills":
                  checks(true);
                  break;
                case "attack":
                  roll("attack", item.optString("id"), item.optString("name"));
                  break;
                default:
                  go(destination);
              }
            }));
  }

  private void heading(String text) {
    body.addView(text(this, text, 27, GOLD));
  }

  private void characterSheet() {
    heading(sheet().optJSONObject("identity").optString("name"));
    body.addView(
        text(
            this,
            "Competencia "
                + signed(derived().optInt("proficiency"))
                + " · Iniciativa "
                + signed(derived().optInt("initiative"))
                + " · Percepción pasiva "
                + derived().optInt("passivePerception"),
            16,
            INK));
    JSONArray a = array(derived(), "abilities");
    for (int i = 0; i < a.length(); i++) {
      JSONObject x = a.optJSONObject(i);
      body.addView(
          text(
              this,
              x.optString("label")
                  + "     "
                  + x.optInt("score")
                  + "  ("
                  + signed(x.optInt("modifier"))
                  + ")",
              18,
              INK));
    }
    body.addView(button(this, "Configurar PV, competencias y recursos", this::configure));
    body.addView(button(this, "Gestionar vida", this::hpDialog));
    body.addView(button(this, "Condiciones y concentración", this::conditions));
    body.addView(button(this, "Reducciones temporales", this::reductions));
    JSONArray traits = array(derived(), "classFeatures");
    for (int i = 0; i < traits.length(); i++) {
      JSONObject trait = traits.optJSONObject(i);
      body.addView(
          button(
              this,
              trait.optString("name"),
              () -> {
                LinearLayout f = column(this);
                f.addView(text(this, trait.optString("description"), 16, INK));
                f.addView(text(this, trait.optString("source"), 12, MUTED));
                dialog(trait.optString("name"), f).show();
              }));
    }
    body.addView(
        button(
            this, "Salvaciones contra muerte", () -> roll("death", "", "Salvación contra muerte")));
    body.addView(
        text(
            this,
            "Excepciones de rasgos no configuradas: resolución con el DM. La aplicación distingue"
                + " cálculos básicos y ajustes declarados.",
            14,
            MUTED));
  }

  private AlertDialog dialog(String title, LinearLayout content) {
    ScrollView scroll = new ScrollView(this);
    scroll.addView(content);
    content.setPadding(px(16), px(12), px(16), px(12));
    return new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(scroll)
        .setNegativeButton("Cerrar", null)
        .create();
  }

  private EditText field(LinearLayout form, String name, String value, boolean number) {
    form.addView(text(this, name, 13, MUTED));
    EditText e = input(this, name, value, number);
    form.addView(e);
    return e;
  }

  private int number(EditText e) {
    return Integer.parseInt(e.getText().toString().trim());
  }

  private void attempt(Runnable action) {
    try {
      action.run();
    } catch (Exception e) {
      Toast.makeText(this, "Revisá los valores ingresados.", Toast.LENGTH_LONG).show();
    }
  }

  private void hpDialog() {
    if (sheet() == null) return;
    LinearLayout form = column(this);
    field(
            form,
            "PV actuales / máximos",
            state().optJSONObject("hp").optInt("current")
                + " / "
                + state().optJSONObject("hp").optInt("max"),
            false)
        .setEnabled(false);
    EditText n = field(form, "Cantidad", "1", true);
    CheckBox critical = check(form, "Impacto crítico (relevante a 0 PV)", false);
    AlertDialog d = dialog("Puntos de vida", form);
    for (String mode : new String[] {"damage", "heal", "temporary"}) {
      String label =
          mode.equals("damage")
              ? "Aplicar daño"
              : mode.equals("heal") ? "Curar" : "Reemplazar PV temporales";
      form.addView(
          button(
              this,
              label,
              () ->
                  attempt(
                      () -> {
                        vm.command(
                            "hp",
                            obj(
                                "mode",
                                mode,
                                "amount",
                                number(n),
                                "critical",
                                critical.isChecked()));
                        d.dismiss();
                      })));
    }
    form.addView(
        text(
            this,
            "Ingresá daño ya ajustado por resistencias. Los temporales reemplazan a los anteriores;"
                + " no se suman.",
            13,
            MUTED));
    d.show();
  }

  private void checks(boolean skills) {
    JSONArray a = array(derived(), skills ? "skills" : "saves");
    LinearLayout form = column(this);
    AlertDialog d = dialog(skills ? "Pruebas de habilidad" : "Tiradas de salvación", form);
    if (!skills) {
      String[] labels = new String[a.length()];
      for (int i = 0; i < a.length(); i++) {
        JSONObject x = a.optJSONObject(i);
        labels[i] = x.optString("label") + "\n" + signed(x.optInt("modifier"));
      }
      form.addView(
          new RadialMenu(
              this,
              labels,
              i -> {
                JSONObject x = a.optJSONObject(i);
                d.dismiss();
                roll("save", x.optString("key"), x.optString("label"));
              }));
    } else {
      EditText search = field(form, "Buscar habilidad", "", false);
      LinearLayout list = column(this);
      form.addView(list);
      Runnable fill =
          () -> {
            list.removeAllViews();
            for (int i = 0; i < a.length(); i++) {
              JSONObject x = a.optJSONObject(i);
              if (x.optString("label")
                  .toLowerCase()
                  .contains(search.getText().toString().toLowerCase()))
                list.addView(
                    button(
                        this,
                        x.optString("label") + "  " + signed(x.optInt("modifier")),
                        () -> {
                          d.dismiss();
                          roll("skill", x.optString("key"), x.optString("label"));
                        }));
            }
          };
      search.addTextChangedListener(watcher(fill));
      fill.run();
    }
    d.show();
  }

  private android.text.TextWatcher watcher(Runnable r) {
    return new android.text.TextWatcher() {
      public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

      public void onTextChanged(CharSequence s, int st, int before, int count) {
        r.run();
      }

      public void afterTextChanged(android.text.Editable e) {}
    };
  }

  private void roll(String kind, String key, String label) {
    LinearLayout form = column(this);
    CheckBox advantage = new CheckBox(this), disadvantage = new CheckBox(this);
    advantage.setText("Ventaja");
    disadvantage.setText("Desventaja");
    advantage.setTextColor(INK);
    disadvantage.setTextColor(INK);
    form.addView(advantage);
    form.addView(disadvantage);
    EditText mod = field(form, "Ajuste situacional adicional", "0", true);
    AlertDialog d = dialog(label, form);
    form.addView(
        button(
            this,
            "Tirar d20",
            () ->
                attempt(
                    () -> {
                      vm.command(
                          "roll",
                          obj(
                              "kind",
                              kind,
                              "key",
                              key,
                              "modifier",
                              number(mod),
                              "advantage",
                              advantage.isChecked(),
                              "disadvantage",
                              disadvantage.isChecked()));
                      d.dismiss();
                    })));
    d.show();
  }

  private void dice() {
    LinearLayout form = column(this);
    final int[] sides = {20};
    String[] labels = {"d4", "d6", "d8", "d10", "d12", "d20", "d100"};
    int[] faces = {4, 6, 8, 10, 12, 20, 100};
    TextView selected = text(this, "Seleccionado: d20", 18, GOLD);
    RadialMenu wheel =
        new RadialMenu(
            this,
            labels,
            i -> {
              sides[0] = faces[i];
              selected.setText("Seleccionado: d" + sides[0]);
            });
    form.addView(wheel);
    form.addView(selected);
    EditText count = field(form, "Cantidad (1–50)", "1", true),
        modifier = field(form, "Modificador", "0", true);
    AlertDialog d = dialog("Dados · tirada libre", form);
    form.addView(
        button(
            this,
            "Tirar",
            () ->
                attempt(
                    () -> {
                      vm.command(
                          "roll",
                          obj(
                              "kind",
                              "free",
                              "sides",
                              sides[0],
                              "count",
                              number(count),
                              "modifier",
                              number(modifier)));
                      d.dismiss();
                    })));
    form.addView(
        text(
            this,
            "El percentil d100 produce 1–100. Una tirada libre no modifica vida ni recursos.",
            13,
            MUTED));
    d.show();
  }

  private void restMenu() {
    LinearLayout form = column(this);
    AlertDialog d = dialog("Descansar", form);
    form.addView(
        new RadialMenu(
            this,
            new String[] {"☾\nCorto", "☀\nLargo"},
            i -> {
              d.dismiss();
              rest(i == 1);
            }));
    d.show();
  }

  private void rest(boolean longRest) {
    LinearLayout form = column(this);
    form.addView(
        text(
            this,
            longRest
                ? "Recupera PV, dados de golpe y recursos según su recuperación. Confirmá el tiempo"
                    + " del mundo con el DM."
                : "Requiere una hora sin interrupción. Luego podés gastar dados de golpe de a uno.",
            16,
            INK));
    EditText hours = field(form, "Horas transcurridas", longRest ? "8" : "1", true),
        world =
            field(
                form,
                "Hora acumulada del mundo al comenzar",
                String.valueOf(
                    state().isNull("lastLongRestEnd") ? 0 : state().optInt("lastLongRestEnd") + 16),
                true);
    EditText interruptions = field(form, "Interrupciones tras las que se reanudó", "0", true);
    CheckBox interrupted =
        check(form, "El descanso terminó interrumpido (solo beneficios cortos si pasó 1 h)", false);
    CheckBox trance = new CheckBox(this);
    trance.setText("Usar Trance (solo si está en tu ficha)");
    trance.setTextColor(INK);
    if (longRest) form.addView(trance);
    AlertDialog d = dialog(longRest ? "Descanso largo" : "Descanso corto", form);
    form.addView(
        button(
            this,
            "Confirmar descanso",
            () ->
                attempt(
                    () -> {
                      vm.command(
                          "rest",
                          obj(
                              "kind",
                              longRest ? "long" : "short",
                              "hours",
                              number(hours),
                              "worldHour",
                              number(world),
                              "trance",
                              trance.isChecked(),
                              "interrupted",
                              interrupted.isChecked(),
                              "interruptions",
                              number(interruptions)));
                      d.dismiss();
                    })));
    if (!longRest && state().optBoolean("shortRestOpen")) {
      JSONArray dice = array(derived(), "hitDice");
      for (int i = 0; i < dice.length(); i++) {
        JSONObject x = dice.optJSONObject(i);
        form.addView(
            button(
                this,
                "Gastar d" + x.optInt("die") + " · " + x.optInt("remaining") + " disponibles",
                () -> {
                  vm.command("hitDie", obj("classKey", x.optString("key")));
                  d.dismiss();
                }));
      }
    }
    d.show();
  }

  private String join(JSONArray values) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < values.length(); i++) {
      if (i > 0) result.append(",");
      result.append(values.optString(i));
    }
    return result.toString();
  }

  private void reductions() {
    LinearLayout f = column(this);
    EditText hp = field(f, "Reducción de PV máximos", state().optString("hpReduction", "0"), true);
    JSONArray abilities = array(derived(), "abilities");
    ArrayList<EditText> values = new ArrayList<>();
    JSONObject current = state().optJSONObject("abilityReductions");
    for (int i = 0; i < abilities.length(); i++) {
      JSONObject a = abilities.optJSONObject(i);
      values.add(
          field(
              f,
              a.optString("label"),
              String.valueOf(current == null ? 0 : current.optInt(a.optString("key"))),
              true));
    }
    AlertDialog d = dialog("Reducciones recuperables al descanso largo", f);
    f.addView(
        button(
            this,
            "Guardar",
            () ->
                attempt(
                    () -> {
                      JSONObject reductions = new JSONObject();
                      for (int i = 0; i < values.size(); i++)
                        try {
                          reductions.put(
                              abilities.optJSONObject(i).optString("key"), number(values.get(i)));
                        } catch (JSONException e) {
                          throw new IllegalArgumentException(e);
                        }
                      vm.command("reductions", obj("hp", number(hp), "abilities", reductions));
                      d.dismiss();
                    })));
    d.show();
  }

  private void conditions() {
    LinearLayout form = column(this);
    EditText exhaustion =
        field(form, "Agotamiento 0–6", String.valueOf(state().optInt("exhaustion")), true);
    EditText conditions =
        field(form, "Condiciones separadas por coma", join(array(state(), "conditions")), false);
    CheckBox end = new CheckBox(this);
    end.setText("Finalizar concentración");
    end.setTextColor(INK);
    form.addView(end);
    AlertDialog d = dialog("Estado del personaje", form);
    form.addView(
        button(
            this,
            "Guardar",
            () ->
                attempt(
                    () -> {
                      JSONArray a = new JSONArray();
                      for (String c : conditions.getText().toString().split(","))
                        if (!c.trim().isEmpty()) a.put(c.trim());
                      vm.command(
                          "status",
                          obj(
                              "exhaustion",
                              number(exhaustion),
                              "conditions",
                              a,
                              "endConcentration",
                              end.isChecked()));
                      d.dismiss();
                    })));
    d.show();
  }

  private void configure() {
    LinearLayout form = column(this);
    JSONObject s = state(), hp = s.optJSONObject("hp"), a = s.optJSONObject("armor");
    form.addView(
        text(
            this,
            "Valores declarados de tu hoja. Confirmá con el DM las elecciones y excepciones de tu"
                + " personaje.",
            14,
            MUTED));
    EditText
        max =
            field(
                form,
                "PV máximos sin reducción",
                hp.optInt("max") == 0 ? "" : hp.optString("baseMax", hp.optString("max")),
                true),
        current = field(form, "PV actuales", hp.optString("current"), true),
        xp = field(form, "XP", s.optString("xp"), true),
        speed = field(form, "Velocidad base (ft)", s.optString("speed"), true),
        base = field(form, "Base de CA sin DES ni escudo", a.optString("base"), true),
        cap = field(form, "Máximo aporte DES (99 = sin límite)", a.optString("dexCap"), true);
    CheckBox dex = new CheckBox(this);
    dex.setText("Añadir modificador DES a CA");
    dex.setTextColor(INK);
    dex.setChecked(a.optBoolean("dexterity"));
    form.addView(dex);
    form.addView(
        text(
            this,
            "Niveles por clase (0 si no pertenece). Deben sumar el nivel del personaje.",
            14,
            MUTED));
    ArrayList<EditText> classLevels = new ArrayList<>();
    ArrayList<Spinner> subclasses = new ArrayList<>();
    JSONObject subclassOptions = derived().optJSONObject("subclassOptions");
    for (int i = 0; i < classKeys.length; i++) {
      int level = 0;
      JSONArray existing = array(s, "classes");
      for (int j = 0; j < existing.length(); j++)
        if (existing.optJSONObject(j).optString("key").equals(classKeys[i]))
          level = existing.optJSONObject(j).optInt("level");
      classLevels.add(field(form, classLabels[i], String.valueOf(level), true));
      JSONArray choices =
          subclassOptions == null ? new JSONArray() : array(subclassOptions, classKeys[i]);
      String[] labels = new String[choices.length() + 1];
      labels[0] = "Sin elección de subclase";
      int selectedSubclass = 0;
      for (int j = 0; j < choices.length(); j++) {
        labels[j + 1] = choices.optJSONObject(j).optString("name");
        for (int k = 0; k < existing.length(); k++)
          if (choices
              .optJSONObject(j)
              .optString("key")
              .equals(existing.optJSONObject(k).optString("subclass"))) selectedSubclass = j + 1;
      }
      subclasses.add(options(form, "Subclase SRD (desde nivel 3)", labels, selectedSubclass));
    }

    final JSONObject skills = new JSONObject();
    JSONArray skillData = array(derived(), "skills");
    ArrayList<Spinner> ranks = new ArrayList<>();
    for (int i = 0; i < skillData.length(); i++) {
      JSONObject skill = skillData.optJSONObject(i);
      LinearLayout row = row(this);
      row.addView(
          text(this, skill.optString("label"), 14, INK), new LinearLayout.LayoutParams(0, -2, 1));
      Spinner sp = new Spinner(this);
      sp.setAdapter(
          new ArrayAdapter<>(
              this,
              android.R.layout.simple_spinner_dropdown_item,
              new String[] {"Sin competencia", "Competencia", "Pericia"}));
      sp.setSelection(skill.optInt("rank"));
      row.addView(sp);
      form.addView(row);
      ranks.add(sp);
    }
    ArrayList<CheckBox> saves = new ArrayList<>();
    JSONArray abilities = array(derived(), "abilities");
    for (int i = 0; i < abilities.length(); i++) {
      JSONObject ability = abilities.optJSONObject(i);
      CheckBox check = new CheckBox(this);
      check.setText("Competencia: " + ability.optString("label"));
      check.setTextColor(INK);
      check.setChecked(
          array(s, "saveProficiencies")
              .toString()
              .contains("\"" + ability.optString("key") + "\""));
      saves.add(check);
      form.addView(check);
    }
    CheckBox trance = new CheckBox(this);
    trance.setText("Rasgo Trance verificado");
    trance.setTextColor(INK);
    trance.setChecked(array(s, "features").toString().contains("trance"));
    form.addView(trance);
    AlertDialog dialog = dialog("Completar ficha", form);
    form.addView(
        button(
            this,
            "Guardar configuración",
            () ->
                attempt(
                    () -> {
                      JSONArray selectedClasses = new JSONArray();
                      for (int j = 0; j < classKeys.length; j++) {
                        int lvl = number(classLevels.get(j));
                        if (lvl < 0) throw new IllegalArgumentException();
                        if (lvl > 0) {
                          JSONObject cl = obj("key", classKeys[j], "level", lvl);
                          int choice = subclasses.get(j).getSelectedItemPosition();
                          if (choice > 0)
                            try {
                              cl.put(
                                  "subclass",
                                  array(subclassOptions, classKeys[j])
                                      .optJSONObject(choice - 1)
                                      .optString("key"));
                            } catch (JSONException e) {
                              throw new IllegalArgumentException(e);
                            }
                          selectedClasses.put(cl);
                        }
                      }
                      JSONArray saveKeys = new JSONArray();
                      for (int i = 0; i < saves.size(); i++)
                        if (saves.get(i).isChecked())
                          saveKeys.put(abilities.optJSONObject(i).optString("key"));
                      for (int i = 0; i < ranks.size(); i++)
                        try {
                          skills.put(
                              skillData.optJSONObject(i).optString("key"),
                              ranks.get(i).getSelectedItemPosition());
                        } catch (Exception ignored) {
                        }
                      vm.command(
                          "configure",
                          obj(
                              "classes",
                              selectedClasses,
                              "hp",
                              obj(
                                  "max",
                                  number(max),
                                  "current",
                                  number(current),
                                  "temp",
                                  hp.optInt("temp")),
                              "xp",
                              number(xp),
                              "speed",
                              number(speed),
                              "armor",
                              obj(
                                  "base",
                                  number(base),
                                  "dexterity",
                                  dex.isChecked(),
                                  "dexCap",
                                  number(cap),
                                  "shield",
                                  0,
                                  "bonus",
                                  0),
                              "skills",
                              skills,
                              "saveProficiencies",
                              saveKeys,
                              "resources",
                              array(s, "resources"),
                              "features",
                              trance.isChecked()
                                  ? new JSONArray().put("trance")
                                  : new JSONArray()));
                      dialog.dismiss();
                    })));
    dialog.show();
  }

  private void inventory() {
    heading("Inventario");
    body.addView(button(this, "+ Añadir objeto", () -> itemEditor(null)));
    JSONArray a = array(state(), "inventory");
    if (a.length() == 0)
      body.addView(
          text(
              this,
              "Tu inventario está vacío. Agregá equipo para obtener sus acciones.",
              16,
              MUTED));
    for (int i = 0; i < a.length(); i++) {
      JSONObject x = a.optJSONObject(i);
      LinearLayout card = panel(this);
      card.addView(text(this, x.optString("name") + "  ×" + x.optInt("quantity"), 21, GOLD));
      card.addView(
          text(
              this,
              (x.optBoolean("equipped") ? "Equipado · " : "") + x.optString("notes"),
              14,
              MUTED));
      card.addView(button(this, "Gestionar", () -> itemEditor(x)));
      body.addView(card);
    }
  }

  private Spinner options(LinearLayout f, String label, String[] items, int selected) {
    f.addView(text(this, label, 13, MUTED));
    Spinner sp = new Spinner(this);
    sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
    sp.setSelection(Math.max(0, selected));
    f.addView(sp);
    return sp;
  }

  private CheckBox check(LinearLayout f, String label, boolean value) {
    CheckBox c = new CheckBox(this);
    c.setText(label);
    c.setTextColor(INK);
    c.setChecked(value);
    f.addView(c);
    return c;
  }

  private void itemEditor(JSONObject existing) {
    JSONObject x = existing == null ? new JSONObject() : existing;
    LinearLayout f = column(this);
    EditText name = field(f, "Nombre", x.optString("name"), false),
        qty = field(f, "Cantidad", x.optString("quantity", "1"), true);
    String[] types = {"gear", "weapon", "armor", "shield"};
    Spinner type =
        options(
            f,
            "Tipo",
            new String[] {"Equipo", "Arma", "Armadura", "Escudo"},
            Arrays.asList(types).indexOf(x.optString("type")));
    CheckBox equipped = check(f, "Equipado", x.optBoolean("equipped")),
        proficient = check(f, "Competente con esta arma", x.optBoolean("proficient")),
        finesse = check(f, "Sutil", x.optBoolean("finesse")),
        ranged = check(f, "Ataque a distancia (DES)", x.optBoolean("ranged"));
    EditText damage = field(f, "Dados de daño (ej. 1d6)", x.optString("damage", "1d4"), false),
        range = field(f, "Alcance", x.optString("range", "5 ft"), false),
        notes = field(f, "Propiedades y notas", x.optString("notes"), false);
    EditText
        armorBase =
            field(
                f,
                "Base CA de armadura",
                x.optJSONObject("armor") == null
                    ? "10"
                    : x.optJSONObject("armor").optString("base"),
                true),
        dexCap =
            field(
                f,
                "Máximo DES (0 pesada, 2 media, 99 ligera)",
                x.optJSONObject("armor") == null
                    ? "99"
                    : x.optJSONObject("armor").optString("dexCap"),
                true);
    AlertDialog d = dialog("Objeto", f);
    f.addView(
        button(
            this,
            "Guardar objeto",
            () ->
                attempt(
                    () -> {
                      JSONObject item =
                          obj(
                              "id",
                              x.optString("id", UUID.randomUUID().toString()),
                              "name",
                              name.getText().toString(),
                              "type",
                              types[type.getSelectedItemPosition()],
                              "quantity",
                              number(qty),
                              "equipped",
                              equipped.isChecked(),
                              "proficient",
                              proficient.isChecked(),
                              "finesse",
                              finesse.isChecked(),
                              "ranged",
                              ranged.isChecked(),
                              "damage",
                              damage.getText().toString(),
                              "range",
                              range.getText().toString(),
                              "notes",
                              notes.getText().toString(),
                              "bonus",
                              x.optInt("bonus"));
                      if (type.getSelectedItemPosition() == 2)
                        try {
                          item.put(
                              "armor",
                              obj(
                                  "base",
                                  number(armorBase),
                                  "dexterity",
                                  true,
                                  "dexCap",
                                  number(dexCap),
                                  "shield",
                                  0,
                                  "bonus",
                                  0));
                        } catch (Exception ignored) {
                        }
                      vm.command("inventory", obj("operation", "upsert", "item", item));
                      d.dismiss();
                    })));
    if (existing != null)
      f.addView(
          button(
              this,
              "Eliminar",
              () ->
                  confirm(
                      "¿Eliminar este objeto?",
                      () -> {
                        vm.command(
                            "inventory", obj("operation", "remove", "id", x.optString("id")));
                        d.dismiss();
                      })));
    d.show();
  }

  private void confirm(String text, Runnable yes) {
    new AlertDialog.Builder(this)
        .setMessage(text)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Confirmar", (d, w) -> yes.run())
        .show();
  }

  private void companions() {
    heading("Compañeros y familiares");
    body.addView(
        text(
            this,
            "Cada vínculo necesita una fuente. Una mascota narrativa no concede acciones de clase.",
            15,
            MUTED));
    body.addView(button(this, "+ Registrar vínculo", () -> companionEditor(null)));
    JSONArray a = array(state(), "companions");
    for (int i = 0; i < a.length(); i++) {
      JSONObject x = a.optJSONObject(i);
      LinearLayout card = panel(this);
      card.addView(text(this, "♧  " + x.optString("name"), 23, GOLD));
      card.addView(
          text(
              this,
              x.optInt("hp") + " / " + x.optInt("maxHp") + " PV · " + x.optString("kind"),
              16,
              INK));
      card.addView(text(this, x.optString("source") + "\n" + x.optString("notes"), 14, MUTED));
      card.addView(button(this, "Abrir vínculo", () -> companionEditor(x)));
      body.addView(card);
    }
  }

  private void companionEditor(JSONObject existing) {
    JSONObject x = existing == null ? new JSONObject() : existing;
    LinearLayout f = column(this);
    EditText name = field(f, "Nombre", x.optString("name"), false),
        source = field(f, "Fuente o aprobación del DM", x.optString("source"), false),
        hp = field(f, "PV actuales", x.optString("hp", "1"), true),
        max = field(f, "PV máximos", x.optString("maxHp", "1"), true),
        notes = field(f, "Ficha, acciones y restricciones", x.optString("notes"), false);
    String[] kinds = {"pet", "familiar", "class", "summon"};
    Spinner kind =
        options(
            f,
            "Tipo",
            new String[] {"Mascota narrativa", "Familiar", "Compañero de clase", "Invocación"},
            Arrays.asList(kinds).indexOf(x.optString("kind")));
    CheckBox active = check(f, "Mostrar en inicio", x.optBoolean("active", true));
    f.addView(
        text(
            this,
            "Find Familiar no concede ataques normalmente. Resolvé excepciones y órdenes con el"
                + " DM.",
            14,
            MUTED));
    AlertDialog d = dialog("Compañero", f);
    f.addView(
        button(
            this,
            "Guardar",
            () ->
                attempt(
                    () -> {
                      vm.command(
                          "companion",
                          obj(
                              "operation",
                              "upsert",
                              "companion",
                              obj(
                                  "id",
                                  x.optString("id", UUID.randomUUID().toString()),
                                  "name",
                                  name.getText().toString(),
                                  "source",
                                  source.getText().toString(),
                                  "kind",
                                  kinds[kind.getSelectedItemPosition()],
                                  "hp",
                                  number(hp),
                                  "maxHp",
                                  number(max),
                                  "notes",
                                  notes.getText().toString(),
                                  "active",
                                  active.isChecked())));
                      d.dismiss();
                    })));
    if (existing != null)
      f.addView(
          button(
              this,
              "Eliminar vínculo",
              () ->
                  confirm(
                      "¿Eliminar el vínculo?",
                      () -> {
                        vm.command(
                            "companion", obj("operation", "remove", "id", x.optString("id")));
                        d.dismiss();
                      })));
    d.show();
  }

  private void arcaneRecovery() {
    LinearLayout f = column(this);
    f.addView(
        text(
            this,
            "Una vez por descanso largo, al finalizar uno corto. Indicá los niveles de espacios"
                + " gastados a recuperar (ejemplo: 1,1 o 2). Ninguno puede superar nivel 5.",
            16,
            INK));
    EditText slots = field(f, "Niveles separados por coma", "1", false);
    AlertDialog d = dialog("Recuperación arcana", f);
    f.addView(
        button(
            this,
            "Recuperar",
            () ->
                attempt(
                    () -> {
                      JSONArray levels = new JSONArray();
                      for (String n : slots.getText().toString().split(","))
                        levels.put(Integer.parseInt(n.trim()));
                      vm.command("arcaneRecovery", obj("slots", levels));
                      d.dismiss();
                    })));
    d.show();
  }

  private void spells() {
    body.addView(new com.miapp.dndcompanion.fragua.SpellsScreen(this, sheet(), this::spellDetail));
    body.addView(button(this, "+ Añadir conjuro de mi hoja", () -> spellEditor(null)));
    body.addView(button(this, "Explorar catálogo 2024", this::catalog));
    JSONObject recovery = derived().optJSONObject("arcaneRecovery");
    if (recovery != null && recovery.optInt("limit") > 0)
      body.addView(
          button(
              this,
              "Recuperación arcana · hasta " + recovery.optInt("limit") + " niveles",
              this::arcaneRecovery));
    body.addView(text(this, "RECURSOS DE CLASE", 18, GOLD));
    JSONArray ares = array(state(), "resources");
    for (int i = 0; i < ares.length(); i++) {
      JSONObject r = ares.optJSONObject(i);
      body.addView(
          button(
              this,
              r.optString("name")
                  + "  "
                  + r.optInt("current")
                  + "/"
                  + r.optInt("max")
                  + " · Usar 1",
              () -> vm.command("resource", obj("id", r.optString("id"), "amount", 1))));
    }
    body.addView(button(this, "+ Configurar recurso", this::resourceEditor));
  }

  private void spellDetail(JSONObject spell) {
    LinearLayout f = column(this);
    f.addView(text(this, spell.optString("description"), 16, INK));
    f.addView(text(this, "Fuente: " + spell.optString("source"), 12, MUTED));
    AlertDialog d = dialog(spell.optString("name"), f);
    f.addView(
        button(
            this,
            "Lanzar",
            () -> {
              d.dismiss();
              cast(spell);
            }));
    f.addView(
        button(
            this,
            "Editar preparación / datos",
            () -> {
              d.dismiss();
              spellEditor(spell);
            }));
    d.show();
  }

  private void cast(JSONObject spell) {
    LinearLayout f = column(this);
    JSONArray slots = array(derived(), "slots");
    ArrayList<JSONObject> eligible = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>();
    for (int i = 0; i < slots.length(); i++) {
      JSONObject x = slots.optJSONObject(i);
      if (x.optInt("level") >= spell.optInt("level") && x.optInt("remaining") > 0) {
        eligible.add(x);
        labels.add(
            "Nivel "
                + x.optInt("level")
                + " · "
                + x.optInt("remaining")
                + " disponibles"
                + (x.has("pool") ? " · Pacto" : ""));
      }
    }
    Spinner choose =
        options(
            f,
            "Espacio",
            labels.isEmpty() ? new String[] {"Sin espacios"} : labels.toArray(new String[0]),
            0);
    CheckBox ritual = check(f, "Ritual (+10 minutos, si corresponde)", false),
        replace = check(f, "Confirmar reemplazo de concentración", false);
    f.addView(
        text(
            this,
            "Confirmá componentes y restricciones del turno. La app registra el gasto; no decide el"
                + " contexto del combate.",
            14,
            MUTED));
    AlertDialog d = dialog("Lanzar " + spell.optString("name"), f);
    f.addView(
        button(
            this,
            "Confirmar lanzamiento",
            () -> {
              JSONObject slot =
                  eligible.isEmpty()
                      ? obj("level", 0)
                      : eligible.get(choose.getSelectedItemPosition());
              vm.command(
                  "cast",
                  obj(
                      "id",
                      spell.optString("id"),
                      "slotLevel",
                      slot.optInt("level"),
                      "pool",
                      slot.optString("pool", "standard"),
                      "ritual",
                      ritual.isChecked(),
                      "replaceConcentration",
                      replace.isChecked()));
              d.dismiss();
            }));
    d.show();
  }

  private void spellEditor(JSONObject existing) {
    JSONObject x = existing == null ? new JSONObject() : existing;
    LinearLayout f = column(this);
    EditText name = field(f, "Nombre", x.optString("name"), false),
        level = field(f, "Nivel base (0 = truco)", x.optString("level", "0"), true),
        source =
            field(
                f, "Fuente y versión", x.optString("source", "SRD 5.2.1 · entrada manual"), false),
        time = field(f, "Tiempo de lanzamiento", x.optString("castingTime", "1 acción"), false),
        range = field(f, "Alcance", x.optString("range"), false),
        desc = field(f, "Descripción y efectos", x.optString("description"), false);
    desc.setMinLines(4);
    Spinner origin =
        options(
            f,
            "Origen de clase",
            classLabels,
            Arrays.asList(classKeys).indexOf(x.optString("origin", "wizard")));
    CheckBox inBook = check(f, "Está en mi libro de conjuros de mago", x.optBoolean("inSpellbook"));
    CheckBox prepared = check(f, "Preparado / disponible", x.optBoolean("prepared", true)),
        concentration = check(f, "Concentración", x.optBoolean("concentration")),
        ritual = check(f, "Ritual", x.optBoolean("ritual"));
    AlertDialog d = dialog("Conjuro del personaje", f);
    f.addView(
        button(
            this,
            "Guardar conjuro",
            () ->
                attempt(
                    () -> {
                      vm.command(
                          "spell",
                          obj(
                              "operation",
                              "upsert",
                              "spell",
                              obj(
                                  "id",
                                  x.optString("id", UUID.randomUUID().toString()),
                                  "name",
                                  name.getText().toString(),
                                  "level",
                                  number(level),
                                  "source",
                                  source.getText().toString(),
                                  "origin",
                                  classKeys[origin.getSelectedItemPosition()],
                                  "castingTime",
                                  time.getText().toString(),
                                  "range",
                                  range.getText().toString(),
                                  "description",
                                  desc.getText().toString(),
                                  "inSpellbook",
                                  inBook.isChecked(),
                                  "prepared",
                                  prepared.isChecked(),
                                  "concentration",
                                  concentration.isChecked(),
                                  "ritual",
                                  ritual.isChecked())));
                      d.dismiss();
                    })));
    if (existing != null && x.has("id"))
      f.addView(
          button(
              this,
              "Quitar de mi hoja",
              () -> {
                vm.command("spell", obj("operation", "remove", "id", x.optString("id")));
                d.dismiss();
              }));
    d.show();
  }

  private void resourceEditor() {
    LinearLayout f = column(this);
    EditText name = field(f, "Nombre del rasgo", "", false),
        max = field(f, "Usos máximos", "1", true),
        current = field(f, "Usos disponibles", "1", true);
    Spinner recovery =
        options(f, "Recuperación", new String[] {"Corto y largo", "Largo", "Manual"}, 1);
    AlertDialog d = dialog("Recurso de clase", f);
    f.addView(
        button(
            this,
            "Guardar",
            () ->
                attempt(
                    () -> {
                      JSONArray resources = new JSONArray();
                      JSONArray old = array(state(), "resources");
                      for (int i = 0; i < old.length(); i++) resources.put(old.optJSONObject(i));
                      resources.put(
                          obj(
                              "id",
                              UUID.randomUUID().toString(),
                              "name",
                              name.getText().toString(),
                              "max",
                              number(max),
                              "current",
                              number(current),
                              "recovery",
                              new String[] {"short", "long", "manual"}
                                  [recovery.getSelectedItemPosition()]));
                      JSONObject s = state();
                      vm.command(
                          "configure",
                          obj(
                              "classes",
                              array(s, "classes"),
                              "hp",
                              obj(
                                  "current",
                                  s.optJSONObject("hp").optInt("current"),
                                  "max",
                                  s.optJSONObject("hp")
                                      .optInt("baseMax", s.optJSONObject("hp").optInt("max")),
                                  "temp",
                                  s.optJSONObject("hp").optInt("temp")),
                              "xp",
                              s.optInt("xp"),
                              "speed",
                              s.optInt("speed"),
                              "armor",
                              s.optJSONObject("armor"),
                              "skills",
                              s.optJSONObject("skills"),
                              "saveProficiencies",
                              array(s, "saveProficiencies"),
                              "resources",
                              resources,
                              "features",
                              array(s, "features")));
                      d.dismiss();
                    })));
    d.show();
  }

  private void actions(String selected) {
    heading("Acciones");
    body.addView(
        text(
            this,
            "Ataques por acción de Atacar: " + derived().optInt("attacksPerAction"),
            17,
            GOLD));
    for (String category : new String[] {"Acción", "Acción adicional", "Reacción", "Otras"}) {
      body.addView(text(this, category.toUpperCase(Locale.ROOT), 18, GOLD));
      if (category.equals("Acción")) {
        JSONArray attacks = array(derived(), "attacks");
        for (int i = 0; i < attacks.length(); i++) {
          JSONObject a = attacks.optJSONObject(i);
          LinearLayout card = panel(this);
          card.addView(text(this, a.optString("name"), 22, GOLD));
          card.addView(
              text(
                  this,
                  a.optString("range")
                      + " · Impacto "
                      + signed(a.optInt("attack"))
                      + " · Daño "
                      + a.optString("damage"),
                  16,
                  INK));
          card.addView(text(this, a.optString("notes"), 14, MUTED));
          card.addView(
              button(
                  this,
                  "Tirar ataque",
                  () -> roll("attack", a.optString("id"), a.optString("name"))));
          body.addView(card);
        }
      }
      JSONArray rules = array(derived(), "actions");
      for (int i = 0; i < rules.length(); i++) {
        JSONObject a = rules.optJSONObject(i);
        if (!a.optString("category").equals(category)) continue;
        LinearLayout card = panel(this);
        card.addView(text(this, a.optString("name"), 21, GOLD));
        card.addView(text(this, a.optString("description"), 15, INK));
        body.addView(card);
      }
      JSONArray spells = array(state(), "spells");
      for (int i = 0; i < spells.length(); i++) {
        JSONObject s = spells.optJSONObject(i);
        if (!s.optBoolean("prepared")) continue;
        String time = s.optString("castingTime").toLowerCase(Locale.ROOT);
        String group =
            time.contains("bonus") || time.contains("adicional")
                ? "Acción adicional"
                : time.contains("reaction") || time.contains("reacción")
                    ? "Reacción"
                    : time.contains("minute") || time.contains("hora") ? "Otras" : "Acción";
        if (group.equals(category))
          body.addView(
              button(
                  this,
                  "✧ " + s.optString("name") + " · " + s.optString("castingTime"),
                  () -> spellDetail(s)));
      }
    }
    body.addView(
        text(
            this,
            "Las circunstancias, objetivos y excepciones se resuelven con el DM. Consultá el"
                + " recurso de cada rasgo antes de usarlo.",
            14,
            MUTED));
    body.addView(button(this, "Recursos y rasgos", () -> go("spells")));
  }

  private void history() {
    heading("Historial");
    body.addView(button(this, "Comprobar / reintentar operación pendiente", vm::reconcile));
    LinearLayout list = column(this);
    body.addView(list);
    repo.read(
        "/characters/" + vm.selected() + "/commands",
        new GameRepository.Result() {
          public void ok(Object data, boolean cached, long at) {
            list.removeAllViews();
            JSONArray a = (JSONArray) data;
            if (a.length() == 0)
              list.addView(
                  text(MainActivity.this, "Todavía no hay operaciones registradas.", 16, MUTED));
            for (int i = 0; i < a.length(); i++) {
              JSONObject entry = a.optJSONObject(i);
              LinearLayout card = panel(MainActivity.this);
              card.addView(text(MainActivity.this, entry.optString("createdAt"), 13, MUTED));
              card.addView(
                  text(
                      MainActivity.this,
                      entry.optString("type", "Operación")
                          + "\n"
                          + readable(entry.optJSONObject("result")),
                      16,
                      INK));
              list.addView(card);
            }
          }

          public void error(String m, int code) {
            Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show();
          }
        });
  }

  private String readable(JSONObject r) {
    if (r == null || r.length() == 0) return "Guardado";
    if (r.has("total"))
      return "Dados "
          + r.optJSONArray("dice")
          + "  "
          + signed(r.optInt("modifier"))
          + "  →  "
          + r.optInt("total");
    if (r.has("healing"))
      return "Dados " + r.optJSONArray("dice") + " · Curación: " + r.optInt("healing");
    if (r.has("spell")) return r.optString("spell") + "\n" + r.optString("reminder");
    if (r.has("concentrationSaveDc"))
      return "Salvación de Constitución para mantener concentración: CD "
          + r.optInt("concentrationSaveDc");
    return r.optString("reminder", "Guardado");
  }

  private void missions() {
    heading("Misiones");
    LinearLayout list = column(this);
    body.addView(list);
    repo.read(
        "/missions",
        new GameRepository.Result() {
          public void ok(Object data, boolean cached, long at) {
            list.removeAllViews();
            JSONArray a = (JSONArray) data;
            if (a.length() == 0)
              list.addView(
                  text(
                      MainActivity.this,
                      "No hay misiones disponibles en tus campañas.",
                      16,
                      MUTED));
            for (int i = 0; i < a.length(); i++) {
              JSONObject m = a.optJSONObject(i);
              LinearLayout card = panel(MainActivity.this);
              card.addView(text(MainActivity.this, m.optString("title"), 23, GOLD));
              card.addView(
                  text(
                      MainActivity.this,
                      m.optString("status")
                          + " · "
                          + m.optInt("rewardXp")
                          + " XP · "
                          + m.optInt("rewardGold")
                          + " oro",
                      14,
                      MUTED));
              card.addView(text(MainActivity.this, m.optString("description"), 16, INK));
              card.addView(text(MainActivity.this, m.optString("reward"), 14, GOLD));
              boolean
                  assigned =
                      m.optJSONArray("assignedTo") != null
                          && m.optJSONArray("assignedTo").toString().contains(repo.account()),
                  accepted =
                      m.optJSONArray("acceptedBy") != null
                          && m.optJSONArray("acceptedBy").toString().contains(repo.account());
              if (assigned && !accepted && !m.optString("status").equals("completed")) {
                TextView accept =
                    button(
                        MainActivity.this,
                        "Aceptar misión",
                        () ->
                            repo.remote(
                                "POST",
                                "/missions/" + m.optString("id") + "/accept",
                                obj(),
                                simple(() -> go("missions"))));
                accept.setEnabled(!cached);
                card.addView(accept);
              }
              list.addView(card);
            }
          }

          public void error(String m, int status) {
            list.addView(text(MainActivity.this, m + " · solo consulta", 14, MUTED));
          }
        });
  }

  private GameRepository.Result simple(Runnable success) {
    return new GameRepository.Result() {
      public void ok(Object data, boolean cached, long at) {
        success.run();
      }

      public void error(String m, int code) {
        Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show();
      }
    };
  }

  private void catalog() {
    LinearLayout form = column(this);
    form.addView(
        text(
            this,
            "Open5e · SRD 2024. Las descripciones se conservan en su idioma original.",
            14,
            MUTED));
    EditText search = field(form, "Buscar nombre (inglés)", "", false);
    LinearLayout results = column(this);
    AlertDialog d = dialog("Catálogo de hechizos", form);
    final int[] page = {1};
    Runnable load =
        () -> {
          String query = android.net.Uri.encode(search.getText().toString().trim());
          repo.read(
              "/catalog/spells?q=" + query + "&page=" + page[0],
              new GameRepository.Result() {
                public void ok(Object data, boolean cached, long at) {
                  results.removeAllViews();
                  JSONObject response = (JSONObject) data;
                  JSONArray rows = array(response, "results");
                  for (int i = 0; i < rows.length(); i++) {
                    JSONObject spell = rows.optJSONObject(i);
                    results.addView(
                        button(
                            MainActivity.this,
                            spell.optString("name") + " · Nivel " + spell.optInt("level"),
                            () -> {
                              d.dismiss();
                              spellEditor(spell);
                            }));
                  }
                  if (rows.length() == 0)
                    results.addView(text(MainActivity.this, "Sin resultados.", 15, MUTED));
                  if (response.optBoolean("hasNext"))
                    results.addView(
                        button(
                            MainActivity.this,
                            "Página siguiente",
                            () -> {
                              page[0]++;
                              loadCatalogPage(form, results, search, page, d);
                            }));
                }

                public void error(String message, int code) {
                  results.addView(text(MainActivity.this, message, 15, MUTED));
                }
              });
        };
    form.addView(
        button(
            this,
            "Buscar",
            () -> {
              page[0] = 1;
              load.run();
            }));
    form.addView(results);
    d.show();
    load.run();
  }

  private void loadCatalogPage(
      LinearLayout form, LinearLayout results, EditText search, int[] page, AlertDialog d) {
    repo.read(
        "/catalog/spells?q="
            + android.net.Uri.encode(search.getText().toString().trim())
            + "&page="
            + page[0],
        new GameRepository.Result() {
          public void ok(Object data, boolean cached, long at) {
            results.removeAllViews();
            JSONObject response = (JSONObject) data;
            JSONArray rows = array(response, "results");
            for (int i = 0; i < rows.length(); i++) {
              JSONObject spell = rows.optJSONObject(i);
              results.addView(
                  button(
                      MainActivity.this,
                      spell.optString("name") + " · Nivel " + spell.optInt("level"),
                      () -> {
                        d.dismiss();
                        spellEditor(spell);
                      }));
            }
            if (page[0] > 1)
              results.addView(
                  button(
                      MainActivity.this,
                      "Página anterior",
                      () -> {
                        page[0]--;
                        loadCatalogPage(form, results, search, page, d);
                      }));
            if (response.optBoolean("hasNext"))
              results.addView(
                  button(
                      MainActivity.this,
                      "Página siguiente",
                      () -> {
                        page[0]++;
                        loadCatalogPage(form, results, search, page, d);
                      }));
          }

          public void error(String m, int code) {
            Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show();
          }
        });
  }

  private void notes() {
    heading("Cuaderno de aventura");
    LinearLayout content = column(this);
    body.addView(content);
    repo.read(
        "/notebooks?page=" + vm.notePage,
        new GameRepository.Result() {
          public void ok(Object data, boolean cached, long at) {
            content.removeAllViews();
            JSONObject book = (JSONObject) data;
            JSONArray sections = array(book, "sections"), notes = array(book, "notes");
            content.addView(
                text(
                    MainActivity.this,
                    cached
                        ? "Copia local · solo consulta"
                        : "Texto y dibujos · guardado al confirmar",
                    14,
                    MUTED));
            TextView add = button(MainActivity.this, "+ Nueva sección", () -> sectionEditor(null));
            add.setEnabled(!cached);
            content.addView(add);
            for (int i = 0; i < sections.length(); i++) {
              JSONObject section = sections.optJSONObject(i);
              LinearLayout group = panel(MainActivity.this);
              group.addView(
                  text(
                      MainActivity.this,
                      section.optString("name").toUpperCase(Locale.ROOT),
                      20,
                      GOLD));
              TextView edit =
                  button(MainActivity.this, "Gestionar sección", () -> sectionEditor(section));
              edit.setEnabled(!cached);
              group.addView(edit);
              for (int j = 0; j < notes.length(); j++) {
                JSONObject note = notes.optJSONObject(j);
                if (note.optString("sectionId").equals(section.optString("id")))
                  group.addView(
                      button(
                          MainActivity.this,
                          note.optString("title") + "  ›",
                          () -> openNote(note.optString("id"), sections)));
              }
              TextView newNote =
                  button(
                      MainActivity.this,
                      "+ Nueva nota",
                      () ->
                          noteEditor(
                              obj(
                                  "id",
                                  UUID.randomUUID().toString(),
                                  "title",
                                  "",
                                  "text",
                                  "",
                                  "version",
                                  -1,
                                  "sectionId",
                                  section.optString("id"),
                                  "strokes",
                                  new JSONArray()),
                              sections,
                              true));
              newNote.setEnabled(!cached);
              group.addView(newNote);
              content.addView(group);
            }
            if (sections.length() == 0)
              content.addView(
                  text(
                      MainActivity.this,
                      "Creá secciones para personajes, lugares, misiones y tus apuntes.",
                      16,
                      MUTED));
            if (book.optBoolean("hasMore"))
              content.addView(
                  text(MainActivity.this, "Hay más notas en la página siguiente.", 14, MUTED));
            if (vm.notePage > 1)
              content.addView(
                  button(
                      MainActivity.this,
                      "Notas anteriores",
                      () -> {
                        vm.notePage--;
                        go("notes");
                      }));
            if (book.optBoolean("hasMore"))
              content.addView(
                  button(
                      MainActivity.this,
                      "Más notas",
                      () -> {
                        vm.notePage++;
                        go("notes");
                      }));
          }

          public void error(String m, int code) {
            content.addView(text(MainActivity.this, m + " · solo consulta", 14, MUTED));
          }
        });
  }

  private void sectionEditor(JSONObject existing) {
    JSONObject s =
        existing == null ? obj("id", UUID.randomUUID().toString(), "order", 0) : existing;
    LinearLayout f = column(this);
    EditText name = field(f, "Nombre", s.optString("name"), false),
        order = field(f, "Orden", s.optString("order", "0"), true);
    AlertDialog d = dialog("Sección", f);
    f.addView(
        button(
            this,
            "Guardar",
            () ->
                attempt(
                    () ->
                        repo.remote(
                            "PUT",
                            "/notebooks/sections/" + s.optString("id"),
                            obj("name", name.getText().toString(), "order", number(order)),
                            simple(
                                () -> {
                                  d.dismiss();
                                  go("notes");
                                })))));
    if (existing != null)
      f.addView(
          button(
              this,
              "Eliminar sección vacía",
              () ->
                  confirm(
                      "¿Eliminar esta sección?",
                      () ->
                          repo.remote(
                              "DELETE",
                              "/notebooks/sections/" + s.optString("id"),
                              null,
                              simple(
                                  () -> {
                                    d.dismiss();
                                    go("notes");
                                  })))));
    d.show();
  }

  private void openNote(String id, JSONArray sections) {
    final AlertDialog[] opened = {null};
    repo.read(
        "/notebooks/notes/" + id,
        new GameRepository.Result() {
          public void ok(Object data, boolean cached, long at) {
            if (opened[0] != null) {
              if (!opened[0].isShowing()) return;
              opened[0].dismiss();
            }
            opened[0] = noteEditor((JSONObject) data, sections, !cached);
          }

          public void error(String m, int code) {
            Toast.makeText(MainActivity.this, m + " · copia local solo consulta", Toast.LENGTH_LONG)
                .show();
          }
        });
  }

  private Runnable captureNote;

  private AlertDialog noteEditor(JSONObject note, JSONArray sections, boolean editable) {
    LinearLayout f = column(this);
    EditText title = field(f, "Título", note.optString("title"), false);
    String[] labels = new String[sections.length()];
    int selected = 0;
    for (int i = 0; i < sections.length(); i++) {
      JSONObject s = sections.optJSONObject(i);
      labels[i] = s.optString("name");
      if (s.optString("id").equals(note.optString("sectionId"))) selected = i;
    }
    Spinner section = options(f, "Sección", labels, selected);
    EditText content = field(f, "Escribir", note.optString("text"), false);
    content.setMinLines(5);
    content.setGravity(Gravity.TOP);
    content.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    title.setEnabled(editable);
    section.setEnabled(editable);
    content.setEnabled(editable);
    DrawingCanvas canvas = new DrawingCanvas(this, array(note, "strokes"), editable);
    if (editable) {
      LinearLayout tools = row(this);
      tools.addView(button(this, "↶", canvas::undo), new LinearLayout.LayoutParams(0, px(48), 1));
      tools.addView(button(this, "↷", canvas::redo), new LinearLayout.LayoutParams(0, px(48), 1));
      tools.addView(
          button(this, "Limpiar", () -> confirm("¿Limpiar el dibujo?", canvas::clear)),
          new LinearLayout.LayoutParams(0, px(48), 2));
      f.addView(tools);
    }
    f.addView(canvas, new LinearLayout.LayoutParams(-1, px(340)));
    AlertDialog d = dialog(editable ? "Editar nota" : "Consultar nota", f);
    captureNote =
        () -> {
          JSONObject draft =
              obj(
                  "id",
                  note.optString("id"),
                  "version",
                  note.optInt("version", -1),
                  "title",
                  title.getText().toString(),
                  "text",
                  content.getText().toString(),
                  "sectionId",
                  sections.optJSONObject(section.getSelectedItemPosition()).optString("id"),
                  "strokes",
                  canvas.strokes());
          vm.noteDraft = obj("note", draft, "sections", sections, "editable", editable);
        };
    if (editable) {
      TextView save =
          button(
              this,
              "Guardar nota",
              () -> {
                captureNote.run();
                JSONObject draft = vm.noteDraft.optJSONObject("note");
                JSONObject payload =
                    obj(
                        "title",
                        draft.optString("title"),
                        "text",
                        draft.optString("text"),
                        "sectionId",
                        draft.optString("sectionId"),
                        "strokes",
                        array(draft, "strokes"));
                repo.remote(
                    "PUT",
                    "/notebooks/notes/" + note.optString("id"),
                    obj("expectedVersion", note.optInt("version", -1), "note", payload),
                    new GameRepository.Result() {
                      public void ok(Object data, boolean c, long at) {
                        vm.noteDraft = null;
                        captureNote = null;
                        d.dismiss();
                        go("notes");
                      }

                      public void error(String m, int code) {
                        Toast.makeText(
                                MainActivity.this,
                                m + ". Tu borrador sigue abierto.",
                                Toast.LENGTH_LONG)
                            .show();
                      }
                    });
              });
      f.addView(save);
      if (note.optInt("version", -1) >= 0)
        f.addView(
            button(
                this,
                "Eliminar nota",
                () ->
                    confirm(
                        "¿Eliminar esta nota y su dibujo?",
                        () ->
                            repo.remote(
                                "DELETE",
                                "/notebooks/notes/"
                                    + note.optString("id")
                                    + "?version="
                                    + note.optInt("version"),
                                null,
                                simple(
                                    () -> {
                                      vm.noteDraft = null;
                                      captureNote = null;
                                      d.dismiss();
                                      go("notes");
                                    })))));
    }
    d.setOnDismissListener(
        ignored -> {
          captureNote = null;
          if (!isChangingConfigurations()) vm.noteDraft = null;
        });
    d.show();
    return d;
  }
}
