package mega.privacy.android.app.components.twemoji;

import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import java.util.List;


public final class EmojiUtils {
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

    private EmojiUtils() {
        throw new AssertionError("No instances.");
    }
}
