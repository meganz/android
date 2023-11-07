package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Invite a list of contacts to a chat room.
 *
 */
class InviteToChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Invoke
     * @param chatId      Chat room id
     * @param contactList list of contacts
     * @return list of [Result] for the addings.
     */
    suspend operator fun invoke(chatId: Long, contactList: List<String>) =
        withContext(ioDispatcher) {
            val deferredList = contactList.map { contact ->
                async {
                    runCatching {
                        chatRepository.inviteParticipantToChat(
                            chatId = chatId,
                            handle = chatRepository.getContactHandle(contact) ?: -1L
                        )
                    }
                }
            }.awaitAll()
            return@withContext deferredList
        }
}