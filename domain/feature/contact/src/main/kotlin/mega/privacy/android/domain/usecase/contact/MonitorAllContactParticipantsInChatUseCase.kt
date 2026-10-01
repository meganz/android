package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Monitor all contact participants in chat use case
 *
 */
class MonitorAllContactParticipantsInChatUseCase @Inject constructor(
    private val repository: ContactsRepository,
    private val areAllContactParticipantsInChatUseCase: AreAllContactParticipantsInChatUseCase,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke(peerHandles: List<Long>) =
        merge(repository.monitorNewContacts().map { false }, // emit false immediately if new contact added
            repository.monitorContactRemoved()
                .filter { userIds -> userIds.any { id -> !peerHandles.contains(id) } } // if remove the user, who is not in the chat
                .map { areAllContactParticipantsInChatUseCase(peerHandles) }
        ).onStart { emit(areAllContactParticipantsInChatUseCase(peerHandles)) }
}