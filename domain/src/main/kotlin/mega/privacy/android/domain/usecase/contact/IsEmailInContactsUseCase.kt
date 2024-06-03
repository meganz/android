package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * A use case to check if the given email exists in the visible contacts.
 *
 * @property contactsRepository The contact-related repository.
 * @property isAMegaContactByEmailUseCase A use case to determine whether the given email already exists in MEGA contacts.
 */
class IsEmailInContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val isAMegaContactByEmailUseCase: IsAMegaContactByEmailUseCase,
) {

    /**
     * Invocation method.
     *
     * @param email The email that needs to be checked.
     * @return Boolean. Whether the email exists.
     */
    suspend operator fun invoke(email: String): Boolean {
        for (visibleContact in contactsRepository.getAvailableContacts()) {
            if (isAMegaContactByEmailUseCase(visibleContact, email)) {
                return true
            }
        }
        return false
    }
}
