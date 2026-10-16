package com.miapp.dndcompanion.creation;

import static org.junit.Assert.*;

import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import com.miapp.dndcompanion.CrearPersonajeActivity;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w320dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CreationActivityTest {
  public static class OfflineActivity extends CrearPersonajeActivity {
    @Override
    protected void loadCatalog() {}
  }

  private CharacterDraft draft() {
    CharacterDraft d = new CharacterDraft();
    d.speciesKey = "elf";
    d.speciesName = "Elfo";
    d.classIndex = 5;
    d.abilities.method = AbilityScores.Method.POINT_BUY;
    d.abilities.balanced();
    d.background.defer();
    d.step = CharacterDraft.Step.ALLOCATION;
    return d;
  }

  private ActivityController<OfflineActivity> start(CharacterDraft draft) {
    ActivityController<OfflineActivity> c = Robolectric.buildActivity(OfflineActivity.class);
    Bundle state = new Bundle();
    state.putSerializable("creation_draft", draft);
    c.create(state).start().resume().visible();
    return c;
  }

  @Test
  public void backAndProcessRecreationKeepAllocationAndIdentity() {
    ActivityController<OfflineActivity> c = start(draft());
    CrearPersonajeActivity a = c.get();
    assertNotNull(find(a, "Puntos disponibles: 0 de 27"));
    find(a, "Atrás").performClick();
    assertNotNull(find(a, "¿Cómo obtenés tus atributos?"));
    find(a, "Continuar").performClick();
    assertNotNull(find(a, "Puntos disponibles: 0 de 27"));
    for (int i = 0; i < 4; i++) find(a, "Continuar").performClick();
    EditText input = edit(a.getWindow().getDecorView());
    assertNotNull(input);
    input.setText("Ada");
    Bundle state = new Bundle();
    c.saveInstanceState(state).pause().stop().destroy();
    ActivityController<OfflineActivity> restored =
        Robolectric.buildActivity(OfflineActivity.class).create(state).start().resume().visible();
    assertEquals("Ada", edit(restored.get().getWindow().getDecorView()).getText().toString());
    find(restored.get(), "Continuar").performClick();
    assertNotNull(find(restored.get(), "Ada"));
    restored.pause().stop().destroy();
  }

  @Test
  public void narrowAllocationScreenshotAndAccessibleControls() throws Exception {
    ActivityController<OfflineActivity> c = start(draft());
    View root = c.get().findViewById(android.R.id.content);
    root.measure(
        View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 320, 640);
    assertNotNull(find(c.get(), "Continuar"));
    assertTrue(find(c.get(), "Continuar").getHeight() >= 48);
    Bitmap bitmap = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(bitmap));
    File file = new File("build/reports/creation/allocation-320.png");
    file.getParentFile().mkdirs();
    try (FileOutputStream out = new FileOutputStream(file)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    c.pause().stop().destroy();
  }

  @Test
  public void retainedViewModelKeepsPendingSubmissionAcrossRotation() {
    ActivityController<OfflineActivity> c = start(draft());
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    model.draft.name = "Ada";
    model.draft.step = CharacterDraft.Step.REVIEW;
    model.changed();
    int[] calls = {0};
    model.save((body, callback) -> calls[0]++);
    c.configurationChange();
    CreationViewModel retained = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    assertSame(model, retained);
    retained.save((body, callback) -> calls[0]++);
    assertEquals(1, calls[0]);
    assertFalse(find(c.get(), "Guardando…").isEnabled());
    assertFalse(find(c.get(), "Atrás").isEnabled());
    c.pause().stop().destroy();
  }

  @Test
  public void processDeathDuringSubmissionRequiresCheckingCharacterList() {
    ActivityController<OfflineActivity> c = start(draft());
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    model.draft.name = "Ada";
    model.draft.step = CharacterDraft.Step.REVIEW;
    model.save((body, callback) -> {});
    Bundle state = new Bundle();
    c.saveInstanceState(state).pause().stop().destroy();
    ActivityController<OfflineActivity> restored =
        Robolectric.buildActivity(OfflineActivity.class).create(state).start().resume().visible();
    CreationViewModel fresh = new ViewModelProvider(restored.get()).get(CreationViewModel.class);
    assertTrue(fresh.uncertainSubmission);
    assertFalse(fresh.saving);
    fresh.save((body, callback) -> fail("Must not post again"));
    assertFalse(find(restored.get(), "Crear personaje").isEnabled());
    assertNotNull(find(restored.get(), "Volver a la ficha"));
    restored.pause().stop().destroy();
  }

  @Test
  public void standardAssignmentSwapsInTheScreenWithoutSpinnerCallbackLoops() {
    CharacterDraft d = draft();
    d.abilities.method = AbilityScores.Method.STANDARD;
    ActivityController<OfflineActivity> c = start(d);
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Asignar Fuerza").setSelection(1);
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Asignar Destreza").setSelection(2);
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Asignar Fuerza").setSelection(2);
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals(14, d.abilities.score(0));
    assertEquals(15, d.abilities.score(1));
    assertFalse(find(c.get(), "Continuar").isEnabled());
    c.pause().stop().destroy();
  }

  @Test
  public void catalogAndClassChangesRequireExplicitRecommendation() throws Exception {
    CharacterDraft d = draft();
    ActivityController<OfflineActivity> c = start(d);
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    try (InputStream in = getClass().getResourceAsStream("/creation-catalog.json")) {
      model.creation =
          new org.json.JSONObject(
              new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
    model.creationLoaded = true;
    model.changed();
    assertEquals(13, d.abilities.score(0));
    assertNotNull(find(c.get(), "Aplicar recomendación de Mago"));
    d.step = CharacterDraft.Step.CLASS;
    model.changed();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Seleccionar clase").setSelection(2);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals(1, d.classIndex);
    assertEquals(13, d.abilities.score(0));
    find(c.get(), "Continuar").performClick();
    find(c.get(), "Continuar").performClick();
    find(c.get(), "Aplicar recomendación de Bardo").performClick();
    assertEquals(15, d.abilities.score(5));
    assertEquals(8, d.abilities.score(0));
    assertEquals(0, d.abilities.remaining());
    d.step = CharacterDraft.Step.CLASS;
    model.changed();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Seleccionar clase").setSelection(0);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals(-1, d.classIndex);
    assertFalse(find(c.get(), "Continuar").isEnabled());
    assertEquals(15, d.abilities.score(5));
    c.pause().stop().destroy();
  }

  @Test
  public void allocationExplainsMissingCatalogAndOffersRetry() {
    ActivityController<OfflineActivity> c = start(draft());
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    model.creationMessage = "Información de clases no disponible. Podés continuar o reintentar.";
    model.changed();
    assertNotNull(find(c.get(), model.creationMessage));
    assertNotNull(find(c.get(), "Reintentar información de clases"));
    assertTrue(find(c.get(), "Continuar").isEnabled());
    c.pause().stop().destroy();
  }

  @Test
  public void speciesPanelFollowsSelectionAndPlaceholderClearsIt() throws Exception {
    CharacterDraft d = new CharacterDraft();
    ActivityController<OfflineActivity> c = start(d);
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    model.species.add(
        new org.json.JSONObject()
            .put("key", "elf")
            .put("name", "Elfo")
            .put("size", "Mediano")
            .put("speed", "30 pies"));
    model.species.add(
        new org.json.JSONObject()
            .put("key", "gnome")
            .put("name", "Gnomo")
            .put("size", "Pequeño")
            .put("speed", "30 pies"));
    model.changed();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Seleccionar especie").setSelection(1);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertNotNull(find(c.get(), "Tamaño: Mediano"));
    assertNull(find(c.get(), "Tamaño: Pequeño"));
    spinner(c.get().getWindow().getDecorView(), "Seleccionar especie").setSelection(2);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertNotNull(find(c.get(), "Tamaño: Pequeño"));
    assertNull(find(c.get(), "Tamaño: Mediano"));
    spinner(c.get().getWindow().getDecorView(), "Seleccionar especie").setSelection(0);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals("", d.speciesKey);
    assertNull(find(c.get(), "Tamaño: Pequeño"));
    assertFalse(find(c.get(), "Continuar").isEnabled());
    c.pause().stop().destroy();
  }

  @Test
  public void backgroundStepGuidesBoostsAndKeepsThemAfterRecreation() throws Exception {
    CharacterDraft d = draft();
    d.background.deferred = false;
    ActivityController<OfflineActivity> c = start(d);
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    try (InputStream in = getClass().getResourceAsStream("/creation-catalog.json")) {
      model.creation =
          new org.json.JSONObject(
              new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
    model.creationLoaded = true;
    model.changed();
    find(c.get(), "Continuar").performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals(CharacterDraft.Step.BACKGROUND, d.step);
    assertFalse(find(c.get(), "Continuar").isEnabled());
    spinner(c.get().getWindow().getDecorView(), "Seleccionar trasfondo").setSelection(3);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals("Sabio", d.background.name);
    assertTrue(d.background.skillGrantsApplied);
    find(c.get(), "Aplicar +1 a los tres").performClick();
    assertTrue(find(c.get(), "Continuar").isEnabled());
    spinner(c.get().getWindow().getDecorView(), "Seleccionar trasfondo").setSelection(4);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertFalse(find(c.get(), "Continuar").isEnabled());
    assertEquals(12, d.abilities.score(3));
    spinner(c.get().getWindow().getDecorView(), "Seleccionar trasfondo").setSelection(3);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Aumento de Inteligencia").setSelection(2);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Aumento de Constitución").setSelection(1);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertTrue(find(c.get(), "Continuar").isEnabled());
    assertEquals(14, d.finalScore(3));
    View root = c.get().findViewById(android.R.id.content);
    root.measure(
        View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 320, 640);
    Bitmap bitmap = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(bitmap));
    File file = new File("build/reports/creation/background-320.png");
    file.getParentFile().mkdirs();
    try (FileOutputStream out = new FileOutputStream(file)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    Bundle state = new Bundle();
    c.saveInstanceState(state).pause().stop().destroy();
    ActivityController<OfflineActivity> restored =
        Robolectric.buildActivity(OfflineActivity.class).create(state).start().resume().visible();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    CreationViewModel fresh = new ViewModelProvider(restored.get()).get(CreationViewModel.class);
    assertEquals("Sabio", fresh.draft.background.name);
    assertTrue(fresh.draft.background.skillGrantsApplied);
    assertEquals(14, fresh.draft.finalScore(3));
    assertTrue(find(restored.get(), "Continuar").isEnabled());
    restored.pause().stop().destroy();
  }

  @Test
  public void soldierRequiresToolAndEquipmentAndKeepsChoicesWhenGoingBack() throws Exception {
    CharacterDraft d = draft();
    d.background.deferred = false;
    d.step = CharacterDraft.Step.BACKGROUND;
    d.name = "Ada";
    ActivityController<OfflineActivity> c = start(d);
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    try (InputStream in = getClass().getResourceAsStream("/creation-catalog.json")) {
      model.creation =
          new org.json.JSONObject(
              new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
    model.creationLoaded = true;
    model.changed();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), "Seleccionar trasfondo").setSelection(4);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    find(c.get(), "Aplicar +1 a los tres").performClick();
    find(c.get(), "Continuar").performClick();
    assertEquals(CharacterDraft.Step.BACKGROUND_LOADOUT, d.step);
    assertFalse(find(c.get(), "Continuar").isEnabled());
    find(c.get(), "Opción B · 50 po").performClick();
    assertFalse(find(c.get(), "Continuar").isEnabled());
    spinner(c.get().getWindow().getDecorView(), "Herramienta del trasfondo").setSelection(3);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertTrue(find(c.get(), "Continuar").isEnabled());
    assertEquals("cards", d.background.toolChoice);
    org.json.JSONObject payload = new org.json.JSONObject(d.payload());
    assertEquals(
        "B",
        payload.getJSONObject("creation").getJSONObject("background").getString("equipmentOption"));
    find(c.get(), "Atrás").performClick();
    find(c.get(), "Continuar").performClick();
    assertEquals("B", d.background.equipmentOption);
    assertEquals("cards", d.background.toolChoice);
    c.pause().stop().destroy();
  }

  @Test
  public void magicInitiateRequiresDistinctCantripsAndSurvivesProcessDeath() throws Exception {
    CharacterDraft d = draft();
    d.background.deferred = false;
    d.step = CharacterDraft.Step.BACKGROUND;
    d.name = "Ada";
    ActivityController<OfflineActivity> c = start(d);
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    try (InputStream in = getClass().getResourceAsStream("/creation-catalog.json")) {
      model.creation =
          new org.json.JSONObject(
              new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
    model.creationLoaded = true;
    model.changed();
    select(c, "Seleccionar trasfondo", 3);
    find(c.get(), "Aplicar +1 a los tres").performClick();
    find(c.get(), "Continuar").performClick();
    find(c.get(), "Opción B · 50 po").performClick();
    find(c.get(), "Continuar").performClick();
    assertEquals(CharacterDraft.Step.ORIGIN_FEAT, d.step);
    assertFalse(find(c.get(), "Continuar").isEnabled());
    select(c, "Característica de Iniciado en la magia", 3);
    select(c, "Primer truco", 1);
    select(c, "Segundo truco", 1);
    select(c, "Conjuro de nivel 1", 1);
    assertFalse(find(c.get(), "Continuar").isEnabled());
    select(c, "Segundo truco", 2);
    assertTrue(find(c.get(), "Continuar").isEnabled());
    java.util.Map<String, Object> payload = d.payload();
    assertEquals(
        "charisma",
        new org.json.JSONObject(payload)
            .getJSONObject("creation")
            .getJSONObject("background")
            .getJSONObject("originFeatChoice")
            .getString("castingAbility"));
    Bundle state = new Bundle();
    c.saveInstanceState(state).pause().stop().destroy();
    ActivityController<OfflineActivity> restored =
        Robolectric.buildActivity(OfflineActivity.class).create(state).start().resume().visible();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    CreationViewModel fresh = new ViewModelProvider(restored.get()).get(CreationViewModel.class);
    assertEquals(payload, fresh.draft.payload());
    assertTrue(find(restored.get(), "Continuar").isEnabled());
    restored.pause().stop().destroy();
  }

  @Test
  public void classSkillsRequireExactCountPersistAndResetOnClassChange() throws Exception {
    CharacterDraft d = draft();
    d.background.select("srd-2024_sage", "Sabio", "", new int[] {2, 3, 4});
    d.background.increases[2] = 1;
    d.background.increases[3] = 2;
    d.name = "Ada";
    d.step = CharacterDraft.Step.CLASS_GUIDE;
    ActivityController<OfflineActivity> c = start(d);
    CreationViewModel model = new ViewModelProvider(c.get()).get(CreationViewModel.class);
    try (InputStream in = getClass().getResourceAsStream("/creation-catalog.json")) {
      model.creation =
          new org.json.JSONObject(
                  new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
              .put("classSkillsVersion", 1);
    }
    model.creationLoaded = true;
    model.changed();
    find(c.get(), "Continuar").performClick();
    assertEquals(CharacterDraft.Step.CLASS_SKILLS, d.step);
    assertFalse(find(c.get(), "Continuar").isEnabled());
    find(c.get(), "Conocimiento arcano").performClick();
    assertFalse(find(c.get(), "Continuar").isEnabled());
    find(c.get(), "Investigación").performClick();
    assertTrue(find(c.get(), "Continuar").isEnabled());
    assertFalse(find(c.get(), "Historia").isEnabled());
    java.util.Map<String, Object> payload = d.payload();
    Bundle state = new Bundle();
    c.saveInstanceState(state).pause().stop().destroy();
    ActivityController<OfflineActivity> restored =
        Robolectric.buildActivity(OfflineActivity.class).create(state).start().resume().visible();
    CreationViewModel fresh = new ViewModelProvider(restored.get()).get(CreationViewModel.class);
    assertEquals(payload, fresh.draft.payload());
    assertTrue(find(restored.get(), "Continuar").isEnabled());
    find(restored.get(), "Conocimiento arcano").performClick();
    assertFalse(find(restored.get(), "Continuar").isEnabled());
    assertTrue(find(restored.get(), "Historia").isEnabled());
    fresh.creation = model.creation;
    fresh.draft.step = CharacterDraft.Step.CLASS;
    fresh.changed();
    select(restored, "Seleccionar clase", 2);
    assertTrue(fresh.draft.classSkills.selected.isEmpty());
    assertEquals(3, fresh.draft.classSkills.count);
    restored.pause().stop().destroy();
  }

  @Test
  public void reviewShowsInitialBaseHpUsingFinalConstitutionAndClassSaves() {
    CharacterDraft d = draft();
    d.background.select("srd-2024_sage", "Sabio", "", new int[] {2, 3, 4});
    d.background.increases[2] = 1;
    d.background.increases[3] = 2;
    d.basicsClassKey = "wizard";
    d.initialHitDie = 6;
    d.initialSaves = "Inteligencia, Sabiduría";
    d.name = "Ada";
    d.step = CharacterDraft.Step.REVIEW;
    ActivityController<OfflineActivity> c = start(d);
    assertEquals(8, d.initialBaseHp());
    assertNotNull(find(c.get(), "PV base de nivel 1: 8 (6 + Constitución)"));
    assertNotNull(find(c.get(), "Salvaciones de clase: Inteligencia, Sabiduría"));
    d.classIndex = 0;
    assertFalse(d.hasClassBasics());
    c.pause().stop().destroy();
  }

  private void select(ActivityController<OfflineActivity> c, String label, int index) {
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    spinner(c.get().getWindow().getDecorView(), label).setSelection(index);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  }

  private Spinner spinner(View v, String description) {
    if (v instanceof Spinner && description.contentEquals(v.getContentDescription()))
      return (Spinner) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        Spinner found = spinner(((ViewGroup) v).getChildAt(i), description);
        if (found != null) return found;
      }
    return null;
  }

  private TextView find(CrearPersonajeActivity a, String text) {
    return find(a.getWindow().getDecorView(), text);
  }

  private TextView find(View v, String text) {
    if (v instanceof TextView && ((TextView) v).getText().toString().equals(text))
      return (TextView) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        TextView found = find(((ViewGroup) v).getChildAt(i), text);
        if (found != null) return found;
      }
    return null;
  }

  private EditText edit(View v) {
    if (v instanceof EditText) return (EditText) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        EditText found = edit(((ViewGroup) v).getChildAt(i));
        if (found != null) return found;
      }
    return null;
  }
}
