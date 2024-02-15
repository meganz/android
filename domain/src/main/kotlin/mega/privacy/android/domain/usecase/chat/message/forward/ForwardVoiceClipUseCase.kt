package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.usecase.chat.message.AttachVoiceClipMessageUseCase
import javax.inject.Inject

/**
 * Use case to forward a voice clip.
 */
class ForwardVoiceClipUseCase @Inject constructor(
    private val attachVoiceClipMessageUseCase: AttachVoiceClipMessageUseCase,
) : ForwardMessageUseCase() {
    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val voiceClipMessage = message as? VoiceClipMessage ?: return null
//        var result: ForwardResult = ForwardResult.Success(targetChatId)
//        runCatching {
//            attachVoiceClipMessageUseCase(
//                chatId = targetChatId,
//                fileNode = (message as VoiceClipMessage).fileNode
//            )
//        }.onFailure {
//            result = if ((it as MegaException).errorCode == -9) {
//                ForwardResult.ErrorNotAvailable
//            } else {
//                ForwardResult.GeneralError
//            }
//        }
//        return result
        return ForwardResult.Success(targetChatId)
    }

}