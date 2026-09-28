package com.miapp.dndcompanion.fragua;

import static com.miapp.dndcompanion.fragua.FraguaUi.*;

import android.content.Context;
import android.view.Gravity;
import android.widget.*;
import org.json.*;

/** Pure native rendering of a server snapshot; no network or mutation lives in this view. */
public final class HomeScreen extends LinearLayout {
  public interface Events {
    void open(String destination, JSONObject item);
  }

  private final Events events;

  public HomeScreen(Context context, JSONObject sheet, boolean online, Events events) {
    super(context);
    this.events = events;
    setOrientation(VERTICAL);

    JSONObject id = sheet.optJSONObject("identity"),
        hp = sheet.optJSONObject("state").optJSONObject("hp");
    LinearLayout identity = row(context);
    identity.addView(
        button(context, "♧\nInventario", () -> navigate("inventory")),
        new LinearLayout.LayoutParams(px(72), px(68)));
    TextView name =
        button(
            context,
            id.optString("name") + "\n" + id.optString("race") + " · Nivel " + id.optInt("level"),
            () -> navigate("sheet"));
    name.setTextSize(19);
    identity.addView(name, new LinearLayout.LayoutParams(0, px(86), 1));
    identity.addView(
        button(context, "▤\nNotas", () -> navigate("notes")),
        new LinearLayout.LayoutParams(px(60), px(68)));
    addView(identity);
    LinearLayout hero = row(context);
    TextView ac =
        text(
            context,
            "♢\nCA\n"
                + (sheet.optJSONObject("derived").isNull("ac")
                    ? "—"
                    : sheet.optJSONObject("derived").optInt("ac")),
            20,
            GOLD);
    ac.setGravity(Gravity.CENTER);
    hero.addView(ac, new LinearLayout.LayoutParams(0, px(100), 1));
    HealthPortrait portrait =
        new HealthPortrait(context, hp.optInt("current"), hp.optInt("max"), hp.optInt("temp"));
    portrait.setOnClickListener(v -> events.open("hp", null));
    hero.addView(
        portrait,
        new LinearLayout.LayoutParams(
            px(Math.min(210, context.getResources().getConfiguration().screenWidthDp * .52f)),
            px(232)));
    TextView speed =
        text(
            context,
            "➤\n" + sheet.optJSONObject("derived").optInt("speed") + " ft\nVelocidad",
            14,
            INK);
    speed.setGravity(Gravity.CENTER);
    hero.addView(speed, new LinearLayout.LayoutParams(0, px(100), 1));
    addView(hero);
    TextView temp =
        text(
            context,
            "PV temporales: "
                + hp.optInt("temp")
                + "  ·  "
                + (online ? "Ficha sincronizada" : "Copia local · solo consulta"),
            12,
            MUTED);
    temp.setGravity(Gravity.CENTER);
    addView(temp);
    JSONObject xp = sheet.optJSONObject("derived").optJSONObject("xp");
    LinearLayout progress = panel(context);
    progress.addView(
        text(
            context,
            "NIVEL "
                + id.optInt("level")
                + "                  "
                + xp.optInt("current")
                + " / "
                + (xp.isNull("next") ? "MÁX" : xp.optInt("next"))
                + " XP",
            13,
            INK));
    ProgressBar bar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
    bar.setMax(1000);
    int start = xp.optInt("start"), end = xp.optInt("next", start + 1);
    bar.setProgress(
        Math.max(
            0, Math.min(1000, (xp.optInt("current") - start) * 1000 / Math.max(1, end - start))));
    bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF8876EF));
    progress.addView(bar);
    addView(progress);
    JSONArray companions = array(sheet.optJSONObject("state"), "companions");
    for (int i = 0; i < companions.length(); i++) {
      JSONObject c = companions.optJSONObject(i);
      if (c.optBoolean("active")) {
        addView(
            button(
                context,
                "♧  " + c.optString("name") + " · Compañero / familiar  ›",
                () -> navigate("companions")));
        break;
      }
    }
    if (!sheet.optJSONObject("state").optBoolean("configured"))
      addView(
          button(context, "Completar la ficha para jugar", () -> events.open("configure", null)));
    RadialMenu radial =
        new RadialMenu(
            context,
            new String[] {
              "♢\nSalvaciones", "✧\nHabilidades", "▱\nHechizos", "▤\nMisiones", "⚔\nAcciones"
            },
            i -> {
              switch (i) {
                case 0:
                  events.open("saves", null);
                  break;
                case 1:
                  events.open("skills", null);
                  break;
                case 2:
                  navigate("spells");
                  break;
                case 3:
                  navigate("missions");
                  break;
                case 4:
                  navigate("actions");
                  break;
              }
            });
    radial.center("◇\nDados", () -> events.open("dice", null));
    addView(radial, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout rest = panel(context);
    rest.addView(button(context, "♨  DESCANSAR", () -> events.open("rest", null)));
    addView(rest);
    JSONArray attacks = array(sheet.optJSONObject("derived"), "attacks");
    for (int i = 0; i < Math.min(2, attacks.length()); i++) {
      JSONObject a = attacks.optJSONObject(i);
      LinearLayout card = panel(context);
      card.addView(text(context, "⚔  " + a.optString("name"), 21, GOLD));
      card.addView(
          text(
              context,
              "d20 " + signed(a.optInt("attack")) + "  ·  Daño " + a.optString("damage"),
              16,
              INK));
      card.addView(button(context, "Atacar", () -> events.open("attack", a)));
      addView(card);
    }
    addView(button(context, "▤  Misiones · activas y disponibles  ›", () -> navigate("missions")));
    addView(button(context, "Compañeros y familiares  ›", () -> navigate("companions")));
    TextView f = text(context, "FRAGUA  ·  SRD 2024  ·  Asistente de mesa", 11, MUTED);
    f.setGravity(Gravity.CENTER);
    addView(f);
  }

  private int px(float n) {
    return dp(getContext(), n);
  }

  private void navigate(String screen) {
    events.open(screen, null);
  }

  private JSONArray array(JSONObject object, String key) {
    JSONArray a = object.optJSONArray(key);
    return a == null ? new JSONArray() : a;
  }
}
