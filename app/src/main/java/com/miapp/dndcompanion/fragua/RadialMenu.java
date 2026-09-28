package com.miapp.dndcompanion.fragua;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;

public final class RadialMenu extends FrameLayout {
  private final String[] labels;
  private final Paint paint = new Paint(3);
  private final TextView[] buttons;
  private TextView center;

  public RadialMenu(Context c, String[] labels, java.util.function.IntConsumer choose) {
    super(c);
    this.labels = labels;
    setWillNotDraw(false);
    buttons = new TextView[labels.length];
    for (int i = 0; i < labels.length; i++) {
      final int index = i;
      buttons[i] = FraguaUi.button(c, labels[i], () -> choose.accept(index));
      buttons[i].setTextSize(13);
      buttons[i].setBackgroundColor(Color.TRANSPARENT);
      addView(buttons[i]);
    }
  }

  public void center(String label, Runnable action) {
    center = FraguaUi.button(getContext(), label, action);
    center.setTextSize(14);
    addView(center);
  }

  @Override
  protected void onMeasure(int w, int h) {
    int size = MeasureSpec.getSize(w);
    setMeasuredDimension(size, size);
    int cell = (int) (size * (labels.length > 6 ? .22f : labels.length == 6 ? .27f : .30f));
    for (TextView b : buttons)
      b.measure(
          MeasureSpec.makeMeasureSpec(cell, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(
              Math.max(FraguaUi.dp(getContext(), 52), cell), MeasureSpec.EXACTLY));
    if (center != null)
      center.measure(
          MeasureSpec.makeMeasureSpec((int) (size * .23), MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec((int) (size * .23), MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean ch, int l, int t, int r, int b) {
    float half = getWidth() / 2f, rad = half * .66f;
    for (int i = 0; i < buttons.length; i++) {
      double angle = Math.toRadians(-90 + 360.0 * i / buttons.length);
      View v = buttons[i];
      int x = (int) (half + Math.cos(angle) * rad - v.getMeasuredWidth() / 2f),
          y = (int) (half + Math.sin(angle) * rad - v.getMeasuredHeight() / 2f);
      v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
    }
    if (center != null) {
      int n = center.getMeasuredWidth();
      center.layout(
          (getWidth() - n) / 2, (getHeight() - n) / 2, (getWidth() + n) / 2, (getHeight() + n) / 2);
    }
  }

  @Override
  protected void onDraw(Canvas c) {
    float h = getWidth() / 2f, r = h - 5;
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(
        new RadialGradient(
            h, h, r, new int[] {0xF0082730, 0xE0021016}, null, Shader.TileMode.CLAMP));
    c.drawCircle(h, h, r, paint);
    paint.setShader(null);
    paint.setColor(FraguaUi.GOLD);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(FraguaUi.dp(getContext(), 1));
    c.drawCircle(h, h, r, paint);
    paint.setColor(0x706B5535);
    c.drawCircle(h, h, r - 7, paint);
    for (int i = 0; i < labels.length; i++) {
      double a = Math.toRadians(-90 + 360.0 * (i + .5) / labels.length);
      c.drawLine(
          h + (float) Math.cos(a) * h * .27f,
          h + (float) Math.sin(a) * h * .27f,
          h + (float) Math.cos(a) * (r - 9),
          h + (float) Math.sin(a) * (r - 9),
          paint);
    }
    paint.setColor(FraguaUi.GOLD);
    c.drawCircle(h, h, h * .25f, paint);
    paint.setStyle(Paint.Style.FILL);
  }
}
