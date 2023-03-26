package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Meeting list state
 *
 * @property meetings
 * @property selectedMeetings
 * @property currentCallChatId
 * @property snackBar            String resource id for showing an snackBar.
 * @property scrollToTop
 */
data class MeetingListState constructor(
    val meetings: List<MeetingRoomItem> = listOf(),
    val selectedMeetings: List<Long> = listOf(),
    val currentCallChatId: Long? = null,
    val snackBar: Int? = null,
    val scrollToTop: Boolean = false,
)
