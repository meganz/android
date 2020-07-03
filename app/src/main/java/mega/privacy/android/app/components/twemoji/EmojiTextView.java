package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import androidx.annotation.CallSuper;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.appcompat.widget.AppCompatTextView;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.ChatUtil.converterShortCodes;

public class EmojiTextView extends AppCompatTextView implements EmojiTexViewInterface {

    private float emojiSize;
    private Context mContext;
    private int textViewMaxWidth = 0;
    private TextUtils.TruncateAt typeEllipsize = TextUtils.TruncateAt.END;
    private boolean necessaryShortCode = true;

    private float lineSpacingMultiplier = 1.0f;
    private float lineAdditionalVerticalPadding = 0.0f;

    private boolean showTrailingIcon = false;
    @DrawableRes
    private int trailingIcon;
    private int trailingIconPaddingLeft = 0;

    private CharSequence latestRawText;
    private BufferType latestBufferType;

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
        latestRawText = rawText;
        latestBufferType = type;

        CharSequence text = rawText == null ? "" : rawText;
        if (isNeccessaryShortCode()) {
            text = converterShortCodes(text.toString());
        }
        SpannableStringBuilder emojiProcessedText = new SpannableStringBuilder(text);
        Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
        EmojiManager.getInstance()
            .replaceWithImages(getContext(), emojiProcessedText, emojiSize, defaultEmojiSize);

        if (mContext == null || (mContext instanceof ContextWrapper
            && ((ContextWrapper) mContext).getBaseContext() == null) || textViewMaxWidth == 0) {
            super.setText(emojiProcessedText, type);
            return;
        }

        int maxLines = getMaxLines();
        Drawable iconDrawable = null;
        if (showTrailingIcon) {
            iconDrawable = getResources().getDrawable(trailingIcon);
        }
        if (iconDrawable == null || maxLines == -1) {
            CharSequence ellipsizedText = TextUtils.ellipsize(emojiProcessedText, getPaint(),
                textViewMaxWidth * maxLines, typeEllipsize);
            super.setText(ellipsizedText, type);
            return;
        }

