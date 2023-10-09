package mega.privacy.android.app.main.dialog.link

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatLinkContent

/**
 * Open link ui state
 *
 * @property linkType the type of the link
 * @property openContactLinkHandle the handle of the contact link to be opened
 * @property checkLinkResult the result of the check link request
 * @property submittedLink the link submitted by the user
 */
data class OpenLinkUiState(
    val submittedLink: String? = null,
    val linkType: RegexPatternType? = null,
    val openContactLinkHandle: Long = -1L,
    val checkLinkResult: Result<ChatLinkContent>? = null,
)