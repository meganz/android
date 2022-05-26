package mega.privacy.android.app.components.twemoji;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiSelectedListener;
import mega.privacy.android.app.components.twemoji.listeners.OnPlaceButtonListener;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.ViewUtils;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;

public class EmojiKeyboard extends LinearLayout {

    private String type;
    private int keyboardHeight;
    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;
    private RecentEmoji recentEmoji;

    private boolean isListenerActivated = true;
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

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiKeyboard, defStyle, 0);
        a.recycle();
    }

    private void initializeCommonVariables(String type, int height) {
        this.type = type;
        View rootView = getRootView();
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

    public void initEmoji(Activity activity, EmojiEditTextInterface editText, ImageButton emojiIcon) {
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

        hideEmojiKeyboard();
        View view = (View) editInterface;
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        ViewUtils.showSoftKeyboardDelayed(view);
    }

    /**
     * Method that controls when the text keyboard changes state, visible or hidden.
     *
     * @param isShown True, if visible. False, if hidden.
     */
    public void updateStatusLetterKeyboard(boolean isShown) {
        isLetterKeyboardShown = isShown;
        changeKeyboardIcon();
        needToReplace();
    }

    public void showEmojiKeyboard() {
        if (isEmojiKeyboardShown || !(editInterface instanceof View)) return;

        hideLetterKeyboard();
        setVisibility(VISIBLE);
        isEmojiKeyboardShown = true;
        changeKeyboardIcon();
        View view = (View) editInterface;
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        needToReplace();
    }

    public void hideBothKeyboard(Activity activity) {
        if (activity == null) return;

        hideEmojiKeyboard();
        hideLetterKeyboard();
        changeKeyboardIcon();
    }

    public void hideLetterKeyboard() {
        if (!isLetterKeyboardShown || !(editInterface instanceof View)) return;

        View view = (View) editInterface;
        view.clearFocus();
        ViewUtils.hideKeyboard(view);
    }

    public void hideKeyboardFromFileStorage(){
        hideEmojiKeyboard();
        hideLetterKeyboard();
        changeKeyboardIcon();
    }

    /**
     * Method to check if the icon is different than emojiIcon
     *
     * @param newIcon The new icon
     * @return True if it's the same, false otherwise
     */
    private boolean isDifferentIcon(Drawable newIcon) {
        Drawable currentDrawable = emojiIcon.getDrawable();
        return !areDrawablesIdentical(currentDrawable, newIcon);
    }

    /**
     * Method controlling the change in the keyboard icon
     */
    public void changeKeyboardIcon() {
        Drawable drawable;
        if (!isLetterKeyboardShown && isEmojiKeyboardShown) {
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_keyboard_white);
            if (isDifferentIcon(drawable)) {
                emojiIcon.setImageDrawable(drawable);
            }
            ImageViewCompat.setImageTintList(emojiIcon, ColorStateList.valueOf(ContextCompat.getColor(getContext(), editInterface.isTextEmpty() ? R.color.grey_020_white_020 : R.color.grey_060_white_060)));
        } else {
            drawable = ContextCompat.getDrawable(getContext(), editInterface.isTextEmpty() ?
                    R.drawable.ic_emoji_unchecked :
                    R.drawable.ic_emoji_checked);
            if (isDifferentIcon(drawable)) {
                ImageViewCompat.setImageTintList(emojiIcon, null);
                emojiIcon.setImageDrawable(drawable);
            }
        }
    }

    public void hideEmojiKeyboard() {
        if (!isEmojiKeyboardShown) return;

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

