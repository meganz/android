package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.LoadChatPreviewUseCase
import javax.inject.Inject

/**
 * Join chat link
 *
 * @property joinPublicChatUseCase
 * @property checkChatLinkUseCase
 */
class JoinChatCallUseCase @Inject constructor(
    private val checkChatLinkUseCase: CheckChatLinkUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val loadChatPreviewUseCase: LoadChatPreviewUseCase,
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
        val chatRequest = checkChatLinkUseCase(chatLink)
        val chatId = chatRequest.chatHandle
        val chatPublicHandle = chatRequest.userHandle
        var exist = false
        val chatRoom = getChatRoomUseCase(chatId) ?: run {
            loadChatPreviewUseCase(chatLink).let {
                exist = it.exist
            }
            getChatRoomUseCase(chatId)
        } ?: throw throw ChatRoomDoesNotExistException()

        if (chatRoom.isPreview && isAutoJoin) {
            joinPublicChatUseCase(
                chatId = chatId,
                chatPublicHandle = chatPublicHandle,
                exist = exist
            )
        }
        return chatId
    }
}
