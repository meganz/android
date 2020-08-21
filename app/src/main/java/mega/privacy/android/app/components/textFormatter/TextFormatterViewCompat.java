package mega.privacy.android.app.components.textFormatter;

import android.graphics.Typeface;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.components.CustomTypefaceSpan;

import static mega.privacy.android.app.components.textFormatter.textFormatterUtils.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public class TextFormatterViewCompat {

    private static final long DELAY_MILLIS = 220L;
    private static final int GENERAL_FLAG = 18;
    private static final int COLOR_SPAN = -7829368;
    private static final Typeface font = Typeface.createFromAsset(MegaApplication.getInstance().getBaseContext().getAssets(), "font/RobotoMono-Regular.ttf");

    public TextFormatterViewCompat() { }

    public static void applyFormatting(final EditText editText, final TextWatcher... watchers) {
        TextWatcher mEditTextWatcher = new TextWatcher() {
            final TextWatcher mainWatcher = this;
            Handler handler = new Handler();
            private Runnable formatRunnable = () -> TextFormatterViewCompat.format(editText, mainWatcher, watchers);

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                TextFormatterViewCompat.sendBeforeTextChanged(watchers, s, start, count, after);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextFormatterViewCompat.sendOnTextChanged(watchers, s, start, before, count);
            }

            public void afterTextChanged(Editable s) {
                this.handler.removeCallbacks(this.formatRunnable);
                this.handler.postDelayed(this.formatRunnable, DELAY_MILLIS);
            }
        };
        String text = editText.getText().toString();
        CharSequence formattedText = getFormattedText(text, false);
        if (formattedText != null) {
            editText.setText(formattedText);
        }

        editText.addTextChangedListener(mEditTextWatcher);
    }

    private static void format(EditText editText, TextWatcher mainWatcher, TextWatcher[] otherWatchers) {
        Editable text = editText.getText();
        CharSequence formatted = extractFlagsForEditText(text);
        removeTextChangedListener(editText, mainWatcher);
        int selectionEnd = editText.getSelectionEnd();
        int selectionStart = editText.getSelectionStart();
        editText.setText(formatted);
        editText.setSelection(selectionStart, selectionEnd);
        Editable formattedEditableText = editText.getText();
        sendAfterTextChanged(otherWatchers, formattedEditableText);
        addTextChangedListener(editText, mainWatcher);
    }

    public static CharSequence getFormattedText(String text, boolean isTextView) {
        if (isTextEmpty(text))
            return null;

        return isTextView? extractFlagsForTextView(text): extractFlagsForEditText(text);
    }

    public static void applyFormatting(final TextView textView, final TextWatcher... watchers) {
        TextWatcher mEditTextWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                TextFormatterViewCompat.sendBeforeTextChanged(watchers, s, start, count, after);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextFormatterViewCompat.sendOnTextChanged(watchers, s, start, before, count);
            }

            public void afterTextChanged(Editable s) {
                CharSequence formatted = TextFormatterViewCompat.extractFlagsForTextView(s);
                TextFormatterViewCompat.removeTextChangedListener(textView, this);
                textView.setText(formatted, TextView.BufferType.EDITABLE);
                Editable formattedEditableText = (Editable) textView.getText();
                TextFormatterViewCompat.sendAfterTextChanged(watchers, formattedEditableText);
                TextFormatterViewCompat.addTextChangedListener(textView, this);
            }
        };
        String text = textView.getText().toString();
        CharSequence formattedText = getFormattedText(text, true);
        if (formattedText != null) {
            textView.setText(formattedText);
        }

        textView.addTextChangedListener(mEditTextWatcher);
    }

    private static void sendAfterTextChanged(TextWatcher[] mListeners, Editable s) {
        if (mListeners != null) {
            for (int i = 0; i < mListeners.length; ++i) {
                mListeners[i].afterTextChanged(s);
            }
        }

    }

    private static void sendOnTextChanged(TextWatcher[] mListeners, CharSequence s, int start, int before, int count) {
        if (mListeners != null) {
            for (int i = 0; i < mListeners.length; ++i) {
                mListeners[i].onTextChanged(s, start, before, count);
            }
        }

    }

    private static void sendBeforeTextChanged(TextWatcher[] mListeners, CharSequence s, int start, int count, int after) {
        if (mListeners != null) {
            for (int i = 0; i < mListeners.length; ++i) {
                mListeners[i].beforeTextChanged(s, start, count, after);
            }
        }

    }

    static void removeTextChangedListener(TextView textView, TextWatcher watcher) {
        textView.removeTextChangedListener(watcher);
    }

    static void addTextChangedListener(TextView textView, TextWatcher watcher) {
        textView.addTextChangedListener(watcher);
    }

    public static CharSequence extractFlagsForTextView(CharSequence text) {
        char[] textChars = text.toString().toCharArray();
        ArrayList<Character> characters = new ArrayList();
        ArrayList<Flag> flags = new ArrayList();
        Flag boldFlag = new Flag(INVALID_INDEX, INVALID_INDEX, BOLD_FLAG);
        Flag quoteFlag = new Flag(INVALID_INDEX, INVALID_INDEX, MONOSPACE_FLAG);

        Flag strikeFlag = new Flag(INVALID_INDEX, INVALID_INDEX, STRIKE_FLAG);
        Flag italicFlag = new Flag(INVALID_INDEX, INVALID_INDEX, ITALIC_FLAG);
        int i = 0;

        for (int j = 0; i < textChars.length; ++i) {
            char c = textChars[i];
            switch (c) {
                case BOLD_FLAG:
                    if (boldFlag.start != INVALID_INDEX) {
                        boldFlag.end = j;
                        flags.add(boldFlag);
                        boldFlag = new Flag(INVALID_INDEX, INVALID_INDEX, BOLD_FLAG);
                        continue;
                    }
                    if (hasFlagSameLine(text, BOLD_FLAG, i + 1)) {
                        boldFlag.start = j;
                        continue;
                    }
                    break;

                case STRIKE_FLAG:
                    if (strikeFlag.start != INVALID_INDEX) {
                        strikeFlag.end = j;
                        flags.add(strikeFlag);
                        strikeFlag = new Flag(INVALID_INDEX, INVALID_INDEX, STRIKE_FLAG);
                        continue;
                    }

                    if (hasFlagSameLine(text, STRIKE_FLAG, i + 1)) {
                        strikeFlag.start = j;
                        continue;
                    }
                    break;

                case ITALIC_FLAG:
                    if (italicFlag.start != INVALID_INDEX) {
                        italicFlag.end = j;
                        flags.add(italicFlag);
                        italicFlag = new Flag(INVALID_INDEX, INVALID_INDEX, ITALIC_FLAG);
                        continue;
                    }

                    if (hasFlagSameLine(text, ITALIC_FLAG, i + 1)) {
                        italicFlag.start = j;
                        continue;
                    }
                    break;

                case MONOSPACE_FLAG:
                    if (quoteFlag.start != INVALID_INDEX) {
                        quoteFlag.end = j;
                        flags.add(quoteFlag);
                        quoteFlag = new Flag(INVALID_INDEX, INVALID_INDEX, MONOSPACE_FLAG);
                        continue;
                    }
                    if (hasFlagSameLine(text, MONOSPACE_FLAG, i + 1)) {
                        quoteFlag.start = j;
                        continue;
                    }
                    break;
            }

            characters.add(c);
            ++j;
        }

        String formatted = getText(characters);
        SpannableStringBuilder builder = new SpannableStringBuilder(formatted);

        for (Flag flag : flags) {
            StyleSpan iss;
            switch (flag.flag) {
                case BOLD_FLAG:
                    iss = new StyleSpan(1);
                    builder.setSpan(iss, flag.start, flag.end, GENERAL_FLAG);
                    break;

                case STRIKE_FLAG:
                    builder.setSpan(new StrikethroughSpan(), flag.start, flag.end, GENERAL_FLAG);
                    break;

                case ITALIC_FLAG:
                    iss = new StyleSpan(2);
                    builder.setSpan(iss, flag.start, flag.end, GENERAL_FLAG);
                    break;

                case MONOSPACE_FLAG:
                    builder.setSpan(new CustomTypefaceSpan("", font), flag.start, flag.end, GENERAL_FLAG);
                    break;
            }
        }

        return builder;
    }

    public static CharSequence extractFlagsForEditText(CharSequence text) {
        char[] textChars = text.toString().toCharArray();
        ArrayList<Character> characters = new ArrayList();
        ArrayList<Flag> flags = new ArrayList();
        Flag boldFlag = new Flag(INVALID_INDEX, INVALID_INDEX, BOLD_FLAG);
        Flag quoteFlag = new Flag(INVALID_INDEX, INVALID_INDEX, MONOSPACE_FLAG);
        Flag strikeFlag = new Flag(INVALID_INDEX, INVALID_INDEX, STRIKE_FLAG);
        Flag italicFlag = new Flag(INVALID_INDEX, INVALID_INDEX, ITALIC_FLAG);
        int i = 0;

        for (int j = 0; i < textChars.length; ++i) {
            char c = textChars[i];
            switch (c) {
                case BOLD_FLAG:
                    if (boldFlag.start == INVALID_INDEX) {
                        if (hasFlagSameLine(text, BOLD_FLAG, i + 1)) {
                            boldFlag.start = j + 1;

                        }
                    } else {
                        boldFlag.end = j;
                        flags.add(boldFlag);
                        boldFlag = new Flag(INVALID_INDEX, INVALID_INDEX, BOLD_FLAG);
                    }
                    break;

                case STRIKE_FLAG:
                    if (strikeFlag.start == INVALID_INDEX) {
                        if (hasFlagSameLine(text, STRIKE_FLAG, i + 1)) {
                            strikeFlag.start = j + 1;
                        }
                    } else {
                        strikeFlag.end = j;
                        flags.add(strikeFlag);
                        strikeFlag = new Flag(INVALID_INDEX, INVALID_INDEX, STRIKE_FLAG);
                    }
                    break;

                case ITALIC_FLAG:
                    if (italicFlag.start == INVALID_INDEX) {
                        if (hasFlagSameLine(text, ITALIC_FLAG, i + 1)) {
                            italicFlag.start = j + 1;
                        }
                    } else {
                        italicFlag.end = j;
                        flags.add(italicFlag);
                        italicFlag = new Flag(INVALID_INDEX, INVALID_INDEX, ITALIC_FLAG);
                    }
                    break;
                case MONOSPACE_FLAG:
                    if (quoteFlag.start == INVALID_INDEX) {
                        if (hasFlagSameLine(text, MONOSPACE_FLAG, i + 1)) {
                            quoteFlag.start = j + 1;

                        }
                    } else {
                        quoteFlag.end = j;
                        flags.add(quoteFlag);
                        quoteFlag = new Flag(INVALID_INDEX, INVALID_INDEX, MONOSPACE_FLAG);
                    }
                    break;
            }

            characters.add(c);
            ++j;
        }

        String formatted = getText(characters);
        SpannableStringBuilder builder = new SpannableStringBuilder(formatted);

        for (Flag flag : flags) {
            StyleSpan iss;
            if (flag.flag == BOLD_FLAG) {
                iss = new StyleSpan(1);
                builder.setSpan(iss, flag.start, flag.end, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.start - 1, flag.start, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.end, flag.end + 1, GENERAL_FLAG);
            } else if (flag.flag == STRIKE_FLAG) {
                builder.setSpan(new StrikethroughSpan(), flag.start, flag.end, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.start - 1, flag.start, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.end, flag.end + 1, GENERAL_FLAG);
            } else if (flag.flag == ITALIC_FLAG) {
                iss = new StyleSpan(2);
                builder.setSpan(iss, flag.start, flag.end, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.start - 1, flag.start, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.end, flag.end + 1, GENERAL_FLAG);
            }else if(flag.flag == MONOSPACE_FLAG){
                builder.setSpan(new CustomTypefaceSpan("", font), flag.start, flag.end, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.start - 1, flag.start, GENERAL_FLAG);
                builder.setSpan(new ForegroundColorSpan(COLOR_SPAN), flag.end, flag.end + 1, GENERAL_FLAG);
            }
        }

        return builder;
    }
}