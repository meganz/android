package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.CallSuper;
import androidx.annotation.DimenRes;
import androidx.annotation.Px;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;

import static mega.privacy.android.app.utils.ChatUtil.getMaxAllowed;

public class EmojiEditText extends AppCompatEditText implements EmojiEditTextInterface {
    private float emojiSize;
    private Context mContext;
    private MediaListener listener;

    public EmojiEditText(final Context context) {
        this(context, null);
        mContext = context;
    }

    public EmojiEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        if (!isInEditMode()) {
            EmojiManager.getInstance().verifyInstalled();
        }

        final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;

        if (attrs == null) {
            emojiSize = defaultEmojiSize;
        } else {
            final TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.EmojiEditText);
            try {
                emojiSize = a.getDimension(R.styleable.EmojiEditText_emojiSize, defaultEmojiSize);
            } finally {
                a.recycle();
            }
        }
        setText(getText());
    }
    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (text.toString().equals("")) {
            return;
        }

        final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
        EmojiManager.getInstance().replaceWithImages(mContext, getText(), emojiSize, defaultEmojiSize);

        if (mContext instanceof GroupChatInfoActivityLollipop || mContext instanceof AddContactActivityLollipop) {
            setFilters(new InputFilter[]{new InputFilter.LengthFilter(getMaxAllowed(getText()))});
            super.onTextChanged(getText(), start, lengthBefore, lengthAfter);

        } else {
            if (lengthAfter > lengthBefore) {
                super.onTextChanged(getText(), start, lengthBefore, lengthAfter);
            }
        }
    }

    @Override
    @CallSuper
    public void input(final Emoji emoji) {
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

    @Override
    public float getEmojiSize() {
        return emojiSize;
    }

    @Override
    public final void setEmojiSize(@Px final int pixels) {
        setEmojiSize(pixels, true);
    }

    @Override
    @CallSuper
    public void backspace() {
        final KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        dispatchKeyEvent(event);
    }

    @Override
    public final void setEmojiSize(@Px final int pixels, final boolean shouldInvalidate) {
        emojiSize = pixels;
        if (shouldInvalidate) {
            setText(getText());
        }
    }

    @Override
    public final void setEmojiSizeRes(@DimenRes final int res) {
        setEmojiSizeRes(res, true);
    }

    @Override
    public final void setEmojiSizeRes(@DimenRes final int res, final boolean shouldInvalidate) {
        setEmojiSize(getResources().getDimensionPixelSize(res), shouldInvalidate);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        final InputConnection ic = super.onCreateInputConnection(editorInfo);
        EditorInfoCompat.setContentMimeTypes(editorInfo, new String[]{"image/gif"});

        final InputConnectionCompat.OnCommitContentListener callback =
                (inputContentInfo, flags, opts) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
                            (flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                        try {
                            inputContentInfo.requestPermission();
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    if (listener != null) {
                        listener.onGifSelected(inputContentInfo.getContentUri().toString());
                    }

                    inputContentInfo.releasePermission();
                    return true;
                };
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback);
    }

    public void setMediaListener(MediaListener listener) {
        this.listener = listener;
    }

    public interface MediaListener {
        void onGifSelected(String path);
    }
}
