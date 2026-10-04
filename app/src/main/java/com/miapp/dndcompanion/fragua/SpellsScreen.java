package com.miapp.dndcompanion.fragua;

import static com.miapp.dndcompanion.fragua.FraguaUi.*;

import android.content.Context;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.miapp.dndcompanion.R;
import java.util.Locale;
import java.util.function.Consumer;
import org.json.*;

/** Presentation of actual character spells, with searchable and collapsible level groups. */
public final class SpellsScreen extends LinearLayout {
  private final JSONArray spells;
  private final Consumer<JSONObject> open;

  public SpellsScreen(Context context, JSONObject sheet, Consumer<JSONObject> open) {
    super(context);
    setOrientation(VERTICAL);
    this.open = open;
    LayoutInflater.from(context).inflate(R.layout.view_fragua_spells, this, true);
    JSONObject derived = sheet.optJSONObject("derived");
    spells = sheet.optJSONObject("state").optJSONArray("spells");
    LinearLayout casting = findViewById(R.id.spells_casting);
    JSONArray casters = derived.optJSONArray("spellcasting");
    if (casters != null)
      for (int i = 0; i < casters.length(); i++) {
        JSONObject caster = casters.optJSONObject(i);
        casting.addView(text(context, caster.optString("label"), 18, GOLD));
        LinearLayout metrics = row(context);
        String[] labels = {
          "MODIFICADOR\n" + signed(caster.optInt("modifier")),
          "ATAQUE\n" + signed(caster.optInt("attack")),
          "CD SALVACIÓN\n" + caster.optInt("dc")
        };
        for (String label : labels) {
          TextView metric = text(context, label, 14, GOLD);
          metric.setGravity(Gravity.CENTER);
          metric.setBackgroundResource(R.drawable.fragua_panel);
          LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(context, 64), 1);
          lp.setMargins(2, 0, 2, 0);
          metrics.addView(metric, lp);
        }
        casting.addView(metrics);
      }
    LinearLayout slots = findViewById(R.id.spells_slots);
    slots.addView(text(context, "ESPACIOS DE CONJURO", 14, GOLD));
    JSONArray pool = derived.optJSONArray("slots");
    if (pool == null || pool.length() == 0)
      slots.addView(text(context, "Sin espacios de conjuro", 14, MUTED));
    if (pool != null)
      for (int i = 0; i < pool.length(); i++) {
        JSONObject slot = pool.optJSONObject(i);
        StringBuilder dots = new StringBuilder();
        for (int j = 0; j < slot.optInt("max"); j++)
          dots.append(j < slot.optInt("remaining") ? "●  " : "○  ");
        TextView line =
            text(
                context,
                slot.optInt("level")
                    + "°   "
                    + dots
                    + "  "
                    + slot.optInt("remaining")
                    + "/"
                    + slot.optInt("max")
                    + (slot.has("pool") ? " · Pacto" : ""),
                16,
                0xFFC6B4FF);
        slots.addView(line);
      }
    EditText search = findViewById(R.id.spells_search);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            render(s.toString());
          }

          public void afterTextChanged(Editable s) {}
        });
    render("");
  }

  private void render(String query) {
    LinearLayout groups = findViewById(R.id.spells_groups);
    groups.removeAllViews();
    String filter = query.trim().toLowerCase(Locale.ROOT);
    int count = 0;
    for (int level = 0; level <= 9; level++) {
      LinearLayout group =
          (LinearLayout)
              LayoutInflater.from(getContext())
                  .inflate(R.layout.view_fragua_spell_group, groups, false);
      group.setBackground(new Frame());
      LinearLayout rows = group.findViewById(R.id.spell_group_rows);
      TextView heading = group.findViewById(R.id.spell_group_title);
      String label = level == 0 ? "TRUCOS · Sin espacios" : "NIVEL " + level;
      heading.setText(label + "  ⌃");
      heading.setOnClickListener(
          v -> {
            boolean visible = rows.getVisibility() == VISIBLE;
            rows.setVisibility(visible ? GONE : VISIBLE);
            heading.setText(label + (visible ? "  ⌄" : "  ⌃"));
          });
      if (spells != null)
        for (int i = 0; i < spells.length(); i++) {
          JSONObject spell = spells.optJSONObject(i);
          if (spell.optInt("level") != level
              || !spell.optString("name").toLowerCase(Locale.ROOT).contains(filter)) continue;
          View item =
              LayoutInflater.from(getContext())
                  .inflate(R.layout.view_fragua_spell_row, rows, false);
          ((TextView) item.findViewById(R.id.spell_name)).setText(spell.optString("name"));
          ((TextView) item.findViewById(R.id.spell_summary))
              .setText(
                  spell.optString("castingTime")
                      + " · "
                      + spell.optString("range")
                      + (spell.optBoolean("concentration") ? " · Concentración" : "")
                      + (!spell.optBoolean("prepared") ? " · No preparado" : ""));
          item.setOnClickListener(v -> open.accept(spell));
          rows.addView(item);
          count++;
        }
      if (rows.getChildCount() > 0) groups.addView(group);
    }
    if (count == 0)
      groups.addView(
          text(
              getContext(),
              filter.isEmpty()
                  ? "Todavía no agregaste hechizos a esta ficha."
                  : "No hay hechizos con ese nombre.",
              16,
              MUTED));
  }
}
