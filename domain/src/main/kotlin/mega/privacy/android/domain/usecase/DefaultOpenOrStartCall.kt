package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Default get chat participants use case implementation.
 */
class DefaultOpenOrStartCall @Inject constructor(
    private val callRepository: CallRepository,
    private val chatRepository: ChatRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : OpenOrStartCall {

    override suspend fun invoke(chatId: Long, video: Boolean, audio: Boolean): ChatCall? {
        withContext(defaultDispatcher) {
            runCatching {
                chatRepository.getChatCall(chatId = chatId)
            }.fold(
                onSuccess = { call ->
                    call?.let {
                        //Call already in progress
                        return@withContext it
                    }
                },
                onFailure = {
                    //Call does not yet exist
                    return@withContext callRepository.startCall(chatId, video, audio)
                }
            )
        }

        return null
    }
}