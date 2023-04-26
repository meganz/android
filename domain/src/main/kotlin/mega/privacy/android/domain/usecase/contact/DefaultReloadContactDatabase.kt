package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

internal class DefaultReloadContactDatabase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val getUserFirstName: GetUserFirstName,
    private val getUserLastName: GetUserLastName,
    private val getCurrentUserAliases: GetCurrentUserAliases,
) : ReloadContactDatabase {
    override suspend fun invoke(isForceReload: Boolean) {
        val contacts = contactsRepository.getContactEmails()
        if (isForceReload || contacts.size != contactsRepository.getContactDatabaseSize()) {
            contactsRepository.clearContactDatabase()
            contacts.forEach { contact ->
                val firstName =
                    getUserFirstName(handle = contact.key, skipCache = true, shouldNotify = true)
                val lastName =
                    getUserLastName(handle = contact.key, skipCache = true, shouldNotify = true)
                contactsRepository.createOrUpdateContact(
                    handle = contact.key,
                    email = contact.value,
                    firstName = firstName,
                    lastName = lastName,
                    nickname = null,
                )
            }
            getCurrentUserAliases()
        }
    }
}