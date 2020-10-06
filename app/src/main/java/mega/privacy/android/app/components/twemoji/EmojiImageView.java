package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;

public final class EmojiImageView extends AppCompatImageView {
  private static final int VARIANT_INDICATOR_PART_AMOUNT = 6;
  private static final int VARIANT_INDICATOR_PART = 5;

  Emoji currentEmoji;

  OnEmojiClickListener clickListener;
  OnEmojiLongClickListener longClickListener;

  private final Paint variantIndicatorPaint = new Paint();
  private final Path variantIndicatorPath = new Path();

  private final Point variantIndicatorTop = new Point();
  private final Point variantIndicatorBottomRight = new Point();
  private final Point variantIndicatorBottomLeft = new Point();

  private ImageLoadingTask imageLoadingTask;

  private boolean hasVariants;

  public EmojiImageView(final Context context) {
    super(context);
  }

  public EmojiImageView(final Context context, final AttributeSet attrs) {
    super(context, attrs);

    variantIndicatorPaint.setColor(ContextCompat.getColor(context, R.color.divider_upgrade_account));
    variantIndicatorPaint.setStyle(Paint.Style.FILL);
    variantIndicatorPaint.setAntiAlias(true);
  }

  @Override public void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    final int measuredWidth = getMeasuredWidth();
    //noinspection SuspiciousNameCombination
    setMeasuredDimension(measuredWidth, measuredWidth);
  }

  @Override protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    variantIndicatorTop.x = w;
    variantIndicatorTop.y = h / VARIANT_INDICATOR_PART_AMOUNT * VARIANT_INDICATOR_PART;
    variantIndicatorBottomRight.x = w;
    variantIndicatorBottomRight.y = h;
    variantIndicatorBottomLeft.x = w / VARIANT_INDICATOR_PART_AMOUNT * VARIANT_INDICATOR_PART;
    variantIndicatorBottomLeft.y = h;

    variantIndicatorPath.rewind();
    variantIndicatorPath.moveTo(variantIndicatorTop.x, variantIndicatorTop.y);
    variantIndicatorPath.lineTo(variantIndicatorBottomRight.x, variantIndicatorBottomRight.y);
    variantIndicatorPath.lineTo(variantIndicatorBottomLeft.x, variantIndicatorBottomLeft.y);
    variantIndicatorPath.close();
  }

  @Override protected void onDraw(final Canvas canvas) {
    super.onDraw(canvas);

    if (hasVariants && getDrawable() != null) {
      canvas.drawPath(variantIndicatorPath, variantIndicatorPaint);
    }
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (imageLoadingTask != null) {
      imageLoadingTask.cancel(true);
      imageLoadingTask = null;
    }
  }

  public Emoji getEmoji() {
    return currentEmoji;
  }

  public void setEmoji(@NonNull final Emoji emoji, boolean isInfoReaction) {
    if(emoji.equals(currentEmoji))
      return;

      setImageDrawable(null);
      currentEmoji = emoji;
      hasVariants = !isInfoReaction;

      if (imageLoadingTask != null) {
        imageLoadingTask.cancel(true);
      }

      imageLoadingTask = new ImageLoadingTask(this);
      imageLoadingTask.execute(emoji);
  }

  public void setEmoji(@NonNull final Emoji emoji) {
    if (!emoji.equals(currentEmoji)) {
      setImageDrawable(null);

      currentEmoji = emoji;
      hasVariants = emoji.getBase().hasVariants();

      if (imageLoadingTask != null) {
        imageLoadingTask.cancel(true);
      }

      setOnClickListener(view -> {
        if (clickListener != null) {
          clickListener.onEmojiClick(EmojiImageView.this, currentEmoji);
        }
      });

      setOnLongClickListener(hasVariants ? new OnLongClickListener() {
        @Override public boolean onLongClick(final View view) {
          longClickListener.onEmojiLongClick(EmojiImageView.this, currentEmoji);

          return true;
        }
      } : null);

      imageLoadingTask = new ImageLoadingTask(this);
      imageLoadingTask.execute(emoji);
    }
  }

  /**
   * Updates the emoji image directly. This should be called only for updating the variant
   * displayed (of the same base emoji), since it does not run asynchronously and does not update
   * the internal listeners.
   *
   * @param emoji The new emoji variant to show.
   */
  public void updateEmoji(@NonNull final Emoji emoji) {
    if (!emoji.equals(currentEmoji)) {
      currentEmoji = emoji;

      setImageDrawable(emoji.getDrawable(this.getContext()));
    }
  }

  void setOnEmojiClickListener(@Nullable final OnEmojiClickListener listener) {
    this.clickListener = listener;
  }

  void setOnEmojiLongClickListener(@Nullable final OnEmojiLongClickListener listener) {
    this.longClickListener = listener;
  }
}
