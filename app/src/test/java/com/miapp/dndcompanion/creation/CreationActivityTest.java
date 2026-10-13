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
    for (int i = 0; i < 3; i++) find(a, "Continuar").performClick();
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
