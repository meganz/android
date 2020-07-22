package mega.privacy.android.app.components.twemoji;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PaddingSpan extends ReplacementSpan {

  private int padding;
  private RectF rect;

  public PaddingSpan(int padding) {
    this.padding = padding;
    rect = new RectF();
  }

  @Override public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
      @Nullable Paint.FontMetricsInt fm) {
    return padding;
  }

  @Override
  public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top,
      int y, int bottom, @NonNull Paint paint) {
    rect.set(x, top, x + padding, bottom);
    // transparent
    paint.setColor(0);
    canvas.drawRect(rect, paint);
  }
}
