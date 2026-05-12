package mega.privacy.android.shared.contact.mapper

import androidx.compose.ui.graphics.Color
import com.vdurmont.emoji.EmojiParser
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtils
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtilsShortcodes
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a domain [ContactItem] to the presentational [ContactItemUiState] consumed
 * by `ContactItemView` in `:feature:contact:contact-snowflake-components`.
 *
 * The status text is taken from a caller-supplied parameter so the mapper stays
 * free of Android `Context` and string-resource resolution. Callers resolve the
 * subtitle (e.g. "Online", "Last seen today at HH:mm", a permission label) and
 * pass it in.
 *
 * @property contactItemStatusMapper
 */
class ContactItemUiStateMapper @Inject constructor(
    private val contactItemStatusMapper: ContactItemStatusMapper
) {

    /**
     * @param contactItem Domain contact.
     */
    operator fun invoke(
        contactItem: ContactItem,
    ): ContactItemUiState = ContactItemUiState(
        handle = contactItem.handle,
        displayName = resolveDisplayName(contactItem),
        status = contactItemStatusMapper(contactItem.status),
        lastSeen = contactItem.lastSeen,
        avatar = mapAvatar(contactItem),
        isVerified = contactItem.areCredentialsVerified,
    )

    private fun resolveDisplayName(contactItem: ContactItem): String {
        val alias = contactItem.contactData.alias
        val fullName = contactItem.contactData.fullName
        return when {
            !alias.isNullOrBlank() -> alias
            !fullName.isNullOrBlank() -> fullName
            else -> contactItem.email
        }
    }

    private fun mapAvatar(contactItem: ContactItem): AvatarData {
        val avatarUri = contactItem.contactData.avatarUri
        return if (avatarUri != null) {
            AvatarData.Image(file = File(avatarUri))
        } else {
            AvatarData.Initials(
                initials = contactItem.getAvatarFirstLetter(),
                avatarColor = parseHexColor(contactItem.defaultAvatarColor),
            )
        }
    }

    private fun ContactItem.getAvatarFirstLetter(): String =
        getAvatarFirstLetter(
            contactData.alias ?: contactData.fullName ?: email
        )

    private fun getAvatarFirstLetter(text: String): String {
        val unknown = "U"

        if (text.isBlank()) {
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

        if (emojis.isNotEmpty() && emojis[0].start == 0) {
            emojis[0].let { first ->
                return resultTitle.substring(first.start, first.end)
            }
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

    private fun isRecognizableCharacter(inputChar: Char): Boolean =
        inputChar.code in 48..57 || inputChar.code in 65..90 || inputChar.code in 97..122

    private fun parseHexColor(hex: String?): Color {
        if (hex.isNullOrBlank()) return Color.Black
        val clean = hex.removePrefix("#")
        return runCatching {
            when (clean.length) {
                6 -> Color((0xFF000000.toInt()) or clean.toInt(16))
                8 -> Color(clean.toLong(16).toInt())
                else -> Color.Black
            }
        }.getOrDefault(Color.Black)
    }

}
