package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.LoadChatPreviewUseCase
import javax.inject.Inject

/**
 * Join chat link
 *
 * @property loadChatPreviewUseCase
 * @property joinPublicChatUseCase
 */
class JoinChatCallUseCase @Inject constructor(
    private val loadChatPreviewUseCase: LoadChatPreviewUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
) {
    /**
     * Invoke
     *
     * @param chatLink
     * @param isAutoJoin
     * @return chatId
     */
    suspend operator fun invoke(
        chatLink: String,
        isAutoJoin: Boolean = true,
    ): Long {
        val chatRequest = loadChatPreviewUseCase(chatLink).request
        val chatId = chatRequest.chatHandle
        val chatPublicHandle = chatRequest.userHandle

        joinPublicChatUseCase(
            chatId = chatId,
            chatPublicHandle = chatPublicHandle,
            autoJoin = isAutoJoin
        )
        return chatId
    }
}
