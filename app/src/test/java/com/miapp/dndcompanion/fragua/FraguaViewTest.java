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
    assertTrue(((ViewGroup) home.getChildAt(0)).getChildAt(0).performClick());
    for (String label : new String[] {"Aster", "▤\nNotas", "♢\nSalvaciones", "✧\nHabilidades"}) {
      TextView button = find(home, label);
      assertNotNull(label, button);
      assertTrue(button.performClick());
    }
    assertEquals(Arrays.asList("inventory", "sheet", "notes", "saves", "skills"), routes);
    assertEquals(before, sheet.toString());
    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(14, 16, 14, 16);
    root.setBackgroundResource(R.drawable.fondo);
    TextView title = FraguaUi.text(activity, "♨  FRAGUA", 31, FraguaUi.GOLD);
    title.setGravity(Gravity.CENTER);
    root.addView(title);
    ScrollView scroll = new ScrollView(activity);
    scroll.addView(home);
    root.addView(scroll);
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
