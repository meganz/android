package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject

/**
 * Chat history load status mapper
 */
internal class ChatHistoryLoadStatusMapper @Inject constructor() {

    /**
     * Map chat history load status to [ChatHistoryLoadStatus]
     */
    operator fun invoke(source: Int): ChatHistoryLoadStatus = when (source) {
        MegaChatApi.SOURCE_ERROR -> ChatHistoryLoadStatus.ERROR
        MegaChatApi.SOURCE_NONE -> ChatHistoryLoadStatus.NONE
        MegaChatApi.SOURCE_LOCAL -> ChatHistoryLoadStatus.LOCAL
        MegaChatApi.SOURCE_REMOTE -> ChatHistoryLoadStatus.REMOTE
        else -> ChatHistoryLoadStatus.ERROR
    }
}