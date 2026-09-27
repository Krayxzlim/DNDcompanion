package com.miapp.dndcompanion.fragua;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Native ornaments: decorative paint is independent from the accessible controls above it. */
public final class FraguaUi {
  public static final int BG = Color.rgb(5, 17, 22),
      PANEL = Color.rgb(9, 29, 35),
      GOLD = Color.rgb(219, 181, 107),
      INK = Color.rgb(242, 226, 191),
      MUTED = Color.rgb(163, 185, 190);

  public static int dp(Context c, float n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }

  public static TextView text(Context c, String s, int size, int color) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    t.setPadding(dp(c, 4), dp(c, 5), dp(c, 4), dp(c, 5));
    return t;
  }

  public static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  public static LinearLayout row(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setGravity(Gravity.CENTER_VERTICAL);
    l.setOrientation(LinearLayout.HORIZONTAL);
    return l;
  }

  public static LinearLayout panel(Context c) {
    LinearLayout l = column(c);
    l.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
    l.setBackground(new Frame());
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(c, 7), 0, dp(c, 7));
    l.setLayoutParams(p);
    return l;
  }

  public static TextView button(Context c, String label, Runnable action) {
    TextView b = text(c, label, 15, GOLD);
    b.setGravity(Gravity.CENTER);
    b.setMinHeight(dp(c, 48));
    b.setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10));
    b.setBackground(new Frame());
    b.setClickable(true);
    b.setFocusable(true);
    b.setContentDescription(label);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  public static EditText input(Context c, String hint, String value, boolean numeric) {
    EditText e = new EditText(c);
    e.setTextColor(INK);
    e.setHintTextColor(MUTED);
    e.setTextSize(16);
    e.setHint(hint);
    e.setText(value);
    e.setPadding(dp(c, 10), dp(c, 8), dp(c, 10), dp(c, 8));
    e.setBackground(new Frame());
    e.setSingleLine(numeric);
    if (numeric)
      e.setInputType(
          android.text.InputType.TYPE_CLASS_NUMBER
              | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    e.setContentDescription(hint);
    return e;
  }

  public static String signed(int n) {
    return n >= 0 ? "+" + n : String.valueOf(n);
  }

  public static final class Frame extends Drawable {
    private final Paint p = new Paint(3);

    @Override
    public void draw(Canvas c) {
      Rect b = getBounds();
      float cut = 12;
      Path path = new Path();
      path.moveTo(b.left + cut, b.top + 1);
      path.lineTo(b.right - cut, b.top + 1);
      path.lineTo(b.right - 1, b.top + cut);
      path.lineTo(b.right - 1, b.bottom - cut);
      path.lineTo(b.right - cut, b.bottom - 1);
      path.lineTo(b.left + cut, b.bottom - 1);
      path.lineTo(b.left + 1, b.bottom - cut);
      path.lineTo(b.left + 1, b.top + cut);
      path.close();
      p.setStyle(Paint.Style.FILL);
      p.setShader(
          new LinearGradient(
              0,
              b.top,
              0,
              b.bottom,
              new int[] {0xF0183035, 0xF0051016},
              null,
              Shader.TileMode.CLAMP));
      c.drawPath(path, p);
      p.setShader(null);
      p.setStyle(Paint.Style.STROKE);
      p.setColor(0xB8C79B50);
      p.setStrokeWidth(1.5f);
      c.drawPath(path, p);
      p.setColor(0x466D5832);
      c.drawLine(b.left + 16, b.top + 6, b.right - 16, b.top + 6, p);
      c.drawLine(b.left + 16, b.bottom - 6, b.right - 16, b.bottom - 6, p);
      p.setStyle(Paint.Style.FILL);
    }

    @Override
    public void setAlpha(int a) {}

    @Override
    public void setColorFilter(ColorFilter f) {}

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
}
