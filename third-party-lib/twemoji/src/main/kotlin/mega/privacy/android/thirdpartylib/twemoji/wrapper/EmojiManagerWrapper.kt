package mega.privacy.android.thirdpartylib.twemoji.wrapper

import mega.privacy.android.thirdpartylib.twemoji.EmojiManager

/**
 * Wrapper for [EmojiManager]
 */
interface EmojiManagerWrapper {

    /**
     * Get first emoji in the [text]
     * @param text
     * @return drawable resource ID of the emoji. Return null if emoji not found.
     *
     */
    suspend fun getFirstEmoji(text: String): Int?
}