        setTextByManuallyEllipsize(iconDrawable, emojiProcessedText, maxLines, type);
    }

    /**
     * TextUtils.ellipsize doesn't take care of line break, so we can't use it to get a proper
     * ellipsized text with it, we have to ellipsize the text manually.
     *
     * We need trim it, because trailing whitespace causes extra space between the trailing
     * icon and non-whitespace character in the name, if the name could fit into two lines.
     *
     * A Unicode character may need two chars to represent, e.g. emoji,
     * but Layout already takes care of it, so it's safe to truncate at line end.
     *
     * It may not fit into two lines after appending padding and icon, so we need truncate more
     * chars in this case.
     * And emojiProcessedText may fit into one line, but after appending padding and
     * icon it may not fit, we don't want only the icon in the second line, so we need
     * add line break manually in this case.
     *
     * We need truncate at least one character, then trim trailing whitespace,
     * because we don't want ellipsize after whitespace.
     * And because the last character may takes two chars, we need to take care of it.
     *
     * If we can find whitespace, then break at here,
     * if not, e.g. the text is Chinese, we just break before the last character,
     * and because the last character may takes two chars, we need to take care of it.
     *
     * Keep the whitespace in the first line
     */
    private void setTextByManuallyEllipsize(Drawable iconDrawable,
        SpannableStringBuilder emojiProcessedText, int maxLines, BufferType type) {

        PaddingSpan padding = new PaddingSpan(trailingIconPaddingLeft);
        iconDrawable.setBounds(0, 0, iconDrawable.getIntrinsicWidth(),
            iconDrawable.getIntrinsicHeight());
        ImageSpan icon = new ImageSpan(iconDrawable, ImageSpan.ALIGN_BASELINE);

        int lastNonWhitespaceOffset = emojiProcessedText.length();
        while (lastNonWhitespaceOffset > 0 && Character.isWhitespace(
            emojiProcessedText.charAt(lastNonWhitespaceOffset - 1))) {
            lastNonWhitespaceOffset--;
        }
        CharSequence workingText =
            lastNonWhitespaceOffset == emojiProcessedText.length() ? emojiProcessedText
                : emojiProcessedText.subSequence(0, lastNonWhitespaceOffset);

        boolean isEllipsizeNecessary = false;
        Layout originLayout = createWorkingLayout(workingText);
        if (originLayout.getLineCount() > maxLines) {
            isEllipsizeNecessary = true;
            workingText = workingText.subSequence(0, originLayout.getLineEnd(maxLines - 1));
        }

        boolean needManualLineBreak = false;
        while (true) {
            SpannableStringBuilder trialText =
                buildFinalText(workingText, isEllipsizeNecessary, padding, icon);
            Layout trialLayout = createWorkingLayout(trialText);
            if (trialLayout.getLineCount() <= maxLines) {
                needManualLineBreak =
                    !isEllipsizeNecessary
                        && trialLayout.getLineCount() > originLayout.getLineCount();
                break;
            }

            isEllipsizeNecessary = true;
            int textEnd = workingText.length() - 1;
            if (!Character.isLetter(workingText.charAt(textEnd))) {
                textEnd--;
            }
            while (textEnd > 0 && Character.isWhitespace(workingText.charAt(textEnd - 1))) {
                textEnd--;
            }
            workingText = workingText.subSequence(0, textEnd);
        }

        if (needManualLineBreak) {
            int breakPos = workingText.length() - 1;
            if (!Character.isLetter(breakPos)) {
                breakPos--;
            }
            for (int i = workingText.length() - 1; i >= 0; i--) {
                if (Character.isWhitespace(workingText.charAt(i))) {
                    breakPos = i + 1;
                    break;
                }
            }
            workingText = new SpannableStringBuilder(workingText.subSequence(0, breakPos))
                .append('\n')
                .append(workingText.subSequence(breakPos, workingText.length()));
        }

        super.setText(buildFinalText(workingText, isEllipsizeNecessary, padding, icon), type);
    }

    /**
     * Build the final text by set padding and icon span. The two whitespace are placeholder chars
     * for padding and icon span
     */
    private SpannableStringBuilder buildFinalText(CharSequence workingText, boolean ellipsized,
        PaddingSpan padding, ImageSpan icon) {
        SpannableStringBuilder finalText = new SpannableStringBuilder(workingText);
        finalText.append(ellipsized ? "\u2026  " : "  ");
        finalText.setSpan(padding, finalText.length() - 2, finalText.length() - 1,
            Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        finalText.setSpan(icon, finalText.length() - 1, finalText.length(),
            Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        return finalText;
    }

    private Layout createWorkingLayout(CharSequence workingText) {
        return new StaticLayout(workingText, getPaint(),
            textViewMaxWidth, Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
    }

    /**
     * Set a trailing icon drawable and its left padding. This can show an icon drawable at the
     * end of text. If the text is too long, it will be ellipsized before appending the icon.
     *
     * To control visibility, check {@link #updateMaxWidthAndIconVisibility(int, boolean)}
     *
     * @param trailingIcon icon drawable res id
     * @param paddingLeft left padding, in px
     */
    public void setTrailingIcon(@DrawableRes int trailingIcon, int paddingLeft) {
        this.trailingIcon = trailingIcon;
        trailingIconPaddingLeft = paddingLeft;
    }

    /**
     * Control whether this icon should be visible, and also update the max width of the whole text.
     *
     * In ContactInfoActivityLollipop, we will display the icon and allow bigger max width if the
     * tool bar is expanded, and hide the icon and set a smaller max width if the tool bar is
     * collapsed.
     *
     * @param maxWidth max width of the whole text
     * @param showTrailingIcon whether show the icon or not
     */
    public void updateMaxWidthAndIconVisibility(int maxWidth, boolean showTrailingIcon) {
        this.showTrailingIcon = showTrailingIcon;
        textViewMaxWidth = maxWidth;
        if (latestRawText != null && latestBufferType != null) {
            setText(latestRawText, latestBufferType);
        }
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        this.lineAdditionalVerticalPadding = add;
        this.lineSpacingMultiplier = mult;
        super.setLineSpacing(add, mult);
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