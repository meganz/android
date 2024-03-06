package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.chat.message.AttachVoiceClipMessageUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import javax.inject.Inject

/**
 * Use case to forward a voice clip.
 */
class ForwardVoiceClipUseCase @Inject constructor(
    private val attachVoiceClipMessageUseCase: AttachVoiceClipMessageUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
) : ForwardMessageUseCase() {
    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val voiceClipMessage = message as? VoiceClipMessage ?: return null
        val fileNode = getChatFileUseCase(voiceClipMessage.chatId, voiceClipMessage.msgId)
            ?: return ForwardResult.ErrorNotAvailable

        return runCatching {
            attachVoiceClipMessageUseCase(
                chatId = targetChatId,
                fileNode = fileNode
            )
            ForwardResult.Success(targetChatId)
        }.getOrElse {
            if ((it as MegaException).errorCode == -9) {
                ForwardResult.ErrorNotAvailable
            } else {
                ForwardResult.GeneralError
            }
        }
    }

}