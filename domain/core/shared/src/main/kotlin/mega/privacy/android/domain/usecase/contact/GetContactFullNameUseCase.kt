package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get user full name
 *
 */
class GetContactFullNameUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    /**
     * invoke
     */
    suspend operator fun invoke(handle: Long): String =
        contactsRepository.getUserFullName(handle)
}