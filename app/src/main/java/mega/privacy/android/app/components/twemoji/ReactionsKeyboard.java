package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiSelectedListener;

import static mega.privacy.android.app.utils.Constants.*;

public class ReactionsKeyboard extends LinearLayout {

    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;
    private RecentEmoji recentEmoji;
    private View rootView;
    private int keyboardHeight;

    private final OnEmojiLongClickListener longClickListener = new OnEmojiLongClickListener() {
        @Override
        public void onEmojiLongClick(@NonNull final EmojiImageView view, @NonNull final Emoji emoji) {
            variantPopup.show(view, emoji);
        }
    };

    private OnEmojiSelectedListener emojiSelectedListener;
    private final OnEmojiClickListener clickListener = new OnEmojiClickListener() {
        @Override
        public void onEmojiClick(@NonNull final EmojiImageView imageView, @NonNull final Emoji emoji) {
            recentEmoji.addEmoji(emoji);
            variantEmoji.addVariant(emoji);
            imageView.updateEmoji(emoji);
            variantPopup.dismiss();

            if (emojiSelectedListener == null)
                return;

            emojiSelectedListener.emojiSelected(emoji);
        }
    };

    public ReactionsKeyboard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ReactionsKeyboard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiKeyboard, defStyle, 0);
        a.recycle();

        this.rootView = getRootView();
        this.variantEmoji = new VariantEmojiManager(getContext(), TYPE_REACTION);
        this.recentEmoji = new RecentEmojiManager(getContext(), TYPE_REACTION);
        this.variantPopup = new EmojiVariantPopup(rootView, clickListener);
        final EmojiView emojiView = new EmojiView(getContext(), clickListener, longClickListener, recentEmoji, variantEmoji, TYPE_REACTION);
        addView(emojiView);
    }

    public void init(int height) {
        this.keyboardHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(keyboardHeight, MeasureSpec.EXACTLY));
    }

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener emojiSelectedListener) {
        this.emojiSelectedListener = emojiSelectedListener;
    }

    public void persistReactionList() {
        recentEmoji.persist();
        variantEmoji.persist();
    }
}

