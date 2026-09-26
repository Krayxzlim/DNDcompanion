package com.miapp.dndcompanion;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.miapp.dndcompanion.network.ApiClient;
import org.json.JSONObject;

/**
 * Pantalla de login con Supabase Auth.
 * Si el usuario ya está logueado, redirige directamente a MainActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private ApiClient api;
    private EditText editEmail, editPassword;
    private Button btnLogin, btnRegistrar;
    private TextView txtError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = ApiClient.get(this);
        setContentView(buildLayout());
        if (api.hasSession()) {
            btnLogin.setEnabled(false); btnRegistrar.setEnabled(false); btnRecuperar.setEnabled(false);
            loadProfile();
        }
    }

    // Construye el layout de login
    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.fondo));
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(32), dp(0), dp(32), dp(0));

        // Logo / título
        TextView icono = new TextView(this);
        icono.setText("⚔");
        icono.setTextColor(color(R.color.dorado));
        icono.setTextSize(56);
        icono.setGravity(Gravity.CENTER);
        icono.setLayoutParams(centrado(0, 0, 0, dp(8)));

        TextView titulo = new TextView(this);
        titulo.setText("D&D Companion");
        titulo.setTextColor(color(R.color.dorado));
        titulo.setTextSize(28);
        titulo.setTypeface(Typeface.create("serif", Typeface.BOLD));
        titulo.setGravity(Gravity.CENTER);
        titulo.setLayoutParams(centrado(0, 0, 0, dp(4)));

        TextView subtitulo = new TextView(this);
        subtitulo.setText("TU AVENTURA, SIEMPRE CONTIGO");
        subtitulo.setTextColor(color(R.color.dorado_claro));
        subtitulo.setTextSize(10);
        subtitulo.setLetterSpacing(0.12f);
        subtitulo.setGravity(Gravity.CENTER);
        subtitulo.setLayoutParams(centrado(0, 0, 0, dp(32)));

        // Separador
        View sep = separadorDorado();
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sepLp.setMargins(dp(20), 0, dp(20), dp(32));
        sep.setLayoutParams(sepLp);

        // Card de formulario
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.seccion_bg);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Label email
        TextView lblEmail = label("✦  CORREO ELECTRÓNICO");
        card.addView(lblEmail);

        // EditText email
        editEmail = editText("aventurero@example.com", false);
        card.addView(editEmail);

        // Espacio
        card.addView(espaciador(dp(12)));

        // Label contraseña
        TextView lblPass = label("✦  CONTRASEÑA");
        card.addView(lblPass);

        // EditText contraseña
        editPassword = editText("••••••••", true);
        card.addView(editPassword);

        // Mensaje de error
        txtError = new TextView(this);
        txtError.setTextColor(0xFFE53935);
        txtError.setTextSize(11);
        txtError.setGravity(Gravity.CENTER);
        txtError.setVisibility(View.GONE);
        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        errLp.setMargins(0, dp(8), 0, 0);
        txtError.setLayoutParams(errLp);
        card.addView(txtError);

        // Separador
        card.addView(espaciador(dp(20)));
        card.addView(separadorBorde());
        card.addView(espaciador(dp(16)));

        // Botón LOGIN
        btnLogin = new Button(this);
        btnLogin.setText("⚔  INGRESAR AL REINO");
        btnLogin.setTextColor(color(R.color.dorado));
        btnLogin.setTextSize(13);
        btnLogin.setTypeface(Typeface.create("serif", Typeface.BOLD));
        btnLogin.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color(R.color.boton_bg)));
        LinearLayout.LayoutParams loginLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        loginLp.setMargins(0, 0, 0, dp(10));
        btnLogin.setLayoutParams(loginLp);
        btnLogin.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            iniciarSesion();
        });
        card.addView(btnLogin);

        // Botón REGISTRAR
        btnRegistrar = new Button(this);
        btnRegistrar.setText("✦  CREAR NUEVA CUENTA");
        btnRegistrar.setTextColor(color(R.color.dorado_claro));
        btnRegistrar.setTextSize(11);
        btnRegistrar.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color(R.color.fondo)));
        LinearLayout.LayoutParams regLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        btnRegistrar.setLayoutParams(regLp);
        btnRegistrar.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            registrar();
        });
        card.addView(btnRegistrar);

        btnRecuperar = new Button(this);
        btnRecuperar.setText("Olvidé mi contraseña");
        btnRecuperar.setOnClickListener(v -> recuperarPassword());
        card.addView(btnRecuperar);

        //Ensamblar
        root.addView(icono);
        root.addView(titulo);
        root.addView(subtitulo);
        root.addView(sep);
        root.addView(card);

        return root;
    }

    // autenticación

    private Button btnRecuperar;

    private void recuperarPassword() {
        String email = editEmail.getText().toString().trim();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("Ingresá un correo válido para recuperar la contraseña.");
            return;
        }
        btnLogin.setEnabled(false);
        btnRegistrar.setEnabled(false);
        btnRecuperar.setEnabled(false);
        txtError.setVisibility(View.GONE);
        api.recoverPassword(email, new ApiClient.Callback() {
            public void success(Object value) {
                if (isFinishing()) return;
                btnLogin.setEnabled(true); btnRegistrar.setEnabled(true); btnRecuperar.setEnabled(true);
                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Revisá tu correo")
                    .setMessage("Si existe una cuenta con ese correo, recibirás un enlace para elegir una contraseña nueva. Revisá también spam. Después volvé a esta app e ingresá con la nueva contraseña.")
                    .setPositiveButton("Entendido", null).show();
            }
            public void failure(String message, int status) { authFailure(message); }
        });
    }

    private void iniciarSesion() {
        String email = editEmail.getText().toString().trim();
        String pass  = editPassword.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            mostrarError("Completá todos los campos.");
            return;
        }

        btnLogin.setEnabled(false);
        btnRegistrar.setEnabled(false);
        btnRecuperar.setEnabled(false);
        txtError.setVisibility(View.GONE);

        api.login(email, pass, new ApiClient.Callback() {
            public void success(Object value) { loadProfile(); }
            public void failure(String message, int status) { authFailure(message); }
        });
    }

    private void registrar() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString();
        if (email.isEmpty() || pass.length() < 6) { mostrarError("Ingresá correo y contraseña de al menos 6 caracteres."); return; }
        btnLogin.setEnabled(false); btnRegistrar.setEnabled(false); btnRecuperar.setEnabled(false);
        api.register(email, pass, new ApiClient.Callback() {
            public void success(Object value) {
                if (api.hasSession()) loadProfile();
                else { authFailure("Revisá tu correo para confirmar la cuenta y luego ingresá."); }
            }
            public void failure(String message, int status) { authFailure(message); }
        });
    }

    private void loadProfile() {
        api.request("GET", "/auth/me", null, new ApiClient.Callback() {
            public void success(Object value) { if (!isFinishing()) irAMain(((JSONObject)value).optString("email")); }
            public void failure(String message, int status) { authFailure(message); }
        });
    }
    private void authFailure(String message) {
        if (isFinishing()) return;
        btnLogin.setEnabled(true); btnRegistrar.setEnabled(true); btnRecuperar.setEnabled(true); mostrarError(message);
    }

    private void irAMain(String email) {
        Intent intent = new Intent(this, MainActivity.class);
        // Pasaje de datos con extras
        intent.putExtra(MainActivity.EXTRA_USER_EMAIL, email);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void mostrarError(String msg) {
        txtError.setText(msg);
        txtError.setVisibility(View.VISIBLE);
    }

    //Help de UI

    private TextView label(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(color(R.color.dorado_claro));
        tv.setTextSize(9);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText editText(String hint, boolean esPassword) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(color(R.color.texto_secundario));
        et.setTextColor(color(R.color.texto));
        et.setTextSize(14);
        et.setBackgroundResource(R.drawable.stat_bg);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if (esPassword) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }
        return et;
    }

    private View espaciador(int altura) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, altura));
        return v;
    }

    private View separadorDorado() {
        View v = new View(this);
        v.setBackgroundColor(color(R.color.dorado_borde));
        return v;
    }

    private View separadorBorde() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(color(R.color.borde));
        return v;
    }

    private LinearLayout.LayoutParams centrado(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private int color(int res) {
        return androidx.core.content.ContextCompat.getColor(this, res);
    }
}