package mega.privacy.android.app.presentation.extensions

import com.vdurmont.emoji.EmojiParser
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.shared.contact.extension.displayName
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtils
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtilsShortcodes
import java.util.Locale

fun ShareRecipient.getAvatarFirstLetter(): String = when (this) {
    is ShareRecipient.NonContact -> {
        getAvatarFirstLetter(email)
    }

    is ShareRecipient.Contact -> {
        getAvatarFirstLetter(contactData.alias ?: contactData.fullName ?: email)
    }
}

/**
 * Retrieve the avatar first letter of a [ContactItem].
 *
 * @return The first letter of the string to be painted in the default avatar.
 */
fun ContactItem.getAvatarFirstLetter(): String =
    getAvatarFirstLetter(displayName())

/**
 * Retrieve the first letter of a String.
 *
 * @param text String to obtain the first letter.
 * @param emojify
 * @param extractEmojiList
 * @return The first letter of the string to be painted in the default avatar.
 */
fun getAvatarFirstLetter(
    text: String,
    emojify: (String) -> String? = EmojiUtilsShortcodes::emojify, //Extracted for testing
    extractEmojiList: (String) -> List<String?>? = EmojiParser::extractEmojis, //Extracted for testing
): String {
    val unknown = "U"

    if (text.isBlank()) {
        return unknown
    }

    val result = text.trim { it <= ' ' }
    if (result.length == 1) {
        return result[0].toString().uppercase(Locale.getDefault())
    }

    val resultTitle = emojify(result)
    if (resultTitle.isNullOrEmpty()) {
        return unknown
    }

    val emojis = EmojiUtils.emojis(resultTitle)

    if (emojis.isNotEmpty() && emojis[0].start == 0) {
        emojis[0].let { first ->
            return resultTitle.substring(first.start, first.end)
        }
    }

    val resultEmojiCompat = getEmojiCompatAtFirst(resultTitle, extractEmojiList)
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
 * @param extractEmojiList
 * @return The emoji if any, null otherwise.
 */
private fun getEmojiCompatAtFirst(
    text: String?,
    extractEmojiList: (String) -> List<String?>?,
): String? {
    if (text.isNullOrEmpty()) {
        return null
    }

    val listEmojis: List<String?>? = extractEmojiList(text)
    return listEmojis?.filterNotNull()?.firstOrNull()?.length?.let {
        val substring = text.substring(0, it)
        val sublistEmojis = extractEmojiList(substring)
        if (!sublistEmojis.isNullOrEmpty()) {
            substring
        } else null
    }
}

/**
 * Retrieve if a char is recognizable.
 *
 * @param inputChar The char to be examined.
 * @return True if the char is recognizable. Otherwise, false.
 */
private fun isRecognizableCharacter(inputChar: Char): Boolean =
    inputChar.code in 48..57 || inputChar.code in 65..90 || inputChar.code in 97..122