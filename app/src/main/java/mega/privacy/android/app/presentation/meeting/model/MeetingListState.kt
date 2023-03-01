package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Meeting list state
 *
 * @property meetings
 * @property currentCallChatId
 */
data class MeetingListState constructor(
    val meetings: List<MeetingRoomItem> = listOf(),
    val currentCallChatId: Long? = null,
)
