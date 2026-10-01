package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Checks if the credentials of a given user are verified.
 */
class AreCredentialsVerifiedUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke.
     *
     * @param userEmail User's email.
     * @return True if credentials are verified, false otherwise.
     */
    suspend operator fun invoke(userEmail: String) =
        contactsRepository.areCredentialsVerified(userEmail)

}
