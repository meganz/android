package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
            localContacts.map { localContact ->
                async {
                    filterOutExistingContactEmails(localContact)
                }
            }.awaitAll()
        }

    private suspend fun filterOutExistingContactEmails(localContact: LocalContact): LocalContact {
        val newEmailList = mutableListOf<String>().apply {
            addAll(localContact.emails)
        }
        contactsRepository.getAvailableContacts().forEach { user ->
            val filteredEmails = localContact.emails.filterNot { email ->
                isAMegaContactByEmailUseCase(user, email)
            }
            newEmailList.clear()
            newEmailList.addAll(filteredEmails)
        }

        return LocalContact(
            id = localContact.id,
            name = localContact.name,
            phoneNumbers = localContact.phoneNumbers,
            normalizedPhoneNumbers = localContact.normalizedPhoneNumbers,
            emails = newEmailList
        )
    }
}
