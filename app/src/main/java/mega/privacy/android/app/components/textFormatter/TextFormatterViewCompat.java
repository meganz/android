package mega.privacy.android.app.components.textFormatter;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;
import java.util.ArrayList;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.components.CustomTypefaceSpan;
import static mega.privacy.android.app.components.textFormatter.TextFormatterUtils.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public class TextFormatterViewCompat {
    private static final int GENERAL_FLAG = 18;
    private static final int NUM_CHAR_MONOSPACE = 2;
    private static final Typeface MONOSPACE_FONT = Typeface.createFromAsset(MegaApplication.getInstance().getBaseContext().getAssets(), "font/RobotoMono-Regular.ttf");

    /**
     * Method that receives a text and returns it properly formatted.
     *
     * @param text The text without format.
     * @return The formatted text.
     */
    public static CharSequence getFormattedText(String text) {
        if (isTextEmpty(text))
            return null;

        return extractFlagsForTextView(text);
    }

    /**
     * Method that controls a TextView, to format the text it contains.
     *
     * @param textView The TextView.
     */
    public static void applyFormatting(final TextView textView) {
        TextWatcher mEditTextWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                CharSequence formatted = TextFormatterViewCompat.extractFlagsForTextView(s);
                TextFormatterViewCompat.removeTextChangedListener(textView, this);
                textView.setText(formatted, TextView.BufferType.EDITABLE);
                TextFormatterViewCompat.addTextChangedListener(textView, this);
            }
        };

        String text = textView.getText().toString();
        CharSequence formattedText = getFormattedText(text);
        if (formattedText != null) {
            textView.setText(formattedText);
        }
        textView.addTextChangedListener(mEditTextWatcher);
    }

    private static void removeTextChangedListener(TextView textView, TextWatcher watcher) {
        textView.removeTextChangedListener(watcher);
    }

    private static void addTextChangedListener(TextView textView, TextWatcher watcher) {
        textView.addTextChangedListener(watcher);
    }

    /**
     * Method that actually formats the text. The first part gets a count of the special characters in the text,
     * and where they are located. The second part is to convert the text according to the special characters found in the first part.
     *
     * @param text The plain text.
     * @return The formatted text.
     */
    public static CharSequence extractFlagsForTextView(CharSequence text) {
        char[] textChars = text.toString().toCharArray();
        ArrayList<Character> characters = new ArrayList<>();
        ArrayList<Flag> flags = new ArrayList<>();
        Flag boldFlag = new Flag(INVALID_INDEX, INVALID_INDEX, BOLD_FLAG);
        Flag monospaceFlag = new Flag(INVALID_INDEX, INVALID_INDEX, MONOSPACE_FLAG);
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
                    if (textChars.length > 6) {
                        if (textChars.length > i + NUM_CHAR_MONOSPACE) {

                            if (monospaceFlag.start != INVALID_INDEX) {
                                if (monospaceFlag.end == INVALID_INDEX && textChars[i + 1] == MONOSPACE_FLAG && textChars[i + NUM_CHAR_MONOSPACE] == MONOSPACE_FLAG) {
                                    j = j - NUM_CHAR_MONOSPACE;
                                    monospaceFlag.end = j;
                                    flags.add(monospaceFlag);
                                    monospaceFlag = new Flag(INVALID_INDEX, INVALID_INDEX, MONOSPACE_FLAG);
                                    i = i + NUM_CHAR_MONOSPACE;
                                    continue;
                                }
                            } else {
                                if (textChars[i + 1] == MONOSPACE_FLAG && textChars[i + NUM_CHAR_MONOSPACE] == MONOSPACE_FLAG) {
                                    monospaceFlag.start = j;
                                    i = i + NUM_CHAR_MONOSPACE;
                                    j = j + NUM_CHAR_MONOSPACE;
                                    continue;
                                }
                            }
                        }
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
                    if (flag.end > builder.length())
                        break;

                    iss = new StyleSpan(Typeface.BOLD);
                    builder.setSpan(iss, flag.start, flag.end, GENERAL_FLAG);
                    break;

                case STRIKE_FLAG:
                    if (flag.end > builder.length())
                        break;

                    builder.setSpan(new StrikethroughSpan(), flag.start, flag.end, GENERAL_FLAG);
                    break;

                case ITALIC_FLAG:
                    if (flag.end > builder.length())
                        break;

                    iss = new StyleSpan(Typeface.ITALIC);
                    builder.setSpan(iss, flag.start, flag.end, GENERAL_FLAG);
                    break;

                case MONOSPACE_FLAG:
                    if (flag.end > builder.length())
                        break;

                    builder.setSpan(new CustomTypefaceSpan("", MONOSPACE_FONT), flag.start, flag.end, GENERAL_FLAG);
                    break;
            }
        }
        return builder;
    }
}