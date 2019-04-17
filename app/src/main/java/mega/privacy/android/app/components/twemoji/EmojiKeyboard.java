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
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiBackspaceClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.components.voiceClip.RecordButton;
import mega.privacy.android.app.components.voiceClip.RecordView;
import mega.privacy.android.app.utils.Util;

public class EmojiKeyboard extends LinearLayout {

    private Activity activity;
    private EmojiEditTextInterface editInterface;
    private RecentEmoji recentEmoji;
    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;
    private View rootView;
    private ImageButton emojiIcon;
    private FrameLayout fragment;
    private RelativeLayout recordButtonLayout;
    private RecordView recordView;
    private int keyboardHeight;
    private int marginBottom;
    private OnEmojiClickListener onEmojiClickListener;
    private OnEmojiBackspaceClickListener onEmojiBackspaceClickListener;

    boolean isListenerActivated  = true;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    private boolean isLetterKeyboardShown = false;
    private boolean isEmojiKeyboardShown = false;

    //Long click in EMOJI
    final OnEmojiLongClickListener longClickListener = new OnEmojiLongClickListener() {
        @Override
        public void onEmojiLongClick(@NonNull final EmojiImageView view, @NonNull final Emoji emoji) {
            if(isListenerActivated){
                variantPopup.show(view, emoji);
            }
        }
    };

    //Click in EMOJI
    final OnEmojiClickListener clickListener = new OnEmojiClickListener() {
        @Override
        public void onEmojiClick(@NonNull final EmojiImageView imageView, @NonNull final Emoji emoji) {
            if(isListenerActivated) {
                editInterface.input(emoji);
                recentEmoji.addEmoji(emoji);
                imageView.updateEmoji(emoji);

                if (onEmojiClickListener != null) {
                    onEmojiClickListener.onEmojiClick(imageView, emoji);
                }
                variantPopup.dismiss();
            }
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

    public void init(Activity context, EmojiEditTextInterface editText, ImageButton emojiIcon, FrameLayout fragment, RelativeLayout recordButtonLayout, RecordView recordView) {
        this.editInterface = editText;
        this.emojiIcon = emojiIcon;
        this.activity = context;
        this.fragment = fragment;
        this.recordButtonLayout = recordButtonLayout;
        this.recordView = recordView;

        display = activity.getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        keyboardHeight = outMetrics.heightPixels / 2 - getActionBarHeight();
        marginBottom = Util.px2dp(48, outMetrics);
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
        this.keyboardHeight = keyboardHeight;
    }

    private int getActionBarHeight() {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (activity != null && activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
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
        emojiIcon.setImageResource(R.drawable.ic_emojicon);
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
                emojiIcon.setImageResource(R.drawable.ic_emojicon);
                paramsRecordButton(marginBottom);
            } else {
                throw new IllegalArgumentException("The provided editInterace isn't a View instance.");
            }
        }else {
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
                emojiIcon.setImageResource(R.drawable.ic_emojicon);
                paramsRecordButton(marginBottom);
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
            paramsRecordButton(keyboardHeight+marginBottom);

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
                final View view = (View) editInterface;
                view.clearFocus();

                final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0, null);
                isLetterKeyboardShown = false;
                paramsRecordButton(marginBottom);

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
            paramsRecordButton(marginBottom);
            isEmojiKeyboardShown = false;
            if (editInterface instanceof View) {
                final View view = (View) editInterface;
                view.clearFocus();
            }
        }
    }

    public void paramsRecordButton(int marginBottomVoicleButton){
        if((fragment!=null)&&(recordButtonLayout!=null)&&(recordView!=null)){
            log("paramsRecordButton() ");

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recordButtonLayout.getLayoutParams();
            params.height = Util.px2dp(48, outMetrics);
            params.width = Util.px2dp(48, outMetrics);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.setMargins(Util.px2dp(0, outMetrics), Util.px2dp(0, outMetrics), Util.px2dp(0, outMetrics), marginBottomVoicleButton);
            recordButtonLayout.setLayoutParams(params);

            FrameLayout.LayoutParams paramsRecordView = (FrameLayout.LayoutParams) recordView.getLayoutParams();
            paramsRecordView.setMargins(0,0,Util.px2dp(0, outMetrics), marginBottomVoicleButton);
            paramsRecordView.gravity = Gravity.BOTTOM |Gravity.RIGHT ;
            recordView.setLayoutParams(paramsRecordView);
        }
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

    public static void log(String message) {
        Util.log("EmojiKeyboard", message);
    }
}

