package mega.privacy.android.app.components.twemoji.wrapper

import mega.privacy.android.app.components.twemoji.EmojiManager

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
    fun getFirstEmoji(text: String): Int?
}