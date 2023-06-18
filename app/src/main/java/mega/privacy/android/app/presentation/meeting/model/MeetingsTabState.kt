package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatItem

/**
 * Meetings tab state
 *
 * @property meetings
 * @property selectedIds
 * @property currentCallChatId
 * @property snackBar
 * @property scrollToTop
 * @constructor Create empty Meetings tab state
 */
data class MeetingsTabState constructor(
    val meetings: List<ChatItem.MeetingChatItem> = listOf(),
    val selectedIds: List<Long> = listOf(),
    val currentCallChatId: Long? = null,
    val snackBar: Int? = null,
    val scrollToTop: Boolean = false,
)
