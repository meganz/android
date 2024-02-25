package mega.privacy.android.app.presentation.meeting.chat.model.messages

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
 */
data class UIMessageState(
    val chatTitle: String?,
    val isOneToOne: Boolean,
    val scheduledMeeting: ChatScheduledMeeting?,
    val lastUpdatedCache: Long,
    val isInSelectMode: Boolean,
    val isChecked: Boolean,
)