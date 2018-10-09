package mega.privacy.android.app.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.support.text.emoji.widget.EmojiAppCompatTextView;

import io.github.rockerhieu.emojicon.EmojiconHandler;
import mega.privacy.android.app.R;


public class WrapEmojiconTextView extends EmojiAppCompatTextView {

    private int mEmojiconSize;
    private int mEmojiconAlignment;
    private int mEmojiconTextSize;
    private int mTextStart = 0;
    private int mTextLength = -1;
    private boolean mUseSystemDefault = false;

    public WrapEmojiconTextView(Context context) {
        super(context);
        init(null);
    }


    public WrapEmojiconTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public WrapEmojiconTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    private void init(AttributeSet attrs) {

        mEmojiconTextSize = (int) getTextSize();
        if (attrs == null) {
            mEmojiconSize = (int) getTextSize();
        } else {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Emojicon);
            mEmojiconSize = (int) a.getDimension(R.styleable.Emojicon_emojiconSize, getTextSize());
            mEmojiconAlignment = a.getInt(R.styleable.Emojicon_emojiconAlignment, DynamicDrawableSpan.ALIGN_BASELINE);
            mTextStart = a.getInteger(R.styleable.Emojicon_emojiconTextStart, 0);
            mTextLength = a.getInteger(R.styleable.Emojicon_emojiconTextLength, -1);
            mUseSystemDefault = a.getBoolean(R.styleable.Emojicon_emojiconUseSystemDefault, false);
            a.recycle();
        }
        setText(getText());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!TextUtils.isEmpty(text)) {
            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            EmojiconHandler.addEmojis(getContext(), builder, mEmojiconSize, mEmojiconAlignment, mEmojiconTextSize, mTextStart, mTextLength, mUseSystemDefault);
            text = builder;
        }
        super.setText(text, type);
    }


    //Set the size of emojicon in pixels.

    public void setEmojiconSizePx(int pixels) {
        mEmojiconSize = pixels;
        super.setText(getText());
    }

    //Set the size of emojicon in sp.

    public void setEmojiconSizeSp(int sp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
        mEmojiconSize = px;
        super.setText(getText());
    }
    //Set whether to use system default emojicon

    public void setUseSystemDefault(boolean useSystemDefault) {
        mUseSystemDefault = useSystemDefault;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Now fix width
        float max = 0;
        Layout layout = getLayout();
        for (int i = 0, size = layout.getLineCount(); i < size; i++) {
            final float lineWidth = layout.getLineMax(i);
            if (lineWidth > max) {
                max = lineWidth;
            }
        }

        final int height = getMeasuredHeight();
        final int width = (int) Math.ceil(max) + getCompoundPaddingLeft() + getCompoundPaddingRight();

        setMeasuredDimension(width, height);
    }



}
