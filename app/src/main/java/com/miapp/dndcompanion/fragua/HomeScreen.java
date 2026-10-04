package com.miapp.dndcompanion.fragua;

import static com.miapp.dndcompanion.fragua.FraguaUi.*;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.miapp.dndcompanion.R;
import org.json.*;

/** XML owns presentation; the snapshot and callbacks own data and navigation. */
public final class HomeScreen extends LinearLayout {
  public interface Events {
    void open(String destination, JSONObject item);
  }

  public HomeScreen(Context context, JSONObject sheet, boolean online, Events events) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_fragua_home, this, true);
    JSONObject id = sheet.optJSONObject("identity"),
        state = sheet.optJSONObject("state"),
        derived = sheet.optJSONObject("derived"),
        hp = state.optJSONObject("hp");
    findViewById(R.id.home_identity).setBackground(new Frame());
    findViewById(R.id.home_companion).setBackground(new Frame());
    findViewById(R.id.home_missions).setBackground(new Frame());
    findViewById(R.id.home_inventory).setOnClickListener(v -> events.open("inventory", null));
    bind(
        R.id.home_identity,
        id.optString("name") + "\n" + id.optString("race") + " · Nivel " + id.optInt("level"));
    findViewById(R.id.home_identity).setOnClickListener(v -> events.open("sheet", null));
    findViewById(R.id.home_notes).setOnClickListener(v -> events.open("notes", null));
    bind(R.id.home_ac, "CA\n" + (derived.isNull("ac") ? "—" : derived.optInt("ac")));
    bind(R.id.home_speed, derived.optInt("speed") + " ft\nVelocidad");
    HealthPortrait portrait =
        new HealthPortrait(context, hp.optInt("current"), hp.optInt("max"), hp.optInt("temp"));
    ((FrameLayout) findViewById(R.id.home_portrait))
        .addView(portrait, new FrameLayout.LayoutParams(-1, -1));
    portrait.setOnClickListener(v -> events.open("hp", null));
    bind(
        R.id.home_hp_status,
        "PV temporales: " + hp.optInt("temp") + (online ? "" : " · Copia local · solo consulta"));
    JSONObject xp = derived.optJSONObject("xp");
    bind(
        R.id.home_xp_label,
        "NIVEL "
            + id.optInt("level")
            + "          "
            + xp.optInt("current")
            + " / "
            + (xp.isNull("next") ? "MÁX" : xp.optInt("next"))
            + " XP");
    int start = xp.optInt("start"), end = xp.optInt("next", start + 1);
    ((ProgressBar) findViewById(R.id.home_xp))
        .setProgress(
            Math.max(
                0,
                Math.min(1000, (xp.optInt("current") - start) * 1000 / Math.max(1, end - start))));
    JSONArray companions = state.optJSONArray("companions");
    if (companions != null)
      for (int i = 0; i < companions.length(); i++) {
        JSONObject c = companions.optJSONObject(i);
        if (c.optBoolean("active")) {
          bind(R.id.home_companion, c.optString("name") + "  ›\nCompañero / Familiar");
          break;
        }
      }
    findViewById(R.id.home_companion).setOnClickListener(v -> events.open("companions", null));
    View configure = findViewById(R.id.home_configure);
    configure.setVisibility(state.optBoolean("configured") ? GONE : VISIBLE);
    configure.setOnClickListener(v -> events.open("configure", null));
    String[] destinations = {"saves", "skills", "spells", "missions", "actions"};
    RadialMenu wheel =
        new RadialMenu(
            context,
            new String[] {"Salvaciones", "Habilidades", "Hechizos", "Misiones", "Acciones"},
            i -> events.open(destinations[i], null));
    wheel.homeStyle();
    wheel.center("Dados", () -> events.open("dice", null));
    ((FrameLayout) findViewById(R.id.home_wheel))
        .addView(wheel, new FrameLayout.LayoutParams(-1, -2));
    TextView rest = findViewById(R.id.home_rest);
    rest.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_fragua_fire, 0, 0);
    rest.setOnClickListener(v -> events.open("rest", null));
    JSONArray attacks = derived.optJSONArray("attacks");
    int[] hosts = {R.id.home_left_weapon, R.id.home_right_weapon};
    for (int i = 0; i < 2; i++) {
      LinearLayout host = findViewById(hosts[i]);
      host.setBackground(new Frame());
      host.setPadding(dp(context, 4), dp(context, 10), dp(context, 4), dp(context, 10));
      JSONObject attack = attacks == null ? null : attacks.optJSONObject(i);
      TextView name = text(context, attack == null ? "Equipo" : attack.optString("name"), 16, GOLD);
      name.setGravity(Gravity.CENTER);
      name.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_fragua_swords, 0, 0);
      host.addView(name);
      if (attack != null) {
        TextView roll = text(context, "d20 " + signed(attack.optInt("attack")), 16, INK);
        roll.setGravity(Gravity.CENTER);
        host.addView(roll);
      }
      host.addView(
          button(
              context,
              attack == null ? "Equipar" : "Atacar",
              () -> events.open(attack == null ? "inventory" : "attack", attack)));
      if (attack != null) {
        TextView damage = text(context, "Daño " + attack.optString("damage"), 12, MUTED);
        damage.setGravity(Gravity.CENTER);
        host.addView(damage);
      }
    }
    findViewById(R.id.home_missions).setOnClickListener(v -> events.open("missions", null));
  }

  private void bind(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }
}
