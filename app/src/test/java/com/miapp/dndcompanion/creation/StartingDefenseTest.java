package com.miapp.dndcompanion.creation;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import com.miapp.dndcompanion.CrearPersonajeActivity;
import com.miapp.dndcompanion.network.ApiClient;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w320dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StartingDefenseTest {
  public static class Screen extends CrearPersonajeActivity {
    ApiClient.Callback callback;

    @Override
    protected void loadCatalog() {}

    @Override
    protected void requestStartingPreview(boolean retry) {
      new ViewModelProvider(this)
          .get(CreationViewModel.class)
          .preview((body, cb) -> callback = cb, retry);
    }
  }

  private CharacterDraft draft() {
    CharacterDraft d = new CharacterDraft();
    d.name = "Ada";
    d.speciesKey = "srd-2024_human";
    d.speciesName = "Human";
    d.classIndex = 4;
    d.abilities.method = AbilityScores.Method.POINT_BUY;
    d.abilities.balanced();
    d.background.select("srd-2024_soldier", "Soldado", "", new int[] {0, 1, 2});
    d.background.increases[0] = 2;
    d.background.increases[2] = 1;
    d.background.loadoutSupported = true;
    d.background.equipmentOption = "B";
    d.background.toolKeys = new String[] {"cards"};
    d.background.toolNames = new String[] {"Naipes"};
    d.background.toolChoice = "cards";
    d.classSkills.classKey = "fighter";
    d.classSkills.count = 2;
    d.classSkills.keys.addAll(java.util.Arrays.asList("athletics", "perception"));
    d.classSkills.names.addAll(java.util.Arrays.asList("Atletismo", "Percepción"));
    d.classSkills.selected.addAll(d.classSkills.keys);
    d.defense.supported = true;
    d.defense.classKey = "fighter";
    d.defense.classGold = 155;
    d.defense.shieldAllowed = true;
    d.defense.keys.addAll(java.util.Arrays.asList("none", "chainMail"));
    d.defense.names.addAll(java.util.Arrays.asList("Sin armadura", "Cota de malla"));
    d.defense.costs.addAll(java.util.Arrays.asList(0, 7500));
    d.defense.armorKey = "chainMail";
    d.defense.shield = true;
    d.step = CharacterDraft.Step.DEFENSE;
    return d;
  }

  private JSONObject response() throws Exception {
    return new JSONObject()
        .put(
            "startingStats",
            new JSONObject()
                .put("hp", 12)
                .put("ac", 18)
                .put("initiative", 1)
                .put("speed", 30)
                .put("copper", 12000));
  }

  @Test
  public void previewMustMatchCurrentChoicesBeforeSavingAndRejectsStaleResponses()
      throws Exception {
    CreationViewModel m = new CreationViewModel();
    m.draft = draft();
    m.save((body, cb) -> fail("No guardar sin comprobar los valores"));
    ApiClient.Callback[] first = {null}, second = {null};
    m.preview((body, cb) -> first[0] = cb, false);
    assertTrue(m.previewLoading);
    m.draft.defense.shield = false;
    m.preview((body, cb) -> second[0] = cb, false);
    first[0].success(response());
    assertFalse(m.previewReady());
    second[0].failure("Sin conexión", 0);
    assertFalse(m.uncertainSubmission);
    assertFalse(m.previewReady());
    m.preview((body, cb) -> fail("No reintentar en bucle"), false);
    JSONObject ready = response();
    m.preview((body, cb) -> cb.success(ready), true);
    assertTrue(m.previewReady());
    int[] calls = {0};
    m.save((body, cb) -> calls[0]++);
    m.save((body, cb) -> calls[0]++);
    assertEquals(1, calls[0]);
  }

  @Test
  public void defenseAndSummarySurviveRecreationAndRequireServerValues() throws Exception {
    CharacterDraft d = draft();
    d.defense.armorKey = "";
    d.defense.shield = false;
    Bundle state = new Bundle();
    state.putSerializable("creation_draft", d);
    ActivityController<Screen> c =
        Robolectric.buildActivity(Screen.class).create(state).start().resume().visible();
    assertFalse(find(c.get().getWindow().getDecorView(), "Continuar").isEnabled());
    Spinner selector = spinner(c.get().getWindow().getDecorView());
    selector.setSelection(2);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    find(c.get().getWindow().getDecorView(), "Agregar escudo · 10 po").performClick();
    capture(c.get(), "proteccion");
    find(c.get().getWindow().getDecorView(), "Continuar").performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertFalse(find(c.get().getWindow().getDecorView(), "Crear personaje").isEnabled());
    c.get().callback.success(response());
    assertNotNull(find(c.get().getWindow().getDecorView(), "Clase de armadura: 18"));
    assertNotNull(find(c.get().getWindow().getDecorView(), "Puntos de golpe: 12"));
    assertTrue(find(c.get().getWindow().getDecorView(), "Crear personaje").isEnabled());
    capture(c.get(), "resumen");
    Bundle saved = new Bundle();
    c.saveInstanceState(saved).pause().stop().destroy();
    ActivityController<Screen> restored =
        Robolectric.buildActivity(Screen.class).create(saved).start().resume().visible();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    CreationViewModel fresh = new ViewModelProvider(restored.get()).get(CreationViewModel.class);
    assertEquals("chainMail", fresh.draft.defense.armorKey);
    assertTrue(fresh.draft.defense.shield);
    assertFalse(find(restored.get().getWindow().getDecorView(), "Crear personaje").isEnabled());
    restored.get().callback.success(response());
    assertTrue(find(restored.get().getWindow().getDecorView(), "Crear personaje").isEnabled());
    restored.pause().stop().destroy();
  }

  @Test
  public void catalogClearsArmorWhenClassChangesAndRequiresBackground() throws Exception {
    CharacterDraft d = draft();
    d.step = CharacterDraft.Step.CLASS;
    Bundle state = new Bundle();
    state.putSerializable("creation_draft", d);
    ActivityController<Screen> c =
        Robolectric.buildActivity(Screen.class).create(state).start().resume().visible();
    CreationViewModel m = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    m.creation =
        read("/creation-catalog.json")
            .put("startingDefenseVersion", 1)
            .put("startingDefense", read("/starting-defense-catalog.json"));
    m.changed();
    d.classIndex = 5;
    m.changed();
    assertEquals("wizard", d.defense.classKey);
    assertEquals("", d.defense.armorKey);
    assertFalse(d.defense.shield);
    assertEquals(java.util.Arrays.asList("none"), d.defense.keys);
    d.background.defer();
    d.step = CharacterDraft.Step.BACKGROUND;
    assertFalse(d.canAdvance());
    c.pause().stop().destroy();
  }

  private JSONObject read(String path) throws Exception {
    try (InputStream in = getClass().getResourceAsStream(path)) {
      return new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  private void capture(Screen screen, String name) throws Exception {
    View root = screen.findViewById(android.R.id.content);
    root.measure(
        View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 320, 640);
    Bitmap bitmap = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(bitmap));
    File file = new File("build/reports/creation/" + name + ".png");
    file.getParentFile().mkdirs();
    try (FileOutputStream out = new FileOutputStream(file)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
  }

  private TextView find(View v, String text) {
    if (v instanceof TextView && text.equals(((TextView) v).getText().toString()))
      return (TextView) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        TextView r = find(((ViewGroup) v).getChildAt(i), text);
        if (r != null) return r;
      }
    return null;
  }

  private Spinner spinner(View v) {
    if (v instanceof Spinner) return (Spinner) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        Spinner r = spinner(((ViewGroup) v).getChildAt(i));
        if (r != null) return r;
      }
    return null;
  }
}
