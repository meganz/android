package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject


/**
 * Default get chat participants use case implementation.
 */
class DefaultOpenOrStartCall @Inject constructor(
    private val callRepository: CallRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : OpenOrStartCall {

    override suspend fun invoke(chatId: Long, video: Boolean, audio: Boolean): ChatCall? =
        withContext(defaultDispatcher) {
            callRepository.getChatCall(chatId)?.let { call ->
                return@withContext call
            }

            callRepository.startCallRinging(chatId, video, audio).let { request ->
                callRepository.getChatCall(request.chatHandle)?.let { call ->
                    return@withContext call
                }
            }

            return@withContext null
        }
}