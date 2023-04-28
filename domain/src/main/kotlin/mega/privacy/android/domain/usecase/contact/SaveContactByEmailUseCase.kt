package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Save contact by email use case
 *
 */
class SaveContactByEmailUseCase @Inject constructor(
    private val getUserFirstName: GetUserFirstName,
    private val getUserLastName: GetUserLastName,
    private val contactsRepository: ContactsRepository,
) {
    /**
     * Invoke
     *
     * @param email
     */
    suspend operator fun invoke(email: String) {
        val handle = contactsRepository.getContactHandleByEmail(email)
        if (handle == -1L) throw IllegalArgumentException("Can not find contact with $email")
        val firstName =
            getUserFirstName(handle = handle, skipCache = true, shouldNotify = true)
        val lastName =
            getUserLastName(handle = handle, skipCache = true, shouldNotify = true)
        // nickname can be throw exception if not found, we can consider it as nickname = null
        val nickname = runCatching { contactsRepository.getUserAlias(handle) }.getOrNull()
        contactsRepository.createOrUpdateContact(
            handle = handle,
            email = email,
            firstName = firstName,
            lastName = lastName,
            nickname = nickname
        )
    }
}