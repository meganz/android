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
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiSelectedListener;
import mega.privacy.android.app.components.twemoji.listeners.OnPlaceButtonListener;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class EmojiKeyboard extends LinearLayout {

    private View rootView;
    private String type;
    private int keyboardHeight;

    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;
    private RecentEmoji recentEmoji;

    private boolean isListenerActivated = true;
    private Activity activity;
    private EmojiEditTextInterface editInterface;
    private ImageButton emojiIcon;
    private OnPlaceButtonListener buttonListener;
    private boolean isLetterKeyboardShown = false;
    private boolean isEmojiKeyboardShown = false;

    private OnEmojiSelectedListener emojiSelectedListener;

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
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiKeyboard, defStyle, 0);
        a.recycle();
    }

    private void initializeCommonVariables(String type, int height) {
        this.type = type;
        this.rootView = getRootView();
        this.variantEmoji = new VariantEmojiManager(getContext(), type);
        this.recentEmoji = new RecentEmojiManager(getContext(), type);
        this.variantPopup = new EmojiVariantPopup(rootView, clickListener);

        final EmojiView emojiView = new EmojiView(getContext(), clickListener, longClickListener, recentEmoji, variantEmoji, type);
        if (type.equals(TYPE_EMOJI)) {
            emojiView.setOnEmojiBackspaceClickListener(v -> editInterface.backspace());
        }
        addView(emojiView);
        this.keyboardHeight = height;
        requestLayout();
    }

    public void initEmoji(Activity context, EmojiEditTextInterface editText, ImageButton emojiIcon) {
        this.activity = context;
        this.editInterface = editText;
        this.emojiIcon = emojiIcon;
        DisplayMetrics outMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        initializeCommonVariables(TYPE_EMOJI, (outMetrics.heightPixels / 2) - getActionBarHeight(activity, getResources()));
    }

    public void initReaction(int height) {
        initializeCommonVariables(TYPE_REACTION, height);
    }

    private final OnEmojiLongClickListener longClickListener = new OnEmojiLongClickListener() {
        @Override
        public void onEmojiLongClick(@NonNull final EmojiImageView view, @NonNull final Emoji emoji) {
            if (type.equals(TYPE_REACTION) || isListenerActivated) {
                variantPopup.show(view, emoji);
            }
        }
    };

    private final OnEmojiClickListener clickListener = new OnEmojiClickListener() {
        @Override
        public void onEmojiClick(@NonNull final EmojiImageView imageView, @NonNull final Emoji emoji) {
            if (type.equals(TYPE_REACTION) || isListenerActivated) {
                if (type.equals(TYPE_EMOJI)) {
                    editInterface.input(emoji);
                }

                recentEmoji.addEmoji(emoji);
                variantEmoji.addVariant(emoji);
                imageView.updateEmoji(emoji);
                variantPopup.dismiss();

                if (type.equals(TYPE_REACTION)) {
                    if (emojiSelectedListener == null)
                        return;

                    emojiSelectedListener.emojiSelected(emoji);
                }
            }
        }
    };

    public void setOnPlaceButtonListener(OnPlaceButtonListener buttonListener) {
        this.buttonListener = buttonListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(keyboardHeight, MeasureSpec.EXACTLY));
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

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener emojiSelectedListener) {
        this.emojiSelectedListener = emojiSelectedListener;
    }

    public void persistReactionList() {
        recentEmoji.persist();
        variantEmoji.persist();
    }
}

