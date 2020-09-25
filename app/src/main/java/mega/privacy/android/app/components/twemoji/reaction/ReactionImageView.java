package mega.privacy.android.app.components.twemoji.reaction;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import mega.privacy.android.app.components.twemoji.ImageLoadingTask;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;

public class ReactionImageView extends AppCompatImageView {

    private Emoji currentEmoji;
    private ImageLoadingTask imageLoadingTask;

    public ReactionImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int measuredWidth = getMeasuredWidth();
        setMeasuredDimension(measuredWidth, measuredWidth);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (imageLoadingTask != null) {
            imageLoadingTask.cancel(true);
            imageLoadingTask = null;
        }
    }

    public Emoji getEmoji() {
        return currentEmoji;
    }

    public void setEmoji(@NonNull final Emoji emoji) {
        if (!emoji.equals(currentEmoji)) {
            addEmojiReaction(emoji);
        }
    }

    public void addEmojiReaction(@NonNull final Emoji emoji){
        setImageDrawable(null);
        currentEmoji = emoji;

        if (imageLoadingTask != null) {
            imageLoadingTask.cancel(true);
        }

        imageLoadingTask = new ImageLoadingTask(this);
        imageLoadingTask.execute(emoji);
    }
}
