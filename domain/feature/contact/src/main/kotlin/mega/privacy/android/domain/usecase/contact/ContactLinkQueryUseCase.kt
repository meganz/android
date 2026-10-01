package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Contact link query use case
 *
 * @property repository
 */
class ContactLinkQueryUseCase @Inject constructor(
    private val repository: ContactsRepository,
) {
    /**
     * Invoke
     *
     * @param userHandle
     */
    suspend operator fun invoke(userHandle: Long) = repository.contactLinkQuery(userHandle)
}