package mega.privacy.android.app.presentation.meeting.chat.model.paging

import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus

/**
 * Paging load result
 *
 * @property loadStatus
 * @property nextMessageUserHandle
 * @property nexMessageIsMine
 */
data class PagingLoadResult(
    val loadStatus: ChatHistoryLoadStatus,
    val nextMessageUserHandle: Long?,
    val nexMessageIsMine: Boolean?,
)