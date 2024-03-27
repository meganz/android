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
        val newEmailList = mutableListOf<String>().apply {
            addAll(emails)
        }
        contactsRepository.getOutgoingContactRequests().forEach { request ->
            val filteredEmails = emails.filterNot { email ->
                isContactRequestByEmailInPendingOrAcceptedStateUseCase(request, email)
            }
            newEmailList.clear()
            newEmailList.addAll(filteredEmails)
        }

        return LocalContact(
            id = id,
            name = name,
            phoneNumbers = phoneNumbers,
            normalizedPhoneNumbers = normalizedPhoneNumbers,
            emails = newEmailList
        )
    }
}
