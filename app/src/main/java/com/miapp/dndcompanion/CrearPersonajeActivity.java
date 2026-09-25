package com.miapp.dndcompanion;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.miapp.dndcompanion.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrearPersonajeActivity extends AppCompatActivity {

    private static final String API_RACES =
            "https://api.open5e.com/v1/races/?limit=50&document__slug=wotc-srd";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Vistas
    private EditText editNombre;
    private Spinner spinnerRaza, spinnerClase, spinnerAlineacion;
    private TextView txtRazaInfo, txtNivelNum;
    private SeekBar seekNivel;

    // Atributos
    private final int[] atributos = {10, 10, 10, 10, 10, 10};
    private final TextView[] txtAtributos = new TextView[6];
    private final TextView[] txtMods      = new TextView[6];
    private static final String[] ATTR_NOMBRES = {"FUE","DES","CON","INT","SAB","CAR"};

    // Datos API
    private final List<String>     razasNombres = new ArrayList<>();
    private final List<String>     razasInfo    = new ArrayList<>();

    private static final String[] CLASES_SRD = {
            "Bárbaro","Bardo","Clérigo","Druida","Guerrero",
            "Mago","Monje","Paladín","Explorador","Pícaro",
            "Hechicero","Brujo"
    };

    private static final String[] ALINEACIONES = {
            "Legal Bueno","Neutral Bueno","Caótico Bueno",
            "Legal Neutral","Neutral Verdadero","Caótico Neutral",
            "Legal Malvado","Neutral Malvado","Caótico Malvado"
    };

    private ApiClient api;
    private Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = ApiClient.get(this);
        setContentView(buildLayout());
        cargarRazas();
    }

    // Layout completo
    private View buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(color(R.color.fondo));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, dp(40));
        scroll.addView(root);

        //Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setBackgroundColor(color(R.color.seccion_fondo));
        header.setPadding(dp(16), dp(36), dp(16), dp(16));

        TextView btnVolver = new TextView(this);
        btnVolver.setText("← VOLVER");
        btnVolver.setTextColor(color(R.color.dorado_claro));
        btnVolver.setTextSize(11);
        btnVolver.setTypeface(Typeface.MONOSPACE);
        btnVolver.setClickable(true);
        btnVolver.setFocusable(true);
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vLp.setMargins(0, 0, 0, dp(10));
        btnVolver.setLayoutParams(vLp);
        btnVolver.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        header.addView(btnVolver);

        TextView titulo = new TextView(this);
        titulo.setText("⚔  CREAR PERSONAJE");
        titulo.setTextColor(color(R.color.dorado));
        titulo.setTextSize(20);
        titulo.setTypeface(Typeface.create("serif", Typeface.BOLD));
        titulo.setGravity(Gravity.CENTER);
        header.addView(titulo);

        TextView sub = new TextView(this);
        sub.setText("RAZAS VÍA OPEN5E API  ·  GUARDADO EN FIRESTORE");
        sub.setTextColor(color(R.color.texto_secundario));
        sub.setTextSize(8);
        sub.setLetterSpacing(0.08f);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(4), 0, 0);
        sub.setLayoutParams(subLp);
        header.addView(sub);

        root.addView(header);
        root.addView(separadorDorado());

        // Nombre
        root.addView(labelSeccion("✦  NOMBRE DEL PERSONAJE", null));
        editNombre = new EditText(this);
        editNombre.setHint("Ej: Arannis, Grog, Mercer...");
        editNombre.setHintTextColor(color(R.color.texto_secundario));
        editNombre.setTextColor(color(R.color.texto));
        editNombre.setTextSize(16);
        editNombre.setBackgroundResource(R.drawable.stat_bg);
        editNombre.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.setMargins(dp(12), 0, dp(12), dp(4));
        editNombre.setLayoutParams(nameLp);
        root.addView(editNombre);

        //Raza (desde API)
        root.addView(labelSeccion("✦  RAZA", "Cargando razas desde la API..."));

        spinnerRaza = new Spinner(this);
        spinnerRaza.setBackgroundResource(R.drawable.stat_bg);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        spLp.setMargins(dp(12), 0, dp(12), dp(6));
        spinnerRaza.setLayoutParams(spLp);
        root.addView(spinnerRaza);

        txtRazaInfo = new TextView(this);
        txtRazaInfo.setTextColor(color(R.color.dorado_claro));
        txtRazaInfo.setTextSize(11);
        txtRazaInfo.setBackgroundResource(R.drawable.stat_bg);
        txtRazaInfo.setPadding(dp(10), dp(8), dp(10), dp(8));
        txtRazaInfo.setVisibility(View.GONE);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoLp.setMargins(dp(12), 0, dp(12), dp(4));
        txtRazaInfo.setLayoutParams(infoLp);
        root.addView(txtRazaInfo);

        //Clase
        root.addView(labelSeccion("✦  CLASE", null));

        spinnerClase = new Spinner(this);
        spinnerClase.setBackgroundResource(R.drawable.stat_bg);
        ArrayAdapter<String> claseAdpt = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CLASES_SRD);
        claseAdpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClase.setAdapter(claseAdpt);
        LinearLayout.LayoutParams claseLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        claseLp.setMargins(dp(12), 0, dp(12), dp(4));
        spinnerClase.setLayoutParams(claseLp);
        root.addView(spinnerClase);

        //Alineación
        root.addView(labelSeccion("✦  ALINEACIÓN", null));

        spinnerAlineacion = new Spinner(this);
        spinnerAlineacion.setBackgroundResource(R.drawable.stat_bg);
        ArrayAdapter<String> alineaAdpt = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ALINEACIONES);
        alineaAdpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlineacion.setAdapter(alineaAdpt);
        spinnerAlineacion.setSelection(5); // Caótico Neutral
        LinearLayout.LayoutParams alineaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        alineaLp.setMargins(dp(12), 0, dp(12), dp(4));
        spinnerAlineacion.setLayoutParams(alineaLp);
        root.addView(spinnerAlineacion);

        // Nivel
        root.addView(labelSeccion("✦  NIVEL", null));

        LinearLayout nivelRow = new LinearLayout(this);
        nivelRow.setOrientation(LinearLayout.HORIZONTAL);
        nivelRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams nivelRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nivelRowLp.setMargins(dp(12), 0, dp(12), dp(8));
        nivelRow.setLayoutParams(nivelRowLp);

        seekNivel = new SeekBar(this);
        seekNivel.setMax(19);
        seekNivel.setProgress(4); // nivel 5 por defecto
        seekNivel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        txtNivelNum = new TextView(this);
        txtNivelNum.setText("5");
        txtNivelNum.setTextColor(color(R.color.dorado));
        txtNivelNum.setTextSize(22);
        txtNivelNum.setTypeface(Typeface.DEFAULT_BOLD);
        txtNivelNum.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nivelNumLp = new LinearLayout.LayoutParams(dp(48),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nivelNumLp.setMargins(dp(10), 0, 0, 0);
        txtNivelNum.setLayoutParams(nivelNumLp);

        seekNivel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                txtNivelNum.setText(String.valueOf(p + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        nivelRow.addView(seekNivel);
        nivelRow.addView(txtNivelNum);
        root.addView(nivelRow);

        // Atributos
        root.addView(labelSeccion("✦  ATRIBUTOS BASE",
                "Usá los botones − / + para ajustar cada valor (3–20)"));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridLp.setMargins(dp(10), dp(4), dp(10), dp(12));
        grid.setLayoutParams(gridLp);

        for (int i = 0; i < 6; i++) {
            final int idx = i;

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            col.setBackgroundResource(R.drawable.stat_bg);
            col.setPadding(dp(4), dp(6), dp(4), dp(6));
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            colLp.setMargins(dp(2), 0, dp(2), 0);
            col.setLayoutParams(colLp);

            TextView label = new TextView(this);
            label.setText(ATTR_NOMBRES[i]);
            label.setTextColor(color(R.color.dorado_claro));
            label.setTextSize(8);
            label.setGravity(Gravity.CENTER);
            label.setLayoutParams(wrapMatch());
            col.addView(label);

            txtAtributos[i] = new TextView(this);
            txtAtributos[i].setText("10");
            txtAtributos[i].setTextColor(color(R.color.texto));
            txtAtributos[i].setTextSize(16);
            txtAtributos[i].setTypeface(Typeface.DEFAULT_BOLD);
            txtAtributos[i].setGravity(Gravity.CENTER);
            txtAtributos[i].setLayoutParams(wrapMatch());
            col.addView(txtAtributos[i]);

            txtMods[i] = new TextView(this);
            txtMods[i].setText("(+0)");
            txtMods[i].setTextColor(color(R.color.texto_secundario));
            txtMods[i].setTextSize(8);
            txtMods[i].setGravity(Gravity.CENTER);
            txtMods[i].setLayoutParams(wrapMatch());
            col.addView(txtMods[i]);

            // Botones − +
            LinearLayout btns = new LinearLayout(this);
            btns.setOrientation(LinearLayout.HORIZONTAL);
            btns.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams btnsLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnsLp.setMargins(0, dp(4), 0, 0);
            btns.setLayoutParams(btnsLp);

            Button btnM = new Button(this);
            btnM.setText("−");
            btnM.setTextSize(12);
            btnM.setTextColor(color(R.color.texto));
            btnM.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    color(R.color.boton_bg)));
            btnM.setPadding(0,0,0,0);
            btnM.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(24)));
            btnM.setOnClickListener(v -> {
                if (atributos[idx] > 3) { atributos[idx]--; refrescarAtributo(idx); }
            });

            Button btnP = new Button(this);
            btnP.setText("+");
            btnP.setTextSize(12);
            btnP.setTextColor(color(R.color.dorado));
            btnP.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    color(R.color.boton_bg)));
            btnP.setPadding(0,0,0,0);
            LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(dp(28), dp(24));
            bpLp.setMargins(dp(2),0,0,0);
            btnP.setLayoutParams(bpLp);
            btnP.setOnClickListener(v -> {
                if (atributos[idx] < 20) { atributos[idx]++; refrescarAtributo(idx); }
            });

            btns.addView(btnM);
            btns.addView(btnP);
            col.addView(btns);
            grid.addView(col);
        }
        root.addView(grid);

        // Botón guardar
        root.addView(separadorDorado());

        btnGuardar = new Button(this);
        btnGuardar.setText("⚔  CREAR PERSONAJE");
        btnGuardar.setTextColor(color(R.color.dorado));
        btnGuardar.setTextSize(14);
        btnGuardar.setTypeface(Typeface.create("serif", Typeface.BOLD));
        btnGuardar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                color(R.color.boton_bg)));
        LinearLayout.LayoutParams guardarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        guardarLp.setMargins(dp(12), dp(16), dp(12), 0);
        btnGuardar.setLayoutParams(guardarLp);
        btnGuardar.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            new Handler(Looper.getMainLooper()).postDelayed(this::guardarPersonaje, 150);
        });
        root.addView(btnGuardar);

        return scroll;
    }

    // Cargar razas desde Open5e
    private void cargarRazas() {
        executor.execute(() -> {
            try {
                String json = fetchUrl(API_RACES);
                if (json == null) throw new Exception("sin respuesta");

                JSONObject obj = new JSONObject(json);
                JSONArray results = obj.getJSONArray("results");

                List<String> nombres = new ArrayList<>();
                List<String> infos   = new ArrayList<>();

                for (int i = 0; i < results.length(); i++) {
                    JSONObject r = results.getJSONObject(i);
                    nombres.add(r.optString("name"));
                    String asi   = strip(r.optString("asi_desc",""));
                    String size  = r.optString("size_raw","");
                    String speed = r.optString("speed","") + " pies";
                    String langs = strip(r.optString("languages",""));
                    StringBuilder info = new StringBuilder();
                    if (!asi.isEmpty())   info.append("Atributos: ").append(asi).append("\n");
                    if (!size.isEmpty())  info.append("Tamaño: ").append(capitalize(size)).append("\n");
                    info.append("Velocidad: ").append(speed);
                    if (!langs.isEmpty()) info.append("\nIdiomas: ").append(langs);
                    infos.add(info.toString().trim());
                }

                mainHandler.post(() -> {
                    razasNombres.addAll(nombres);
                    razasInfo.addAll(infos);

                    ArrayAdapter<String> adpt = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, razasNombres);
                    adpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerRaza.setAdapter(adpt);

                    spinnerRaza.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            if (pos < razasInfo.size()) {
                                txtRazaInfo.setText(razasInfo.get(pos));
                                txtRazaInfo.setVisibility(View.VISIBLE);
                            }
                        }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });
                    if (!razasNombres.isEmpty()) spinnerRaza.setSelection(0);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    // Fallback local si la API falla
                    String[] fallback = {"Humano","Elfo","Enano","Mediano","Gnomo",
                            "Semiorco","Semielfo","Tiefling","Dracónido"};
                    ArrayAdapter<String> adpt = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, fallback);
                    adpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerRaza.setAdapter(adpt);
                    Toast.makeText(this, "API no disponible — usando razas locales",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Guardar por la API compartida y devolver resultado
    private void guardarPersonaje() {
        String nombre = editNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(this, "✦ Ingresá un nombre para el personaje",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String raza      = spinnerRaza.getSelectedItem() != null
                ? spinnerRaza.getSelectedItem().toString() : "Humano";
        String clase     = CLASES_SRD[spinnerClase.getSelectedItemPosition()];
        String alineacion= ALINEACIONES[spinnerAlineacion.getSelectedItemPosition()];
        int nivel        = seekNivel.getProgress() + 1;

        Map<String, Object> doc = new HashMap<>();
        doc.put("name", nombre);     doc.put("race", raza);
        doc.put("characterClass", clase);       doc.put("level", nivel);
        doc.put("alignment", alineacion);
        doc.put("strength", atributos[0]);  doc.put("dexterity", atributos[1]);
        doc.put("constitution", atributos[2]);  doc.put("intelligence", atributos[3]);
        doc.put("wisdom", atributos[4]);  doc.put("charisma", atributos[5]);


        // Intent resultado (siempre, con o sin sesión)
        Intent result = new Intent();
        result.putExtra("personaje_nombre",    nombre);
        result.putExtra("personaje_raza",      raza);
        result.putExtra("personaje_clase",     clase);
        result.putExtra("personaje_nivel",     nivel);
        result.putExtra("personaje_alineacion",alineacion);
        result.putExtra("personaje_fue",  atributos[0]);
        result.putExtra("personaje_des",  atributos[1]);
        result.putExtra("personaje_con",  atributos[2]);
        result.putExtra("personaje_int",  atributos[3]);
        result.putExtra("personaje_sab",  atributos[4]);
        result.putExtra("personaje_car",  atributos[5]);

        btnGuardar.setEnabled(false);
        api.request("POST", "/characters", new JSONObject(doc), new ApiClient.Callback() {
            public void success(Object value) {
                if (isFinishing()) return;
                result.putExtra("personaje_id", ((JSONObject)value).optString("id"));
                Toast.makeText(CrearPersonajeActivity.this, "Personaje guardado", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK, result); finish();
            }
            public void failure(String message, int status) {
                if (isFinishing()) return;
                btnGuardar.setEnabled(true);
                Toast.makeText(CrearPersonajeActivity.this, message, Toast.LENGTH_LONG).show();
                if (status == 401) {
                    Intent login = new Intent(CrearPersonajeActivity.this, LoginActivity.class);
                    login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(login); finish();
                }
            }
        });
    }

    private void refrescarAtributo(int idx) {
        int val = atributos[idx];
        txtAtributos[idx].setText(String.valueOf(val));
        int mod = (val - 10) / 2;
        txtMods[idx].setText(mod >= 0 ? "(+" + mod + ")" : "(" + mod + ")");
    }

    private View labelSeccion(String titulo, String subtitulo) {
        LinearLayout sec = new LinearLayout(this);
        sec.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, dp(8));
        sec.setLayoutParams(lp);

        TextView tv = new TextView(this);
        tv.setText(titulo);
        tv.setTextColor(color(R.color.dorado));
        tv.setTextSize(11);
        tv.setTypeface(Typeface.create("serif", Typeface.BOLD));
        tv.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLp.setMargins(dp(14), 0, dp(14), dp(4));
        tv.setLayoutParams(tvLp);
        sec.addView(tv);

        if (subtitulo != null) {
            TextView sv = new TextView(this);
            sv.setText(subtitulo);
            sv.setTextColor(color(R.color.texto_secundario));
            sv.setTextSize(10);
            LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            svLp.setMargins(dp(14), 0, dp(14), dp(4));
            sv.setLayoutParams(svLp);
            sec.addView(sv);
        }

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        sep.setBackgroundColor(color(R.color.dorado_borde));
        sec.addView(sep);
        return sec;
    }

    private View separadorDorado() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(color(R.color.dorado_borde));
        return v;
    }

    private LinearLayout.LayoutParams wrapMatch() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private String strip(String s) {
        if (s == null) return "";
        return s.replaceAll("\\*{1,3}","").replaceAll("#+\\s","")
                .replaceAll("_","").trim();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String fetchUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept","application/json");
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private int color(int res) {
        return androidx.core.content.ContextCompat.getColor(this, res);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}