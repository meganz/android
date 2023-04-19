package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatConnectionState
import javax.inject.Inject

internal class ChatConnectionStateMapper @Inject constructor(private val chatConnectionStatusMapper: ChatConnectionStatusMapper) {

    operator fun invoke(handle: Long, state: Int) = ChatConnectionState(
        chatId = handle,
        chatConnectionStatus = chatConnectionStatusMapper(state)
    )
}