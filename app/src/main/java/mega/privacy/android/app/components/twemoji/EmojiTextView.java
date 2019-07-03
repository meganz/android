package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
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

public class EmojiTextView extends AppCompatTextView implements EmojiTexViewInterface {

    public static final int LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT = 190;
    public static final int TITLE_TOOLBAR_CHAT_PORTRAIT = 170;
    public static final int TITLE_TOOLBAR_CALL_PORTRAIT = 200;
    public static final int TITLE_TOOLBAR_CHAT_LANDSCAPE = 300;
    public static final int TITLE_TOOLBAR_CALL_LANDSCAPE = 350;
    public static final int LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE = 260;
    private float emojiSize;
    private Context mContext;
    private Display display;
    private DisplayMetrics mOutMetrics = new DisplayMetrics();
    private int textViewMaxWidth;

    public EmojiTextView(final Context context) {
        this(context, null);
        mContext = context;
    }

    public EmojiTextView(Context context, @Nullable AttributeSet attrs) {
        this(context,attrs, 0);
        mContext = context;
    }

    public EmojiTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context);
        mContext = context;

        if (mContext == null) {
            return;
        }

        if (!isInEditMode()) {
            EmojiManager.getInstance().verifyInstalled();
        }

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

        if (mContext != null) {
            if (mContext instanceof GroupChatInfoActivityLollipop) {
                display = ((GroupChatInfoActivityLollipop) mContext).getWindowManager().getDefaultDisplay();
                setTextViewMax(LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE, LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT);
            } else if (mContext instanceof ManagerActivityLollipop) {
                display = ((ManagerActivityLollipop) mContext).getWindowManager().getDefaultDisplay();
                setTextViewMax(LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE, LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT);
            } else if (mContext instanceof ChatExplorerActivity) {
                display = ((ChatExplorerActivity) mContext).getWindowManager().getDefaultDisplay();
                setTextViewMax(LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE, LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT);
            } else if (mContext instanceof ArchivedChatsActivity) {
                display = ((ArchivedChatsActivity) mContext).getWindowManager().getDefaultDisplay();
                setTextViewMax(LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE, LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT);
            } else if (mContext instanceof ContextWrapper) {
                Context inContext = ((ContextWrapper) mContext).getBaseContext();
                if (inContext == null) {
                    return;
                }
                if (inContext instanceof ChatCallActivity) {
                    display = ((ChatCallActivity) inContext).getWindowManager().getDefaultDisplay();
                    setTextViewMax(TITLE_TOOLBAR_CALL_LANDSCAPE, TITLE_TOOLBAR_CALL_PORTRAIT);

                } else if (inContext instanceof ChatActivityLollipop) {
                    display = ((ChatActivityLollipop) inContext).getWindowManager().getDefaultDisplay();
                    setTextViewMax(TITLE_TOOLBAR_CHAT_LANDSCAPE, TITLE_TOOLBAR_CHAT_PORTRAIT);
                }
            }
        }
        setText(getText());
    }

    public static void log(String message) {
        Util.log("EmojiTextView", message);
    }

    @Override
    public void setText(CharSequence rawText, BufferType type) {

        CharSequence text = rawText == null ? "" : rawText;
        String resultText = EmojiUtilsShortcodes.emojify(text.toString());
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(resultText);
        Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
        EmojiManager.getInstance().replaceWithImages(getContext(), spannableStringBuilder, emojiSize, defaultEmojiSize);
        if((mContext!=null) && ((mContext instanceof GroupChatInfoActivityLollipop) ||
                (mContext instanceof ManagerActivityLollipop) ||
                (mContext instanceof ArchivedChatsActivity) ||
                (mContext instanceof ChatExplorerActivity) ||
                ((mContext instanceof ContextWrapper) && (((ContextWrapper) mContext).getBaseContext()!=null) &&
                        ((((ContextWrapper) mContext).getBaseContext() instanceof ChatActivityLollipop) ||
                                (((ContextWrapper) mContext).getBaseContext() instanceof ChatCallActivity))))) {
            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
            super.setText(textF, type);
        }else{
            super.setText(spannableStringBuilder, type);
        }

//        if (mContext == null) super.setText(spannableStringBuilder, type);
//
//        EmojiManager.getInstance().replaceWithImages(mContext, spannableStringBuilder, emojiSize, defaultEmojiSize);
//
//        if (mContext instanceof GroupChatInfoActivityLollipop || mContext instanceof ManagerActivityLollipop || mContext instanceof ArchivedChatsActivity || mContext instanceof ChatExplorerActivity) {
//            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
//            super.setText(textF, type);
//        }
//
//        if (mContext instanceof ContextWrapper && ((ContextWrapper) mContext).getBaseContext() == null)
//            super.setText(spannableStringBuilder, type);
//
//        if (mContext instanceof ContextWrapper && (((ContextWrapper) mContext).getBaseContext() instanceof ChatActivityLollipop || ((ContextWrapper) mContext).getBaseContext() instanceof ChatCallActivity)) {
//            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
//            super.setText(textF, type);
//        }

    }

    private void setTextViewMax(int landscape, int portrait) {

        display.getMetrics(mOutMetrics);

        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textViewMaxWidth = Util.scaleWidthPx(landscape, mOutMetrics);
        } else {
            textViewMaxWidth = Util.scaleWidthPx(portrait, mOutMetrics);
        }
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

