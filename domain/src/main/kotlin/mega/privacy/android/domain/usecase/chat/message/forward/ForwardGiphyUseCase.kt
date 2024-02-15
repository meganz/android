package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.usecase.chat.message.SendGiphyMessageUseCase
import javax.inject.Inject

/**
 * Use case to forward a Giphy message.
 */
class ForwardGiphyUseCase @Inject constructor(
    private val sendGiphyMessageUseCase: SendGiphyMessageUseCase,
) : ForwardMessageUseCase() {
    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val giphyMessage = message as? GiphyMessage ?: return null
        giphyMessage.chatGifInfo?.let {
            sendGiphyMessageUseCase(
                chatId = targetChatId,
                srcMp4 = it.mp4Src,
                srcWebp = it.webpSrc,
                sizeMp4 = it.mp4Size.toLong(),
                sizeWebp = it.webpSize.toLong(),
                width = it.width,
                height = it.height,
                title = it.title
            )
        }
        return ForwardResult.Success(targetChatId)
    }
}