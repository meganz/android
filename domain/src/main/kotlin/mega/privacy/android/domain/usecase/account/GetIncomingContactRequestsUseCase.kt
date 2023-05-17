package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get list of incoming contact requests
 */
class GetIncomingContactRequestsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    /**
     * Get list of incoming contact requests
     * @return list of incoming contact requests
     */
    suspend operator fun invoke(): List<ContactRequest> =
        contactsRepository.getIncomingContactRequests()
}