//public class EmojiTextView extends AppCompatTextView implements EmojiTexViewInterface {
//
//    public EmojiTextView(final Context context) {
//        this(context, null);
//        log("********** 1");
//
//        mContext = context;
//    }
//
//    public EmojiTextView(Context context, @Nullable AttributeSet attrs) {
//        this(context,attrs, 0);
//        log("********** 2");
//
//        mContext = context;
//    }
//
//    public EmojiTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        mContext = context;
//        log("********** 3");
//
//        if (mContext == null) {
//            return;
//        }
//
//        if (!isInEditMode()) {
//            EmojiManager.getInstance().verifyInstalled();
//        }
//
//        final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
//        final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
//        if (attrs == null) {
//            emojiSize = defaultEmojiSize;
//        } else {
//            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiTextView);
//            try {
//                emojiSize = a.getDimension(R.styleable.EmojiTextView_emojiSize, defaultEmojiSize);
//            } finally {
//                a.recycle();
//            }
//        }
//
//        if (mContext instanceof GroupChatInfoActivityLollipop || mContext instanceof ManagerActivityLollipop || mContext instanceof ChatExplorerActivity || mContext instanceof ArchivedChatsActivity) {
//
//            display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
//            display.getMetrics(mOutMetrics);
//            textViewMaxWidth = Util.px2dp(LAST_MESSAGE_TEXTVIEW_WIDTH, mOutMetrics);
//
//        } else if (mContext instanceof ContextWrapper) {
//
//            Context inContext = ((ContextWrapper) mContext).getBaseContext();
//            if (inContext == null) return;
//            display = ((Activity) inContext).getWindowManager().getDefaultDisplay();
//            display.getMetrics(mOutMetrics);
//            textViewMaxWidth = Util.px2dp(TITLE_TOOLBAR, mOutMetrics);
//
//        }
//
//        setText(getText());
//
//
////        if (mContext instanceof GroupChatInfoActivityLollipop) {
////
////            display = ((GroupChatInfoActivityLollipop) mContext).getWindowManager().getDefaultDisplay();
////            display.getMetrics(mOutMetrics);
////            textViewMaxWidth = Util.px2dp(LAST_MESSAGE_TEXTVIEW_WIDTH, mOutMetrics);
////
////        } else if (mContext instanceof ManagerActivityLollipop) {
////
////            display = ((ManagerActivityLollipop) mContext).getWindowManager().getDefaultDisplay();
////            display.getMetrics(mOutMetrics);
////            textViewMaxWidth = Util.px2dp(LAST_MESSAGE_TEXTVIEW_WIDTH, mOutMetrics);
////
////        } else if (mContext instanceof ChatExplorerActivity) {
////
////            display = ((ChatExplorerActivity) mContext).getWindowManager().getDefaultDisplay();
////            display.getMetrics(mOutMetrics);
////            textViewMaxWidth = Util.px2dp(LAST_MESSAGE_TEXTVIEW_WIDTH, mOutMetrics);
////
////        } else if (mContext instanceof ArchivedChatsActivity) {
////
////            display = ((ArchivedChatsActivity) mContext).getWindowManager().getDefaultDisplay();
////            display.getMetrics(mOutMetrics);
////            textViewMaxWidth = Util.px2dp(LAST_MESSAGE_TEXTVIEW_WIDTH, mOutMetrics);
////
////        } else if (mContext instanceof ContextWrapper) {
////
////            Context inContext = ((ContextWrapper) mContext).getBaseContext();
////            if (inContext == null) return;
////
////            if (inContext instanceof ChatCallActivity) {
////                display = ((ChatCallActivity) inContext).getWindowManager().getDefaultDisplay();
////                display.getMetrics(mOutMetrics);
////
////                textViewMaxWidth = Util.px2dp(TITLE_TOOLBAR_CALL, mOutMetrics);
////
////            } else if (inContext instanceof ChatActivityLollipop) {
////                display = ((ChatActivityLollipop) inContext).getWindowManager().getDefaultDisplay();
////                display.getMetrics(mOutMetrics);
////
////                textViewMaxWidth = Util.px2dp(TITLE_TOOLBAR_CHAT, mOutMetrics);
////            }
////        }
//
////        setText(getText());
//    }
//
//    @Override
//    public void setText(CharSequence rawText, BufferType type) {
//        CharSequence text = rawText == null ? "" : rawText;
//        String resultText = EmojiUtilsShortcodes.emojify(text.toString());
//        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(resultText);
//        Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
//        float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
//        EmojiManager.getInstance().replaceWithImages(getContext(), spannableStringBuilder, emojiSize, defaultEmojiSize);
//
//        if (mContext == null || mContext instanceof ContextWrapper && ((ContextWrapper) mContext).getBaseContext() == null)
//            super.setText(spannableStringBuilder, type);
//
//        if (mContext instanceof GroupChatInfoActivityLollipop ||
//                mContext instanceof ManagerActivityLollipop ||
//                mContext instanceof ArchivedChatsActivity ||
//                mContext instanceof ChatExplorerActivity ||
//                mContext instanceof ContextWrapper &&
//                        ((ContextWrapper) mContext).getBaseContext() instanceof ChatActivityLollipop ||
//                ((ContextWrapper) mContext).getBaseContext() instanceof ChatCallActivity) {
//            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
//            super.setText(textF, type);
//        }
//
//
////        if ((mContext != null) && ((mContext instanceof GroupChatInfoActivityLollipop) ||
////                (mContext instanceof ManagerActivityLollipop) ||
////                (mContext instanceof ArchivedChatsActivity) ||
////                (mContext instanceof ChatExplorerActivity) ||
////                ((mContext instanceof ContextWrapper) && (((ContextWrapper) mContext).getBaseContext() != null) &&
////                        ((((ContextWrapper) mContext).getBaseContext() instanceof ChatActivityLollipop) ||
////                                (((ContextWrapper) mContext).getBaseContext() instanceof ChatCallActivity))))) {
////            CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
////            super.setText(textF, type);
////        } else {
////            super.setText(spannableStringBuilder, type);
////        }
//    }
//

//}