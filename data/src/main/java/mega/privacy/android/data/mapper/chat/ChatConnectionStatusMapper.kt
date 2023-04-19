package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject

/**
 * ChatConnectionStateMapper
 *
 * Maps MegaChatAPI Chat Connection to [ChatConnectionStatus]
 */
internal class ChatConnectionStatusMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param state MegaChatAPI Chat Connection state
     */
    operator fun invoke(state: Int): ChatConnectionStatus = when (state) {
        MegaChatApi.CHAT_CONNECTION_OFFLINE -> ChatConnectionStatus.Offline
        MegaChatApi.CHAT_CONNECTION_IN_PROGRESS -> ChatConnectionStatus.InProgress
        MegaChatApi.CHAT_CONNECTION_LOGGING -> ChatConnectionStatus.Logging
        MegaChatApi.CHAT_CONNECTION_ONLINE -> ChatConnectionStatus.Online
        else -> ChatConnectionStatus.Unknown
    }
}