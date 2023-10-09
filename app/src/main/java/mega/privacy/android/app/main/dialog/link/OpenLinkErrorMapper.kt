package mega.privacy.android.app.main.dialog.link

import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.exception.MegaException
import javax.inject.Inject

/**
 * Open link error mapper
 *
 */
internal class OpenLinkErrorMapper @Inject constructor() {
    @StringRes
    operator fun invoke(
        isJoinMeeting: Boolean,
        isChatScreen: Boolean,
        submittedLink: String?,
        linkType: RegexPatternType?,
        checkLinkResult: Result<ChatLinkContent>?,
    ): Int? {
        if (submittedLink != null && submittedLink.isEmpty()) {
            return when {
                isJoinMeeting -> R.string.invalid_meeting_link_empty
                isChatScreen -> R.string.invalid_chat_link_empty
                else -> R.string.invalid_file_folder_link_empty
            }
        }
        if (checkLinkResult != null && checkLinkResult.exceptionOrNull() is MegaException) {
            return if (isJoinMeeting) R.string.invalid_meeting_link_args else R.string.invalid_chat_link_args
        }
        if (linkType != null) {
            return when (linkType) {
                RegexPatternType.CHAT_LINK -> R.string.valid_chat_link
                RegexPatternType.CONTACT_LINK -> R.string.valid_contact_link
                RegexPatternType.FILE_LINK,
                RegexPatternType.FOLDER_LINK,
                RegexPatternType.PASSWORD_LINK,
                RegexPatternType.ALBUM_LINK,
                -> null

                else -> R.string.invalid_file_folder_link
            }
        }
        return null
    }
}