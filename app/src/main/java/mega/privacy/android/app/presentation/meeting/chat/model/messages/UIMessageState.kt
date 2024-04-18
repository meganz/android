package mega.privacy.android.app.presentation.meeting.chat.model.messages

import mega.privacy.android.app.presentation.meeting.chat.view.LastItemAvatarPosition
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

/**
 * UI message state
 *
 * @property chatTitle
 * @property isOneToOne
 * @property scheduledMeeting
 * @property lastUpdatedCache
 * @property isInSelectMode
 * @property isChecked
 * @property lastItemAvatarPosition the position for the avatar in case it's the last visible message in the list and needs to bedrawn, null otherwise.
 */
data class UIMessageState(
    val chatTitle: String?,
    val isOneToOne: Boolean,
    val scheduledMeeting: ChatScheduledMeeting?,
    val lastUpdatedCache: Long,
    val isInSelectMode: Boolean,
    val isChecked: Boolean,
    val lastItemAvatarPosition: LastItemAvatarPosition?,
)