package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes
import javax.inject.Inject

/**
 * Mapper that maps an emoji string to a short code
 *
 */
class EmojiShortCodeMapper @Inject constructor() {

    /**
     * invoke
     *
     * @param emoji value of emoji
     * @return short code
     */
    operator fun invoke(emoji: String): String =
        EmojiUtilsShortcodes.shortCodify(emoji)
}