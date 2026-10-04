package com.miapp.dndcompanion.fragua;

import static org.junit.Assert.*;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import com.miapp.dndcompanion.R;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FraguaViewTest {
  @Test
  public void homeRoutesCharacterInventoryNotesAndRadialWithoutChangingSnapshot() throws Exception {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    JSONObject sheet =
        new JSONObject(
            new String(
                getClass().getResourceAsStream("/sheet.json").readAllBytes(),
                StandardCharsets.UTF_8));
    String before = sheet.toString();
    List<String> routes = new ArrayList<>();
    HomeScreen home =
        new HomeScreen(activity, sheet, false, (destination, item) -> routes.add(destination));
    assertTrue(home.findViewById(R.id.home_inventory).performClick());
    for (String label : new String[] {"Aster", "Notas", "Salvaciones", "Habilidades"}) {
      TextView button = find(home, label);
      assertNotNull(label, button);
      assertTrue(button.performClick());
    }
    assertEquals(Arrays.asList("inventory", "sheet", "notes", "saves", "skills"), routes);
    assertEquals(before, sheet.toString());
    LinearLayout root =
        (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.view_fragua_shell, null);
    ((LinearLayout) root.findViewById(R.id.fragua_body)).addView(home);
    activity.setContentView(root);
    root.measure(
        View.MeasureSpec.makeMeasureSpec(393, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 393, 1200);
    Bitmap image = Bitmap.createBitmap(393, 1200, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(image));
    File file = new File("build/reports/fragua/home.png");
    file.getParentFile().mkdirs();
    try (FileOutputStream out = new FileOutputStream(file)) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
  }

  @Test
  public void sixSavesHaveDistinctAccessibleTouchTargetsAtSmallWidth() {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    RadialMenu wheel =
        new RadialMenu(
            activity,
            new String[] {
              "Fuerza", "Destreza", "Constitución", "Inteligencia", "Sabiduría", "Carisma"
            },
            i -> {});
    wheel.measure(
        View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY));
    wheel.layout(0, 0, 320, 320);
    for (int i = 0; i < 6; i++) {
      View child = wheel.getChildAt(i);
      assertTrue(child.isClickable());
      assertTrue(child.getWidth() >= 48);
      assertTrue(child.getHeight() >= 48);
      Rect a = new Rect();
      child.getHitRect(a);
      assertTrue(new Rect(0, 0, 320, 320).contains(a));
      for (int j = 0; j < i; j++) {
        Rect b = new Rect();
        wheel.getChildAt(j).getHitRect(b);
        assertFalse(Rect.intersects(a, b));
      }
    }
  }

  @Test
  public void notebookDrawingKeepsNormalizedStrokesAndDoesNotMutateReadOnlyInput()
      throws Exception {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    JSONArray source =
        new JSONArray("[{\"color\":\"#DBB56B\",\"width\":3,\"points\":[{\"x\":0.2,\"y\":0.3}]}]");
    DrawingCanvas canvas = new DrawingCanvas(activity, source, true);
    canvas.undo();
    assertEquals(0, canvas.strokes().length());
    canvas.redo();
    assertEquals(1, canvas.strokes().length());
    canvas.clear();
    assertEquals(1, source.length());
    DrawingCanvas readOnly = new DrawingCanvas(activity, source, false);
    MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50, 50, 0);
    assertFalse(readOnly.onTouchEvent(down));
    down.recycle();
    assertEquals(source.toString(), readOnly.strokes().toString());
  }

  @Test
  public void radialWholeSectorsRespondAndDraggingDoesNotChoose() {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    List<Integer> selected = new ArrayList<>();
    RadialMenu wheel =
        new RadialMenu(
            activity,
            new String[] {"Saves", "Skills", "Spells", "Missions", "Actions"},
            selected::add);
    wheel.homeStyle();
    wheel.center("Dice", () -> selected.add(5));
    wheel.measure(
        View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY));
    wheel.layout(0, 0, 360, 360);
    double[] angles = {225, 315, 30, 90, 150};
    for (double angle : angles) {
      float x = 180 + (float) Math.cos(Math.toRadians(angle)) * 162;
      float y = 180 + (float) Math.sin(Math.toRadians(angle)) * 162;
      touch(wheel, MotionEvent.ACTION_DOWN, x, y);
      touch(wheel, MotionEvent.ACTION_UP, x, y);
    }
    touch(wheel, MotionEvent.ACTION_DOWN, 180, 180);
    touch(wheel, MotionEvent.ACTION_UP, 180, 180);
    assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), selected);
    touch(wheel, MotionEvent.ACTION_DOWN, 180, 340);
    touch(wheel, MotionEvent.ACTION_MOVE, 180, 280);
    touch(wheel, MotionEvent.ACTION_UP, 180, 280);
    touch(wheel, MotionEvent.ACTION_DOWN, 180, 340);
    touch(wheel, MotionEvent.ACTION_CANCEL, 180, 340);
    touch(wheel, MotionEvent.ACTION_UP, 180, 340);
    assertEquals(6, selected.size());
  }

  private void touch(View view, int action, float x, float y) {
    MotionEvent event = MotionEvent.obtain(0, 10, action, x, y, 0);
    view.dispatchTouchEvent(event);
    event.recycle();
  }

  @Test
  public void spellsSearchAndCollapseKeepActualSpellNavigation() throws Exception {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    JSONObject sheet =
        new JSONObject(
            new String(
                getClass().getResourceAsStream("/sheet.json").readAllBytes(),
                StandardCharsets.UTF_8));
    sheet
        .getJSONObject("state")
        .put(
            "spells",
            new JSONArray(
                "[{\"name\":\"Luz\",\"level\":0,\"prepared\":true},{\"name\":\"Escudo\",\"level\":1,\"prepared\":true}]"));
    List<String> opened = new ArrayList<>();
    SpellsScreen screen =
        new SpellsScreen(activity, sheet, spell -> opened.add(spell.optString("name")));
    EditText search = screen.findViewById(R.id.spells_search);
    search.setText("escudo");
    assertNull(find(screen, "Luz"));
    TextView spell = find(screen, "Escudo");
    assertNotNull(spell);
    ((View) spell.getParent().getParent()).performClick();
    assertEquals(Arrays.asList("Escudo"), opened);
    TextView heading = find(screen, "NIVEL 1");
    heading.performClick();
    assertEquals(View.GONE, screen.findViewById(R.id.spell_group_rows).getVisibility());
    search.setText("");
    screen.setBackgroundResource(R.drawable.fondo);
    screen.measure(
        View.MeasureSpec.makeMeasureSpec(393, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY));
    screen.layout(0, 0, 393, 1200);
    Bitmap image = Bitmap.createBitmap(393, 1200, Bitmap.Config.ARGB_8888);
    screen.draw(new Canvas(image));
    File file = new File("build/reports/fragua/spells.png");
    file.getParentFile().mkdirs();
    try (FileOutputStream out = new FileOutputStream(file)) {
      image.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
  }

  private TextView find(View view, String prefix) {
    if (view instanceof TextView && ((TextView) view).getText().toString().startsWith(prefix))
      return (TextView) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        TextView found = find(group.getChildAt(i), prefix);
        if (found != null) return found;
      }
    }
    return null;
  }
}
