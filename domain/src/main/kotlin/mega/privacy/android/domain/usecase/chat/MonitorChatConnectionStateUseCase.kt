package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * MonitorChatConnectionStateUseCase
 *
 * UseCase for returning ChatConnectionState
 * returns updates on onChatConnectionStateUpdate from MegaChatListenerInterface
 */
class MonitorChatConnectionStateUseCase @Inject constructor(private val contactsRepository: ContactsRepository) {

    /**
     * Invoke
     *
     * @return Flow[ChatConnectionState]
     */
    operator fun invoke() = contactsRepository.monitorChatConnectionStateUpdates()
}