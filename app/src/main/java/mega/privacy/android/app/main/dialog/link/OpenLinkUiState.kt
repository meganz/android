package mega.privacy.android.app.main.dialog.link

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatLinkContent

/**
 * Open link ui state
 *
 * @property linkType the type of the link
 * @property openContactLinkHandle the handle of the contact link to be opened
 * @property checkLinkResult the result of the check link request
 * @property submittedLink the link submitted by the user
 * @property joinMeetingEvent fires when the UI should join a meeting via [ChatLinkContent.MeetingLink]
 * @property openChatEvent fires when the UI should open a chat via [ChatLinkContent.ChatLink]
 * @property meetingEndedEvent fires when the UI should show the meeting-ended dialog
 * @property dismissEvent fires when the dialog should be dismissed after the link has been handled internally
 */
data class OpenLinkUiState(
    val submittedLink: String? = null,
    val linkType: RegexPatternType? = null,
    val openContactLinkHandle: Long = -1L,
    val checkLinkResult: Result<ChatLinkContent>? = null,
    val joinMeetingEvent: StateEventWithContent<ChatLinkContent.MeetingLink> = consumed(),
    val openChatEvent: StateEventWithContent<ChatLinkContent.ChatLink> = consumed(),
    val dismissEvent: StateEvent = consumed,
)
