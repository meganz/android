package mega.privacy.android.core.formatter.emoji

import mega.privacy.android.thirdpartylib.twemoji.EmojiUtilsShortcodes
import javax.inject.Inject

internal class DefaultEmojiShortcodeConverter @Inject constructor() : EmojiShortcodeConverter {

    override fun convert(text: String): String = EmojiUtilsShortcodes.emojify(text)
}
