package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Meeting list state
 *
 * @property meetings
 * @property currentCallChatId
 * @property snackBar            String resource id for showing an snackBar.
 */
data class MeetingListState constructor(
    val meetings: List<MeetingRoomItem> = listOf(),
    val currentCallChatId: Long? = null,
    val snackBar: Int? = null,
)
