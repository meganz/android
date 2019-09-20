package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

import mega.privacy.android.app.components.twemoji.emoji.Emoji;

final class EmojiSpan extends ReplacementSpan {
  private final float size;
  private final Context context;
  private final Emoji emoji;
  private Drawable deferredDrawable;

  public EmojiSpan(final Context context, final Emoji emoji, final float size) {
    this.context = context;
    this.emoji = emoji;
    this.size = size;
  }

   public Drawable getDrawable() {

     if (deferredDrawable == null) {
       deferredDrawable = emoji.getDrawable(context);
       deferredDrawable.setBounds(0, 0, (int) size, (int) size);
    }
    return deferredDrawable;
  }
  @Override public int getSize(final Paint paint, final CharSequence text, final int start, final int end, final Paint.FontMetricsInt fontMetrics) {
    if (fontMetrics != null) {
      final Paint.FontMetrics paintFontMetrics = paint.getFontMetrics();
      final float fontHeight = paintFontMetrics.descent - paintFontMetrics.ascent;
      final float centerY = paintFontMetrics.ascent + fontHeight / 2;
      fontMetrics.ascent = (int) (centerY - size / 2);
      fontMetrics.top = fontMetrics.ascent;
      fontMetrics.bottom = (int) (centerY + size / 2);
      fontMetrics.descent = fontMetrics.bottom;
    }
    return (int) size;
  }
  @Override public void draw(final Canvas canvas, final CharSequence text, final int start, final int end, final float x, final int top, final int y, final int bottom, final Paint paint) {
    final Drawable drawable = getDrawable();
    final Paint.FontMetrics paintFontMetrics = paint.getFontMetrics();
    final float fontHeight = paintFontMetrics.descent - paintFontMetrics.ascent;
    final float centerY = y + paintFontMetrics.descent - fontHeight / 2;
    final float transitionY = centerY - size / 2;
    canvas.save();
    canvas.translate(x, transitionY);
    drawable.draw(canvas);
    canvas.restore();
  }
}