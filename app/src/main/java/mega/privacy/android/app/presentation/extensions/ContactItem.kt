package mega.privacy.android.app.presentation.extensions

import com.vdurmont.emoji.EmojiParser
import mega.privacy.android.app.components.twemoji.EmojiUtils
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes
import mega.privacy.android.domain.entity.contacts.ContactItem
import java.util.Locale

/**
 * Retrieve the avatar first letter of a [ContactItem].
 *
 * @return The first letter of the string to be painted in the default avatar.
 */
fun ContactItem.getAvatarFirstLetter(): String =
    getAvatarFirstLetter(contactData.alias ?: contactData.fullName ?: email)

/**
 * Retrieve the first letter of a String.
 *
 * @param text String to obtain the first letter.
 * @return The first letter of the string to be painted in the default avatar.
 */
private fun getAvatarFirstLetter(text: String): String {
    val unknown = "U"

    if (text.isEmpty()) {
        return unknown
    }

    val result = text.trim { it <= ' ' }
    if (result.length == 1) {
        return result[0].toString().uppercase(Locale.getDefault())
    }

    val resultTitle = EmojiUtilsShortcodes.emojify(result)
    if (resultTitle.isNullOrEmpty()) {
        return unknown
    }

    val emojis = EmojiUtils.emojis(resultTitle)

    if (emojis.size > 0 && emojis[0].start == 0) {
        return resultTitle.substring(emojis[0].start, emojis[0].end)
    }

    val resultEmojiCompat = getEmojiCompatAtFirst(resultTitle)
    if (resultEmojiCompat != null) {
        return resultEmojiCompat
    }

    val resultChar = resultTitle[0].toString().uppercase(Locale.getDefault())
    return if (resultChar.trim { it <= ' ' }
            .isEmpty() || resultChar == "(" || !isRecognizableCharacter(
            resultChar[0])
    ) {
        unknown
    } else resultChar

}

/**
 * Gets the first character as an emoji if any.
 *
 * @param text Text to check.
 * @return The emoji if any, null otherwise.
 */
private fun getEmojiCompatAtFirst(text: String?): String? {
    if (text.isNullOrEmpty()) {
        return null
    }

    val listEmojis = EmojiParser.extractEmojis(text)

    if (listEmojis != null && listEmojis.isNotEmpty()) {
        val substring = text.substring(0, listEmojis[0].length)
        val sublistEmojis = EmojiParser.extractEmojis(substring)
        if (sublistEmojis != null && sublistEmojis.isNotEmpty()) {
            return substring
        }
    }

    return null
}

/**
 * Retrieve if a char is recognizable.
 *
 * @param inputChar The char to be examined.
 * @return True if the char is recognizable. Otherwise false.
 */
private fun isRecognizableCharacter(inputChar: Char): Boolean =
    inputChar.code in 48..57 || inputChar.code in 65..90 || inputChar.code in 97..122