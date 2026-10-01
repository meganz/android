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
 * Filter local contacts by emails
 *
 * @property contactsRepository [ContactsRepository]
 * @property isAMegaContactByEmailUseCase [IsAMegaContactByEmailUseCase]
 */
class FilterLocalContactsByEmailUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val isAMegaContactByEmailUseCase: IsAMegaContactByEmailUseCase,
) {

    /**
     * Invocation method
     *
     * @param localContacts Current list of local contacts
     * @return List of filtered local contacts with emails that are not registered as MEGA contacts
     */
    suspend operator fun invoke(localContacts: List<LocalContact>): List<LocalContact> =
        coroutineScope {
            val semaphore = Semaphore(8)
            localContacts.map { localContact ->
                async {
                    semaphore.withPermit {
                        localContact.filterOutExistingContactEmails()
                    }
                }
            }.awaitAll()
        }

    private suspend fun LocalContact.filterOutExistingContactEmails(): LocalContact {
        val availableContacts = contactsRepository.getAvailableContacts()
        val filteredEmails = if (availableContacts.isEmpty()) {
            emails
        } else {
            emails.filterNot { email ->
                availableContacts.any { user ->
                    isAMegaContactByEmailUseCase(user, email)
                }
            }
        }
        return copy(emails = filteredEmails)
    }
}
