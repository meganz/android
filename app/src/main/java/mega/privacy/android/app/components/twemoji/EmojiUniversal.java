package mega.privacy.android.app.components.twemoji;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiBackspaceClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;

public class EmojiUniversal{

//public class EmojiUniversal extends LinearLayout implements View.OnClickListener, View.OnFocusChangeListener {

//    private EmojiEditTextInterface editInterface;
//    private RecentEmoji recentEmoji;
//    private VariantEmoji variantEmoji;
//    private Activity context;
//    private int keyboardHeight = 500;
//    private OnEmojiClickListener onEmojiClickListener;
//    private OnEmojiBackspaceClickListener onEmojiBackspaceClickListener;
//    private EditText editText;
//
//    final OnEmojiClickListener clickListener = new OnEmojiClickListener() {
//        @Override
//        public void onEmojiClick(@NonNull final EmojiImageView imageView, @NonNull final Emoji emoji) {
//            editInterface.input(emoji);
//
//            recentEmoji.addEmoji(emoji);
//            imageView.updateEmoji(emoji);
//
//            if (onEmojiClickListener != null) {
//                onEmojiClickListener.onEmojiClick(imageView, emoji);
//            }
//        }
//    };
//
//    final OnEmojiLongClickListener longClickListener = new OnEmojiLongClickListener() {
//        @Override
//        public void onEmojiLongClick(@NonNull final EmojiImageView view, @NonNull final Emoji emoji) {
//        }
//    };
//
//    public EmojiUniversal(Context context) {
//        super(context);
//        init(null, 0);
//    }
//
//    public EmojiUniversal(Context context, @Nullable AttributeSet attrs) {
//        super(context, attrs);
//        init(attrs, 0);
//    }
//
//    public EmojiUniversal(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        init(attrs, defStyleAttr);
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    public EmojiUniversal(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        init(attrs, defStyleAttr);
//    }
//
//    private void init(AttributeSet attrs, int defStyle) {
//        // Load attributes
//        final TypedArray a = getContext().obtainStyledAttributes(
//                attrs, R.styleable.EmojiUniversal, defStyle, 0);
//
//        a.recycle();
//        this.variantEmoji = new VariantEmojiManager(getContext());
//        this.recentEmoji = new RecentEmojiManager(getContext());
//        final EmojiView emojiView = new EmojiView(getContext(), clickListener, longClickListener, recentEmoji, variantEmoji);
//        emojiView.setOnEmojiBackspaceClickListener(new OnEmojiBackspaceClickListener() {
//            @Override
//            public void onEmojiBackspaceClick(final View v) {
//                editInterface.backspace();
//
//                if (onEmojiBackspaceClickListener != null) {
//                    onEmojiBackspaceClickListener.onEmojiBackspaceClick(v);
//                }
//            }
//        });
//        addView(emojiView);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(keyboardHeight, MeasureSpec.EXACTLY));
//    }
//
//
//    public void init(EmojiEditText editText, Activity context) {
//        this.editInterface = editText;
//        this.editText = editText;
//        this.context = context;
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        keyboardHeight = displayMetrics.heightPixels / 2 - getActionBarHeight();
//        requestLayout();
//        this.editText.setOnClickListener(this);
//        this.editText.setOnFocusChangeListener(this);
//    }
//
//    public void setEditInterface(EmojiEditTextInterface editInterface) {
//        this.editInterface = editInterface;
//    }
//
//    public void setOnEmojiClickListener(OnEmojiClickListener onEmojiClickListener) {
//        this.onEmojiClickListener = onEmojiClickListener;
//    }
//
//    public void toggle() {
//        hideSoftKeyboard(context);
//        show(!isShown());
//    }
//
//    public static void hideSoftKeyboard(Activity activity) {
//        if (activity == null) return;
//        View focusedView = activity.getCurrentFocus();
//        if (focusedView != null) {
//            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
//            inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
//            ViewGroup rootView = (ViewGroup) focusedView.getRootView();
//            int dfValue = rootView.getDescendantFocusability();
//            rootView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
//            focusedView.clearFocus();
//            rootView.setDescendantFocusability(dfValue);
//        }
//    }
//
//    public void dismiss() {
//        recentEmoji.persist();
//        variantEmoji.persist();
//        show(false);
//    }
//
//    public void setOnEmojiBackspaceClickListener(OnEmojiBackspaceClickListener onEmojiBackspaceClickListener) {
//        this.onEmojiBackspaceClickListener = onEmojiBackspaceClickListener;
//    }
//
//    @Override
//    public void onClick(View view) {
//        show(false);
//    }
//
//    public int getKeyboardHeight() {
//        return keyboardHeight;
//    }
//
//    public void setKeyboardHeight(int keyboardHeight) {
//        this.keyboardHeight = keyboardHeight;
//    }
//
//    @Override
//    public void onFocusChange(View view, boolean b) {
//        if (b) {
//            show(false);
//        }
//    }
//
//    private int getActionBarHeight() {
//        int actionBarHeight = 0;
//        TypedValue tv = new TypedValue();
//        if (context != null && context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv,
//                true))
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(
//                    tv.data, getResources().getDisplayMetrics());
//        return actionBarHeight;
//    }
//
//    private void show(boolean isShow){
//        if (isShow){
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    setVisibility(VISIBLE);
//                }
//            },200);
//        }else {
//            setVisibility(GONE);
//        }
//    }
}