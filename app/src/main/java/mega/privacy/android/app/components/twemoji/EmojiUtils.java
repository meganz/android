package mega.privacy.android.app.components.twemoji;

import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;


public final class EmojiUtils {
    private static final Pattern SPACE_REMOVAL = Pattern.compile("[\\s]");

    //returns true when the string contains only emojis. Note that whitespace will be filtered out.
    public boolean isOnlyEmojis(@Nullable final String text) {
        try {
            if (!TextUtils.isEmpty(text)) {

                final String inputWithoutSpaces = SPACE_REMOVAL.matcher(text).replaceAll(Matcher.quoteReplacement(""));

                return EmojiManager.getInstance()
                        .getEmojiRepetitivePattern()
                        .matcher(inputWithoutSpaces)
                        .matches();
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return false;
    }

    /**
     * Method for obtaining the emojis that were found in a text.
     *
     * @param text The text.
     * @return List of emojis.
     */
    public static List<EmojiRange> emojis(final String text) {
        if (isTextEmpty(text))
            return null;

        return EmojiManager.getInstance().findAllEmojis(text);
    }

    //returns a class that contains all of the emoji information that was found in the given text
    @NonNull
    public EmojiInformation emojiInformation(@Nullable final String text) {
        final List<EmojiRange> emojis = EmojiManager.getInstance().findAllEmojis(text);

        final boolean isOnlyEmojis = isOnlyEmojis(text);

        return new EmojiInformation(isOnlyEmojis, emojis);
    }

    private EmojiUtils() {
        throw new AssertionError("No instances.");
    }
}
