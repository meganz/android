package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Filter out pending or accepted local contacts by emails
 *
 * @property contactsRepository [ContactsRepository]
 * @property isContactRequestByEmailInPendingOrAcceptedStateUseCase [IsContactRequestByEmailInPendingOrAcceptedStateUseCase]
 */
class FilterPendingOrAcceptedLocalContactsByEmailUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val isContactRequestByEmailInPendingOrAcceptedStateUseCase: IsContactRequestByEmailInPendingOrAcceptedStateUseCase,
) {

    /**
     * Invocation method
     *
     * @return List of filtered local contacts
     */
    suspend operator fun invoke(localContacts: List<LocalContact>): List<LocalContact> =
        coroutineScope {
            val semaphore = Semaphore(8)
            localContacts.map { localContact ->
                async {
                    semaphore.withPermit {
                        localContact.filterOutPendingContactEmails()
                    }
                }
            }.awaitAll()
        }

    private suspend fun LocalContact.filterOutPendingContactEmails(): LocalContact {
        val outGoingContactRequest = contactsRepository.getOutgoingContactRequests()
        val filteredEmails = if (outGoingContactRequest.isEmpty()) {
            emails
        } else {
            emails.filterNot { email ->
                outGoingContactRequest.any { user ->
                    isContactRequestByEmailInPendingOrAcceptedStateUseCase(user, email)
                }
            }
        }
        return copy(emails = filteredEmails)
    }
}
