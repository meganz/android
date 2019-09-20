package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.support.annotation.CallSuper;
import android.support.annotation.DimenRes;
import android.support.annotation.Px;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;


//Reference implementation for an EditText with emoji support
public class EmojiEditText extends AppCompatEditText implements EmojiEditTextInterface {
    private float emojiSize;

    public EmojiEditText(final Context context) {
        this(context, null);
    }
    public EmojiEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            EmojiManager.getInstance().verifyInstalled();
        }

        final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;

        if (attrs == null) {
            emojiSize = defaultEmojiSize;
        } else {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiEditText);
            try {
                emojiSize = a.getDimension(R.styleable.EmojiEditText_emojiSize, defaultEmojiSize);
            } finally {
                a.recycle();
            }
        }
        setText(getText());
    }

    @Override protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if(text.toString().equals("")){
        }else{
            if(lengthAfter>lengthBefore){
                final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
                final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
                EmojiManager.getInstance().replaceWithImages(getContext(), getText(), emojiSize, defaultEmojiSize);
                super.onTextChanged( getText(),start,lengthBefore,lengthAfter);
            }

        }
    }

    @Override @CallSuper public void input(final Emoji emoji) {
        if (emoji != null) {
            final int start = getSelectionStart();
            final int end = getSelectionEnd();

            if (start < 0) {
                append(emoji.getUnicode());
            } else {
                getText().replace(Math.min(start, end), Math.max(start, end), emoji.getUnicode(), 0, emoji.getUnicode().length());
            }
        }
    }

    @Override public float getEmojiSize() {
        return emojiSize;
    }

    @Override @CallSuper public void backspace() {
        final KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        dispatchKeyEvent(event);
    }

//    @Override public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
//        return new SoftKeyboardInputConnection(super.onCreateInputConnection(outAttrs), true);
//    }
//
//    private class SoftKeyboardInputConnection extends InputConnectionWrapper {
//
//        public SoftKeyboardInputConnection(InputConnection target, boolean mutable) {
//            super(target, mutable);
//        }
//
//        @Override
//        public boolean sendKeyEvent(KeyEvent event) {
//            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
//                backspace();
//            }
//            return false;
//        }
//
//        @Override
//        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
//            backspace();
//            return false;
//        }
//    }

    @Override public final void setEmojiSize(@Px final int pixels) {
        setEmojiSize(pixels, true);
    }
    @Override public final void setEmojiSize(@Px final int pixels, final boolean shouldInvalidate) {
        emojiSize = pixels;
        if (shouldInvalidate) {
            setText(getText());
        }
    }
    @Override public final void setEmojiSizeRes(@DimenRes final int res) {
        setEmojiSizeRes(res, true);
    }
    @Override public final void setEmojiSizeRes(@DimenRes final int res, final boolean shouldInvalidate) {
        setEmojiSize(getResources().getDimensionPixelSize(res), shouldInvalidate);
    }
}
