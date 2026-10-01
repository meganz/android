package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get user online status from user handle
 */
class GetUserOnlineStatusByHandleUseCase @Inject constructor(private val contactsRepository: ContactsRepository) {
    /**
     * Invoke
     *
     * @param userHandle user handle is the reference id to the use
     */
    suspend operator fun invoke(userHandle: Long) =
        contactsRepository.getUserOnlineStatusByHandle(userHandle)
}