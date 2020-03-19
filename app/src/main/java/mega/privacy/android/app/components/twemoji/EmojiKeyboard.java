package mega.privacy.android.app.components.twemoji;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiBackspaceClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class EmojiKeyboard extends LinearLayout {

    private boolean isListenerActivated = true;
    private Activity activity;
    private EmojiEditTextInterface editInterface;
    private RecentEmoji recentEmoji;
    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;

    private final OnEmojiLongClickListener longClickListener = new OnEmojiLongClickListener() {
        @Override
        public void onEmojiLongClick(@NonNull final EmojiImageView view, @NonNull final Emoji emoji) {
            if (isListenerActivated) {
                variantPopup.show(view, emoji);
            }
        }
    };

    private View rootView;
    private ImageButton emojiIcon;
    private int keyboardHeight;

    //Click in EMOJI
    private final OnEmojiClickListener clickListener = new OnEmojiClickListener() {
        @Override
        public void onEmojiClick(@NonNull final EmojiImageView imageView, @NonNull final Emoji emoji) {
            if (isListenerActivated) {
                editInterface.input(emoji);
                recentEmoji.addEmoji(emoji);
                imageView.updateEmoji(emoji);
                variantPopup.dismiss();
            }
        }
    };

    private OnPlaceButtonListener buttonListener;
    private boolean isLetterKeyboardShown = false;
    private boolean isEmojiKeyboardShown = false;

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

    public void setOnPlaceButtonListener(OnPlaceButtonListener buttonListener) {
        this.buttonListener = buttonListener;
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
        this.activity = context;

        DisplayMetrics outMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        keyboardHeight = (outMetrics.heightPixels / 2) - getActionBarHeight(activity, getResources());
        requestLayout();
    }

    private void needToReplace() {
        if (buttonListener == null) return;
        buttonListener.needToPlace();
    }

    //KEYBOARDS:
    public void showLetterKeyboard() {
        if (isLetterKeyboardShown || !(editInterface instanceof View)) return;
        logDebug("showLetterKeyboard()");
        hideEmojiKeyboard();
        View view = (View) editInterface;
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.showSoftInput(view, 0, null);
        isLetterKeyboardShown = true;
        changeKeyboardIcon(false);
        needToReplace();
    }

    public void showEmojiKeyboard() {
        if (isEmojiKeyboardShown) return;
        logDebug("showEmojiKeyboard");
        hideLetterKeyboard();
        setVisibility(VISIBLE);
        isEmojiKeyboardShown = true;
        changeKeyboardIcon(true);
        if (editInterface instanceof View) {
            View view = (View) editInterface;
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }

        needToReplace();
    }

    public void hideBothKeyboard(Activity activity) {
        if (activity == null) return;
        logDebug("hideBothKeyboard()");
        hideEmojiKeyboard();
        hideLetterKeyboard();
        changeKeyboardIcon(false);
    }

    public void hideLetterKeyboard() {
        if (!isLetterKeyboardShown || !(editInterface instanceof View)) return;
        logDebug("hideLetterKeyboard() ");
        View view = (View) editInterface;
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0, null);
        isLetterKeyboardShown = false;
        needToReplace();
    }
    public void hideKeyboardFromFileStorage(){
        hideEmojiKeyboard();
        hideLetterKeyboard();
        changeKeyboardIcon(true);
    }

    public void changeKeyboardIcon(boolean isKeyboard){
        Drawable drawable;
        if(isKeyboard){
            drawable = getResources().getDrawable(R.drawable.ic_keyboard_white);
        }else {
            drawable = getResources().getDrawable(R.drawable.ic_emojicon);
        }
        emojiIcon.setImageDrawable(drawable);
    }

    public void hideEmojiKeyboard() {
        if (!isEmojiKeyboardShown) return;
        logDebug("hideEmojiKeyboard() ");
        recentEmoji.persist();
        variantEmoji.persist();
        setVisibility(GONE);

        isEmojiKeyboardShown = false;
        if (editInterface instanceof View) {
            View view = (View) editInterface;
            view.clearFocus();
        }
        needToReplace();
    }

    public void setListenerActivated(boolean listenerActivated) {
        isListenerActivated = listenerActivated;
    }

    public boolean getLetterKeyboardShown() {
        return isLetterKeyboardShown;
    }

    public boolean getEmojiKeyboardShown() {
        return isEmojiKeyboardShown;
    }
}

