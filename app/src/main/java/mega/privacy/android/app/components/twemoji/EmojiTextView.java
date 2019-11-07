package mega.privacy.android.app.components.twemoji;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.support.annotation.CallSuper;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.ChatUtil.*;

public class EmojiTextView extends AppCompatTextView implements EmojiTexViewInterface {

    public static final int LAST_MESSAGE_TEXTVIEW_WIDTH = 190;
    public static final int TITLE_TOOLBAR = 180;
    private float emojiSize;
    private Context mContext;
    private Display display;
    private DisplayMetrics mOutMetrics = new DisplayMetrics();
    private int textViewMaxWidth;
    private boolean neccessaryShortCode = true;

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

        if (mContext instanceof GroupChatInfoActivityLollipop || mContext instanceof ManagerActivityLollipop || mContext instanceof ChatExplorerActivity || mContext instanceof ArchivedChatsActivity) {
            display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
            display.getMetrics(mOutMetrics);
            textViewMaxWidth = Util.px2dp(LAST_MESSAGE_TEXTVIEW_WIDTH, mOutMetrics);
        } else if (mContext instanceof ContextWrapper && (((ContextWrapper) mContext).getBaseContext() instanceof ChatActivityLollipop || ((ContextWrapper) mContext).getBaseContext() instanceof ChatCallActivity)) {
            display = ((Activity) ((ContextWrapper) mContext).getBaseContext()).getWindowManager().getDefaultDisplay();
            display.getMetrics(mOutMetrics);
            textViewMaxWidth = Util.px2dp(TITLE_TOOLBAR, mOutMetrics);
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

        if (mContext == null || (mContext instanceof ContextWrapper && ((ContextWrapper) mContext).getBaseContext() == null)) {
            super.setText(spannableStringBuilder, type);
        } else if (mContext instanceof GroupChatInfoActivityLollipop || mContext instanceof ManagerActivityLollipop || mContext instanceof ArchivedChatsActivity || mContext instanceof ChatExplorerActivity) {
            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
            super.setText(textF, type);
        } else if (mContext instanceof ContextWrapper && (((ContextWrapper) mContext).getBaseContext() instanceof ChatActivityLollipop || ((ContextWrapper) mContext).getBaseContext() instanceof ChatCallActivity)) {
            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
            super.setText(textF, type);
        } else {
            super.setText(spannableStringBuilder, type);
        }

    }

    public boolean isNeccessaryShortCode() {
        return neccessaryShortCode;
    }

    public void setNeccessaryShortCode(boolean neccessaryShortCode) {
        this.neccessaryShortCode = neccessaryShortCode;
    }

    @Override
    protected void onTextChanged(CharSequence rawText, int start, int lengthBefore, int lengthAfter) {
    }

    @Override
    @CallSuper
    public void backspace() {
        final KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        dispatchKeyEvent(event);
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