package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import javax.inject.Inject

/**
 * Default get chat participants use case implementation.
 */
class DefaultGetChatParticipants @Inject constructor(
    private val chatParticipantsRepository: ChatParticipantsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetChatParticipants {

    override fun invoke(chatId: Long): Flow<List<ChatParticipant>> = flow {
        emit(getAllParticipants(chatId))
    }.flowOn(defaultDispatcher)

    private suspend fun getAllParticipants(chatId: Long): List<ChatParticipant> {
        return chatParticipantsRepository.getAllChatParticipants(chatId).toMutableList()
    }
}