package com.miapp.dndcompanion.fragua;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
import org.json.*;

public final class DrawingCanvas extends View {
  private JSONArray strokes = new JSONArray();
  private final ArrayDeque<JSONObject> redo = new ArrayDeque<>();
  private JSONObject active;
  private final Paint p = new Paint(3);
  private boolean editing;

  public DrawingCanvas(Context c, JSONArray input, boolean editing) {
    super(c);
    this.editing = editing;
    try {
      strokes = new JSONArray(input.toString());
    } catch (Exception ignored) {
    }
    setBackgroundColor(0xFF08151A);
    setContentDescription("Lienzo de dibujo. Deshacer y limpiar disponibles encima.");
  }

  public JSONArray strokes() {
    return strokes;
  }

  public void undo() {
    if (strokes.length() > 0) {
      redo.push(strokes.optJSONObject(strokes.length() - 1));
      strokes.remove(strokes.length() - 1);
      invalidate();
    }
  }

  public void redo() {
    if (!redo.isEmpty()) {
      strokes.put(redo.pop());
      invalidate();
    }
  }

  public void clear() {
    strokes = new JSONArray();
    redo.clear();
    invalidate();
  }

  @Override
  protected void onDraw(Canvas c) {
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeCap(Paint.Cap.ROUND);
    p.setStrokeJoin(Paint.Join.ROUND);
    for (int i = 0; i < strokes.length(); i++) {
      JSONObject s = strokes.optJSONObject(i);
      JSONArray points = s.optJSONArray("points");
      p.setColor(Color.parseColor(s.optString("color", "#DBB56B")));
      p.setStrokeWidth((float) s.optDouble("width", 2));
      Path path = new Path();
      for (int j = 0; j < points.length(); j++) {
        JSONObject point = points.optJSONObject(j);
        float x = (float) point.optDouble("x") * getWidth(),
            y = (float) point.optDouble("y") * getHeight();
        if (j == 0) path.moveTo(x, y);
        else path.lineTo(x, y);
      }
      c.drawPath(path, p);
    }
  }

  @Override
  public boolean onTouchEvent(android.view.MotionEvent e) {
    if (!editing) return false;
    try {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        if (strokes.length() >= 300) return false;
        getParent().requestDisallowInterceptTouchEvent(true);
        active =
            new JSONObject().put("color", "#DBB56B").put("width", 3).put("points", new JSONArray());
        strokes.put(active);
        redo.clear();
      }
      if (active != null && active.getJSONArray("points").length() < 2000)
        active
            .getJSONArray("points")
            .put(
                new JSONObject()
                    .put("x", Math.max(0, Math.min(1, e.getX() / getWidth())))
                    .put("y", Math.max(0, Math.min(1, e.getY() / getHeight()))));
      if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
        getParent().requestDisallowInterceptTouchEvent(false);
        active = null;
        performClick();
      }
      invalidate();
      return true;
    } catch (Exception error) {
      return false;
    }
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }
}
