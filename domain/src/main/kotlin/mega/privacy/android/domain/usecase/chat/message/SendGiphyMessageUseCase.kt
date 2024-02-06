package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for sending a location message to a chat.
 */
class SendGiphyMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id.
     * @param srcMp4 Source location of the mp4
     * @param srcWebp Source location of the webp
     * @param sizeMp4 Size in bytes of the mp4
     * @param sizeWebp Size in bytes of the webp
     * @param width Width of the giphy
     * @param height Height of the giphy
     * @param title Title of the giphy
     */
    suspend operator fun invoke(
        chatId: Long,
        srcMp4: String?,
        srcWebp: String?,
        sizeMp4: Long,
        sizeWebp: Long,
        width: Int,
        height: Int,
        title: String?,
    ) {
        val sentMessage = chatMessageRepository.sendGiphy(
            chatId = chatId,
            srcMp4 = srcMp4,
            srcWebp = srcWebp,
            sizeMp4 = sizeMp4,
            sizeWebp = sizeWebp,
            width = width,
            height = height,
            title = title
        )
        val request = createSaveSentMessageRequestUseCase(sentMessage)
        chatRepository.storeMessages(chatId, listOf(request))
    }
}