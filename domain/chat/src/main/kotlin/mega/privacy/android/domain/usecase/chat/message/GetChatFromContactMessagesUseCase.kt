package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import javax.inject.Inject

/**
 * Use case for getting the chat from messages.
 * If only one message, it will get a 1on1 conversation.
 * If more than one message, it will create a group chat room.
 */
class GetChatFromContactMessagesUseCase @Inject constructor(
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val createGroupChatRoomUseCase: CreateGroupChatRoomUseCase,
) {

    /**
     * Invoke.
     *
     * @param messages List of messages.
     * @return Chat conversation Handle.
     */
    suspend operator fun invoke(messages: List<ContactAttachmentMessage>) =
        if (messages.size == 1) {
            get1On1ChatIdUseCase(messages.first().contactHandle)
        } else {
            createGroupChatRoomUseCase(
                emails = messages.map { it.contactEmail },
                title = null,
                isEkr = false,
                addParticipants = true,
                chatLink = false
            )
        }
}