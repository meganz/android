package mega.privacy.android.app.components.twemoji;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiBackspaceClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.utils.Util;

public class EmojiKeyboard extends LinearLayout {

    private Activity context;
    private EmojiEditTextInterface editInterface;
    private RecentEmoji recentEmoji;
    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;
    private View rootView;
    private ImageButton emojiIcon;
    private int keyboardHeight;
    private OnEmojiClickListener onEmojiClickListener;
    private OnEmojiBackspaceClickListener onEmojiBackspaceClickListener;

    private boolean isLetterKeyboardShown = false;
    private boolean isEmojiKeyboardShown = false;

    //Long click in EMOJI
    final OnEmojiLongClickListener longClickListener = new OnEmojiLongClickListener() {
        @Override
        public void onEmojiLongClick(@NonNull final EmojiImageView view, @NonNull final Emoji emoji) {
            variantPopup.show(view, emoji);
        }
    };

    //Click in EMOJI
    final OnEmojiClickListener clickListener = new OnEmojiClickListener() {
        @Override
        public void onEmojiClick(@NonNull final EmojiImageView imageView, @NonNull final Emoji emoji) {
            editInterface.input(emoji);
            recentEmoji.addEmoji(emoji);
            imageView.updateEmoji(emoji);

            if (onEmojiClickListener != null) {
                onEmojiClickListener.onEmojiClick(imageView, emoji);
            }
            variantPopup.dismiss();
        }
    };

    public EmojiKeyboard(Context context) {
        super(context);
        init(null, 0);
    }

    public EmojiKeyboard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public EmojiKeyboard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EmojiKeyboard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiKeyboard, defStyle, 0);
        a.recycle();
        this.rootView = getRootView();
        this.variantEmoji = new VariantEmojiManager(getContext());
        this.recentEmoji = new RecentEmojiManager(getContext());
        this.variantPopup = new EmojiVariantPopup(rootView, clickListener);

        final EmojiView emojiView = new EmojiView(getContext(), clickListener, longClickListener, recentEmoji, variantEmoji);
        emojiView.setOnEmojiBackspaceClickListener(new OnEmojiBackspaceClickListener() {
            @Override
            public void onEmojiBackspaceClick(final View v) {
                editInterface.backspace();
                if (onEmojiBackspaceClickListener != null) {
                    onEmojiBackspaceClickListener.onEmojiBackspaceClick(v);
                }
            }
        });
        addView(emojiView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(keyboardHeight, MeasureSpec.EXACTLY));
    }

    public void init(Activity context, EmojiEditTextInterface editText, ImageButton emojiIcon) {
        this.editInterface = editText;
        this.emojiIcon = emojiIcon;
        this.context = context;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        keyboardHeight = displayMetrics.heightPixels / 2 - getActionBarHeight();
        requestLayout();
    }

    public void setOnEmojiClickListener(OnEmojiClickListener onEmojiClickListener) {
        this.onEmojiClickListener = onEmojiClickListener;
    }
    public void setOnEmojiBackspaceClickListener(OnEmojiBackspaceClickListener onEmojiBackspaceClickListener) {
        this.onEmojiBackspaceClickListener = onEmojiBackspaceClickListener;
    }

    public int getKeyboardHeight() {
        return keyboardHeight;
    }

    public void setKeyboardHeight(int keyboardHeight) {
        log("setKeyboardHeight(): "+keyboardHeight);
        this.keyboardHeight = keyboardHeight;
    }

    private int getActionBarHeight() {
        log("getActionBarHeight()");
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (context != null && context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    //KEYBOARDS:
    public void hideBothKeyboard(Activity activity){
        if (activity == null) return;
        log("hideBothKeyboard()");
        hideEmojiKeyboard();
        hideLetterKeyboard();
        emojiIcon.setImageResource(R.drawable.ic_emoticon_white);
    }

    public void showLetterKeyboard(){
        if(!isLetterKeyboardShown){
            if (editInterface instanceof View){
                log("showLetterKeyboard()");
                hideEmojiKeyboard();
                final View view = (View) editInterface;
                view.setFocusableInTouchMode(true);
                view.requestFocus();

                final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.showSoftInput(view, 0, null);
                isLetterKeyboardShown = true;
                emojiIcon.setImageResource(R.drawable.ic_emoticon_white);
            } else {
                throw new IllegalArgumentException("The provided editInterace isn't a View instance.");
            }
        }
    }

    public void showEmojiKeyboard(){
        if(!isEmojiKeyboardShown){
            log("showEmojiKeyboard()");
            hideLetterKeyboard();
            setVisibility(VISIBLE);
            isEmojiKeyboardShown = true;
            emojiIcon.setImageResource(R.drawable.ic_keyboard_white);
            if (editInterface instanceof View){
                final View view = (View) editInterface;
                view.setFocusableInTouchMode(true);
                view.requestFocus();
            }
        }
    }

    public void hideLetterKeyboard() {
        if(isLetterKeyboardShown){
            if (editInterface instanceof View) {
                log("hideLetterKeyboard() ");
                final View view = (View) editInterface;
                view.clearFocus();

                final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0, null);
                isLetterKeyboardShown = false;
            } else {
                throw new IllegalArgumentException("The provided editInterace isn't a View instance.");
            }
        }
    }

    public void hideEmojiKeyboard(){
        if(isEmojiKeyboardShown){
            log("hideEmojiKeyboard() ");
            recentEmoji.persist();
            variantEmoji.persist();
            setVisibility(GONE);
            isEmojiKeyboardShown = false;
            if (editInterface instanceof View) {
                final View view = (View) editInterface;
                view.clearFocus();
            }
        }
    }

    public boolean getLetterKeyboardShown() {
        return isLetterKeyboardShown;
    }

    public boolean getEmojiKeyboardShown() {
        return isEmojiKeyboardShown;
    }

    public static void log(String message) {
        Util.log("EmojiKeyboard", message);
    }
}

