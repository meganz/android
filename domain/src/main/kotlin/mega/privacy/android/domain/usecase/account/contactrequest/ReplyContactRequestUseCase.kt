package mega.privacy.android.domain.usecase.account.contactrequest

import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Reply to a pending contact request by applying the given [ContactRequestAction].
 *
 * The request direction decides which repository call is made: outgoing (sent) requests are
 * managed with [ContactsRepository.manageSentContactRequest]; incoming (received) requests with
 * [ContactsRepository.manageReceivedContactRequest].
 */
class ReplyContactRequestUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke
     *
     * @param handle contact request identifier.
     * @param action action to apply to the request.
     * @param isOutgoing whether the request is outgoing (sent); `false` for an incoming (received)
     * one.
     */
    suspend operator fun invoke(
        handle: Long,
        action: ContactRequestAction,
        isOutgoing: Boolean,
    ) = if (isOutgoing) {
        contactsRepository.manageSentContactRequest(handle, action)
    } else {
        contactsRepository.manageReceivedContactRequest(handle, action)
    }
}
