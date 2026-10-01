package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the contact's username from the database
 *
 * @property contactsRepository [ContactsRepository]
 */
class GetContactUserNameFromDatabaseUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invocation function
     *
     * @param user The user, which can be potentially nullable
     * @return The username from the database, which can be potentially nullable
     */
    suspend operator fun invoke(user: String?): String? =
        contactsRepository.getContactUserNameFromDatabase(user)
}