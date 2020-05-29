package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Paint;
import androidx.annotation.CallSuper;
import androidx.annotation.DimenRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.ChatUtil.*;

public class EmojiTextView extends AppCompatTextView implements EmojiTexViewInterface {

    private float emojiSize;
    private Context mContext;
    private int textViewMaxWidth = 0;
    private TextUtils.TruncateAt typeEllipsize = TextUtils.TruncateAt.END;
    private boolean necessaryShortCode = true;

    public EmojiTextView(final Context context) {
        this(context, null);
        mContext = context;
    }

    public EmojiTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
    }

    public EmojiTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;

        if (mContext == null || (mContext instanceof ContextWrapper && ((ContextWrapper) mContext).getBaseContext() == null))
            return;

        if (!isInEditMode()) EmojiManager.getInstance().verifyInstalled();

        final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
        if (attrs == null) {
            emojiSize = defaultEmojiSize;
        } else {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiTextView);
            try {
                emojiSize = a.getDimension(R.styleable.EmojiTextView_emojiSize, defaultEmojiSize);
            } finally {
                a.recycle();
            }
        }
        setText(getText());
    }

    @Override
    public void setText(CharSequence rawText, BufferType type) {
        CharSequence text = rawText == null ? "" : rawText;
        if(isNeccessaryShortCode()){
            text = converterShortCodes(text.toString());
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
        Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
        EmojiManager.getInstance().replaceWithImages(getContext(), spannableStringBuilder, emojiSize, defaultEmojiSize);

        if(mContext == null || (mContext instanceof ContextWrapper && ((ContextWrapper) mContext).getBaseContext() == null) || (textViewMaxWidth == 0)){
            super.setText(spannableStringBuilder, type);
        }else{
            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, typeEllipsize);
            super.setText(textF, type);
        }
    }

    public boolean isNeccessaryShortCode() {
        return necessaryShortCode;
    }

    public void setNeccessaryShortCode(boolean neccessaryShortCode) {
        this.necessaryShortCode = neccessaryShortCode;
    }

    @Override
    protected void onTextChanged(CharSequence rawText, int start, int lengthBefore, int lengthAfter) {}

    @Override
    @CallSuper
    public void backspace() {
        final KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        dispatchKeyEvent(event);
    }

    @Override
    public final void setMaxWidthEmojis(final int maxWidth) {
        this.textViewMaxWidth = maxWidth;
    }

    public void setTypeEllipsize(TextUtils.TruncateAt typeEllipsize) {
        this.typeEllipsize = typeEllipsize;
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
}