package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get contact link use case
 *
 * @property repository
 */
class GetContactLinkUseCase @Inject constructor(
    private val repository: ContactsRepository,
) {
    /**
     * Invoke
     *
     * @param userHandle
     */
    suspend operator fun invoke(userHandle: Long) = repository.getContactLink(userHandle)
}