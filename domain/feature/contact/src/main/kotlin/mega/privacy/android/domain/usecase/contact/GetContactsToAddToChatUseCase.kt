package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import javax.inject.Inject

/**
 * Use case that returns the visible MEGA contacts that can still be added to a chat, i.e. the
 * contacts that are not already participants of the given chat. Backs the "add chat participants"
 * picker, which must not offer contacts that already belong to the chat.
 *
 * @property getContactsUseCase
 * @property getChatRoomUseCase
 */
class GetContactsToAddToChatUseCase @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId the chat the selected contacts will be added to.
     * @return a [Flow] emitting the contacts that are not already participants of [chatId].
     */
    operator fun invoke(chatId: Long): Flow<List<ContactItem>> =
        getContactsUseCase().map { contacts ->
            val participantHandles = getChatRoomUseCase(chatId)?.peerHandlesList?.toSet().orEmpty()
            contacts.filterNot { it.handle in participantHandles }
        }
}
