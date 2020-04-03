package mega.privacy.android.app.components.twemoji;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiSelectedListener;

public class ReactionsKeyboard extends LinearLayout {

    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;
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
            if (emojiSelectedListener == null)
                return;

            emojiSelectedListener.emojiSelected(emoji);
        }
    };
    private View rootView;
    private int keyboardHeight;

    public ReactionsKeyboard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ReactionsKeyboard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ReactionsKeyboard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr);
    }


    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiKeyboard, defStyle, 0);
        a.recycle();
        this.rootView = getRootView();
        this.variantEmoji = new VariantEmojiManager(getContext());
        this.variantPopup = new EmojiVariantPopup(rootView, clickListener);

        final ReactionView emojiView = new ReactionView(getContext(), clickListener, longClickListener, variantEmoji);
        addView(emojiView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(keyboardHeight, MeasureSpec.EXACTLY));
    }

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener emojiSelectedListener) {
        this.emojiSelectedListener = emojiSelectedListener;
    }

    public void init(Activity context, int height) {
        this.keyboardHeight = height;
        requestLayout();
    }


}

