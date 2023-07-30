package mega.privacy.android.app.presentation.chat.list.model

import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem

/**
 * Chats tab state
 *
 * @property chats
 * @property meetings
 * @property selectedIds
 * @property currentChatStatus
 * @property currentCallChatId
 * @property searchQuery
 * @property snackBar
 * @property isParticipatingInChatCallResult
 * @property tooltipToBeShown
 * @constructor Create empty Chat tab state
 */
data class ChatsTabState constructor(
    val chats: List<ChatRoomItem> = emptyList(),
    val meetings: List<ChatRoomItem> = emptyList(),
    val selectedIds: List<Long> = emptyList(),
    val currentChatStatus: ChatStatus? = null,
    val currentCallChatId: Long? = null,
    val searchQuery: String? = null,
    val snackBar: SnackBarItem? = null,
    val isParticipatingInChatCallResult: Boolean? = null,
    val tooltipToBeShown: MeetingTooltipItem = MeetingTooltipItem.NONE,
)
