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
  private boolean home;
  private int pressed = -1;
  private float downX, downY;
  private static final float[] HOME_ANGLES = {225, 315, 30, 90, 150};

  public void homeStyle() {
    home = true;
    int[] icons = {
      com.miapp.dndcompanion.R.drawable.ic_fragua_shield,
      com.miapp.dndcompanion.R.drawable.ic_fragua_skill,
      com.miapp.dndcompanion.R.drawable.ic_fragua_book,
      com.miapp.dndcompanion.R.drawable.ic_fragua_scroll,
      com.miapp.dndcompanion.R.drawable.ic_fragua_swords
    };
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].setCompoundDrawablesWithIntrinsicBounds(0, icons[i], 0, 0);
      buttons[i].setCompoundDrawablePadding(FraguaUi.dp(getContext(), 4));
      buttons[i].setPadding(0, 0, 0, 0);
      buttons[i].setTextSize(15);
    }
    requestLayout();
    invalidate();
  }

  private double angle(int i) {
    return home ? HOME_ANGLES[i] : -90 + 360.0 * i / buttons.length;
  }

  private int target(float x, float y) {
    float h = getWidth() / 2f, dx = x - h, dy = y - h;
    double distance = Math.hypot(dx, dy);
    if (distance > h - 5) return -1;
    if (distance < h * .27f) return center == null ? -1 : buttons.length;
    double a = (Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360;
    if (home) return a < 60 ? 2 : a < 120 ? 3 : a < 180 ? 4 : a < 270 ? 0 : 1;
    return (int) Math.floor(((a + 90 + 180.0 / buttons.length) % 360) / (360.0 / buttons.length));
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        pressed = target(event.getX(), event.getY());
        downX = event.getX();
        downY = event.getY();
        invalidate();
        return pressed >= 0;
      case MotionEvent.ACTION_MOVE:
        if (Math.hypot(event.getX() - downX, event.getY() - downY)
            > ViewConfiguration.get(getContext()).getScaledTouchSlop()) pressed = -1;
        invalidate();
        return true;
      case MotionEvent.ACTION_UP:
        int selected = pressed;
        pressed = -1;
        invalidate();
        if (selected >= 0 && selected == target(event.getX(), event.getY())) {
          (selected == buttons.length ? center : buttons[selected]).performClick();
        }
        return true;
      case MotionEvent.ACTION_CANCEL:
        pressed = -1;
        invalidate();
        return true;
      default:
        return true;
    }
  }

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
    center.setBackgroundResource(com.miapp.dndcompanion.R.drawable.fragua_medallion);
    if (home) {
      center.setCompoundDrawablesWithIntrinsicBounds(
          0, com.miapp.dndcompanion.R.drawable.ic_fragua_dice, 0, 0);
      center.setPadding(0, 0, 0, 0);
    }
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
      double angle = Math.toRadians(angle(i));
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
    if (home) {
      // Reference silhouette: clipped upper panels, diagonal lower panels and mission fan.
      float[][] outlines = {
        {
          .08f, .45f, .14f, .25f, .25f, .10f, .44f, .10f, .48f, .16f, .48f, .41f, .41f, .48f, .12f,
          .48f
        },
        {
          .92f, .45f, .86f, .25f, .75f, .10f, .56f, .10f, .52f, .16f, .52f, .41f, .59f, .48f, .88f,
          .48f
        },
        {.90f, .52f, .61f, .52f, .53f, .59f, .71f, .85f, .81f, .83f, .91f, .66f},
        {.50f, .66f, .73f, .89f, .65f, .96f, .35f, .96f, .27f, .89f},
        {.10f, .52f, .39f, .52f, .47f, .59f, .29f, .85f, .19f, .83f, .09f, .66f}
      };
      for (int i = 0; i < outlines.length; i++) {
        Path sector = new Path();
        float[] points = outlines[i];
        sector.moveTo(points[0] * getWidth(), points[1] * getWidth());
        for (int j = 2; j < points.length; j += 2)
          sector.lineTo(points[j] * getWidth(), points[j + 1] * getWidth());
        sector.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pressed == i ? 0xB06E5127 : 0xA0031720);
        c.drawPath(sector, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(FraguaUi.dp(getContext(), 1));
        paint.setColor(FraguaUi.GOLD);
        c.drawPath(sector, paint);
        c.save();
        c.scale(.96f, .96f, h, h);
        paint.setColor(0x806F512B);
        c.drawPath(sector, paint);
        c.restore();
      }
      for (int i = 0; i < 4; i++) {
        c.save();
        c.rotate(i * 90, h, h);
        Path star = new Path();
        star.moveTo(h, 0);
        star.lineTo(h + 4, 11);
        star.lineTo(h + 12, 16);
        star.lineTo(h + 4, 20);
        star.lineTo(h, 30);
        star.lineTo(h - 4, 20);
        star.lineTo(h - 12, 16);
        star.lineTo(h - 4, 11);
        star.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(FraguaUi.GOLD);
        c.drawPath(star, paint);
        c.restore();
      }
    }
    paint.setColor(FraguaUi.GOLD);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(FraguaUi.dp(getContext(), 1));
    c.drawCircle(h, h, r, paint);
    paint.setColor(0x706B5535);
    c.drawCircle(h, h, r - 7, paint);
    for (int i = 0; !home && i < labels.length; i++) {
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
