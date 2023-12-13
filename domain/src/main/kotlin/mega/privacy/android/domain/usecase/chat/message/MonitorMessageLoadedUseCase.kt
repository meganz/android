package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Monitor message loaded use case
 *
 * @property chatRepository [ChatRepository]
 */
class MonitorMessageLoadedUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val createTypedMessageUseCases: Map<@JvmSuppressWildcards ChatMessageType, @JvmSuppressWildcards CreateTypedMessageUseCase>,
    private val createInvalidMessageUseCase: CreateInvalidMessageUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id.
     * @return Flow of [ChatMessage]
     */
    suspend operator fun invoke(chatId: Long): Flow<TypedMessage> {
        val myUserHandle = chatRepository.getMyUserHandle()
        return chatRepository.monitorOnMessageLoaded(chatId)
            .filterNotNull()
            .filter { message -> message.type != ChatMessageType.UNKNOWN }
            .map { message ->
                val isMine = myUserHandle == message.userHandle
                createTypedMessageUseCases[message.type]?.invoke(message, isMine)
                    ?: createInvalidMessageUseCase(message, isMine)
            }
    }
}

