package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Is Contact Request Sent
 *
 */
class IsContactRequestSentUseCase @Inject constructor(
    private val repository: ContactsRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(email: String) = repository.isContactRequestSent(email)
}