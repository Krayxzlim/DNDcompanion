package com.miapp.dndcompanion;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.miapp.dndcompanion.network.ApiClient;
import org.json.JSONObject;
import org.json.JSONArray;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_USER_EMAIL = "user_email";

    private ApiClient api;
    private String userEmail = "";

    //Vistas
    LinearLayout layoutDisponibles, layoutActivas, layoutSpells;
    TextView txtResultado, txtTipoDado;
    ImageView imgAvatar;

    // Vistas del personaje (placeholder)
    TextView txtNombrePersonaje, txtRazaPersonaje, txtClasePersonaje,
            txtAlineacionPersonaje, txtPersonajePlaceholder;
    TextView txtCA, txtPG, txtNivelStat;
    TextView txtFUE, txtDES, txtCON, txtINT, txtSAB, txtCAR;
    TextView txtFUEmod, txtDESmod, txtCONmod, txtINTmod, txtSABmod, txtCARmod;
    LinearLayout layoutAvatarPlaceholder;

    //Launchers
    ActivityResultLauncher<String> pickImageLauncher;
    ActivityResultLauncher<Intent> crearPersonajeLauncher;

    int contador = 0;
    Random random = new Random();
    ArrayList<String> historial = new ArrayList<>();

    Animation animBtn, animDado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userEmail = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        if (userEmail == null) userEmail = "";

        api = ApiClient.get(this);
        if (!api.hasSession()) { volverAlLogin(); return; }

        // Launcher para seleccionar imagen del avatar
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imgAvatar.setImageURI(uri);
                        imgAvatar.setVisibility(View.VISIBLE);
                        layoutAvatarPlaceholder.setVisibility(View.GONE);
                    }
                }
        );

        // Launcher para recibir resultado del CrearPersonajeActivity
        crearPersonajeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        aplicarDatosPersonaje(
                                data.getStringExtra("personaje_nombre"),
                                data.getStringExtra("personaje_raza"),
                                data.getStringExtra("personaje_clase"),
                                data.getIntExtra("personaje_nivel", 1),
                                data.getStringExtra("personaje_alineacion"),
                                data.getIntExtra("personaje_fue", 10),
                                data.getIntExtra("personaje_des", 10),
                                data.getIntExtra("personaje_con", 10),
                                data.getIntExtra("personaje_int", 10),
                                data.getIntExtra("personaje_sab", 10),
                                data.getIntExtra("personaje_car", 10)
                        );
                    }
                }
        );

        setContentView(R.layout.activity_main);

        animBtn  = AnimationUtils.loadAnimation(this, R.anim.btn_press);
        animDado = AnimationUtils.loadAnimation(this, R.anim.dado_roll);

        //Bindear vistas
        layoutDisponibles = findViewById(R.id.layoutDisponibles);
        layoutActivas     = findViewById(R.id.layoutActivas);
        layoutSpells      = findViewById(R.id.layoutSpells);
        txtResultado      = findViewById(R.id.txtResultado);
        txtTipoDado       = findViewById(R.id.txtTipoDado);
        imgAvatar         = findViewById(R.id.imgAvatar);

        // Personaje
        txtNombrePersonaje    = findViewById(R.id.txtNombrePersonaje);
        txtRazaPersonaje      = findViewById(R.id.txtRazaPersonaje);
        txtClasePersonaje     = findViewById(R.id.txtClasePersonaje);
        txtAlineacionPersonaje= findViewById(R.id.txtAlineacionPersonaje);
        txtPersonajePlaceholder= findViewById(R.id.txtPersonajePlaceholder);
        txtCA                 = findViewById(R.id.txtCA);
        txtPG                 = findViewById(R.id.txtPG);
        txtNivelStat          = findViewById(R.id.txtNivelStat);
        txtFUE = findViewById(R.id.txtFUE); txtFUEmod = findViewById(R.id.txtFUEmod);
        txtDES = findViewById(R.id.txtDES); txtDESmod = findViewById(R.id.txtDESmod);
        txtCON = findViewById(R.id.txtCON); txtCONmod = findViewById(R.id.txtCONmod);
        txtINT = findViewById(R.id.txtINT); txtINTmod = findViewById(R.id.txtINTmod);
        txtSAB = findViewById(R.id.txtSAB); txtSABmod = findViewById(R.id.txtSABmod);
        txtCAR = findViewById(R.id.txtCAR); txtCARmod = findViewById(R.id.txtCARmod);
        layoutAvatarPlaceholder = findViewById(R.id.layoutAvatarPlaceholder);

        //Click en avatar placeholder → galería
        imgAvatar.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            pickImageLauncher.launch("image/*");
        });
        layoutAvatarPlaceholder.setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));

        //Header "TU PERSONAJE" → CrearPersonajeActivity
        View btnHeaderPersonaje = findViewById(R.id.btnHeaderPersonaje);
        btnHeaderPersonaje.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent i = new Intent(this, CrearPersonajeActivity.class);
                i.putExtra(EXTRA_USER_EMAIL, userEmail);
                crearPersonajeLauncher.launch(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 150);
        });

        //Tomo Arcano + "VER TODOS" → SpellListActivity
        View btnTomo = findViewById(R.id.btnTomoArcano);
        btnTomo.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            new Handler(Looper.getMainLooper()).postDelayed(() -> abrirGrimorio(), 150);
        });

        View txtVerTodos = findViewById(R.id.txtVerTodosHechizos);
        txtVerTodos.setOnClickListener(v -> abrirGrimorio());

        // Las misiones se recargan desde la API al volver a esta pantalla.

        //Hechizos fijos (con imagen personalizada de Supabase)
        for (SpellModel spell : SpellModel.getHechizosFijos()) {
            agregarSpell(spell);
        }

        //Dados
        configurarBtnDado(R.id.btnD4,  4);
        configurarBtnDado(R.id.btnD6,  6);
        configurarBtnDado(R.id.btnD8,  8);
        configurarBtnDado(R.id.btnD10, 10);
        configurarBtnDado(R.id.btnD12, 12);
        configurarBtnDado(R.id.btnD20, 20);

        findViewById(R.id.btnAleatorio).setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            int[] dados = {4, 6, 8, 10, 12, 20};
            tirarConAnimacion(dados[random.nextInt(dados.length)]);
        });

        findViewById(R.id.btnHistorial).setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            new Handler(Looper.getMainLooper()).postDelayed(this::mostrarHistorial, 150);
        });

        // Refresh data shared with the DM web app.
        findViewById(R.id.btnAgregar).setOnClickListener(v -> cargarDatos());
        if (findViewById(R.id.btnAgregar) instanceof TextView) ((TextView)findViewById(R.id.btnAgregar)).setText("Actualizar");
        configurarMenu();
    }

    @Override protected void onResume() {
        super.onResume();
        if (api != null && api.hasSession() && layoutDisponibles != null) cargarDatos();
    }

    //Aplicar datos de personaje creado
    private void aplicarDatosPersonaje(String nombre, String raza, String clase, int nivel,
                                       String alineacion, int fue, int des, int con,
                                       int intVal, int sab, int car) {
        if (nombre != null) {
            txtNombrePersonaje.setText(nombre);
            txtNombrePersonaje.setTextColor(color(R.color.texto));
        }
        if (raza != null) {
            txtRazaPersonaje.setText(raza);
            txtRazaPersonaje.setTextColor(color(R.color.texto_secundario));
        }
        if (clase != null) {
            txtClasePersonaje.setText(clase + " Nivel " + nivel);
            txtClasePersonaje.setTextColor(color(R.color.texto_secundario));
        }
        if (alineacion != null) {
            txtAlineacionPersonaje.setText(alineacion);
            txtAlineacionPersonaje.setTextColor(color(R.color.texto_secundario));
        }

        txtNivelStat.setText(String.valueOf(nivel));
        txtNivelStat.setTextColor(color(R.color.texto));

        // CA estimada (10 + mod DES) y PG estimados (nivel * 6)
        int modDes = (des - 10) / 2;
        txtCA.setText(String.valueOf(10 + modDes));
        txtCA.setTextColor(color(R.color.texto));
        txtPG.setText(String.valueOf(nivel * 6));
        txtPG.setTextColor(color(R.color.texto));

        // Atributos
        setAtributo(txtFUE, txtFUEmod, fue);
        setAtributo(txtDES, txtDESmod, des);
        setAtributo(txtCON, txtCONmod, con);
        setAtributo(txtINT, txtINTmod, intVal);
        setAtributo(txtSAB, txtSABmod, sab);
        setAtributo(txtCAR, txtCARmod, car);

        // Ocultar placeholder
        txtPersonajePlaceholder.setVisibility(View.GONE);
    }

    private void setAtributo(TextView txtVal, TextView txtMod, int valor) {
        txtVal.setText(String.valueOf(valor));
        txtVal.setTextColor(color(R.color.texto));
        int mod = (valor - 10) / 2;
        txtMod.setText(mod >= 0 ? "(+" + mod + ")" : "(" + mod + ")");
    }

    //Abrir grimorio de hechizos
    private void abrirGrimorio() {
        Intent i = new Intent(this, SpellListActivity.class);
        i.putExtra(EXTRA_USER_EMAIL, userEmail);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void volverAlLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent); finish();
    }
    private void apiError(String message, int status) {
        if (isFinishing()) return;
        if (status == 401) { volverAlLogin(); return; }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    private void cargarDatos() {
        api.request("GET", "/missions", null, new ApiClient.Callback() {
            public void success(Object value) {
                if (isFinishing()) return;
                layoutDisponibles.removeAllViews(); layoutActivas.removeAllViews();
                JSONArray missions = (JSONArray)value;
                for (int i = 0; i < missions.length(); i++) {
                    JSONObject m = missions.optJSONObject(i);
                    if (m == null) continue;
                    String reward = m.optString("reward") + " · " + m.optInt("rewardXp") + " XP · " + m.optInt("rewardGold") + " PO";
                    boolean assigned = contains(m.optJSONArray("assignedTo"), api.userId());
                    boolean accepted = contains(m.optJSONArray("acceptedBy"), api.userId());
                    if (!assigned) continue;
                    if (m.optString("status").equals("completed")) {
                        layoutActivas.addView(txt("✓ " + m.optString("title") + " — Completada", 13, R.color.dorado, true));
                    } else if (accepted) agregarMisionActivaAnimada(m.optString("title"), reward);
                    else agregarMisionDisponible(m.optString("id"), m.optString("title"), m.optString("description"), reward);
                }
                if (layoutDisponibles.getChildCount() == 0) layoutDisponibles.addView(txt("No tenés misiones pendientes de aceptar.", 12, R.color.texto_secundario, false));
            }
            public void failure(String message, int status) { apiError(message, status); }
        });
        api.request("GET", "/characters", null, new ApiClient.Callback() {
            public void success(Object value) {
                if (isFinishing()) return;
                JSONArray characters = (JSONArray)value;
                JSONObject c = characters.optJSONObject(0);
                if (c != null) aplicarDatosPersonaje(c.optString("name"), c.optString("race"), c.optString("characterClass"), c.optInt("level", 1), c.optString("alignment"), c.optInt("strength",10), c.optInt("dexterity",10), c.optInt("constitution",10), c.optInt("intelligence",10), c.optInt("wisdom",10), c.optInt("charisma",10));
            }
            public void failure(String message, int status) { apiError(message, status); }
        });
    }
    private boolean contains(JSONArray array, String value) {
        if (array == null) return false;
        for (int i = 0; i < array.length(); i++) if (value.equals(array.optString(i))) return true;
        return false;
    }

    //Hechizos
    private void agregarSpell(SpellModel spell) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, dp(6), 0, dp(6));
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setBackground(getDrawable(android.R.drawable.list_selector_background));

        int[] badgeColors = getBadgeColors(spell.levelInt);
        TextView badgeView = new TextView(this);
        badgeView.setText(spell.getBadgeLabel());
        badgeView.setTextColor(badgeColors[1]);
        badgeView.setTextSize(9);
        badgeView.setPadding(dp(8), dp(4), dp(8), dp(4));
        badgeView.setBackgroundColor(badgeColors[0]);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(0, 0, dp(10), 0);
        badgeView.setLayoutParams(bLp);

        TextView txtNombreSpell = txt(spell.name, 13, R.color.texto, false);
        txtNombreSpell.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView arrow = txt("›", 16, R.color.dorado_borde, false);

        item.addView(badgeView);
        item.addView(txtNombreSpell);
        item.addView(arrow);

        item.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> abrirDetalleHechizo(spell), 150);
        });

        layoutSpells.addView(item);
    }

    private void abrirDetalleHechizo(SpellModel spell) {
        Intent intent = new Intent(this, DetalleHechizo.class);
        intent.putExtra(DetalleHechizo.EXTRA_SLUG,         spell.slug);
        intent.putExtra(DetalleHechizo.EXTRA_NAME,         spell.name);
        intent.putExtra(DetalleHechizo.EXTRA_DESC,         spell.desc);
        intent.putExtra(DetalleHechizo.EXTRA_HIGHER_LEVEL, spell.higherLevel);
        intent.putExtra(DetalleHechizo.EXTRA_RANGE,        spell.range);
        intent.putExtra(DetalleHechizo.EXTRA_COMPONENTS,   spell.components);
        intent.putExtra(DetalleHechizo.EXTRA_MATERIAL,     spell.material);
        intent.putExtra(DetalleHechizo.EXTRA_CASTING_TIME, spell.castingTime);
        intent.putExtra(DetalleHechizo.EXTRA_LEVEL,        spell.level);
        intent.putExtra(DetalleHechizo.EXTRA_LEVEL_INT,    spell.levelInt);
        intent.putExtra(DetalleHechizo.EXTRA_SCHOOL,       spell.school);
        intent.putExtra(DetalleHechizo.EXTRA_DURATION,     spell.duration);
        intent.putExtra(DetalleHechizo.EXTRA_CONCENTRATION,spell.requiresConcentration);
        intent.putExtra(DetalleHechizo.EXTRA_RITUAL,       spell.canBeCastAsRitual);
        intent.putExtra(DetalleHechizo.EXTRA_DND_CLASS,    spell.dndClass);
        intent.putExtra(DetalleHechizo.EXTRA_IMAGEN_URL,   spell.imagenUrl);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private int[] getBadgeColors(int levelInt) {
        switch (levelInt) {
            case 0:  return new int[]{color(R.color.badge_truco),  color(R.color.badge_truco_texto)};
            case 1:  return new int[]{color(R.color.badge_nivel1), color(R.color.badge_nivel1_texto)};
            case 2:  return new int[]{color(R.color.badge_nivel2), color(R.color.badge_nivel2_texto)};
            default: return new int[]{color(R.color.badge_nivel3), color(R.color.badge_nivel3_texto)};
        }
    }

    //Misiones
    private void aceptarMisionConAnimacion(String nombre, String recompensa,
                                           LinearLayout wrapper) {
        if (wrapper.getParent() == null) return;
        if (Boolean.TRUE.equals(wrapper.getTag())) return;
        wrapper.setTag(true);

        wrapper.animate()
                .alpha(0f).translationX(200f).setDuration(250)
                .withEndAction(() -> {
                    wrapper.clearAnimation();
                    wrapper.post(() -> {
                        if (wrapper.getParent() != null)
                            layoutDisponibles.removeView(wrapper);
                        if (layoutDisponibles.getChildCount() > 0) {
                            View primerChild = layoutDisponibles.getChildAt(0);
                            if (primerChild instanceof LinearLayout) {
                                LinearLayout primerWrapper = (LinearLayout) primerChild;
                                if (primerWrapper.getChildCount() > 0
                                        && "sep".equals(primerWrapper.getChildAt(0).getTag())) {
                                    primerWrapper.removeViewAt(0);
                                }
                            }
                        }
                        agregarMisionActivaAnimada(nombre, recompensa);
                    });
                }).start();
    }

    private void agregarMisionActivaAnimada(String nombre, String recompensa) {
        if (layoutActivas.getChildCount() > 0) agregarSeparador(layoutActivas);
        LinearLayout item = buildItemMisionActiva(nombre, recompensa);
        layoutActivas.addView(item);
        item.setAlpha(0f);
        item.setTranslationX(-200f);
        item.animate().alpha(1f).translationX(0f).setDuration(250).start();
    }

    private LinearLayout buildItemMisionActiva(String nombre, String recompensa) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, dp(10), 0, dp(10));
        item.setGravity(Gravity.CENTER_VERTICAL);

        TextView icono = new TextView(this);
        icono.setText("⚔️"); icono.setTextSize(22);
        icono.setPadding(0, 0, dp(10), 0);
        icono.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        info.addView(txt(nombre, 13, R.color.texto, true));
        info.addView(txt("Recompensa:  " + recompensa, 11, R.color.dorado_claro, false));

        TextView badge = new TextView(this);
        badge.setText("EN CURSO");
        badge.setTextColor(color(R.color.dorado));
        badge.setTextSize(9);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badge.setBackgroundColor(color(R.color.boton_bg));

        item.addView(icono); item.addView(info); item.addView(badge);
        return item;
    }

    private void agregarMisionDisponible(String missionId, String nombre, String descripcion, String recompensa) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        if (layoutDisponibles.getChildCount() > 0) {
            View sep = new View(this);
            sep.setTag("sep");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, dp(3), 0, dp(3));
            sep.setLayoutParams(lp);
            sep.setBackgroundColor(color(R.color.borde));
            wrapper.addView(sep);
        }

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, dp(10), 0, dp(10));
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setTag(wrapper);

        TextView icono = new TextView(this);
        icono.setText("📜"); icono.setTextSize(22);
        icono.setPadding(0, 0, dp(10), 0);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        info.addView(txt(nombre, 13, R.color.texto, true));
        info.addView(txt(descripcion, 11, R.color.texto_secundario, false));
        info.addView(txt("Recompensa:  " + recompensa, 11, R.color.dorado_claro, false));

        androidx.appcompat.widget.AppCompatButton btn =
                new androidx.appcompat.widget.AppCompatButton(this);
        btn.setText("ACEPTAR");
        btn.setTextSize(10);
        btn.setTextColor(color(R.color.dorado));
        btn.setBackgroundColor(color(R.color.boton_bg));
        btn.setOnClickListener(v -> {
            btn.setEnabled(false);
            btn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            api.request("POST", "/missions/" + missionId + "/accept", null, new ApiClient.Callback() {
                public void success(Object value) { if (!isFinishing()) cargarDatos(); }
                public void failure(String message, int status) { btn.setEnabled(true); apiError(message, status); }
            });
        });

        item.addView(icono); item.addView(info); item.addView(btn);
        wrapper.addView(item);
        layoutDisponibles.addView(wrapper);
    }

    //Menú
    private void configurarMenu() {
        findViewById(R.id.menuInicio).setOnClickListener(v ->
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press)));

        findViewById(R.id.menuInventario).setOnClickListener(v ->
                pulsarYEjecutar(v, () -> {
                    Intent i = new Intent(this, InventarioActivity.class);
                    i.putExtra(EXTRA_USER_EMAIL, userEmail);
                    startActivity(i);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.menuNotas).setOnClickListener(v ->
                pulsarYEjecutar(v, () -> {
                    Intent i = new Intent(this, NotasActivity.class);
                    i.putExtra(EXTRA_USER_EMAIL, userEmail);
                    startActivity(i);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.menuAjustes).setOnClickListener(v ->
                pulsarYEjecutar(v, this::mostrarDialogoAjustes));
    }

    private void mostrarDialogoAjustes() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.seccion_bg);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView t = txt("✦  AJUSTES  ✦", 14, R.color.dorado, true);
        t.setTypeface(Typeface.create("serif", Typeface.BOLD));
        t.setGravity(Gravity.CENTER);
        root.addView(t);
        root.addView(separadorDorado());

        if (!userEmail.isEmpty()) {
            TextView tvEmail = new TextView(this);
            tvEmail.setText("Sesión iniciada como:\n" + userEmail);
            tvEmail.setTextColor(color(R.color.texto_secundario));
            tvEmail.setTextSize(12);
            tvEmail.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(12), 0, dp(4));
            tvEmail.setLayoutParams(lp);
            root.addView(tvEmail);
        }

        TextView m = txt("⚙️  Más ajustes estarán\ndisponibles en la próxima versión.",
                13, R.color.texto, false);
        m.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLp.setMargins(0, dp(8), 0, dp(12));
        m.setLayoutParams(mLp);
        root.addView(m);
        root.addView(separadorDorado());

        Button btnCerrar = new Button(this);
        btnCerrar.setText("⚔  CERRAR SESIÓN");
        btnCerrar.setTextColor(0xFFE53935);
        btnCerrar.setTextSize(11);
        btnCerrar.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color(R.color.boton_bg)));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        cLp.setMargins(0, dp(10), 0, dp(6));
        btnCerrar.setLayoutParams(cLp);
        root.addView(btnCerrar);

        Button btnOk = new Button(this);
        btnOk.setText("ACEPTAR");
        btnOk.setTextColor(color(R.color.dorado));
        btnOk.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color(R.color.boton_bg)));
        btnOk.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));
        root.addView(btnOk);

        AlertDialog d = new AlertDialog.Builder(this, R.style.DialogoOscuro)
                .setView(root).create();
        if (d.getWindow() != null)
            d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnOk.setOnClickListener(v -> d.dismiss());
        btnCerrar.setOnClickListener(v -> {
            d.dismiss();
            api.logout(new ApiClient.Callback() {
                public void success(Object value) { volverAlLogin(); }
                public void failure(String message, int status) { Toast.makeText(MainActivity.this, "Sesión local cerrada; no se pudo confirmar el cierre remoto.", Toast.LENGTH_LONG).show(); volverAlLogin(); }
            });

        });
        d.show();
    }

    //Historial de dados
    private void mostrarHistorial() {
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundResource(R.drawable.seccion_bg);

        LinearLayout encabezado = new LinearLayout(this);
        encabezado.setOrientation(LinearLayout.VERTICAL);
        encabezado.setGravity(Gravity.CENTER);
        encabezado.setPadding(dp(16), dp(16), dp(16), dp(12));
        encabezado.setBackgroundColor(color(R.color.seccion_fondo));

        LinearLayout filaTitulo = new LinearLayout(this);
        filaTitulo.setOrientation(LinearLayout.HORIZONTAL);
        filaTitulo.setGravity(Gravity.CENTER_VERTICAL);
        TextView e1 = txt("✦", 14, R.color.dorado, false); e1.setPadding(0, 0, dp(8), 0);
        TextView titulo = txt("HISTORIAL DE TIRADAS", 14, R.color.dorado, true);
        titulo.setTypeface(Typeface.create("serif", Typeface.BOLD));
        TextView e2 = txt("✦", 14, R.color.dorado, false); e2.setPadding(dp(8), 0, 0, 0);
        filaTitulo.addView(e1); filaTitulo.addView(titulo); filaTitulo.addView(e2);
        encabezado.addView(filaTitulo);

        TextView subtitulo = txt(historial.isEmpty() ? "Sin tiradas registradas" :
                        "Últimas " + Math.min(historial.size(), 15) + " tiradas",
                10, R.color.texto_secundario, false);
        subtitulo.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(4), 0, 0);
        subtitulo.setLayoutParams(subLp);
        encabezado.addView(subtitulo);
        raiz.addView(encabezado);
        raiz.addView(separadorDorado());

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(300)));
        scroll.setBackgroundColor(color(R.color.fondo));

        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        lista.setPadding(0, dp(4), 0, dp(4));

        if (historial.isEmpty()) {
            LinearLayout vacio = new LinearLayout(this);
            vacio.setOrientation(LinearLayout.VERTICAL);
            vacio.setGravity(Gravity.CENTER);
            vacio.setPadding(0, dp(40), 0, dp(40));
            TextView ico = txt("🎲", 36, R.color.borde, false);
            ico.setGravity(Gravity.CENTER);
            ico.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            TextView msg = txt("Aún no tiraste ningún dado", 13, R.color.texto_secundario, false);
            msg.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            msgLp.setMargins(0, dp(8), 0, 0);
            msg.setLayoutParams(msgLp);
            vacio.addView(ico); vacio.addView(msg);
            lista.addView(vacio);
        } else {
            int mostrar = Math.min(historial.size(), 15);
            for (int i = 0; i < mostrar; i++) {
                String[] p = historial.get(i).split("\\|");
                String tipoDado = p.length > 0 ? p[0] : "?";
                String valStr   = p.length > 1 ? p[1] : "?";
                int val = 0;
                try { val = Integer.parseInt(valStr); } catch (Exception ignored) {}

                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setGravity(Gravity.CENTER_VERTICAL);
                fila.setPadding(dp(16), dp(10), dp(16), dp(10));
                fila.setBackgroundColor(i % 2 == 0 ? color(R.color.seccion_fondo) : color(R.color.fondo));

                TextView numView = txt("#" + (i + 1), 10, R.color.borde, false);
                numView.setGravity(Gravity.CENTER);
                numView.setTypeface(Typeface.MONOSPACE);
                LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(dp(28),
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                numLp.setMargins(0, 0, dp(10), 0);
                numView.setLayoutParams(numLp);

                TextView badgeDado = new TextView(this);
                badgeDado.setText(tipoDado);
                badgeDado.setTextColor(color(R.color.dorado));
                badgeDado.setTextSize(11);
                badgeDado.setTypeface(Typeface.create("serif", Typeface.BOLD));
                badgeDado.setBackgroundColor(color(R.color.boton_bg));
                badgeDado.setPadding(dp(8), dp(3), dp(8), dp(3));
                LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                badgeLp.setMargins(0, 0, dp(12), 0);
                badgeDado.setLayoutParams(badgeLp);

                View spacer = new View(this);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                int colorRes;
                String sufijo = "";
                if (val == 1) { colorRes = 0; sufijo = "  💀"; }
                else if (tipoDado.equals("d20") && val == 20) { colorRes = R.color.dorado; sufijo = "  ✦"; }
                else { colorRes = R.color.texto; }

                TextView resView = new TextView(this);
                resView.setText(valStr + sufijo);
                resView.setTextSize(20);
                resView.setTypeface(Typeface.DEFAULT_BOLD);
                resView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                if (colorRes == 0) resView.setTextColor(Color.parseColor("#C05050"));
                else resView.setTextColor(color(colorRes));

                fila.addView(numView); fila.addView(badgeDado);
                fila.addView(spacer); fila.addView(resView);
                lista.addView(fila);
            }
        }
        scroll.addView(lista);
        raiz.addView(scroll);
        raiz.addView(separadorDorado());

        Button cerrar = new Button(this);
        cerrar.setText("✦  CERRAR  ✦");
        cerrar.setTextColor(color(R.color.dorado));
        cerrar.setTextSize(12);
        cerrar.setTypeface(Typeface.create("serif", Typeface.BOLD));
        cerrar.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color(R.color.boton_bg)));
        LinearLayout.LayoutParams cerrarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        cerrarLp.setMargins(dp(16), dp(10), dp(16), dp(16));
        cerrar.setLayoutParams(cerrarLp);
        raiz.addView(cerrar);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.DialogoOscuro)
                .setView(raiz).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        cerrar.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 150);
        });
        dialog.show();
    }

    //Dado con animación
    private void configurarBtnDado(int btnId, int caras) {
        View btn = findViewById(btnId);
        btn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> tirarConAnimacion(caras), 100);
        });
    }

    private void tirarConAnimacion(int caras) {
        int resultado = random.nextInt(caras) + 1;
        final int DURACION = 700, INTERVALO = 80, FLASHES = DURACION / INTERVALO;
        txtResultado.setText("?");
        txtTipoDado.setText("d" + caras);

        ObjectAnimator rotAnim = ObjectAnimator.ofFloat(txtResultado, "rotation", 0f, 360f);
        rotAnim.setDuration(DURACION);
        rotAnim.start();

        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < FLASHES; i++) {
            final int idx = i;
            handler.postDelayed(() -> {
                txtResultado.setText(String.valueOf(random.nextInt(caras) + 1));
                txtResultado.setTextColor(idx % 2 == 0
                        ? color(R.color.dorado) : color(R.color.texto));
            }, idx * INTERVALO);
        }

        handler.postDelayed(() -> {
            historial.add(0, "d" + caras + "|" + resultado);
            if (historial.size() > 20) historial.remove(historial.size() - 1);

            int colorFinal;
            String textoTipo;
            if (caras == 20 && resultado == 1) {
                colorFinal = Color.parseColor("#C05050");
                textoTipo = "(d20)  💀";
            } else if (caras == 20 && resultado == 20) {
                colorFinal = color(R.color.dorado);
                textoTipo = "(d20)  ✦";
            } else {
                colorFinal = color(R.color.dorado);
                textoTipo = "(d" + caras + ")";
            }

            txtResultado.setText(String.valueOf(resultado));
            txtResultado.setTextColor(colorFinal);
            txtTipoDado.setText(textoTipo);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(txtResultado, "scaleX", 0.3f, 1.15f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(txtResultado, "scaleY", 0.3f, 1.15f, 1.0f);
            scaleX.setDuration(400); scaleY.setDuration(400);
            scaleX.setInterpolator(new OvershootInterpolator(2.5f));
            scaleY.setInterpolator(new OvershootInterpolator(2.5f));
            scaleX.start(); scaleY.start();

            if (caras == 20 && resultado == 20) {
                ObjectAnimator pulso = ObjectAnimator.ofFloat(txtResultado, "alpha", 1f, 0.3f, 1f);
                pulso.setDuration(300); pulso.setRepeatCount(3); pulso.setStartDelay(400);
                pulso.start();
            }
        }, DURACION + 50);
    }

    //Utilities
    private void pulsarYEjecutar(View v, Runnable accion) {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.btn_press);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) { accion.run(); }
        });
        v.startAnimation(anim);
    }

    private void agregarSeparador(LinearLayout parent) {
        View sep = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(3), 0, dp(3));
        sep.setLayoutParams(lp);
        sep.setBackgroundColor(color(R.color.borde));
        parent.addView(sep);
    }

    private View separadorDorado() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(color(R.color.dorado_borde));
        return v;
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private int color(int res) {
        return androidx.core.content.ContextCompat.getColor(this, res);
    }

    private TextView txt(String texto, float sp, int colorRes, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(sp);
        tv.setTextColor(color(colorRes));
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return tv;
    }
}