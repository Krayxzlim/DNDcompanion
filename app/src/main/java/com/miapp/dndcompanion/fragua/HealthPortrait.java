package com.miapp.dndcompanion.fragua;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import com.miapp.dndcompanion.R;

public final class HealthPortrait extends View {
  private final Paint p = new Paint(3);
  private final Bitmap avatar;
  private int hp, max, temp;

  public HealthPortrait(Context c, int hp, int max, int temp) {
    super(c);
    this.hp = hp;
    this.max = max;
    this.temp = temp;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 3;
    avatar = BitmapFactory.decodeResource(getResources(), R.drawable.avatar, options);
    setContentDescription(
        "Puntos de vida " + hp + " de " + max + ", temporales " + temp + ". Gestionar vida");
    setFocusable(true);
    setClickable(true);
  }

  @Override
  protected void onDraw(Canvas c) {
    float x = getWidth() / 2f, y = getHeight() / 2f, r = Math.min(x, y) - 14;
    RectF bounds = new RectF(x - r, y - r, x + r, y + r);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(8);
    p.setColor(0xFF173D40);
    c.drawCircle(x, y, r, p);
    p.setColor(hp == 0 ? 0xFFE66658 : hp < max * .25 ? 0xFFEEA94A : 0xFF51DB89);
    c.drawArc(bounds, -90, max > 0 ? 360f * hp / max : 0, false, p);
    p.setStrokeWidth(2);
    p.setColor(FraguaUi.GOLD);
    c.drawCircle(x, y, r + 7, p);
    c.drawCircle(x, y, r - 8, p);
    if (temp > 0) {
      p.setColor(0xFF6098FF);
      p.setStrokeWidth(4);
      c.drawArc(new RectF(x - r - 11, y - r - 11, x + r + 11, y + r + 11), -90, 90, false, p);
    }
    p.setStyle(Paint.Style.FILL);
    Path clip = new Path();
    clip.addCircle(x, y, r - 11, Path.Direction.CW);
    c.save();
    c.clipPath(clip);
    c.drawBitmap(avatar, null, new RectF(x - r, y - r, x + r, y + r), p);
    p.setShader(
        new LinearGradient(0, y, 0, y + r, Color.TRANSPARENT, 0xFF020D11, Shader.TileMode.CLAMP));
    c.drawRect(x - r, y, x + r, y + r, p);
    p.setShader(null);
    p.setColor(FraguaUi.INK);
    p.setTypeface(Typeface.create("serif", Typeface.BOLD));
    p.setTextSize(FraguaUi.dp(getContext(), 23));
    p.setTextAlign(Paint.Align.CENTER);
    c.drawText(max > 0 ? hp + " / " + max : "Sin configurar", x, y + r - 27, p);
    c.restore();
  }
}
