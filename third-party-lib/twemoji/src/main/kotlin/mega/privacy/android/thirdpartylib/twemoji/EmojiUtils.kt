package mega.privacy.android.thirdpartylib.twemoji

object EmojiUtils {
    /**
     * Method for obtaining the emojis that were found in a text.
     *
     * @param text The text.
     * @return List of emojis.
     */
    @JvmStatic
    fun emojis(text: String?): List<EmojiRange>? {
        if (text.isNullOrEmpty()) {
            return null
        }
        return EmojiManager.getInstance().findAllEmojis(text)
    }
}


