package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get contact information from user email
 */
class GetContactFromEmailUseCase @Inject constructor(private val contactsRepository: ContactsRepository) {

    /**
     * invoke
     *
     * @param email user email
     * @param skipCache If true, force read from backend, refresh cache and return.
     *                  If false, use value in cache
     * @return [ContactItem] which contains contact information of selected user
     */
    suspend operator fun invoke(email: String, skipCache: Boolean) =
        contactsRepository.getContactItemFromUserEmail(email, skipCache)
}